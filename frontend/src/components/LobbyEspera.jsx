import JugadorLobby from './JugadorLobby';
import CodigoEnLobby from './CodigoEnLobby';
export default function LobbyEspera({ lobby }) {
    return (
        <main>
            <section className="lobby__hero">
                <div className="container lobby__hero-inner">
                    <p className="lobby__label">Partida creada por</p>
                    <h1 className="lobby__title">
                        <span className="lobby__host">{lobby.anfitrion}</span>
                    </h1>
                </div>
            </section>
            <section className="lobby__code-section">
                <div className="container">
                    <p className="lobby__code-label">Código de la partida</p>
                    <CodigoEnLobby codigo={lobby.codigo} />
                </div>
            </section>
            <section className="lobby__players-section">
                <div className="container">
                    <p className="lobby__players-label">
                        Jugadores ({lobby.jugadores.length}/{lobby.maxJugadores})
                    </p>
                    <div className="lobby__players-grid">
                        {Array.from({ length: lobby.maxJugadores }).map((_, i) => (
                            <JugadorLobby key={i} jugador={lobby.jugadores[i]} />
                        ))}
                    </div>
                </div>
            </section>
        </main>
    );
}