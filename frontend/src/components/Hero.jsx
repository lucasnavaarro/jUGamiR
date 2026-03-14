import { Link } from 'react-router-dom';


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
                    <Link to="/register" className="btn btn--primary btn--lg" id="cta-jugar">
                        🎯 Empezar a jugar
                    </Link>
                    <Link to="/login" className="btn btn--outline btn--lg2">
                        Iniciar Sesión
                    </Link>
                </div>
            </div>
        </section>
    );
}
