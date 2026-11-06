import test from 'node:test';
import assert from 'node:assert/strict';
import {spawnSync} from 'node:child_process';
import {mkdtempSync, readFileSync, rmSync} from 'node:fs';
import {tmpdir} from 'node:os';
import {join} from 'node:path';

test('a failed QA case fails the process while preserving later observations', () => {
    const directory = mkdtempSync(join(tmpdir(), 'jugamir-runner-'));
    try {
        const moduleUrl = new URL('../common.mjs', import.meta.url).href;
        const code = `import {caseTest} from ${JSON.stringify(moduleUrl)};
            await caseTest('QA-01','expected failure',()=>{throw new Error('controlled failure')});
            await caseTest('QA-02','following observation',()=>{});`;
        const result = spawnSync(process.execPath, ['--input-type=module', '-e', code], {
            env: {...process.env, QA_OUT: directory, QA_CASES: '', QA_SKIP_CASES: ''}, encoding: 'utf8',
        });
        assert.equal(result.status, 1, result.stdout + result.stderr);
        const records = readFileSync(join(directory, 'observaciones.ndjson'), 'utf8').trim().split('\n').map(JSON.parse);
        assert.deepEqual(records.map(record => record.result), ['FALLO', 'OK']);
    } finally {
        rmSync(directory, {recursive: true, force: true});
    }
});

test('QA endpoint overrides cannot target a public application', () => {
    const directory = mkdtempSync(join(tmpdir(), 'jugamir-endpoint-'));
    try {
        const moduleUrl = new URL('../common.mjs', import.meta.url).href;
        const result = spawnSync(process.execPath, ['--input-type=module', '-e', `await import(${JSON.stringify(moduleUrl)})`], {
            env: {...process.env, QA_OUT: directory, QA_BASE: 'https://jugamir.es'}, encoding: 'utf8',
        });
        assert.equal(result.status, 1);
        assert.match(result.stderr, /QA may only target loopback endpoints/);
    } finally {
        rmSync(directory, {recursive: true, force: true});
    }
});
