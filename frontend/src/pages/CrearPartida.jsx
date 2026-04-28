import { useState, useEffect } from "react";
import { useNavigate, Link } from "react-router-dom";
import { apiFetch } from "../services/api";

export default function CrearPartida() {
    const navigate = useNavigate();
    const [categorias, setCategorias] = useState([]);
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [form, setForm] = useState({
        tipo: 'PUBLICA',
        maxJugadores: 4,
        dificultad: 'MEDIO',
        categoriaIds: [],
        tiempoRespuesta: 30,
    });

    useEffect(() => {
        apiFetch('/api/categorias')
            .then(res => { if (!res || !res.ok) throw new Error(); return res.json(); })
            .then(data => {
                setCategorias(data);
                setForm(prev => ({ ...prev, categoriaIds: data.map(c => c.id) })); //Selecciona todas las categorías por defecto
            })
            .catch(() => setError('Error al cargar las categorías'))
    }, []);

    function handleToggleCategoria(id) {
        setForm(prev => {
            const ids = prev.categoriaIds.includes(id)
                ? prev.categoriaIds.filter(cid => cid !== id)
                : [...prev.categoriaIds, id];
            return { ...prev, categoriaIds: ids };
        });
    }

    async function handleSubmit(e) {
        e.preventDefault();
        if (form.categoriaIds.length === 0) {
            setError('Debes seleccionar al menos una categoría');
            return;
        }
        setIsLoading(true);
        setError('');

        try {
            const res = await apiFetch('/api/lobby/crear', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(form),
            });

            if (!res || !res.ok) throw new Error();
            const data = await res.json();
            navigate(`/partida/${data.idPartida}`);

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

                <form className="crear-partida__form" onSubmit={handleSubmit}>
                    {/* TIPO */}
                    <fieldset className="crear-partida__fieldset">
                        <legend className="crear-partida__legend">Tipo de partida</legend>
                        <div className="crear-partida__options">
                            <label className="crear-partida__options">
                                {[['PUBLICA', '🌐 Pública'], ['PRIVADA', '🔒 Privada']].map(([val, label]) => (
                                    <label key={val} className={`crear-partida__option ${form.tipo === val ? 'crear-partida__option--active' : ''}`}>
                                        <input type="radio" name="tipo" value={val} checked={form.tipo === val} onChange={() => setForm(prev => ({ ...prev, tipo: val }))} />
                                        {label}
                                    </label>
                                ))}
                            </label>
                        </div>
                    </fieldset>

                    { /* JUGADORES */}
                    <div className="crear-partida__field">
                        <label className="crear-partida__label">
                            Número de jugadores: <strong>{form.maxJugadores}</strong>
                        </label>
                        <input type="range" min={2} max={6} value={form.maxJugadores}
                            onChange={e => setForm(prev => ({ ...prev, maxJugadores: Number(e.target.value) }))}
                            className="crear-partida__range" />
                        <div className="crear-partida__range-labels">
                            {[2, 3, 4, 5, 6].map(n => <span key={n}>{n}</span>)}
                        </div>
                    </div>
                    { /*DIFICULTAD*/}
                    <fieldset className="crear-partida__fieldset">
                        <legend className="crear-partida__legend">Dificultad</legend>
                        <div className="crear-partida__options">
                            {[['FACIL', '🌱 Fácil'], ['MEDIO', '🌿 Medio'], ['DIFICIL', '🌳 Difícil']].map(([val, label]) => (
                                <label key={val} className={`crear-partida__option ${form.dificultad === val ? 'crear-partida__option--active' : ''}`}>
                                    <input type="radio" name="dificultad" value={val} checked={form.dificultad === val} onChange={() => setForm(prev => ({ ...prev, dificultad: val }))} />
                                    {label}
                                </label>
                            ))}
                        </div>
                    </fieldset>
                    { /*CATEGORIAS*/}
                    <fieldset className="crear-partida__fieldset">
                        <legend className="crear-partida__legend">Categorías de las preguntas</legend>
                        <div className="crear-partida__categories">
                            {categorias.map(cat => (
                                <label key={cat.id} className={`crear-partida__cat ${form.categoriaIds.includes(cat.id) ? 'crear-partida__cat--active' : ''}`}>
                                    <input type="checkbox" checked={form.categoriaIds.includes(cat.id)} onChange={() => handleToggleCategoria(cat.id)} />
                                    <span className="crear-partida__cat-color" style={{ background: cat.color }} />
                                    {cat.nombre}
                                </label>
                            ))}
                        </div>
                    </fieldset>
                    { /*TIEMPO DE RESPUESTA*/}
                    <div className="crear-partida__field">
                        <label className="crear-partida__label">
                            Tiempo máximo de respuesta: <strong>{form.tiempoRespuesta}s</strong>
                        </label>
                        <input type="range" min={15} max={300} step={5} value={form.tiempoRespuesta}
                            onChange={e => setForm(prev => ({ ...prev, tiempoRespuesta: Number(e.target.value) }))}
                            className="crear-partida__range" />
                        <div className="crear-partida__range-labels">
                            <span>15s</span><span>300s</span>
                        </div>
                    </div>
                    {error && <p className="crear-partida__error">{error}</p>}
                    { /*BOTON CREAR*/}
                    <button type="submit" className="btn btn--primary btn--full" disabled={isLoading}>
                        {isLoading ? 'Creando...' : 'Crear partida'}
                    </button>
                </form>
            </div>
        </main>
    );
}