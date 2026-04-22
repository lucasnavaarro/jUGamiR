import { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Client } from '@stomp/stompjs';
import JuegoEnCurso from '../components/JuegoEnCurso';
import LobbyEspera from '../components/LobbyEspera';

export default function Lobby() {

    const { idPartida } = useParams();
    const [lobby, setLobby] = useState(null);
    const [error, setError] = useState('');

    useEffect(() => {
        const jwt = localStorage.getItem('jwt');

        if (!jwt) {
            setError('Debes iniciar sesión para acceder al lobby');
            return;
        }

        const client = new Client({
            webSocketFactory: () => new WebSocket('/ws'), //Crea conexión apuntando a la ruta /ws del backend
            connectHeaders: { Authorization: `Bearer ${jwt}` }, //Envía el token para autenticarse
            onConnect: () => {
                //Suscribirse al canal de esta partida
                client.subscribe(`/topic/lobby/${idPartida}`, //ruta de escucha
                    (message) => {
                        const data = JSON.parse(message.body); //Saca el texto y lo convierte a objeto JS
                        setLobby(data); //metemos los nuevos datos en la memoria
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

    if (lobby.estado === 'EN_CURSO') {
        return <JuegoEnCurso lobby={lobby} />;
    }

    return <LobbyEspera lobby={lobby} />;

}
