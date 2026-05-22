import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../services/api';

export default function EditarPreguntas() {
    const navigate = useNavigate();
    const [query, setQuery] = useState('');
    const [resultados, setResultados] = useState([]);
    const [asignaturas, setAsignaturas] = useState([]);
    const [editandoId, setEditandoId] = useState(null);
    const [editForm, setEditForm] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [exito, setExito] = useState('');
    const [sinResultados, setSinResultados] = useState(false);
    const inputImagenRef = useRef(null);


    useEffect(() => {
        if (error || exito) {
            const timer = setTimeout(() => {
                setError('');
                setExito('');
            }, 5000);
            return () => clearTimeout(timer);
        }
    }, [error, exito]);


    useEffect(() => {
        apiFetch('/api/profesor/asignaturas')
            .then(res => res.json())
            .then(setAsignaturas)
            .catch(() => { });
    }, []);

    async function handleBuscar(e) {
        e.preventDefault();
        setLoading(true);
        setError('');
        setEditandoId(null);
        try {
            const res = await apiFetch(`/api/profesor/preguntas/buscar?q=${encodeURIComponent(query)}`);
            const data = await res.json();
            setResultados(data);
            setSinResultados(data.length === 0);
            setTimeout(() => setSinResultados(false), 5000);
        } catch {
            setError('Error al buscar preguntas');
        } finally {
            setLoading(false);
        }
    }

    function iniciarEdicion(p) {
        setEditandoId(p.id);
        setEditForm({
            identificador: p.identificador,
            tituloIndice: p.tituloIndice || '',
            enunciado: p.enunciado,
            imagenUrl: p.imagenUrl || '',
            comentario: p.comentario || '',
            anulada: p.anulada,
            dificultad: p.dificultad,
            asignaturaId: String(p.asignaturaId),
            respuestas: p.respuestas.map(r => ({ ...r })),
        });
        setError('');
        setExito('');
    }

    async function handleGuardar() {
        if (!editForm.identificador.trim()) { setError('El identificador es obligatorio'); return; }
        if (!editForm.tituloIndice.trim()) { setError('El título índice es obligatorio'); return; }
        if (!editForm.enunciado.trim()) { setError('El enunciado es obligatorio'); return; }
        if (!editForm.asignaturaId) { setError('La asignatura es obligatoria'); return; }
        if (editForm.respuestas.some(r => !r.texto.trim())) { setError('Todas las respuestas deben tener texto'); return; }
        if (!editForm.respuestas.some(r => r.esCorrecta)) { setError('Debe haber una respuesta correcta'); return; }

        if (editForm.imagenUrl && !editForm.imagenUrl.startsWith('imagenes/')) {
            setError('La URL de imagen debe empezar por imagenes/');
            return;
        }
        setLoading(true);
        setError('');
        try {
            const res = await apiFetch(`/api/profesor/preguntas/${editandoId}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ ...editForm, asignaturaId: Number(editForm.asignaturaId) }),
            });
            if (!res.ok) throw new Error('Error al guardar');
            const updated = await res.json();
            //recorre el array existente (prev) y sustituye solo la pregunta que se acaba de editar 
            setResultados(prev => prev.map(p => p.id === editandoId ? updated : p));
            setEditandoId(null);
            setExito('Pregunta actualizada correctamente');
        } catch {
            setError('Error al guardar');
        } finally {
            setLoading(false);
        }
    }

    async function handleEliminar(id) {
        if (!confirm('¿Estás seguro de eliminar esta pregunta?')) return;
        setError('');
        try {
            const res = await apiFetch(`/api/profesor/preguntas/${id}`, {
                method: 'DELETE'
            });
            if (!res.ok) throw new Error('Error al eliminar');
            setResultados(prev => prev.filter(p => p.id !== id));
            if (editandoId === id) setEditandoId(null);
            setExito('Pregunta eliminada correctamente');
        } catch {
            setError('Error al eliminar la pregunta');
        }
    }

    function setRespuestaCorrecta(index) {
        setEditForm(prev => ({
            ...prev,
            respuestas: prev.respuestas.map((r, i) => ({ ...r, esCorrecta: i === index }))
        }));
    }

    return (
        <main className="gestion">
            <div className="gestion__container">
                <button className="btn btn--outline" onClick={() => navigate('/profesor')}>Volver</button>
                <h1 className="gestion__titulo">Editar / eliminar preguntas</h1>

                {error && <p className="gestion__error">{error}</p>}
                {exito && <p className="gestion__exito">{exito}</p>}

                <form className="gestion__form-inline" onSubmit={handleBuscar}>
                    <input
                        className="gestion__input"
                        style={{ flex: 2 }}
                        placeholder="Identificador o enunciado..."
                        value={query}
                        onChange={e => setQuery(e.target.value)}
                    />
                    <button className="btn btn--primary" type="submit" disabled={loading}>
                        {loading ? 'Buscando...' : 'Buscar'}
                    </button>
                </form>

                {resultados.length > 0 && (
                    <section className="gestion__seccion">
                        <h2 className="gestion__subtitulo">{resultados.length} resultado{resultados.length !== 1 ? 's' : ''}</h2>
                        <div className="gestion__tabla-wrapper">
                            <table className="gestion__tabla" style={{ tableLayout: 'fixed' }}>
                                <thead>
                                    <tr>
                                        <th>Identificador</th>
                                        <th>Enunciado</th>
                                        <th style={{ textAlign: 'right' }}>Acciones</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {resultados.map(p => (
                                        <>
                                            <tr key={p.id} className={editandoId === p.id ? 'gestion__fila--activa' : ''}>
                                                <td style={{ whiteSpace: 'nowrap' }}>{p.identificador}</td>
                                                <td><div className="editar__enunciado">{p.enunciado}</div></td>
                                                <td className="gestion__acciones" style={{ textAlign: 'right', justifyContent: 'flex-end' }}>
                                                    <button type="button" className="btn btn--outline btn--sm"
                                                        onClick={() => editandoId === p.id ? setEditandoId(null) : iniciarEdicion(p)}>
                                                        {editandoId === p.id ? 'Cerrar' : 'Editar'}
                                                    </button>
                                                    <button type="button" className="btn btn--danger btn--sm"
                                                        onClick={() => handleEliminar(p.id)}>🗑</button>
                                                </td>
                                            </tr>
                                            {editandoId === p.id && editForm && (
                                                <tr key={`panel-${p.id}`}>
                                                    <td colSpan={3} style={{ padding: 0 }}>
                                                        <div className="editar__panel">
                                                            <section className="gestion__seccion editar__panel">
                                                                <h2 className="gestion__subtitulo">Editando: {editForm.identificador}</h2>
                                                                <div className="editar__grid">
                                                                    <div className="editar__campo">
                                                                        <label>Identificador <span className="editar__obligatorio">*</span></label>
                                                                        <input className="gestion__input" value={editForm.identificador}
                                                                            onChange={e => setEditForm(p => ({ ...p, identificador: e.target.value }))} />
                                                                    </div>
                                                                    <div className="editar__campo">
                                                                        <label>Título índice <span className="editar__obligatorio">*</span></label>
                                                                        <input className="gestion__input" value={editForm.tituloIndice}
                                                                            onChange={e => setEditForm(p => ({ ...p, tituloIndice: e.target.value }))} />
                                                                    </div>
                                                                    <div className="editar__campo editar__campo--full">
                                                                        <label>Enunciado <span className="editar__obligatorio">*</span></label>
                                                                        <textarea className="gestion__input editar__textarea" value={editForm.enunciado}
                                                                            onChange={e => setEditForm(p => ({ ...p, enunciado: e.target.value }))} />
                                                                    </div>
                                                                    <div className="editar__campo">
                                                                        <label>Dificultad <span className="editar__obligatorio">*</span></label>
                                                                        <select className="gestion__select" value={editForm.dificultad}
                                                                            onChange={e => setEditForm(p => ({ ...p, dificultad: e.target.value }))}>
                                                                            <option value="FACIL">Fácil</option>
                                                                            <option value="MEDIO">Medio</option>
                                                                            <option value="DIFICIL">Difícil</option>
                                                                        </select>
                                                                    </div>
                                                                    <div className="editar__campo">
                                                                        <label>Asignatura <span className="editar__obligatorio">*</span></label>
                                                                        <select className="gestion__select" value={editForm.asignaturaId}
                                                                            onChange={e => setEditForm(p => ({ ...p, asignaturaId: e.target.value }))}>
                                                                            {asignaturas.map(a => (
                                                                                <option key={a.id} value={a.id}>{a.nombre}</option>
                                                                            ))}
                                                                        </select>
                                                                    </div>
                                                                    <div className="editar__campo editar__campo--full">
                                                                        <label>URL imagen</label>
                                                                        {editForm.imagenUrl && (
                                                                            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 'var(--space-sm)', marginBottom: 'var(--space-sm)' }}>
                                                                                {editForm.imagenUrl.split(' | ').map((url, i) => (
                                                                                    <div key={i} style={{ position: 'relative', display: 'inline-flex', alignItems: 'center', gap: 'var(--space-xs)', padding: 'var(--space-xs) var(--space-sm)', background: 'var(--bg-surface)', border: '1px solid var(--color-border)', borderRadius: 'var(--radius-sm)', fontSize: '0.85rem' }}>
                                                                                        <span>🖼️ {url.split('/').pop()}</span>
                                                                                        <button
                                                                                            type="button"
                                                                                            style={{ background: 'none', border: 'none', color: 'var(--color-danger)', cursor: 'pointer', fontSize: '0.9rem', padding: 0 }}
                                                                                            onClick={() => {
                                                                                                const urls = editForm.imagenUrl.split(' | ').filter((_, j) => j !== i);
                                                                                                setEditForm(p => ({ ...p, imagenUrl: urls.join(' | ') }));
                                                                                            }}
                                                                                        >✕</button>
                                                                                    </div>
                                                                                ))}
                                                                            </div>
                                                                        )}

                                                                        <div style={{ display: 'flex', gap: 'var(--space-sm)', marginTop: 'var(--space-xs)' }}>
                                                                            <input
                                                                                className="gestion__input"
                                                                                placeholder="imagenes/ruta/archivo.png"
                                                                                value={editForm._rutaNuevaImagen || ''}
                                                                                onChange={e => setEditForm(p => ({ ...p, _rutaNuevaImagen: e.target.value }))}
                                                                            />
                                                                            <label className="btn btn--outline btn--sm" style={{ cursor: 'pointer', whiteSpace: 'nowrap' }}>
                                                                                Subir imagen
                                                                                <input ref={inputImagenRef} type="file" accept="image/*" style={{ display: 'none' }}
                                                                                    onChange={async e => {
                                                                                        const file = e.target.files[0];
                                                                                        if (!file) return;
                                                                                        const ruta = editForm._rutaNuevaImagen?.trim();
                                                                                        if (!ruta?.startsWith('imagenes/')) {
                                                                                            setError('La ruta debe empezar por imagenes/');
                                                                                            return;
                                                                                        }
                                                                                        const urlsExistentes = editForm.imagenUrl ? editForm.imagenUrl.split(' | ') : [];
                                                                                        if (urlsExistentes.includes(ruta)) {
                                                                                            setError('Esa imagen ya está añadida');
                                                                                            return;
                                                                                        }
                                                                                        const formData = new FormData();
                                                                                        formData.append('file', file);
                                                                                        formData.append('ruta', ruta);
                                                                                        const res = await apiFetch('/api/profesor/imagenes', { method: 'POST', body: formData });
                                                                                        if (res.ok) {
                                                                                            const data = await res.json();
                                                                                            setEditForm(p => ({
                                                                                                ...p,
                                                                                                imagenUrl: p.imagenUrl ? p.imagenUrl + ' | ' + data.url : data.url,
                                                                                                _rutaNuevaImagen: ''
                                                                                            }));
                                                                                            setExito('Imagen subida correctamente');
                                                                                            if (inputImagenRef.current) {
                                                                                                inputImagenRef.current.value = '';
                                                                                            }
                                                                                        } else {
                                                                                            setError('Error al subir la imagen');
                                                                                        }
                                                                                    }} />
                                                                            </label>
                                                                        </div>
                                                                    </div>
                                                                    <div className="editar__campo">
                                                                        <label>Comentario</label>
                                                                        <input className="gestion__input" value={editForm.comentario}
                                                                            onChange={e => setEditForm(p => ({ ...p, comentario: e.target.value }))} />
                                                                    </div>
                                                                    <div className="editar__campo">
                                                                        <label className="editar__check">
                                                                            <input type="checkbox" checked={editForm.anulada}
                                                                                onChange={e => setEditForm(p => ({ ...p, anulada: e.target.checked }))} />
                                                                            Anulada
                                                                        </label>
                                                                    </div>
                                                                </div>

                                                                {editForm.respuestas.length > 0 && (
                                                                    <div className="editar__respuestas">
                                                                        <label className="gestion__subtitulo">Respuestas <span className="editar__obligatorio">*</span></label>
                                                                        {editForm.respuestas.map((r, i) => (
                                                                            <div key={r.id} className={`editar__respuesta ${r.esCorrecta ? 'editar__respuesta--correcta' : ''}`}>
                                                                                <input type="radio" name={`correcta-${editandoId}`}
                                                                                    checked={r.esCorrecta} onChange={() => setRespuestaCorrecta(i)} />
                                                                                <span className="editar__respuesta-letra">{['A', 'B', 'C', 'D', 'E'][i]}.</span>
                                                                                <input className="gestion__input" value={r.texto}
                                                                                    onChange={e => setEditForm(prev => ({
                                                                                        ...prev,
                                                                                        respuestas: prev.respuestas.map((resp, j) =>
                                                                                            j === i ? { ...resp, texto: e.target.value } : resp)
                                                                                    }))} />
                                                                            </div>
                                                                        ))}
                                                                    </div>
                                                                )}
                                                                <div className="gestion__acciones" style={{ marginTop: 'var(--space-md)' }}>
                                                                    <button type="button" className="btn btn--primary" onClick={handleGuardar} disabled={loading}>
                                                                        Guardar cambios
                                                                    </button>
                                                                    <button type="button" className="btn btn--outline" onClick={() => setEditandoId(null)}>
                                                                        Cancelar
                                                                    </button>
                                                                </div>
                                                            </section>
                                                        </div>
                                                    </td>
                                                </tr>
                                            )}
                                        </>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </section>
                )}
                {sinResultados && (
                    <p className="gestion__error">No se han encontrado preguntas que coincidan con tu búsqueda.</p>
                )}
            </div>
        </main>
    );
}