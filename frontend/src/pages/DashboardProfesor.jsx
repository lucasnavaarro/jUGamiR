import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';


export default function DashboardProfesor() {
    const nombre = localStorage.getItem('nombre') || 'Profesor';
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
            id: 'preguntas',
            icon: '➕',
            titulo: 'Añadir preguntas',
            descripcion: 'Contribuye al banco de preguntas del MIR añadiendo nuevas cuestiones para los jugadores.',
            boton: 'Gestionar preguntas',
            color: 'var(--cat-5)',
            onClick: () => navigate('/preguntas'),
        },
        {
            id: 'crear-partida',
            icon: '🎮',
            titulo: 'Crear partida',
            descripcion: 'Crea una nueva partida pública o privada e invita a tus estudiantes a jugar.',
            boton: 'Crear partida',
            color: 'var(--color-primary)',
            onClick: () => navigate('/crear-partida'),
        },
        {
            id: 'clasificacion',
            icon: '🏆',
            titulo: 'Ver clasificación',
            descripcion: 'Consulta el ranking global y el rendimiento de tus estudiantes en las partidas.',
            boton: 'Ver ranking',
            color: 'var(--color-accent)',
            onClick: () => navigate('/clasificacion'),
        },
    ];

    return (
        <DashboardLayout
            nombreUsuario={nombre}
            subtitulo="Gestiona el contenido y crea partidas para que tus estudiantes practiquen el MIR."
            acciones={acciones}
            gridClass="dashboard__grid--3"
        />
    );
}
