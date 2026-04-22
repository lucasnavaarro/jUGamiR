import { Navigate } from 'react-router-dom';

export default function ProtectedRoute({ rolRequerido, children }) {

    const jwt = localStorage.getItem('jwt');
    const rol = localStorage.getItem('rol');

    if (!jwt) return <Navigate to="/login" replace />;
    if (rolRequerido && rol !== rolRequerido) {
        const ruta = rol === 'JUGADOR' ? '/jugador' : '/profesor';
        return <Navigate to={ruta} replace />;
    }

    return children;
}