import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Link } from 'react-router-dom';
import { Client } from '@stomp/stompjs';


export default function PartidasPublicas() {

    const [partidas, setPartidas] = useState([]);
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {

        const jwt = localStorage.getItem('jwt');

        fetch('api/lobby/publicas', {
            headers: { Authorization: `Bearer ${jwt}` }
        })
            .then(res => { if (!res.ok) throw new Error(); return res.json(); })
            .then(data => setPartidas(data))
            .catch(() => setError("No se pudieron cargar las partidas públicas"))
            .finally(() => setIsLoading(false));
    }, []);

    //Segundo useEffect para actualizar la lista cuando se cree una partida publica nueva
    useEffect(() => {
        const jwt = localStorage.getItem('jwt');
        const client = new Client({
            webSocketFactory: () => new WebSocket('/ws'),
            connectHeaders: { Authorization: `Bearer ${jwt}` },
            onConnect: () => {
                client.subscribe('/topic/partidas-publicas', (message) => {
                    const data = JSON.parse(message.body);
                    setPartidas(data);
                });
            },
        });
        client.activate();
        return () => client.deactivate();
    }, []);

    function entrar(idPartida) {
        const jwt = localStorage.getItem('jwt');
        fetch(`api/lobby/unirse/publica/${idPartida}`, {
            method: 'POST',
            headers: { Authorization: `Bearer ${jwt}` }
        })
            .then(res => { if (!res.ok) throw new Error(); })
            .then(() => navigate(`/partida/${idPartida}`)) //Redirige al lobby creado
            .catch(() => setError("No se pudo unir a la partida"));
    }

    if (isLoading) return (
        <main className="login-page">
            <p className="lobby__loading">Cargando partidas públicas...</p>
        </main>
    );


    return (
        <main>
            <section className="partidas-publicas__hero">
                <div className="container">
                    <Link to="/unirse/partida/privada" className="join-card__back">
                        Volver
                    </Link>
                    <h1 className="partidas-publicas__title">Partidas públicas</h1>
                    <p className="partidas-publicas__subtitle">Únete a una partida pública</p>
                </div>
            </section>

            <section className="partidas-publicas__list">
                <div className="container">
                    {error && <p className="lobby__error">{error}</p>}
                    {partidas.length === 0 && !error && (
                        <p className="partidas-publicas__empty">No hay partidas públicas disponibles ahora mismo</p>
                    )}

                    {partidas.length > 0 && (
                        <>
                            <div className="partidas-publicas__header">
                                <span>Partida</span>
                                <span>Creador</span>
                                <span>Jugadores</span>
                                <span></span>

                            </div>
                            {partidas.map(p => (
                                <div key={p.id} className="partidas-publicas__row">
                                    <span className="partidas-publicas__nombre partidas-publicas__cell">Partida de {p.anfitrion}</span>
                                    <span className="partidas-publicas__email partidas-publicas__cell">{p.emailCreador}</span>
                                    <span className="partidas-publicas__count partidas-publicas__cell">{p.jugadoresActuales}/{p.maxJugadores}</span>
                                    <button
                                        className="btn btn--primary partidas-publicas__cell"
                                        onClick={() => entrar(p.id)}
                                        disabled={p.jugadoresActuales >= p.maxJugadores}
                                    >
                                        {p.jugadoresActuales >= p.maxJugadores ? 'Llena' : 'Entrar'}
                                    </button>
                                </div>
                            ))}
                        </>
                    )}
                </div>
            </section>
        </main>
    )



}