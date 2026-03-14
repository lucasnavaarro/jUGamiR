import { useState } from "react";
import { Link } from 'react-router-dom';

export default function ForgotPasswordForm() {

    const [email, setEmail] = useState('');
    const [enviado, setEnviado] = useState(false);
    const [error, setError] = useState(null);
    const [isLoading, setIsLoading] = useState(false);

    async function handleSubmit(e) {
        e.preventDefault(); //Para no recargar la página al enviar el formulario
        setError(null); //Para limpiar el error anterior

        if (!email) {
            setError('Introduce el email asociado a tu cuenta');
            return;
        }

        setIsLoading(true);
        const res = await fetch('/api/auth/forgot-password',
            {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' }, //Tipo de contenido
                body: JSON.stringify({ email }), //Datos que enviamos
            }
        );

        setIsLoading(false);

        if (res.ok)
            setEnviado(true);
        else
            setError("No existe ninguna cuenta asociada a ese email");

    }

    return (
        <div className="auth-card">
            {enviado ? (
                <>
                    {/* Mensaje de confirmación */}
                    <h1 className="auth-card__title">Comprueba tu correo electrónico</h1>
                    <p style={{ marginBottom: '1.5rem' }}>Te hemos enviado un enlace para restablecer tu contraseña. El enlace caducará en 15 minutos</p>
                    <Link to="/login" className="auth-link">Volver al inicio de sesión</Link>
                </>
            ) : (
                <>
                    {/* Formulario de recuperación de contraseña */}
                    <h1 className="auth-card__title">Recuperar contraseña</h1>
                    <p style={{ marginBottom: '1rem' }}>Introduce el email asociado a tu cuenta y te enviaremos un enlace para restablecer tu contraseña</p>
                    <form onSubmit={handleSubmit} className="auth-form" noValidate>
                        <div className="auth-form__group">
                            <label htmlFor="email">Email</label>
                            <input id="email" type="email" placeholder="Intrduce tu email" value={email} onChange={e => setEmail(e.target.value)} />
                        </div>
                        {error && <p className="auth-form__error">{error}</p>}
                        <button type="submit" className="btn btn--secondary btn--full" disabled={isLoading}>
                            {isLoading ? 'Enviando...' : 'Enviar'}
                            {isLoading && <span className="spinner"></span>}
                        </button>
                        <Link to="/login" className="auth-link">Volver al inicio de sesión</Link>
                    </form>
                </>
            )}
        </div>
    );
}