const pasos = [
    { titulo: 'Crea o únete', descripcion: 'Crea una sala y comparte el código con tus compañeros o únete con el código de otro.' },
    { titulo: 'Elige categoría', descripcion: 'Cada quesito del tablero corresponde a una especialidad médica del MIR.' },
    { titulo: 'Responde rápido', descripcion: 'Tienes tiempo limitado por pregunta, así que no te duermas o perderás tu turno!' },
    { titulo: '¡Gana la partida!', descripcion: 'El primero en conseguir los 6 quesitos gana la partida.' },
];

export default function HowToPlay() {
    return (
        <section className="how-to" aria-label="Cómo se juega">
            <div className="container">
                <div className="section-header">
                    <h2>¿Cómo se juega?</h2>
                </div>

                <div className="steps">
                    {pasos.map((paso) => (
                        <div className="step" key={paso.titulo}>
                            <h3>{paso.titulo}</h3>
                            <p>{paso.descripcion}</p>
                        </div>
                    ))}
                </div>
            </div>
        </section>
    );
}
