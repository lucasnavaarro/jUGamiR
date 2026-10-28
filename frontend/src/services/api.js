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


export async function apiFetch(url, options = {}) {

    const jwt = localStorage.getItem("jwt");

    const res = await fetch(url, {
        ...options,
        headers: {
            ...options.headers,
            Authorization: `Bearer ${jwt}`
        },
    });

    if (res.status === 401) {

        const token = await refreshAccessToken();
        if (token) {
            // Reintentamos la petición original con el nuevo token
            return fetch(url, {
                ...options,
                headers: {
                    ...options.headers,
                    Authorization: `Bearer ${token}`
                },
            });

        } else {
            // Si el refresh falla, cerramos sesión
            localStorage.clear();
            window.dispatchEvent(new CustomEvent('session-expired'));
            // Devolvemos una promesa que nunca resuelve: el event listener
            // redirigirá al usuario antes de que el caller intente leer la respuesta
            return new Promise(() => {});
        }

    }

    return res;


}
let pendingRefresh;
export function refreshAccessToken() {
    if (!pendingRefresh) pendingRefresh = fetch(API_BASE + '/auth/refresh', { method: 'POST' })
        .then(async response => {
            if (!response.ok) return null;
            const { token } = await response.json();
            localStorage.setItem('jwt', token);
            return token;
        }).finally(() => { pendingRefresh = null; });
    return pendingRefresh;
}
