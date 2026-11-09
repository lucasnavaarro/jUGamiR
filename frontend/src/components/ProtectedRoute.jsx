import { Navigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { apiFetch } from '../services/api';

export default function ProtectedRoute({ rolRequerido, children }) {

    const jwt = localStorage.getItem('jwt');
    const rol = localStorage.getItem('rol');
    const [validado, setValidado] = useState(false);
    const [error, setError] = useState('');
    const [attempt, setAttempt] = useState(0);

    useEffect(() => {
        if (!jwt) return;
        let active = true;
        setError('');
        // Valida que el JWT siga siendo válido en el backend
        // Si fue invalidado (logout en otro dispositivo), devuelve 401 y apiFetch limpia la sesión
        apiFetch('/api/auth/me')
            .then(res => {
                if (!active) return;
                if (res && res.ok) setValidado(true);
                else setError('No se pudo validar la sesión. Inténtalo de nuevo');
            })
            .catch(() => {
                if (active) setError('No se pudo conectar para validar la sesión. Inténtalo de nuevo');
            });
        return () => { active = false; };
    }, [attempt, jwt]);

    if (!jwt) return <Navigate to="/" replace />;
    if (rolRequerido && rol !== rolRequerido) {
        const ruta = rol === 'JUGADOR' ? '/jugador' : '/profesor';
        return <Navigate to={ruta} replace />;
    }

    // Mientras se verifica con el backend, no renderiza nada (evita flash de contenido)
    if (!validado) return <main className="login-page" aria-busy={!error}>
        {error ? <div role="alert"><p>{error}</p><button className="btn btn--secondary" onClick={() => setAttempt(v => v + 1)}>Reintentar</button></div>
            : <p>Cargando sesión…</p>}
    </main>;

    return children;
}
