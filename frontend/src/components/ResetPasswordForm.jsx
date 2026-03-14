import { useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

export default function ResetPasswordForm() {

    const [searchParams] = useSearchParams();
    const token = searchParams.get('token');
    const navigate = useNavigate();

    const [newPassword, setNewPassword] = useState('');
    const [confirmar, setConfirmar] = useState('');
    const [error, setError] = useState(null);
    const [showPassword, setShowPassword] = useState(false);
    const [showConfirmar, setShowConfirmar] = useState(false);
    const [isLoading, setIsLoading] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault(); //Para no recargar la página al enviar el formulario
        setError(null); //Para limpiar el error anterior

        if (!newPassword) { setError('Introduce la nueva contraseña'); return; }
        if (newPassword.length < 8) { setError('La contraseña debe tener al menos 8 caracteres'); return; }
        if (newPassword !== confirmar) { setError('Las contraseñas no coinciden'); return; }
        if (!/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/.test(newPassword)) { setError('La contraseña debe tener al menos una mayúscula, una minúscula, un número y un carácter especial'); return; }

        setIsLoading(true);
        const res = await fetch('/api/auth/reset-password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }, //Tipo de contenido
            body: JSON.stringify({ token, nuevaPassword: newPassword }) //Datos que enviamos
        });

        setIsLoading(false);

        if (res.ok)
            navigate('/password-changed');
        else
            setError('Error al restablecer la contraseña');

    }

    return (
        <div className="auth-card">
            <h1 className="auth-card__title">Reestablecer contraseña</h1>
            <form onSubmit={handleSubmit} className="auth-form" noValidate>
                <div className="auth-form__group">
                    <label htmlFor="newPassword">Nueva Contraseña</label>
                    <div className="input-wrapper">
                        <input
                            id="newPassword"
                            type={showPassword ? 'text' : 'password'}
                            placeholder="Introduce tu nueva contraseña"
                            value={newPassword}
                            onChange={e => setNewPassword(e.target.value)}
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
                {/* Mensaje de error. Si hay error lo pinta de rojo */}
                {error && <p className="auth-form__error">{error}</p>}
                {/* Botón de envío */}
                <button type="submit" className="btn btn--secondary btn--full" disabled={isLoading}>
                    {isLoading ? 'Cargando...' : 'Restablecer contraseña'}
                    {isLoading && <span className="spinner"></span>}
                </button>
            </form>
        </div>
    );
}