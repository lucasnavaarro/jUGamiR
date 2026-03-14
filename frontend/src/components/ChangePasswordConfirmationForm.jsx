import { Link } from "react-router-dom";

export default function ChangePasswordConfirmationForm() {
    return (
        <div className="auth-card" style={{ textAlign: 'center' }}>
            <svg xmlns="http://www.w3.org/2000/svg" width="96" height="96" viewBox="0 0 64 64" style={{ marginBottom: '1.5rem', filter: 'drop-shadow(0 4px 12px rgba(21,128,61,0.4))' }}>
                {/* Círculo verde */}
                <circle cx="32" cy="32" r="32" fill="#15803d" />
                {/* Tick blanco */}
                <polyline
                    points="18,33 27,43 46,22"
                    fill="none"
                    stroke="white"
                    strokeWidth="5"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                />
            </svg>
            <p style={{ marginBottom: '1.5rem' }}>Tu contraseña ha sido cambiada exitosamente</p>
            <Link to="/login" className="auth-link">Volver al inicio de sesión</Link>
        </div>
    );
}