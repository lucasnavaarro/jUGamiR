import { useNavigate } from 'react-router-dom';
import DashboardLayout from '../components/DashboardLayout';


export default function DashboardProfesor() {
    const nombre = localStorage.getItem('nombre') || 'Profesor';
    const navigate = useNavigate();

    const acciones = [
        {
            id: 'importar-preguntas',
            icon: '📥',
            titulo: 'Importar preguntas',
            descripcion: 'Añade nuevas preguntas al banco subiendo un archivo CSV.',
            boton: 'Importar CSV',
            color: 'var(--cat-5)',
            onClick: () => navigate('/profesor/preguntas/importar'),
        },
        {
            id: 'editar-preguntas',
            icon: '✏️',
            titulo: 'Editar preguntas',
            descripcion: 'Busca, edita o elimina preguntas del banco de preguntas.',
            boton: 'Gestionar',
            color: 'var(--color-primary)',
            onClick: () => navigate('/profesor/preguntas/editar'),
        },
        {
            id: 'asignaturas',
            icon: '📚',
            titulo: 'Gestionar asignaturas',
            descripcion: 'Añade, edita o elimina las asignaturas del banco de preguntas.',
            boton: 'Gestionar',
            color: 'var(--color-accent)',
            onClick: () => navigate('/profesor/asignaturas'),
        },

    ];

    return (
        <DashboardLayout
            nombreUsuario={nombre}
            subtitulo="Gestiona preguntas y asignaturas para que tus estudiantes practiquen el MIR."
            acciones={acciones}
            gridClass="dashboard__grid--3"
        />
    );
}
