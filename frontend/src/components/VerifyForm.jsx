import { useState } from 'react';
import { useNavigate } from 'react-router-dom';

export default function VerifyForm({ email }) {
    const [codigo, setCodigo] = useState('');
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const navigate = useNavigate();

    async function handleSubmit(e) {
        e.preventDefault();
        setError(null);

        if (!codigo) {
            setError('Código obligatorio');
            return;
        }

        setIsLoading(true);
        const res = await fetch('/api/auth/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, codigo }),
        });
        setIsLoading(false);

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
        </div>
    );
}
