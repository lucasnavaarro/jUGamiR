export default function JugadorLobby({ jugador, esAnfitrion, soyAnfitrion, onExpulsar }) {
    return (
        <div className={`lobby__player ${jugador ? 'lobby__player--active' : 'lobby__player-empty'}`}>

            {/* Corona dibujada con la clase limpia del CSS */}
            {esAnfitrion && jugador && (
                <div className="lobby__player-crown">👑</div>
            )}

            <div className="lobby__player-avatar">
                {jugador ? jugador.nick[0].toUpperCase() : '?'}
            </div>
            <span className="lobby__player-nick">
                {jugador ? jugador.nick : 'Esperando jugador...'}
            </span>

            {soyAnfitrion && jugador && !esAnfitrion && (
                <button
                    className="btn btn--danger lobby__player-expulsar"
                    onClick={onExpulsar}
                    title={`Expulsar a ${jugador.nick}`}
                >
                    Expulsar
                </button>
            )}
        </div>
    );
}
