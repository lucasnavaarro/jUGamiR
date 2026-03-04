const categorias = [
    { icon: '🧠', name: 'Metabolismo y Mente', info: 'Digestivo · Cirugía Endocrina · Psiquiatría' },
    { icon: '❤️', name: 'Cardio y Sangre', info: 'Cardiología · Hematología · Vascular' },
    { icon: '🧠', name: 'Neuro, Renal e Infecciosas', info: 'Urología · Nefrología · Infecciosas · Neurología' },
    { icon: '🫁', name: 'Respiratorio y Reproducción', info: 'Obstetricia · Ginecología · Respiratorio' },
    { icon: '🦴', name: 'Locomotor, Piel y Defensas', info: 'Reumatología · Traumatología · Dermatología · Inmunología' },
    { icon: '👶', name: 'Pediatría y Primaria', info: 'Pediatría I · Pediatría II · Atención Primaria' },
];

export default function Categories() {
    return (
        <section className="categories" id="categories" aria-label="Categorías del juego">
            <div className="container">
                <div className="section-header">
                    <h2>Elige tu especialidad</h2>
                    <p>Cada partida pone a prueba tus conocimientos en estas 6 categorías</p>
                </div>

                <div className="categories__grid">
                    {categorias.map((cat) => (
                        <article className="cat-card" tabIndex={0} aria-label={`Categoría ${cat.name}`} key={cat.name}>
                            <div className="cat-card__icon">{cat.icon}</div>
                            <h3 className="cat-card__name">{cat.name}</h3>
                            <p className="cat-card__info">{cat.info}</p>
                            <div className="cat-card__bar"></div>
                        </article>
                    ))}
                </div>
            </div>
        </section>
    );
}
