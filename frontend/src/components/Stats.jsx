const stats = [
    { number: '5.000+', value: 'Preguntas MIR reales' },
    { number: '6', value: 'Categorías de preguntas' },
    { number: '30+', value: 'Años de exámenes' },
];

export default function Stats() {
    return (
        <section className="stats" aria-label="Estadísticas">
            <div className="container">
                <div className="stats__grid">
                    {stats.map((s) => (
                        <div key={s.value}>
                            <div className="stat__number">{s.number}</div>
                            <div className="stat__label">{s.value}</div>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}
