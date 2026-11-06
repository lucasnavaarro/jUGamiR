export default function PreguntaCard({ enunciado, focusRef }) {
    return (
        <div className="pregunta-card">
            <p ref={focusRef} tabIndex={-1} className="pregunta-card__texto">{enunciado}</p>
        </div>
    );
}
