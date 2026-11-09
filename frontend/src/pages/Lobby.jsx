import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { authenticatedClient } from '../services/socket';
import { apiFetch } from '../services/api';
import JuegoEnCurso from '../components/JuegoEnCurso';
import LobbyEspera from '../components/LobbyEspera';
import ExpulsadoModal from '../components/ExpulsadoModal';

export default function Lobby() {

    const navigate = useNavigate();
    const { idPartida } = useParams();
    const [lobby, setLobby] = useState(null);
    const [error, setError] = useState('');
    const [expulsado, setExpulsado] = useState(false);
    const abandonandoRef = useRef(false);

    useEffect(() => {
        const jwt = localStorage.getItem('jwt');

        if (!jwt) {
            setError('Debes iniciar sesión para acceder al lobby');
            return;
        }
        let active = true;
        const loadLobby = () => apiFetch(`/api/lobby/${idPartida}`)
            .then(res => {
                if (res.status === 403) { if (active && !abandonandoRef.current) setExpulsado(true); return null; }
                if (!res.ok) throw new Error('Error al cargar el lobby');
                return res.json();
            })
            .then(data => {
                if (active && data) {
                    setError('');
                    setLobby(prev => prev && prev.estado !== 'ESPERANDO' && data.estado === 'ESPERANDO' ? prev : data);
                }
            })
            .catch(err => { if (active) setError(err.message); });
        loadLobby();
        // REST reconciles missed events, including starting before subscription is ready.
        const poll = setInterval(loadLobby, 5000);
        const client = authenticatedClient({
            accessUrl: `/api/lobby/${idPartida}`,
            onAccessLost: () => setExpulsado(true),
            onConnect: () => {
                //Suscribirse al canal de esta partida
                client.subscribe(`/topic/lobby/${idPartida}`, //ruta de escucha
                    (message) => {
                        const data = JSON.parse(message.body); //Saca el texto y lo convierte a objeto JS
                        setLobby(data); //metemos los nuevos datos en la memoria

                        //Detecta si han echado al usuario
                        const miIdUsuario = parseInt(localStorage.getItem('idUsuario'), 10);
                        const soyAnfitrion = data.idAnfitrion === miIdUsuario;

                        //Comprueba si el usuario está en la lista de jugadores
                        const estaEnLobby = data.jugadores.some(j => j.idJugador === miIdUsuario);

                        //Si ya no está en el lobby, lo echa
                        if (!abandonandoRef.current && !soyAnfitrion && !estaEnLobby) {
                            setExpulsado(true);
                            client.stop(); //Cierra la conexión al ser expulsado
                        }
                    });
                loadLobby();
            },
        });

        client.activate();

        //Limpiar al salir de la página
        return () => {
            active = false;
            clearInterval(poll);
            client.stop(); //Cierra la conexión para no dejar procesos abiertos
        }
    }, [idPartida]); //Se ejecuta cada vez que cambia el idPartida


    if (error) {
        return (
            <main className="login-page">
                <p className="lobby__error">{error}</p>
            </main>
        );
    }

    if (!lobby) {
        return (
            <main className="login-page">
                <p className="lobby__loading">Cargando lobby...</p>
            </main>
        );
    }

    if (expulsado) {
        return (
            <main>
                <ExpulsadoModal onAceptar={() => navigate('/jugador')} />
                <LobbyEspera lobby={lobby} /> {/* Se renderiza de fondo para que no quede la pantalla en blanco */}
            </main>
        )

    }

    if (lobby.estado === 'EN_CURSO' || lobby.estado === 'TERMINADA') {
        return <JuegoEnCurso lobby={lobby} />;
    }

    return <LobbyEspera lobby={lobby} onAbandonar={() => { abandonandoRef.current = true; }} />;

}
