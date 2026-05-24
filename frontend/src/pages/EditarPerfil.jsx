import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../services/api';

function Campo({ label, name, type = 'text', form, setForm }) {
    return (
        <>
            <label className="gestion__label">{label}</label>
            <input
                className="gestion__input"
                type={type}
                value={form[name]}
                onChange={e => setForm(p => ({ ...p, [name]: e.target.value }))}
            />
        </>
    );
}

export default function EditarPerfil() {
    const navigate = useNavigate();
    const [form, setForm] = useState({
        nombre: '',
        apellidos: '',
        email: '',
        nick: '',
        departamento: '',
    });

    const [rol, setRol] = useState('');
    const [error, setError] = useState('');
    const [exito, setExito] = useState('');
    const [loading, setLoading] = useState(false);
    const [emailContrasena, setEmailContrasena] = useState('');

    useEffect(() => {
        async function cargarPerfil() {
            try {
                const res = await apiFetch('/api/usuario/perfil');
                const data = await res.json();
                setRol(data.rol);
                setForm({
                    nombre: data.nombre,
                    apellidos: data.apellidos,
                    email: data.email,
                    nick: data.nick || '',
                    departamento: data.departamento || ''
                });
                setEmailContrasena(data.email);
            } catch (err) {
                setError('Error al cargar el perfil');
            }
        }
        cargarPerfil();
    }, []);

    useEffect(() => {
        if (error || exito) {
            const t = setTimeout(() => { setError(''); setExito(''); }, 5000);
            return () => clearTimeout(t);
        }
    }, [error, exito]);

    async function handleGuardar(e) {
        e.preventDefault();
        if (!form.nombre.trim() || !form.apellidos.trim() || !form.email.trim()) {
            setError('Nombre, apellidos y email son obligatorios');
            return;
        }
        setLoading(true);
        setError('');
        try {
            const res = await apiFetch('/api/usuario/perfil', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(form)
            });
            if (!res.ok) {
                const data = await res.json().catch(() => ({}));
                throw new Error(data.message || 'Error al guardar');
            }

            //Si email cambia, el backend devuelve unu nuevo JWT
            const contentType = res.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                const data = await res.json();
                if (data.token) localStorage.setItem('jwt', data.token);
            }
            setExito('Perfil actualizado correctamente');
        } catch (err) {
            setError(err.message || 'Error al guardar');
        } finally {
            setLoading(false);
        }
    }

    async function handleCambiarContrasena(e) {
        try {
            await apiFetch('/api/auth/forgot-password', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email: emailContrasena })
            });
            setExito('Revisa tu correo para cambiar la contraseña');
        } catch {
            setError('Error al enviar el correo');
        }
    }


    return (
        <main className="gestion">
            <div className="gestion__container">
                <div style={{ textAlign: 'left' }}>
                    <button className="crear-partida__back" onClick={() => navigate(-1)}>Volver</button>
                </div>
                <h1 className="gestion__titulo">Editar perfil</h1>

                {error && <p className="gestion__error">{error}</p>}
                {exito && <p className="gestion__exito">{exito}</p>}

                <form className="gestion__form" onSubmit={handleGuardar}>
                    <Campo label="Nombre" name="nombre" form={form} setForm={setForm} />
                    <Campo label="Apellidos" name="apellidos" form={form} setForm={setForm} />
                    <Campo label="Email" name="email" type="email" form={form} setForm={setForm} />
                    {rol === 'JUGADOR' && <Campo label="Nick" name="nick" form={form} setForm={setForm} />}
                    {rol === 'PROFESOR' && <Campo label="Departamento" name="departamento" form={form} setForm={setForm} />}


                    <button className="btn btn--primary" type="submit" disabled={loading}>
                        {loading ? 'Guardando...' : 'Guardar cambios'}
                    </button>
                </form>

                <hr style={{ margin: 'var(--space-lg) 0', borderColor: 'var(--border)' }} />

                <h2 className="gestion__titulo">Contraseña</h2>
                <p className="gestion__label">
                    Te enviaremos un enlace a tu correo para que puedas cambiarla.
                </p>
                <button className="btn btn--outline" onClick={handleCambiarContrasena}>
                    Cambiar contraseña
                </button>
            </div>
        </main>
    );
}