/**
 * api.js — Módulo de comunicación con el backend jUGamiR
 * Todas las llamadas al servidor se hacen desde aquí.
 */

const API_BASE = '/api';

/**
 * Comprueba si el backend está disponible.
 * @returns {Promise<string>} Mensaje del servidor o null si hay error.
 */
export async function healthCheck() {
    try {
        const res = await fetch('/actuator/health', { signal: AbortSignal.timeout(5000) });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const data = await res.json();
        return data.status === 'UP' ? 'Backend online' : null;
    } catch {
        return null;
    }
}

// ── Próximas llamadas ──────────────────────────────────────────────────────
// export async function login(email, password) { ... }
// export async function getPartidas() { ... }
// export async function getPregunta(id) { ... }
