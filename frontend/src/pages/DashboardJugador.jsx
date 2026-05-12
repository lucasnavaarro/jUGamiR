import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';


export default function DashboardJugador() {
    const nick = localStorage.getItem('nick') || localStorage.getItem('email') || 'Jugador';
    const navigate = useNavigate();

    const acciones = [
        {
            id: 'unirse',
            icon: '🎮',
            titulo: 'Unirse a una partida',
            descripcion: 'Únete a una partida pública o introduce un código para unirte a una privada.',
            boton: 'Buscar partida',
            color: 'var(--color-primary)',
            onClick: () => navigate('/unirse/partida/privada'),
        },
        {
            id: 'crear-partida',
            icon: '🎮',
            titulo: 'Crear partida',
            descripcion: 'Crea una nueva partida pública o privada e invita a tus estudiantes a jugar.',
            boton: 'Crear partida',
            color: 'var(--color-primary)',
            onClick: () => navigate('/crear/partida'),
        },
        {
            id: 'estadisticas',
            icon: '📊',
            titulo: 'Ver estadísticas',
            descripcion: 'Consulta tus estadísticas y progreso.',
            boton: 'Ver estadísticas',
            color: 'var(--color-accent)',
            onClick: () => navigate('/stats'),
        },
    ];

    return (
        <DashboardLayout
            nombreUsuario={nick}
            subtitulo="¿Listo para demostrar tus conocimientos? Elige una opción para empezar."
            acciones={acciones}
        />
    );
}
