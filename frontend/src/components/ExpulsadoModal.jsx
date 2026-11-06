import ModalDialog from './ModalDialog';
export default function ExpulsadoModal({ onAceptar }) {
    return (
        <ModalDialog title="Has sido expulsado">
                <p className="session-modal__text">El anfitrión de la partida te ha expulsado</p>
                <button className="btn btn--secondary btn--full" onClick={onAceptar}>Volver al panel</button>
        </ModalDialog>
    );
}
