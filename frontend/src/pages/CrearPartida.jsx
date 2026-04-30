import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { apiFetch } from "../services/api";
import CrearPartidaForm from "../components/CrearPartidaForm";

export default function CrearPartida() {
    const navigate = useNavigate();
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);

    async function handleCrearPartida(formData) {
        setIsLoading(true);
        setError('');

        try {
            const res = await apiFetch('/api/lobby/crear', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData), //convierte el la información del formulario en un string JSON para enviarlo al backend
            });

            if (!res || !res.ok) throw new Error();
            const data = await res.json(); //convierte el string JSON en un objeto JavaScript
            navigate(`/partida/${data.idPartida}`); //redirige a la partida creada

        } catch {
            setError('Error al crear la partida. Inténtalo de nuevo');

        } finally {
            setIsLoading(false);
        }
    }

    return (
        <main className="crear-partida">
            <div className="crear-partida__container">
                <Link to="/jugador" className="crear-partida__back"> Volver</Link>
                <h1 className="crear-partida__title">¡Configura tu partida!</h1>
                
                <CrearPartidaForm 
                    onSubmit={handleCrearPartida} 
                    isLoading={isLoading} 
                    serverError={error} 
                />
            </div>
        </main>
    );
}