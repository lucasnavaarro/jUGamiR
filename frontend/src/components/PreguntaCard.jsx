export default function PreguntaCard({ enunciado }) {
    return (
        <div className="pregunta-card">
            <p className="pregunta-card__texto">{enunciado}</p>
        </div>
    );
}