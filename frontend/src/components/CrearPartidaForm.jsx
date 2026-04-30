import { useState, useEffect } from "react";
import { apiFetch } from "../services/api";

export default function CrearPartidaForm({ onSubmit, isLoading, serverError }) {
    const [categorias, setCategorias] = useState([]);
    const [localError, setLocalError] = useState('');
    const [jugadoresText, setJugadoresText] = useState('4');
    const [tiempoText, setTiempoText] = useState('30');
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
                //Selecciona todas las categorías por defecto,
                // ...prev es para no perder los otros valores del formulario
                setForm(prev => ({ ...prev, categoriaIds: data.map(c => c.id) }));
            })
            .catch(() => setLocalError('Error al cargar las categorías'))
    }, []);

    //Seleccionar o deseleccionar categorías
    function seleccionarCategorias(id) {
        setForm(prev => {
            const ids = prev.categoriaIds.includes(id)
                ? prev.categoriaIds.filter(cid => cid !== id)   //si ya está seleccionado, lo quita. El filter se queda con los que no son iguales al id.
                : [...prev.categoriaIds, id];   //si no está seleccionado, lo añade
            return { ...prev, categoriaIds: ids }; //devuelve el objeto anterior con el nuevo array de categorías
        });
    }

    function handleSubmit(e) {
        e.preventDefault();
        setLocalError('');

        if (form.categoriaIds.length === 0) {
            setLocalError('Debes seleccionar al menos una categoría');
            return;
        }
        if (!['PUBLICA', 'PRIVADA'].includes(form.tipo)) {
            setLocalError('Tipo de partida inválido');
            return;
        }
        if (!['FACIL', 'MEDIO', 'DIFICIL'].includes(form.dificultad)) {
            setLocalError('Dificultad inválida');
            return;
        }
        if (form.maxJugadores < 2 || form.maxJugadores > 6) {
            const clamped = Math.min(6, Math.max(2, form.maxJugadores));
            setJugadoresText(String(clamped));
            setForm(prev => ({ ...prev, maxJugadores: clamped }));
            setLocalError('El número de jugadores debe estar entre 2 y 6');
            return;
        }
        if (form.tiempoRespuesta < 30 || form.tiempoRespuesta > 300) {
            const clamped = Math.min(300, Math.max(30, form.tiempoRespuesta));
            setTiempoText(String(clamped));
            setForm(prev => ({ ...prev, tiempoRespuesta: clamped }));
            setLocalError('El tiempo de respuesta debe estar entre 30 y 300 segundos');
            return;
        }

        // Si todo es válido, enviamos el formulario al componente padre
        onSubmit(form);
    }

    const displayError = localError || serverError;

    return (
        <form className="crear-partida__form" onSubmit={handleSubmit}>
            {/* TIPO */}
            <div className="crear-partida__group">
                <label className="crear-partida__group-title">Tipo de partida</label>
                <div className="crear-partida__options">
                    {[['PUBLICA', '🌐 Pública'], ['PRIVADA', '🔒 Privada']].map(([val, label]) => (
                        //Los dos botones tienen la clase crear-partida__option, la diferencia es que si está seleccionado, se le añade la clase crear-partida__option--active
                        <label key={val} className={`crear-partida__option ${form.tipo === val ? 'crear-partida__option--active' : ''}`}>
                            <input type="radio" name="tipo" value={val} checked={form.tipo === val} onChange={() => setForm(prev => ({ ...prev, tipo: val }))} />
                            {label}
                        </label>
                    ))}
                </div>
            </div>

            { /* JUGADORES */}
            <div className="crear-partida__field">
                <label className="crear-partida__group-title">Número de jugadores</label>
                <div className="crear-partida__range-row">
                    <input type="range" min={2} max={6} value={form.maxJugadores}
                        onChange={e => {
                            const v = Number(e.target.value);
                            setForm(prev => ({ ...prev, maxJugadores: v }));
                            setJugadoresText(String(v));
                        }}
                        className="crear-partida__range"
                    />
                    <input type="number" value={jugadoresText}
                        onChange={e => {
                            const raw = e.target.value;
                            setJugadoresText(raw);
                            const v = parseInt(raw, 10);
                            if (!isNaN(v)) {
                                setForm(prev => ({ ...prev, maxJugadores: v }));
                            }
                        }}
                        className="crear-partida__number-input"
                    />
                </div>
            </div>
            { /*DIFICULTAD*/}
            <div className="crear-partida__group">
                <label className="crear-partida__group-title">Dificultad</label>
                <div className="crear-partida__options">
                    {[
                        {
                            val: 'FACIL', text: 'Fácil',
                            icon: (
                                <svg viewBox="0 0 24 24" fill="none" stroke="#22C55E" strokeWidth="2" strokeLineCap="round" strokeLinejoin="round" width="24" height="24">
                                    <polyline points="3,12 8,12 10,10 14,14 16,12 21,12" />
                                </svg>
                            )
                        },
                        {
                            val: 'MEDIO', text: 'Medio',
                            icon: (
                                <svg viewBox="0 0 24 24" fill="none" stroke="#F59E0B" strokeWidth="2" strokeLineCap="round" strokeLinejoin="round" width="24" height="24">
                                    <polyline points="3,12 7,12 9,6 13,18 15,12 21,12" />
                                </svg>
                            )
                        },
                        {
                            val: 'DIFICIL', text: 'Difícil',
                            icon: (
                                <svg viewBox="0 0 24 24" fill="none" stroke="#EF4444" strokeWidth="2" strokeLineCap="round" strokeLinejoin="round" width="24" height="24">
                                    <polyline points="2,12 4,12 6,6 10,20 14,4 18,16 20,12 22,12" />
                                </svg>
                            )
                        },
                    ].map(({ val, text, icon }) => (
                        <label key={val} className={`crear-partida__option ${form.dificultad === val ? 'crear-partida__option--active' : ''}`}>
                            <input type="radio" name="dificultad" value={val} checked={form.dificultad === val} onChange={() => setForm(prev => ({ ...prev, dificultad: val }))} />
                            {icon}
                            {text}
                        </label>
                    ))}
                </div>
            </div>
            { /*CATEGORIAS*/}
            <div className="crear-partida__group">
                <label className="crear-partida__group-title">Categorías de las preguntas</label>
                <div className="crear-partida__categories">
                    {categorias.map(cat => (
                        <label key={cat.id} className={`crear-partida__cat ${form.categoriaIds.includes(cat.id) ? 'crear-partida__cat--active' : ''}`}>
                            <input type="checkbox" checked={form.categoriaIds.includes(cat.id)} onChange={() => seleccionarCategorias(cat.id)} />
                            <span className="crear-partida__cat-color" style={{ background: cat.color }} />
                            {cat.nombre}
                        </label>
                    ))}
                </div>
            </div>
            { /*TIEMPO DE RESPUESTA*/}
            <div className="crear-partida__field">
                <label className="crear-partida__group-title">
                    Tiempo máximo de respuesta (segundos)
                </label>
                <div className="crear-partida__range-row">
                    <input type="range" min={30} max={300} step={5} value={form.tiempoRespuesta}
                        onChange={e => {
                            const v = Number(e.target.value);
                            setForm(prev => ({ ...prev, tiempoRespuesta: v }));
                            setTiempoText(String(v));
                        }} className="crear-partida__range" />
                    <input type="number" value={tiempoText}
                        onChange={e => {
                            const raw = e.target.value;
                            setTiempoText(raw);
                            const v = parseInt(raw, 10);
                            if (!isNaN(v)) {
                                setForm(prev => ({ ...prev, tiempoRespuesta: v }));
                            }
                        }}
                        className="crear-partida__number-input" />
                </div>
            </div>
            {displayError && <p className="crear-partida__error">{displayError}</p>}
            { /*BOTON CREAR*/}
            <button type="submit" className="btn btn--primary btn--full" disabled={isLoading}>
                {isLoading ? 'Creando...' : 'Crear partida'}
            </button>
        </form>
    );
}
