import StatusBadge from './StatusBadge';
import { Link, useLocation } from 'react-router-dom';

export default function Navbar() {

    const { pathname } = useLocation();
    const isAuthPage = pathname === '/login'
        || pathname === '/register'
        || pathname === '/forgot-password'
        || pathname === '/reset-password'
        || pathname === '/password-changed'
        || pathname === '/register-confirmation';

    return (
        <header className="navbar">
            <div className="container navbar__inner">
                <Link to="/" className="navbar__logo" aria-label="Inicio jUGamiR">
                    <div className="navbar__logo-icon">⚕️</div>
                    <span>
                        <span className="navbar__brand-j">j</span>
                        <span className="navbar__brand-u">U</span>
                        <span className="navbar__brand-g">G</span>
                        <span className="navbar__brand-am">am</span>
                        <span className="navbar__brand-i">i</span>
                        <span className="navbar__brand-r">R</span>
                    </span>
                </Link>

                {!isAuthPage && (
                    <>
                        <StatusBadge />
                        <nav className="navbar__actions">
                            <Link to="/login" className="btn btn--outline" id="btn-login">Iniciar sesión</Link>
                            <Link to="/register" className="btn btn--primary" id="btn-play">¡Jugar!</Link>
                        </nav>
                    </>
                )}
            </div>
        </header>
    );
}
