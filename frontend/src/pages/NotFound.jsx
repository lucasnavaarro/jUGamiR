import { Link } from 'react-router-dom';

export default function NotFound() {

    const jwt = localStorage.getItem('jwt');
    const rol = localStorage.getItem('rol');

    //Ver a dónde redirige el botón
    let destino, textoBoton;
    if (!jwt) {
        destino = '/';
        textoBoton = "Volver al inicio";

    } else if (rol == 'JUGADOR') {
        destino = '/jugador';
        textoBoton = "Ir a mi dashboard";

    } else {
        destino = '/profesor';
        textoBoton = "Ir a mi dashboard";
    }

    return (
        <main className="login-page">
            <div className="auth-card">
                <div className="auth-card__header">
                    <div style={{ fontSize: 'clamp(3rem, 15vw, 6rem)', marginBottom: '1rem' }}>🔍</div>
                    <h1 className="auth-card__title">404 - Página no encontrada</h1>
                    <p style={{ color: 'var(--text-secondary)', marginTop: '0.5rem' }}>
                        La dirección que buscas no existe o ha sido movida.
                    </p>
                </div>
                <Link to={destino} className="btn btn--primary btn--full" style={{ marginTop: '1.5rem', justifyContent: 'center' }}>
                    {textoBoton}
                </Link>
            </div>
        </main>
    );
}