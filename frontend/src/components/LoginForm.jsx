import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';


export default function LoginForm({ onEmailSent }) {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [showPassword, setShowPassword] = useState(false);
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault();
        setError(null);


        if (!email && !password) {
            setError('El email y la contraseña son obligatorios');
            return;
        }

        if (!email) {
            setError('El email es obligatorio');
            return;
        }

        if (!password) {
            setError('La contraseña es obligatoria');
            return;
        }

        setIsLoading(true);
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password }),
        });
        setIsLoading(false);

        if (res.ok) {
            onEmailSent(email);
        } else {
            setError('Email o contraseña incorrectos');
        }
    }


    return (

        <div className="auth-card">
            <Link to="/" className="join-card__back">Cancelar</Link>
            <div className="auth-card__header">
                <h1 className="auth-card__title"> Iniciar sesión</h1>
            </div>

            <form onSubmit={handleSubmit} className="auth-form" noValidate>
                <div className="auth-form__group">
                    <label htmmlFor="email">Email</label>
                    <input
                        id="email"
                        type="email"
                        placeholder="Introduce tu correo electrónico"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}

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
                            onChange={(e) => setPassword(e.target.value)}
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

                <div style={{ textAlign: 'right' }}>
                    <Link to="/forgot-password" className="auth-link">He olvidado mi contraseña</Link>
                </div>

                {error && <p className="auth-form__error">{error}</p>}
                <button type="submit" className="btn btn--secondary btn--full" disabled={isLoading}>{isLoading ? 'Entrando...' : 'Entrar'}</button>
                <Link to="/register" className="btn btn--outline btn--full" id="btn-register">Crear una cuenta</Link>
            </form>
        </div>
    );
}
