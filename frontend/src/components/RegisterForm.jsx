import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";

export default function RegisterForm() {

    const navigate = useNavigate();
    const [tipo, setTipo] = useState('jugador');
    const [nombre, setNombre] = useState('');
    const [apellidos, setApellidos] = useState('');
    const [email, setEmail] = useState('');
    const [dni, setDni] = useState('');
    const [password, setPassword] = useState('');
    const [confirmar, setConfirmar] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmar, setShowConfirmar] = useState(false);
    const [nick, setNick] = useState('');
    const [departamento, setDepartamento] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    async function handleSubmit(e) {
        e.preventDefault(); // Evita que la página se recargue
        setError(null); // Limpia el error anterior

        if (!nombre) {
            setError('El campo nombre es obligatorio');
            return;
        }

        if (!apellidos) {
            setError('El campo apellidos es obligatorio');
            return;
        }

        if (tipo === 'profesor' && !email.endsWith('@ugr.es')) {
            setError('Debes registrarte con el email de la UGR (@ugr.es)');
            return;
        }

        if (!email) {
            setError('El campo email es obligatorio');
            return;
        }

        if (!dni) {
            setError('El campo DNI es obligatorio');
            return;
        }

        if (!/^\d{8}[A-Za-z]$/.test(dni)) {
            setError('El DNI no es válido');
            return;
        }

        if (!password) {
            setError('El campo contraseña es obligatorio');
            return;
        }

        if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/.test(password)) {
            setError('La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial');
            return;
        }

        if (password !== confirmar) {
            setError('Las contraseñas no coinciden');
            return;
        }

        if (tipo === 'jugador' && !nick) {
            setError('El campo nick es obligatorio');
            return;
        }

        if (tipo === 'profesor' && !departamento) {
            setError('El campo departamento es obligatorio');
            return;
        }

        const endpoint = tipo === 'jugador' ? '/api/auth/register/jugador' : '/api/auth/register/profesor';
        const body = tipo === 'jugador' ? { nombre, apellidos, email, dni, password, nick } : { nombre, apellidos, email, dni, password, departamento };

        setIsLoading(true);
        const res = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }, //Tipo de contenido que se envía
            body: JSON.stringify(body), //Datos que enviamos
        });
        setIsLoading(false);

        if (res.ok) {
            navigate('/register-confirmation');
        } else {
            const mensaje = await res.text();
            setError('Error al crear al cuenta' || mensaje);
        }
    }

    return (
        <div className="auth-card">
            <Link to="/" className="join-card__back">Cancelar</Link>
            <div className="auth-card__header">
                <h1 className="auth-card__title"> Crear cuenta</h1>
            </div>
            <div className="auth-tabs">
                {/* Botones para cambiar entre jugador y profesor */}
                <button type="button" className={`auth-tab ${tipo === 'jugador' ? 'auth-tab--active' : ''}`} onClick={() => setTipo('jugador')}>
                    Jugador
                </button>
                <button type="button" className={`auth-tab ${tipo === 'profesor' ? 'auth-tab--active' : ''}`} onClick={() => setTipo('profesor')}>
                    Profesor
                </button>
            </div>

            <form onSubmit={handleSubmit} className="auth-form" noValidate>

                {/* Campos comunes */}
                <div className="auth-form__group">
                    <label htmlFor="nombre">Nombre</label>
                    <input
                        id="nombre"
                        type="text"
                        placeholder="Introduce tu nombre"
                        value={nombre}
                        onChange={e => setNombre(e.target.value)}
                    />
                </div>
                <div className="auth-form__group">
                    <label htmlFor="apellidos">Apellidos</label>
                    <input
                        id="apellidos"
                        type="text"
                        placeholder="Introduce tus apellidos"
                        value={apellidos}
                        onChange={e => setApellidos(e.target.value)}
                    />
                </div>
                <div className="auth-form__group">
                    <label htmlFor="email">Email</label>
                    <input
                        id="email"
                        type="email"
                        placeholder="Introduce tu email"
                        value={email}
                        onChange={e => setEmail(e.target.value)}
                    />
                </div>
                <div className="auth-form__group">
                    <label htmlFor="dni">DNI</label>
                    <input
                        id="dni"
                        type="text"
                        placeholder="Introduce tu DNI"
                        value={dni}
                        onChange={e => setDni(e.target.value)}
                    />
                </div>
                <div className="auth-form__group">
                    <label htmlFor="password">Contraseña</label>
                    <div className="input-wrapper">
                        <input
                            id="password"
                            type={showPassword ? 'text' : 'password'}
                            placeholder="Introduce tu contraseña"
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                        />
                        <button type="button" className="input-eye" onClick={() => setShowPassword(!showPassword)}>
                            {showPassword ? (
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"
                                    fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    {/* Lineas curvas que dibujan el ojo */}
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                                    {/* Linea que tacha el ojo*/}
                                    <line x1="1" y1="1" x2="23" y2="23" />
                                    {/* Círculo que simula la pupila */}
                                    <circle cx="12" cy="12" r="3" />
                                </svg>
                            ) : (
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"
                                    fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    {/* Linea curva que dibuja el ojo */}
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                    {/* Círculo que simula la pupila */}
                                    <circle cx="12" cy="12" r="3" />
                                </svg>
                            )}
                        </button>
                    </div>
                </div>
                <div className="auth-form__group">
                    <label htmlFor="confirmar">Confirmar contraseña</label>
                    <div className="input-wrapper">
                        <input
                            id="confirmar"
                            type={showConfirmar ? 'text' : 'password'}
                            placeholder="Repite la contraseña"
                            value={confirmar}
                            onChange={e => setConfirmar(e.target.value)}
                        />
                        <button type="button" className="input-eye" onClick={() => setShowConfirmar(!showConfirmar)}>
                            {showConfirmar ? (
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"
                                    fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    {/* Lineas curvas que dibujan el ojo */}
                                    <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94" />
                                    <path d="M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19" />
                                    {/* Linea que tacha el ojo*/}
                                    <line x1="1" y1="1" x2="23" y2="23" />
                                    {/* Círculo que simula la pupila */}
                                    <circle cx="12" cy="12" r="3" />
                                </svg>
                            ) : (
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24"
                                    fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    {/* Linea curva que dibuja el ojo */}
                                    <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                    {/* Círculo que simula la pupila */}
                                    <circle cx="12" cy="12" r="3" />
                                </svg>
                            )}
                        </button>
                    </div>
                </div>
                {/* Campos específicos para cada tipo */}
                {tipo === 'jugador' ? (
                    <div className="auth-form__group">
                        <label htmlFor="nick">Nombre de usuario</label>
                        <input
                            id="nick"
                            type="text"
                            placeholder="Introduce tu nombre de usuario"
                            value={nick}
                            onChange={e => setNick(e.target.value)}
                        />
                    </div>
                ) : (
                    <div className="auth-form__group">
                        <label htmlFor="departamento">Departamento</label>
                        <input
                            id="departamento"
                            type="text"
                            placeholder="Introduce tu departamento"
                            value={departamento}
                            onChange={e => setDepartamento(e.target.value)}
                        />
                    </div>
                )}

                {/* Mensaje de error */}
                {error && <p className="auth-form__error">{error}</p>}

                {/* Botón de envío */}
                <button type="submit" className="btn btn--secondary btn--full" disabled={isLoading}>
                    {isLoading ? 'Creando cuenta...' : 'Crear cuenta'}
                    {isLoading && <span className="spinner"></span>}
                </button>

                <Link to="/login" className="btn btn--outline btn--full" id="btn-register">¿Ya tienes una cuenta? Inicia sesión</Link>

            </form>
        </div>
    )
}    