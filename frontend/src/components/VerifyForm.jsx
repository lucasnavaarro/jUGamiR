import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';

export default function VerifyForm({ email, challengeId, onRestart }) {
    const [codigo, setCodigo] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [reenvCod, setReenvCod] = useState('');
    const [resending, setResending] = useState(false);
    const navigate = useNavigate();

    async function handleResend() {
        if (resending) return;
        setResending(true);
        try {
            setReenvCod('');
            const res = await fetch('/api/auth/reenviar-codigo', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ challengeId }),
            });
            setReenvCod(res.ok ? 'Correo reenviado correctamente.' : 'No se pudo reenviar el correo.');
        } catch {
            setReenvCod('No se pudo reenviar el correo. Comprueba tu conexión.');
        } finally {
            setResending(false);
        }
    }

    async function handleSubmit(e) {
        e.preventDefault();
        if (isLoading) return;
        setError(null);

        if (!codigo) {
            setError('Código obligatorio');
            return;
        }

        setIsLoading(true);
        try {
            const res = await fetch('/api/auth/verify', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ challengeId, codigo }),
            });

            if (res.ok) {
                const data = await res.json();
                localStorage.setItem('jwt', data.token);
                localStorage.setItem('rol', data.rol);
                localStorage.setItem('email', data.email);
                localStorage.setItem('nick', data.nick);
                localStorage.setItem('nombre', data.nombre);
                localStorage.setItem('idUsuario', data.idUsuario);

                if (data.rol === "JUGADOR") navigate('/jugador');
                else navigate('/profesor');

            } else {
                setError('Código incorrecto o expirado');
            }
        } catch {
            setError("No se pudo conectar. Comprueba tu conexión y vuelve a intentarlo.");
        } finally {
            setIsLoading(false);
        }
    }

    return (
        <div className="auth-card">
            <div className="auth-card__header">
                <h1 className="auth-card__title"> Verificar cuenta</h1>
            </div>
            <form onSubmit={handleSubmit}>
                <p style={{ marginBottom: '1.5rem' }}>Código enviado a <strong>{email}</strong></p>
                <div className="auth-form__group" style={{ marginBottom: '1rem' }}>
                    <input
                        type="text"
                        placeholder="Código 6 dígitos"
                        aria-label="Código de verificación de seis dígitos"
                        value={codigo}
                        onChange={e => setCodigo(e.target.value)}
                        required
                    />
                </div>

                {error && <p className="auth-form__error">{error}</p>}
                <button type="submit" className="btn btn--secondary btn--full" disabled={isLoading}>
                    {isLoading ? 'Verificando...' : 'Verificar'}
                </button>
            </form>
            {reenvCod && <p className={reenvCod.includes('correctamente') ? 'resend-msg--ok' : 'resend-msg--error'}>{reenvCod}</p>}
            <button type="button" className="auth-link resend-button" onClick={handleResend} disabled={resending}>
                ¿No has recibido ningún correo? Volver a enviar.
            </button>
            <Link to="/login" className="auth-link" onClick={onRestart}>Volver al inicio de sesión</Link>
        </div>
    );
}
