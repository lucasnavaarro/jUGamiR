import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../services/api';

export default function GestionAsignaturas() {

    const navigate = useNavigate();
    const [asignaturas, setAsignaturas] = useState([]);
    const [categorias, setCategorias] = useState([])
    const [form, setForm] = useState({ nombre: '', categoriaId: '' });
    const [editandoId, setEditandoId] = useState(null);
    const [editForm, setEditForm] = useState({ nombre: '', categoriaId: '' });
    const [error, setError] = useState('');
    const [exito, setExito] = useState('');
    const [loading, setLoading] = useState(false);

    useEffect(() => {
        cargarDatos();
        apiFetch('/api/categorias')
            .then(res => res.json())
            .then(setCategorias)
            .catch(() => { })
    }, []);

    async function cargarDatos() {
        const res = await apiFetch('/api/profesor/asignaturas');
        const data = await res.json();
        setAsignaturas(data);
    }

    async function handleCrear(e) {
        e.preventDefault();
        setError('');
        setExito('');
        if (!form.nombre.trim() || !form.categoriaId) {
            setError('Rellena todos los campos');
            return;
        }
        setLoading(true);
        try {
            const res = await apiFetch('/api/profesor/asignaturas', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nombre: form.nombre.trim(), categoriaId: Number(form.categoriaId) }),
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.message || 'Error al crear la asignatura');
            }
            setForm({ nombre: '', categoriaId: '' });
            setExito('Asignatura creada correctamente');
            await cargarDatos();
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    function iniciarEdicion(a) {
        setEditandoId(a.id);
        setEditForm({ nombre: a.nombre, categoriaId: String(a.categoriaId) });
        setError('');
        setExito('');
    }

    async function handleEditar(id) {
        console.log('handleEditar llamado, loading:', loading);
        setError('');
        setExito('');
        if (!editForm.nombre.trim() || !editForm.categoriaId) {
            setError('Rellena todos los campos');
            return;
        }
        setLoading(true);
        try {
            const res = await apiFetch(`/api/profesor/asignaturas/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ nombre: editForm.nombre.trim(), categoriaId: Number(editForm.categoriaId) }),
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.message || 'Error al editar la asignatura');
            }
            setEditandoId(null);
            setEditForm({ nombre: '', categoriaId: '' });
            setExito('Asignatura editada correctamente');
            await cargarDatos();
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }

    async function handleEliminar(id) {
        if (!confirm('¿Seguro que quieres eliminar esta asignatura?')) return;
        setError('');
        setExito('');
        try {
            const res = await apiFetch(`/api/profesor/asignaturas/${id}`, { method: 'DELETE' });
            if (!res.ok) throw new Error('Error al eliminar');
            setExito('Asignatura eliminada correctamente');
            await cargarDatos();
        } catch (err) {
            setError(err.message);
        }
    }

    return (
        <main className="gestion">
            <div className="gestion__container">
                <button className="btn btn--outline" onClick={() => navigate('/profesor')}> Volver</button>
                <h1 className="gestion__titulo">Gestión de asignaturas</h1>

                {error && <p className="gestion__error">{error}</p>}
                {exito && <p className="gestion__exito">{exito}</p>}

                {/* AÑADIR */}
                <section className="gestion__seccion">
                    <h2 className="gestion__subtitulo"> Añadir asignatura</h2>
                    <form className="gestion__form-inline" onSubmit={handleCrear}>
                        <input
                            className="gestion__input"
                            type="text"
                            placeholder="Nombre de la asignatura"
                            value={form.nombre}
                            onChange={e => setForm(prev => ({ ...prev, nombre: e.target.value }))}
                        />
                        <select
                            className="gestion__select"
                            value={form.categoriaId}
                            onChange={e => setForm(prev => ({ ...prev, categoriaId: e.target.value }))}
                        >
                            <option value="">Selecciona categoría</option>
                            {categorias.map(c => (
                                <option key={c.id} value={c.id}>{c.nombre}</option>
                            ))}
                        </select>
                        <button className="btn btn--primary" type="submit" disabled={loading}>
                            Añadir
                        </button>
                    </form>
                </section>

                {/* Tabla */}
                <section className="gestion__seccion">
                    <h2 className="gestion__subtitulo"> Asignaturas existentes ({asignaturas.length})</h2>
                    <div className="gestion__tabla-wrapper">
                        <table className="gestion__tabla">
                            <thead>
                                <tr>
                                    <th>Nombre</th>
                                    <th>Categoría</th>
                                    <th>Acciones</th>
                                </tr>
                            </thead>
                            <tbody>
                                {asignaturas.map(a => (
                                    <tr key={a.id}>
                                        {editandoId === a.id ? (
                                            <>
                                                <td>
                                                    <input
                                                        className="gestion__input gestion__input--sm"
                                                        value={editForm.nombre}
                                                        onChange={e => setEditForm(prev => ({ ...prev, nombre: e.target.value }))}
                                                    />
                                                </td>
                                                <td>
                                                    <select
                                                        className="gestion__select gestion__select--sm"
                                                        value={editForm.categoriaId}
                                                        onChange={e => setEditForm(prev => ({ ...prev, categoriaId: e.target.value }))}
                                                    >
                                                        {categorias.map(c => (
                                                            <option key={c.id} value={c.id}>{c.nombre}</option>
                                                        ))}
                                                    </select>
                                                </td>
                                                <td className="gestion__acciones">
                                                    <button className="btn btn--primary btn--sm" onClick={() => handleEditar(a.id)} disabled={loading}>Guardar</button>
                                                    <button className="btn btn--outline btn--sm" onClick={() => setEditandoId(null)}>Cancelar</button>
                                                </td>
                                            </>
                                        ) : (
                                            <>
                                                <td>{a.nombre}</td>
                                                <td>{a.categoriaNombre}</td>
                                                <td className="gestion__acciones">
                                                    <button className="btn btn--outline btn--sm" onClick={() => iniciarEdicion(a)}>Editar</button>
                                                    <button className="btn btn--danger btn--sm" onClick={() => handleEliminar(a.id)}>🗑</button>
                                                </td>
                                            </>
                                        )}
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                </section>
            </div>
        </main>
    )
}