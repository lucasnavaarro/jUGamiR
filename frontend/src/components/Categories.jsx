const categorias = [
    { icon: '🧠', name: 'Sentidos y Metabolismo', info: 'Digestivo · Cirugía Endocrina · ORL · Oftalmología' },
    { icon: '❤️', name: 'Cardio, Sangre y Piel', info: 'Cardiología · Dermatología · Hematología · Inmunología · Cirugía Vascular' },
    { icon: '🦴', name: 'Riñón, Infección y Autoinmunidad', info: 'Nefrología · Enfermedades Infecciosas · Urología · Reumatología Y Enfermedades Sistémicas' },
    { icon: '👶', name: 'Pediatría, Primaria y Reproducción', info: 'Obstetricia · Ginecología · Pediatría I · Pediatría II · Atención Primaria' },
    { icon: '🫁', name: 'Cuerpo y Mente', info: 'Respiratorio · Psiquiatría · Traumatología · Neurología Y Neurocirugía' },
    { icon: '🧬', name: 'Ciencias Básicas', info: 'Anatomía · Bioquímica Y Bioquímica Médica · Fisiología · Farmacología · Biología Molecular' },
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
