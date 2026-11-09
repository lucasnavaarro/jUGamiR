import test from 'node:test';
import assert from 'node:assert/strict';
import { setImmediate } from 'node:timers';
import { apiFetch, healthCheck, refreshAccessToken } from './api.js';

function setup(t) {
    const originalStorage = globalThis.localStorage;
    const values = new Map([['jwt', 'old-jwt']]);
    globalThis.localStorage = {
        getItem: key => values.get(key) ?? null,
        setItem: (key, value) => values.set(key, value),
        clear: () => values.clear(),
    };
    t.after(() => {
        if (originalStorage === undefined) delete globalThis.localStorage;
        else globalThis.localStorage = originalStorage;
    });
    return values;
}

test('NAV-01 health check recognizes UP and rejects DOWN', async t => {
    t.mock.method(globalThis, 'fetch', async () => new Response('{"status":"UP"}'));
    assert.equal(await healthCheck(), 'Backend online');
    globalThis.fetch.mock.mockImplementation(async () => new Response('{"status":"DOWN"}'));
    assert.equal(await healthCheck(), null);
});

test('UI-06 health check tolerates network failure', async t => {
    t.mock.method(globalThis, 'fetch', async () => { throw new TypeError('Network unavailable'); });
    assert.equal(await healthCheck(), null);
});

test('SES-02 authenticated requests carry JWT and preserve request body', async t => {
    setup(t);
    t.mock.method(globalThis, 'fetch', async (url, options) => {
        assert.equal(url, '/api/juego/1/responder');
        assert.equal(options.headers.Authorization, 'Bearer old-jwt');
        assert.equal(options.headers['Content-Type'], 'application/json');
        assert.equal(options.method, 'POST');
        assert.equal(options.body, '{"respuestaId":2,"preguntaPartidaId":3}');
        return new Response(null, { status: 200 });
    });
    assert.equal((await apiFetch('/api/juego/1/responder', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: '{"respuestaId":2,"preguntaPartidaId":3}',
    })).status, 200);
});

test('SES-02 expired JWT refreshes and retries the original write with new token', async t => {
    const values = setup(t);
    const calls = [];
    t.mock.method(globalThis, 'fetch', async (url, options) => {
        calls.push({ url, options });
        if (url === '/api/auth/refresh') return new Response('{"token":"new-jwt"}');
        return new Response(null, { status: options.headers.Authorization === 'Bearer old-jwt' ? 401 : 200 });
    });
    const result = await apiFetch('/api/usuario/perfil', { method: 'PUT', body: '{"nombre":"QA"}' });
    assert.equal(result.status, 200);
    assert.equal(calls.length, 3);
    assert.equal(calls[1].url, '/api/auth/refresh');
    assert.equal(calls[1].options.method, 'POST');
    assert.equal(calls[2].options.method, 'PUT');
    assert.equal(calls[2].options.body, '{"nombre":"QA"}');
    assert.equal(calls[2].options.headers.Authorization, 'Bearer new-jwt');
    assert.equal(values.get('jwt'), 'new-jwt');
});

test('SES-03 concurrent unauthorized requests share one pending refresh', async t => {
    setup(t);
    let refreshCount = 0;
    let release;
    const gate = new Promise(resolve => { release = resolve; });
    t.mock.method(globalThis, 'fetch', async (url, options) => {
        if (url === '/api/auth/refresh') {
            refreshCount++;
            await gate;
            return new Response('{"token":"new-jwt"}');
        }
        return new Response(null, { status: options.headers.Authorization === 'Bearer old-jwt' ? 401 : 200 });
    });
    const requests = ['/api/usuario/perfil', '/api/stats/jugador', '/api/auth/me'].map(url => apiFetch(url));
    // Flush the initial 401 continuations while the refresh response remains pending.
    await new Promise(resolve => setImmediate(resolve));
    assert.equal(refreshCount, 1);
    release();
    assert.deepEqual((await Promise.all(requests)).map(response => response.status), [200, 200, 200]);
});

test('API-01 forbidden response does not trigger refresh or automatic retry', async t => {
    setup(t);
    t.mock.method(globalThis, 'fetch', async () => new Response(null, { status: 403 }));
    assert.equal((await apiFetch('/api/profesor/asignaturas')).status, 403);
    assert.equal(globalThis.fetch.mock.callCount(), 1);
});


test('SES-08 late refresh cannot restore credentials after local logout', async t => {
    const values=setup(t);let release;
    const gate=new Promise(resolve=>{release=resolve;});
    t.mock.method(globalThis,'fetch',async()=>{await gate;return new Response('{"token":"late-jwt"}');});
    const refreshing=refreshAccessToken();values.clear();release();
    assert.equal(await refreshing,null);assert.equal(values.has('jwt'),false);
});

test('SES-08 late refresh never replaces a newer login', async t => {
    const values = setup(t); let release;
    const gate = new Promise(resolve => { release = resolve; });
    t.mock.method(globalThis, 'fetch', async () => { await gate; return new Response('{"token":"previous-account-jwt"}'); });
    const refreshing = refreshAccessToken();
    values.set('jwt', 'new-account-jwt'); release();
    assert.equal(await refreshing, null);
    assert.equal(values.get('jwt'), 'new-account-jwt');
});

test('SES-08 old unauthorized write is not retried under a newer login', async t => {
    const values = setup(t); let release;
    const gate = new Promise(resolve => { release = resolve; });
    t.mock.method(globalThis, 'fetch', async url => {
        if (url === '/api/auth/refresh') return new Response('{"token":"refreshed-other-account"}');
        await gate; return new Response(null, { status: 401 });
    });
    const request = apiFetch('/api/usuario/perfil', { method: 'PUT', body: '{"nombre":"Previous account"}' });
    values.set('jwt', 'new-account-jwt'); release();
    assert.equal((await request).status, 401);
    assert.equal(globalThis.fetch.mock.callCount(), 1);
    assert.equal(values.get('jwt'), 'new-account-jwt');
});
