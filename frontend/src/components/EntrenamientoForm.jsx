import { useState, useEffect } from "react";
import { apiFetch } from "../services/api";
import Deslizante from "./Deslizante";

export default function EntrenamientoForm({ onSubmit, isLoading, serverError }) {
    const [todasCategorias, setTodasCategorias] = useState([]);
    const [categoriasStats, setCategoriasStats] = useState([]);
    const [localError, setLocalError] = useState('');
    const [tiempoText, setTiempoText] = useState('30');
    const [aciertosText, setAciertosText] = useState('3');
    const [criterio, setCriterio] = useState('TIEMPO');
    const [direccion, setDireccion] = useState('MEJORAR');
    const [form, setForm] = useState({
        dificultades: ['MEDIO'],
        categoriaIds: [],
        tiempoRespuesta: 30,
        aciertosParaQuesito: 3,
    });


    useEffect(() => {
        apiFetch('/api/categorias')
            .then(res => res.json())
            .then(data => {
                setTodasCategorias(data);
                setForm(prev => ({ ...prev, categoriaIds: data.map(c => c.id) }));
            })
            .catch(() => setLocalError('Error al carggar las categorías'));
    }, []);

    useEffect(() => {
        apiFetch(`/api/stats/jugador/categorias-entrenamiento?criterio=${criterio}&direccion=${direccion}`)
            .then(res => res.json())
            .then(data => setCategoriasStats(Array.isArray(data) ? data : []))
            .catch(() => setCategoriasStats([]));
    }, [criterio, direccion]);

    function toggleCategoria(id) {
        setForm(prev => {
            const ids = prev.categoriaIds.includes(id)
                ? prev.categoriaIds.filter(cid => cid !== id)
                : [...prev.categoriaIds, id];
            return { ...prev, categoriaIds: ids };
        });
    }

    function handleSubmit(e) {
        e.preventDefault();
        setLocalError('');

        if (form.categoriaIds.length === 0) {
            setLocalError("Debes seleccionar al menos una categoría.");
            return;
        }

        if (form.dificultades.length === 0) {
            setLocalError('Debes seleccionar al menos una dificultad');
            return;
        }

        if (form.tiempoRespuesta < 30 || form.tiempoRespuesta > 300) {
            const clamped = Math.min(300, Math.max(30, form.tiempoRespuesta));
            setTiempoText(String(clamped));
            setForm(prev => ({ ...prev, tiempoRespuesta: clamped }));
            setLocalError('El tiempo de respuesta debe estar entre 30 y 300 segundos');
            return;
        }

        if (form.aciertosParaQuesito < 1 || form.aciertosParaQuesito > 30) {
            const clamped = Math.min(30, Math.max(1, form.aciertosParaQuesito));
            setAciertosText(String(clamped));
            setForm(prev => ({ ...prev, aciertosParaQuesito: clamped }));
            setLocalError('Los aciertos para obtener un quesito deben estar entre 1 y 30');
            return;
        }

        const statsIds = new Set(categoriasStats.map(c => c.categoriaId));
        const selectedConStats = categoriasStats.filter(c => form.categoriaIds.includes(c.categoriaId));
        const selectedSinStats = todasCategorias.filter(c => form.categoriaIds.includes(c.id) && !statsIds.has(c.id));
        const n = selectedConStats.length + selectedSinStats.length;

        const categoriaPesos = {};
        selectedConStats.forEach((cat, index) => {
            categoriaPesos[cat.categoriaId] = n - index;
        });

        selectedSinStats.forEach(cat => {
            categoriaPesos[cat.id] = 1;
        });

        onSubmit({
            tipo: 'PRIVADA',
            maxJugadores: 1,
            modoEntrenamiento: true,
            dificultades: form.dificultades,
            categoriaIds: form.categoriaIds,
            tiempoRespuesta: form.tiempoRespuesta,
            aciertosParaQuesito: form.aciertosParaQuesito,
            categoriaPesos,
        });
    }

    const statsIds = new Set(categoriasStats.map(c => c.categoriaId));
    const categoriasSinStats = todasCategorias.filter(c => !statsIds.has(c.id));
    const displayError = localError || serverError;

    const hintTexto = {
        'TIEMPO-MEJORAR': 'De mayor a menor tiempo · las más lentas primero',
        'TIEMPO-AFIANZAR': 'De menor a mayor tiempo · las más rápidas primero',
        'ACIERTOS-MEJORAR': 'De menor a mayor % acierto · las peores primero',
        'ACIERTOS-AFIANZAR': 'De mayor a menor % acierto · las mejores primero',
    }[`${criterio}-${direccion}`];


    return (
        <form className="crear-partida__form" onSubmit={handleSubmit}>

            {/* A PRACTICAR*/}
            <div className="crear-partida__group">
                <label className="crear-partida__group-title">¿Qué quieres practicar?</label>
                <div className="crear-partida__options">
                    {[['TIEMPO', '⏱ Tiempo de respuesta'], ['ACIERTOS', '✓ Aciertos']].map(([val, label]) => (
                        <label key={val} className={`crear-partida__option ${criterio === val ? 'crear-partida__option--active' : ''}`}>
                            <input type="radio" checked={criterio === val} onChange={() => setCriterio(val)} />
                            {label}
                        </label>
                    ))}
                </div>
            </div>

            {/* DIRECCIÓN*/}
            <div className="crear-partida__group">
                <label className="crear-partida__group-title">¿Con qué objetivo?</label>
                <div className="crear-partida__options">
                    {[['MEJORAR', '⬆ Mejorar las más flojas'], ['AFIANZAR', '★ Afianzar los mejores']].map(([val, label]) => (
                        <label key={val} className={`crear-partida__option ${direccion === val ? 'crear-partida__option--active' : ''}`}>
                            <input type="radio" checked={direccion === val} onChange={() => setDireccion(val)} />
                            {label}
                        </label>
                    ))}
                </div>
            </div>

            {/* Categorias */}
            <div className="crear-partida__group">
                <label className="crear-partida__group-title">Categorías de las preguntas</label>
                {hintTexto && <p className="entrenamiento__hint">{hintTexto}</p>}
                <div className="entrenamiento__categorias">
                    {categoriasStats.map((cat, i) => (
                        <label key={cat.categoriaId}
                            className={`entrenamiento__cat ${form.categoriaIds.includes(cat.categoriaId) ? 'entrenamiento__cat--activa' : ''}`}>
                            <input
                                type="checkbox"
                                checked={form.categoriaIds.includes(cat.categoriaId)}
                                onChange={() => toggleCategoria(cat.categoriaId)}
                            />
                            <span className="entrenamiento__cat-rank">#{i + 1}</span>
                            <span className="entrenamiento__cat-dot" style={{ background: cat.color }} />
                            <span className="entrenamiento__cat-nombre">{cat.nombre}</span>
                            <span className="entrenamiento__cat-stat">
                                {criterio === 'TIEMPO'
                                    ? `${(cat.tiempoMedioMs / 1000).toFixed(1)}s`
                                    : `${cat.porcentajeAcierto.toFixed(1)}%`}
                            </span>
                        </label>
                    ))}

                    {categoriasSinStats.map(cat => (
                        <label key={cat.id} className={`entrenamiento__cat ${form.categoriaIds.includes(cat.id) ? 'entrenamiento__cat--activa' : ''}`}>
                            <input type="checkbox"
                                checked={form.categoriaIds.includes(cat.id)}
                                onChange={() => toggleCategoria(cat.id)}
                            />
                            <span className="entrenamiento__cat-rank">—</span>
                            <span className="entrenamiento__cat-dot" style={{ background: cat.color }} />
                            <span className="entrenamiento__cat-nombre">{cat.nombre}</span>
                            <span className="entrenamiento__cat-stat entrenamiento__cat-stat--vacio">Sin datos</span>
                        </label>
                    ))}
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
                        <label key={val} className={`crear-partida__option ${form.dificultades.includes(val) ? 'crear-partida__option--active' : ''}`}>
                            <input
                                type="checkbox"
                                value={val}
                                checked={form.dificultades.includes(val)}
                                onChange={() => setForm(prev => {
                                    const difs = prev.dificultades.includes(val)
                                        ? prev.dificultades.filter(d => d !== val)
                                        : [...prev.dificultades, val];
                                    return { ...prev, dificultades: difs };
                                })}
                            />
                            {icon}
                            {text}
                        </label>
                    ))}
                </div>
            </div>
            { /*TIEMPO DE RESPUESTA*/}
            <Deslizante
                label="Tiempo máximo de respuesta (segundos)"
                min={30}
                max={300}
                step={5}
                value={form.tiempoRespuesta}
                text={tiempoText}
                onSliderChange={v => { setForm(prev => ({ ...prev, tiempoRespuesta: v })); setTiempoText(String(v)); }}
                onTextChange={raw => { setTiempoText(raw); const v = parseInt(raw, 10); if (!isNaN(v)) { setForm(prev => ({ ...prev, tiempoRespuesta: v })); } }}
            />
            { /*ACIERTO PARA QUESITO*/}
            <Deslizante
                label="Aciertos para obtener un quesito"
                min={1}
                max={30}
                value={form.aciertosParaQuesito}
                text={aciertosText}
                onSliderChange={v => { setForm(prev => ({ ...prev, aciertosParaQuesito: v })); setAciertosText(String(v)); }}
                onTextChange={raw => { setAciertosText(raw); const v = parseInt(raw, 10); if (!isNaN(v)) { setForm(prev => ({ ...prev, aciertosParaQuesito: v })); } }}
            />
            {displayError && <p className="crear-partida__error">{displayError}</p>}
            { /*BOTON CREAR*/}
            <button type="submit" className="btn btn--primary btn--full" disabled={isLoading}>
                {isLoading ? 'Iniciando...' : 'Empezar entrenamiento'}
            </button>
        </form>
    )

}