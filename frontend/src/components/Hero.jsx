export default function Hero() {
    return (
        <section className="hero" aria-label="Presentación">
            <div className="container hero__inner">


                <h1 className="hero__title">
                    El trivial <br />
                    <span className="highlight">que te prepara</span><br />
                    para el MIR
                </h1>

                <p className="hero__subtitle">
                    ¡Compite con tus amigos en el clásico juego del trivial respondiendo preguntas reales del MIR y demuestra quién sabe más.
                </p>

                <div className="hero__cta">
                    <a href="#" className="btn btn--primary btn--lg" id="cta-jugar">
                        🎯 Empezar a jugar
                    </a>
                    <a href="#categories" className="btn btn--outline btn--lg2">
                        Ver categorías
                    </a>
                </div>
            </div>
        </section>
    );
}
