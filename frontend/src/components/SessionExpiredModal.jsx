export default function SessionExpiredModal({ onAceptar }) {
    return (
        <div className="session-modal-overlay">
            <div className="session-modal">
                <h2 className="session-modal__title">Sesión cerrada</h2>
                <p className="session-modal__text">Tu sesión ha expirado o se ha cerrado en otro dispositivo. Por favor, inicia sesión de nuevo para continuar</p>
                <button className="btn btn--secondary btn--full" onClick={onAceptar}>Volver al inicio</button>
            </div>
        </div>
    );
}