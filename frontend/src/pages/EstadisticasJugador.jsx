import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../services/api';

export default function EstadisticasJugador() {
    const navigate = useNavigate();
    const [stats, setStats] = useState(null);
    const [isLoading, setIsLoading] = useState(true);
    const [error, setError] = useState('');
    const [categoriaSeleccionada, setCategoriaSeleccionada] = useState(null);
    const [asignaturas, setAsignaturas] = useState(null);
    const [cargandoAsig, setCargandoAsig] = useState(false);
    const [ordenCat, setOrdenCat] = useState('acierto');

    useEffect(() => {
        apiFetch('/api/stats/jugador')
            .then(res => res.json())
            .then(data => {
                setStats(data);
                setIsLoading(false);
            })
            .catch(() => {
                setError("Error al cargar las estadísticas");
                setIsLoading(false);
            })
    }, []);

    async function seleccionarCategoria(cat) {
        //Si la categoria ya esta seleccionada, deseleccionarla
        if (categoriaSeleccionada?.categoriaId === cat.categoriaId) {
            setCategoriaSeleccionada(null);
            setAsignaturas(null);
            return;
        }
        setCategoriaSeleccionada(cat);
        setAsignaturas(null);
        setCargandoAsig(true);
        const res = await apiFetch(`/api/stats/jugador/categoria/${cat.categoriaId}`)
        const data = await res.json();
        setAsignaturas(data);
        setCargandoAsig(false);
    }

    if (isLoading) return <p className="stats__loading">Cargando estadísticas...</p>
    if (error) return <p>{error}</p>

    const categoriasOrdenadas = ordenCat == 'acierto'
        ? [...stats.categorias].sort((a, b) => b.porcentajeAcierto - a.porcentajeAcierto) //Ordena de mayor a menor porcentaje de acierto
        : [...stats.categorias].sort((a, b) => a.tiempoMedioMs - b.tiempoMedioMs); //Ordena de menor a mayor tiempo medio

    return (
        <main className="stats">
            <div className="stats__header">
                <button className="btn btn--outline" onClick={() => navigate('/jugador')}>
                    Volver
                </button>
                <h1 className="stats_titulo">Mis estadísticas</h1>
            </div>

            <section className="stats__seccion">
                <h2 className="stats__subtitulo">Partidas</h2>
                <div className="stats__cards">
                    <div className="stats__card">
                        <span className="stats__card-valor">{stats.partidasJugadas}</span>
                        <span className="stats__card-label">Jugadas</span>
                    </div>
                    <div className="stats__card">
                        <span className="stats__card-valor">{stats.victorias}</span>
                        <span className="stats__card-label">Victorias</span>
                    </div>
                    <div className="stats__card stats__card--accent">
                        <span className="stats__card-valor">{stats.porcentajeVictorias.toFixed(1)}%</span>
                        <span className="stats__card-label">% Victorias</span>
                    </div>
                </div>
            </section>

            <section className="stats__seccion">
                <h2 className="stats__subtitulo">Preguntas</h2>
                <div className="stats__cards">
                    <div className="stats__card">
                        <span className="stats__card-valor">{stats.totalRespondidas}</span>
                        <span className="stats__card-label">Respondidas</span>
                    </div>
                    <div className="stats__card stats__card--correct">
                        <span className="stats__card-valor">{stats.totalAciertos}</span>
                        <span className="stats__card-label">Aciertos</span>
                    </div>
                    <div className="stats__card stats__card--wrong">
                        <span className="stats__card-valor">{stats.totalFallos}</span>
                        <span className="stats__card-label">Fallos</span>
                    </div>
                    <div className="stats__card stats__card--accent">
                        <span className="stats__card-valor">{stats.porcentajeAciertos.toFixed(1)}%</span>
                        <span className="stats__card-label">% Acierto Global</span>
                    </div>
                    <div className="stats__card stats__card--neutral">
                        <span className="stats__card-valor">{stats.totalNoRespondidas}</span>
                        <span className="stats__card-label">No Respondidas</span>
                    </div>
                </div>
            </section>

            <section className="stats__seccion">
                <h2 className="stats__subtitulo">Tiempo medio de respuesta</h2>
                <div className="stats__cards">
                    <div className="stats__card stats__card--lg">
                        <div className="stats__card-valor">{(stats.tiempoMedioMs / 1000).toFixed(2)}s</div>
                        <div className="stats__card-label">por pregunta (global)</div>
                    </div>
                </div>
            </section>

            {stats.categorias.length === 0 ? (
                <p className="stats__empty">Todavía no has jugado ninguna partida.</p>

            ) : (
                <section className="stats__seccion">
                    <div className="stats__cat-header">
                        <h2 className="stats__subtitulo">Por categoría</h2>
                        <div className="stats__orden-btns">
                            <button
                                className={`btn btn--sm ${ordenCat === 'acierto' ? 'btn--primary' : 'btn--outline'}`}
                                onClick={() => setOrdenCat('acierto')}
                            >
                                % Acierto
                            </button>
                            <button
                                className={`btn btn--sm ${ordenCat === 'tiempo' ? 'btn--primary' : 'btn--outline'}`}
                                onClick={() => setOrdenCat('tiempo')}
                            >
                                Tiempo
                            </button>
                        </div>
                    </div>
                    <p className="stats__hint">Pulsa una categoria para ver el desglose por asignatura</p>

                    <div className="stats__categorias">
                        {categoriasOrdenadas.map((cat, i) => (
                            <div
                                key={cat.categoriaId}
                                className={`stats__cat-card ${categoriaSeleccionada?.categoriaId === cat.categoriaId ? 'stats__cat-card--activa' : ''}`}
                                style={{ '--cat-clor': cat.color }}
                                onClick={() => seleccionarCategoria(cat)}
                            >
                                {i === 0 && (
                                    <span className="stats__cat-badge stats__cat-badge--best">
                                        {ordenCat === 'acierto' ? '★ Mejor' : '⚡ Más rápida'}
                                    </span>
                                )}
                                {i === categoriasOrdenadas.length - 1 && (
                                    <span className="stats__cat-badge stats__cat-badge--worst">
                                        {ordenCat === 'acierto' ? '↓ Peor' : '🐢 Más lenta'}
                                    </span>
                                )}
                                <div className="stats__cat-nombre">{cat.nombre}</div>
                                <div className="stats__cat-nums">
                                    <span>{cat.total} pregs.</span>
                                    <span className="stats__cat-acierto">{cat.aciertos} ✓</span>
                                    <span className="stats__cat-fallo">{cat.fallos} ✗</span>
                                </div>
                                <div className="stats__cat-barra">
                                    <div className="stats__cat-barra-fill" style={{ width: `${cat.porcentajeAcierto}%` }} />
                                </div>
                                <div className="stats__cat-footer">
                                    <span>{cat.porcentajeAcierto.toFixed(1)}%</span>
                                    <span>{(cat.tiempoMedioMs / 1000).toFixed(1)}s media</span>
                                </div>
                            </div>
                        ))}
                    </div>

                    {categoriaSeleccionada && (
                        <div className="stats__asignaturas">
                            <h3 className="stats__subtitulo--sm">Asignaturas — {categoriaSeleccionada.nombre}</h3>
                            {cargandoAsig ? <p>Cargando...</p> : (
                                <div className="stats__asig-lista">
                                    {asignaturas?.map(a => (
                                        <div key={a.asignaturaId} className="stats__asig-fila">
                                            <span className="stats__asig-nombre">{a.nombre}</span>
                                            <div className="stats__asig-nums">
                                                <span className="stats__cat-acierto">{a.aciertos} ✓</span>
                                                <span className="stats__cat-fallo">{a.fallos} ✗</span>
                                            </div>
                                            <div className="stats__cat-barra stats__cat-barra--sm">
                                                <div className="stats__cat-barra-fill" style={{ width: `${a.porcentajeAcierto}%`, '--cat-color': categoriaSeleccionada.color }} />
                                            </div>
                                            <span className="stats__asig-pct">{a.porcentajeAcierto.toFixed(1)}%</span>
                                            <span className="stats__asig-tiempo">{(a.tiempoMedioMs / 1000).toFixed(1)}s media</span>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    )}
                </section>
            )}
        </main>
    );
} 