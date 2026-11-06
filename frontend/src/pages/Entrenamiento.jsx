import { useState, useRef, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { apiFetch } from "../services/api";
import EntrenamientoForm from "../components/EntrenamientoForm";

export default function Entrenamiento() {
    const navigate = useNavigate();
    const [isLoading, setIsLoading] = useState(false);
    const mounted = useRef(true);
    const sending = useRef(false);
    useEffect(() => { mounted.current = true; return () => { mounted.current = false; }; }, []);
    const [error, setError] = useState('');

    async function handleCrear(formData) {
        if (sending.current) return;
        sending.current = true;
        setIsLoading(true);
        setError('');

        try {
            const res = await apiFetch("/api/lobby/crear", {
                method: "POST",
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData),
            });

            if (!res || !res.ok) throw new Error();
            const data = await res.json();
            if (mounted.current) navigate(`/partida/${data.idPartida}`);
        } catch (err) {
            if (mounted.current) setError('Error al crear la sesión de entrenamiento');
        } finally {
            sending.current = false;
            if (mounted.current) setIsLoading(false);
        }
    }

    return (
        <main className="crear-partida">
            <div className="crear-partida__container">
                <Link to="/jugador" className="crear-partida__back">Volver</Link>
                <h1 className="crear-partida__title">Entrenamiento personalizado</h1>
                <EntrenamientoForm
                    onSubmit={handleCrear}
                    isLoading={isLoading}
                    serverError={error}
                />
            </div>
        </main>
    )
}