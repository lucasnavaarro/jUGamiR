import ModalDialog from './ModalDialog';
export default function SessionExpiredModal({ onAceptar }) {
    return (
        <ModalDialog title="Sesión cerrada">
                <p className="session-modal__text">Tu sesión ha expirado o se ha cerrado en otro dispositivo. Por favor, inicia sesión de nuevo para continuar</p>
                <button className="btn btn--secondary btn--full" onClick={onAceptar}>Volver al inicio</button>
        </ModalDialog>
    );
}
