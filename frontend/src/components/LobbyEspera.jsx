import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../services/api';
import JugadorLobby from './JugadorLobby';
import CodigoEnLobby from './CodigoEnLobby';
export default function LobbyEspera({ lobby, onAbandonar }) {

    const navigate = useNavigate();

    const esAnfitrion = lobby.idAnfitrion === parseInt(localStorage.getItem('idUsuario'), 10);

    async function abandonarPartida() {
        try {
            if (onAbandonar) onAbandonar(); // Marca que estamos abandonando voluntariamente y asi no se muestra el modal de expulsado
            const res = await apiFetch(`/api/lobby/abandonar/${lobby.idPartida}`, { method: 'DELETE' });

            if (!res.ok) throw new Error();
            navigate('/jugador');

        } catch (error) {
            alert('Error al abandonar la partida: ');
        }
    }

    async function empezarPartida() {
        try {
            const res = await apiFetch(`/api/lobby/iniciar/${lobby.idPartida}`, { method: 'POST' });
            if (!res.ok) throw new Error();
            // El backend mandará un mensaje por WebSocket que cambiará a JuegoEnCurso automáticamente
        } catch {
            alert('Error al iniciar la partida');
        }
    }

    async function expulsarJugador(idJugador) {

        try {
            const res = await apiFetch(`/api/lobby/expulsar/${lobby.idPartida}/${idJugador}`, { method: 'DELETE' });

            if (!res.ok) throw new Error();

        } catch (error) {
            alert('Error al expulsar al jugador');
        }
    }

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
                            <JugadorLobby
                                key={i}
                                jugador={lobby.jugadores[i]}
                                esAnfitrion={i === 0}
                                soyAnfitrion={esAnfitrion}
                                onExpulsar={() => lobby.jugadores[i] ? expulsarJugador(lobby.jugadores[i].idJugador) : null}
                            />
                        ))}
                    </div>

                    <div className="lobby_buttons">
                        <button className="btn btn--danger" onClick={abandonarPartida}>
                            Abandonar
                        </button>
                        {esAnfitrion && (
                            <button className="btn btn--primary" onClick={empezarPartida} disabled={lobby.jugadores.length < lobby.maxJugadores}>
                                Empezar partida
                            </button>
                        )}
                    </div>
                </div>
            </section>
        </main>
    );
}