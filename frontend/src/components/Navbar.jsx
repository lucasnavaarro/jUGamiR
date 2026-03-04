import StatusBadge from './StatusBadge';

export default function Navbar() {
    return (
        <header className="navbar">
            <div className="container navbar__inner">
                <a href="#" className="navbar__logo" aria-label="Inicio jUGamiR">
                    <div className="navbar__logo-icon">⚕️</div>
                    <span>
                        <span className="navbar__brand-j">j</span>
                        <span className="navbar__brand-u">U</span>
                        <span className="navbar__brand-g">G</span>
                        <span className="navbar__brand-am">am</span>
                        <span className="navbar__brand-i">i</span>
                        <span className="navbar__brand-r">R</span>
                    </span>
                </a>

                <nav className="navbar__actions">
                    <StatusBadge />
                    <a href="#" className="btn btn--outline" id="btn-login">Iniciar sesión</a>
                    <a href="#" className="btn btn--primary" id="btn-play">¡Jugar!</a>
                </nav>
            </div>
        </header>
    );
}
