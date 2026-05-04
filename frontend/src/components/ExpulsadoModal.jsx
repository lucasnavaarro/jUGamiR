export default function ExpulsadoModal({ onAceptar }) {
    return (
        <div className="session-modal-overlay">
            <div className="session-modal">
                <h2 className="session-modal__title">Has sido expulsado</h2>
                <p className="session-modal__text">El anfitrión de la partida te ha expulsado</p>
                <button className="btn btn--secondary btn--full" onClick={onAceptar}>Volver al panel</button>
            </div>
        </div>
    );
}