import { useState, useEffect, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
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
        //Cargar datos iniciales usando fetch directo
        apiFetch(`/api/lobby/${idPartida}`)
            .then(res => {
                if (!res.ok) throw new Error('Error al cargar el lobby');
                return res.json(); //devuelve los datos en formato JSON
            })
            .then(data => setLobby(data))
            .catch(err => setError(err.message));
        /*CONSTRUIR LA URL CON EL PROTOCOLO CORRECTO BASADO EN EL ENTORNO*/
        //Si estamos en desarrollo, usamos localhost
        //Si estamos en producción, usamos el protocolo correcto (https o wss)
        const wsUrl = import.meta.env.DEV
            ? 'ws://localhost:8080/ws'
            : `${window.location.protocol === 'https:' ? 'wss' : 'ws'}://${window.location.host}/ws`;
        /******************************************************************/
        const client = new Client({
            webSocketFactory: () => new WebSocket(wsUrl), //Crea conexión apuntando a la ruta /ws del backend
            connectHeaders: { Authorization: `Bearer ${jwt}` }, //Envía el token para autenticarse
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
                            client.deactivate(); //Cierra la conexión al ser expulsado
                        }
                    });
            },
        });

        client.activate();

        //Limpiar al salir de la página
        return () => {
            client.deactivate(); //Cierra la conexión para no dejar procesos abiertos
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

    if (lobby.estado === 'EN_CURSO') {
        return <JuegoEnCurso lobby={lobby} />;
    }

    return <LobbyEspera lobby={lobby} onAbandonar={() => { abandonandoRef.current = true; }} />;

}
