// Read-only smoke check, suitable for an external monitor after deployment.
import {performance} from 'node:perf_hooks';

const started = performance.now();
try {
    if (!process.argv[2]) throw new Error('Usage: node scripts/qa/public-health.mjs https://your-domain/');
    const origin = new URL(process.argv[2]);
    if (origin.username || origin.password) throw new Error('Credentials are not accepted in the URL');
    if (origin.protocol !== 'https:' && !(origin.protocol === 'http:' && ['localhost', '127.0.0.1', '[::1]'].includes(origin.hostname))) {
        throw new Error('Use HTTPS for a public server');
    }
    const url = new URL('/actuator/health', origin);
    const response = await fetch(url, {signal: AbortSignal.timeout(10000), redirect: 'error'});
    if (response.status !== 200) throw new Error(`Health HTTP ${response.status}`);
    const mediaType = (response.headers.get('content-type') || '').split(';', 1)[0].trim().toLowerCase();
    if (mediaType !== 'application/json' && !mediaType.endsWith('+json')) throw new Error('Health must return JSON');
    const data = await response.json();
    if (data.status !== 'UP') throw new Error('Health is not UP');
    console.log(JSON.stringify({ok: true, origin: origin.origin, durationMs: Math.round(performance.now() - started)}));
} catch (error) {
    console.error(JSON.stringify({ok: false, error: error.message, durationMs: Math.round(performance.now() - started)}));
    process.exitCode = 1;
}
