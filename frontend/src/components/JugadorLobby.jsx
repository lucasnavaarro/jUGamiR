export default function JugadorLobby({ jugador }) {
    return (
        <div className={`lobby__player ${jugador ? 'lobby__player--active' : 'lobby__player-empty'}`}>
            <div className="lobby__player-avatar">
                {jugador ? jugador.nick[0].toUpperCase() : '?'}
            </div>
            <span className="lobby__player-nick">
                {jugador ? jugador.nick : 'Esperando jugador...'}
            </span>
        </div>
    );
}