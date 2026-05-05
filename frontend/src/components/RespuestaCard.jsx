export default function RespuestaCard({ letra, texto, onClick, deshabilitada, estado }) {

    const colores = { A: '#22C55E', B: '#3B82F6', C: '#EAB308', D: '#F97316' };

    return (
        <button
            className={`respuesta-card respuesta-card--${estado}`}
            onClick={onClick}
            disabled={deshabilitada}
            style={{ '--color-letra': colores[letra] }}
        >
            <span className="respuesta-card__letra">{letra}</span>
            <span className="respuesta-card__texto">{texto}</span>
        </button>
    );
}
