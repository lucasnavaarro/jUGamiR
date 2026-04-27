import { Navigate, useNavigate } from 'react-router-dom';
import { useEffect, useState } from 'react';
import { apiFetch } from '../services/api';

export default function ProtectedRoute({ rolRequerido, children }) {

    const jwt = localStorage.getItem('jwt');
    const rol = localStorage.getItem('rol');
    const navigate = useNavigate();
    const [validado, setValidado] = useState(false);

    useEffect(() => {
        if (!jwt) return;
        // Valida que el JWT siga siendo válido en el backend
        // Si fue invalidado (logout en otro dispositivo), devuelve 401 y apiFetch limpia la sesión
        apiFetch('/api/auth/me')
            .then(res => {
                if (res && res.ok) setValidado(true);
            })
            .catch(() => {
                localStorage.removeItem('jwt');
                localStorage.removeItem('rol');
                navigate('/', { replace: true });
            });
    }, []);

    if (!jwt) return <Navigate to="/" replace />;
    if (rolRequerido && rol !== rolRequerido) {
        const ruta = rol === 'JUGADOR' ? '/jugador' : '/profesor';
        return <Navigate to={ruta} replace />;
    }

    // Mientras se verifica con el backend, no renderiza nada (evita flash de contenido)
    if (!validado) return null;

    return children;
}