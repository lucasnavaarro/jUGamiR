export default function ScoreboardJugador({ nick, quesitos, progreso = [], categorias, esTurno, abandonado = false }) {

    return (
        <div className={`scoreboard__jugador ${esTurno ? 'scoreboard__jugador--turno' : ''} ${abandonado ? 'scoreboard__jugador--abandonado' : ''}`}>
            <span className="scoreboard__nick">{nick}</span>
            {abandonado
                ? <span className="scoreboard__abandonado-texto">Abandonado</span>
                : (
                    <div className="scoreboard__quesitos">
                        {categorias.map(cat => {
                            const ganado = quesitos.some(q => q.categoriaId === cat.id);
                            const aciertos = progreso.find(p => p.categoriaId === cat.id)?.aciertos ?? 0;
                            return (
                                <span
                                    key={cat.id}
                                    className={`scoreboard__quesito ${ganado ? '' : 'scoreboard__quesito--vacio'}`}
                                    style={{
                                        backgroundColor: ganado ? cat.color : undefined,
                                        borderColor: cat.color
                                    }}
                                    title={`${cat.nombre}: ${ganado ? '✓' : `${aciertos}/5`}`}
                                >
                                    {!ganado && aciertos}
                                </span>
                            );
                        })}
                    </div>
                )
            }
        </div>
    );
}