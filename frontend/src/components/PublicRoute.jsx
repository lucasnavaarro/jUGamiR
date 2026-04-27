import { Navigate } from 'react-router-dom';

export default function PublicRoute({ children }) {

    const jwt = localStorage.getItem('jwt');
    const rol = localStorage.getItem('rol');

    if (jwt) {
        return <Navigate to={rol === 'JUGADOR' ? '/jugador' : '/profesor'} replace />;
    }

    return children;
}