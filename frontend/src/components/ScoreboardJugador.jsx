export default function ScoreboardJugador({ nick, quesitos, categorias, esTurno }) {

    return (
        <div className={`scoreboard__jugador ${esTurno ? 'scoreboard__jugador--turno' : ''}`}>
            <span className="scoreboard__nick">{nick}</span>
            <div className="scoreboard__quesitos">
                {categorias.map(cat => {
                    const ganado = quesitos.some(q => q.categoriaId === cat.id);

                    return (
                        <span
                            key={cat.id}
                            className={`scoreboard__quesito ${ganado ? '' : 'scoreboard__quesito--vacio'}`}
                            style={{
                                backgroundColor: ganado ? cat.color : undefined
                            }}
                            title={cat.nombre}
                        />
                    );
                })}
            </div>
        </div>
    );
}