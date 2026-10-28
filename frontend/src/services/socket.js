import { Client } from '@stomp/stompjs';
import { apiFetch, refreshAccessToken } from './api';

// All connections use the same origin, a current JWT and fresh membership checks.
export function authenticatedClient({ onConnect, accessUrl, onAccessLost }) {
    let stopped = false;
    let checking = false;
    const client = new Client({
        brokerURL: `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`,
        reconnectDelay: 3000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        beforeConnect: async () => {
            const response = await apiFetch(accessUrl || '/api/auth/me');
            if (stopped) return;
            if (!response.ok) {
                stop();
                onAccessLost?.();
                return;
            }
            client.connectHeaders = { Authorization: `Bearer ${localStorage.getItem('jwt')}` };
        },
        onConnect,
    });
    const timer = setInterval(async () => {
        if (checking || stopped) return;
        checking = true;
        try {
            const token = localStorage.getItem('jwt');
            let expiry = 0;
            try { expiry = JSON.parse(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/'))).exp * 1000; } catch { /* refresh invalid token */ }
            if (expiry < Date.now() + 60000) await refreshAccessToken();
            const response = await apiFetch(accessUrl || '/api/auth/me');
            if (!response.ok) { stop(); onAccessLost?.(); return; }
            if (!stopped && token !== localStorage.getItem('jwt')) client.forceDisconnect();
        } catch { /* Retry on the next interval after a temporary network failure. */ }
        finally { checking = false; }
    }, 30000);
    function stop() {
        stopped = true;
        clearInterval(timer);
        return client.deactivate();
    }
    client.stop = stop;
    return client;
}
