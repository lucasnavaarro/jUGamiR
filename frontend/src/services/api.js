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

        const refreshRes = await fetch(API_BASE + "/auth/refresh", {
            method: "POST",
        });

        if (refreshRes.ok) {
            const { token } = await refreshRes.json();
            localStorage.setItem("jwt", token);

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
            localStorage.removeItem("jwt");
            localStorage.removeItem("rol");
            window.location.href = "/";
        }

    }

    return res;


}