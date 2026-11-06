import { useState, useRef, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { apiFetch } from "../services/api";
import CrearPartidaForm from "../components/CrearPartidaForm";

export default function CrearPartida() {
    const navigate = useNavigate();
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const mounted = useRef(true);
    const sending = useRef(false);
    useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);

    async function handleCrearPartida(formData) {
        if (sending.current) return;
        sending.current = true;
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
            if (mounted.current) navigate(`/partida/${data.idPartida}`); //redirige a la partida creada

        } catch {
            if (mounted.current) setError('Error al crear la partida. Inténtalo de nuevo');

        } finally {
            sending.current = false;
            if (mounted.current) setIsLoading(false);
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