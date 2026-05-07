export default function RespuestaCard({ letra, texto, onClick, deshabilitada, estado }) {

    const colores = { A: '#14B8A6', B: '#3B82F6', C: '#EAB308', D: '#F97316', E: '#A855F7' };

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
