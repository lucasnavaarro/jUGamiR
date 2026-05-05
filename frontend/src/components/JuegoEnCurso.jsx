import { useState, useEffect, useRef } from 'react';
import { Client } from '@stomp/stompjs';
import { apiFetch } from '../services/api';
import ScoreboardJugador from './ScoreboardJugador';
import Ruleta from './Ruleta';
import Temporizador from './Temporizador';
import PreguntaCard from './PreguntaCard';
import RespuestaCard from './RespuestaCard';


export default function JuegoEnCurso({ lobby }) {

    const [estadoJuego, setEstadoJuego] = useState(null);
    const [fase, setFase] = useState('ESPERANDO_TIRADA');
    const [categoriaActual, setCategoriaActual] = useState(null);
    const [preguntaActual, setPreguntaActual] = useState(null);
    const [respuestas, setRespuestas] = useState([]);
    const [respuestaElegida, setRespuestaElegida] = useState(null);
    const [resultado, setResultado] = useState(null);
    const [error, setError] = useState('');

    const miIdUsuario = parseInt(localStorage.getItem('idUsuario'), 10);
    const tiempoInicioRef = useRef(null);

    //Cargar estado inicial + conectar WebSocket

    useEffect(() => {

        const jwt = localStorage.getItem('jwt');

        if (!jwt) {
            setError('Debes iniciar sesión para acceder al lobby');
            return;
        }

        apiFetch(`/api/juego/${lobby.idPartida}/estado`)
            .then(res => res.json())
            .then(data => setEstadoJuego(data))
            .catch(err => console.error('Error cargando estado:', err));

        /*CONSTRUIR LA URL CON EL PROTOCOLO CORRECTO BASADO EN EL ENTORNO*/
        //Si estamos en desarrollo, usamos localhost
        //Si estamos en producción, usamos el protocolo correcto (https o wss)
        const wsUrl = import.meta.env.DEV
            ? 'ws://localhost:8080/ws'
            : `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`;
        /******************************************************************/
        const client = new Client({
            webSocketFactory: () => new WebSocket(wsUrl),
            connectHeaders: {
                Authorization: `Bearer ${jwt}`
            },
            onConnect: () => {
                client.subscribe(`/topic/juego/${lobby.idPartida}`, (message) => {
                    const data = JSON.parse(message.body);
                    handleEvent(data);
                });
            },
        });

        client.activate();
        return () => client.deactivate();

    }, [lobby.idPartida]);

    // Manejar los eventos WebSocket
    function handleEvent(data) {
        switch (data.evento) {

            case 'PREGUNTA':
                setCategoriaActual(data.categoria);
                setPreguntaActual(data.pregunta);
                setRespuestas(data.respuestas);
                setRespuestaElegida(null);
                setResultado(null);
                setFase('ESPERANDO_ RESPUESTA');
                setTimeout(() => {
                    setFase('MOSTRANDO_PREGUNTA');
                    tiempoInicioRef.current = Date.now();
                }, 3000);
                break;

            case 'RESULTADO':
                setResultado(data);
                setFase('MOSTRANDO_RESULTADO');
                setEstadoJuego(prev => ({
                    ...prev,
                    turnoActual: data.turnoActual,
                    estado: data.estado,
                    jugadores: prev.jugadores.map(j =>
                        j.idJugador === data.jugadorId
                            ? { ...j, quesitos: data.quesitos }
                            : j
                    )
                }));
                if (data.estado === 'TERMINADA') {
                    setTimeout(() => setFase('PARTIDA_TERMINADA'), 3000);
                } else {
                    setTimeout(() => {
                        setFase('ESPERANDO_TIRADA');
                        setPreguntaActual(null);
                        setRespuestas([]);
                        setResultado(null);
                    }, 3000);
                }

                break;

            case 'TURNO_PASADO':
                setEstadoJuego(prev => ({
                    ...prev,
                    turnoActual: data.turnoActual
                }));
                setFase('ESPERANDO_TIRADA');
                setPreguntaActual(null);
                setRespuestas([]);
                setResultado(null);
                break;
        }
    }

    async function tirarRuleta() {
        await apiFetch(`/api/juego/${lobby.idPartida}/girar`, { method: 'POST' });
    }

    async function responder(respuestaId) {

        if (respuestaElegida) return;
        setRespuestaElegida(respuestaId);

        const tiempoMs = Date.now() - tiempoInicioRef.current;
        await apiFetch(`/api/juego/${lobby.idPartida}/responder`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                respuestaId,
                tiempoMs,
            }),
        });
    }

    async function onTiempoAgotado() {

        if (esMiTurno) {
            await apiFetch(`/api/juego/${lobby.idPartida}/pasarse`, { method: 'POST' });
        }
    }

    async function abandonarPartida() {

        if (!confirm('¿Seguro que quieres abandonar la partida?')) return;

        await apiFetch(`/api/lobby/abandonar/${lobby.idPartida}`, { method: 'DELETE' });
        navigate('/jugador');
    }

    if (!estadoJuego) {
        return <p className="juego__loading">Cargando juego...</p>;
    }

    const miJugador = estadoJuego.jugadores.find(j => j.idJugador === miIdUsuario);
    const esMiTurno = miJugador && miJugador.ordenTurno === estadoJuego.turnoActual;
    const jugadorDelTurno = estadoJuego.jugadores.find(j => j.ordenTurno === estadoJuego.turnoActual);

    if (fase === 'PARTIDA_TERMINADA') {

        const ganador = estadoJuego.jugadores.find(j => j.quesitos.length >= estadoJuego.categorias.length);
        const heGanado = ganador?.idJugador === miIdUsuario;

        return (
            <main className="juego juego--fin">
                <h1 className="juego__fin--titulo">
                    {heGanado ? '¡Has ganado!' : 'La partida ha finalizado'}
                </h1>
                <p className="juego__fin--mensaje">
                    {heGanado
                        ? '¡Enhorabuena! Has conseguido todos los quesitos.'
                        : `${ganador?.nick} ha ganado la partida.`
                    }
                </p>
                {/* Se muestra el marcador final */}
                <section className="juego__scoreboard">
                    {estadoJuego.jugadores.map(j => (
                        <ScoreboardJugador
                            key={j.idJugador}
                            nick={j.nick}
                            quesitos={j.quesitos}
                            categorias={estadoJuego.categorias}
                            esTurno={false}
                        />
                    ))}
                </section>
                <button
                    className="btn btn--primary btn--lg"
                    onClick={() => navigate('/jugador')}
                >
                    Volver al inicio
                </button>
            </main>
        );
    }

    return (
        <main className="juego">
            {/* Marcadores de jugadores */}
            <section className="juego__scoreboard">
                {estadoJuego.jugadores.map(j => (
                    <ScoreboardJugador
                        key={j.idJugador}
                        nick={j.nick}
                        quesitos={j.quesitos}
                        categorias={estadoJuego.categorias}
                        esTurno={j.ordenTurno === estadoJuego.turnoActual}
                    />
                ))}
            </section>

            <section className="juego__centro">

                {/* Ruleta */}
                <Ruleta
                    categorias={estadoJuego.categorias}
                    categoriaSeleccionada={categoriaActual}
                    girando={fase === 'GIRANDO_RULETA'}
                />

                {/* Temporizador */}
                {fase === 'MOSTRANDO_PREGUNTA' && (
                    <Temporizador
                        segundos={estadoJuego.tiempoRespuesta}
                        onFin={onTiempoAgotado}
                    />
                )}

                {/* Botón Girar */}
                {fase === 'ESPERANDO_TIRADA' && esMiTurno && (
                    <button className="btn btn--primary btn--lg" onClick={tirarRuleta}>
                        Tirar Ruleta
                    </button>
                )}

                {/* Mensaje espera — para los demás */}
                {fase === 'ESPERANDO_TIRADA' && !esMiTurno && (
                    <p className="juego__espera">
                        Esperando a que {jugadorDelTurno?.nick} tire...
                    </p>
                )}

                {/* Pregunta */}
                {preguntaActual && fase !== 'GIRANDO_RULETA' && (
                    <PreguntaCard
                        enunciado={preguntaActual.enunciado}
                    />
                )}

                {/* Respuestas */}
                {respuestas.length > 0 && fase !== 'GIRANDO_RULETA' && (
                    <div className="juego__respuestas">
                        {respuestas.map((r, i) => (
                            <RespuestaCard
                                key={r.id}
                                letra={['A', 'B', 'C', 'D'][i]}
                                texto={r.texto}
                                onClick={() => responder(r.id)}
                                deshabilitada={!esMiTurno || respuestaElegida !== null || fase === 'MOSTRANDO_RESULTADO'}
                                estado={
                                    resultado
                                        ? r.id === resultado.respuestaCorrectaId
                                            ? "correcta"
                                            : r.id === respuestaElegida
                                                ? "incorrecta"
                                                : 'normal'
                                        : r.id === respuestaElegida
                                            ? "seleccionada"
                                            : "normal"
                                }
                            />
                        ))}
                    </div>
                )}
                <button className="btn btn--danger" onClick={abandonarPartida}>
                    Abandonar partida
                </button>
            </section>
        </main>
    );
}
