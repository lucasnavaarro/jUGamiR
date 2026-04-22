export default function ActionCard({ id, icon, titulo, descripcion, boton, color, onClick }) {
    return (
        <div
            className="action-card"
            style={{ '--action-color': color }}
        >
            <div className="action-card__icon">{icon}</div>
            <h3 className="action-card__title">{titulo}</h3>
            <p className="action-card__desc">{descripcion}</p>
            <button
                className="btn btn--secondary action-card__btn"
                onClick={onClick}
                id={`btn-${id}`}
            >
                {boton}
            </button>
            <div className="action-card__bar" />
        </div>
    );
}
