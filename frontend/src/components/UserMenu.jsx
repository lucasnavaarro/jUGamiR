import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';

export default function UserMenu() {

    const [open, setOpen] = useState(false);
    const navigate = useNavigate();
    const ref = useRef(null);

    useEffect(() => {
        function handleClickOutside(e) {
            if (ref.current && !ref.current.contains(e.target)) {
                setOpen(false);
            }
        }

        document.addEventListener("mousedown", handleClickOutside);
        return () => document.removeEventListener("mousedown", handleClickOutside);
    }, []);

    async function handleLogout() {

        await fetch("/api/auth/logout", {
            method: "POST",
        });
        localStorage.removeItem("jwt");
        localStorage.removeItem("rol");
        navigate("/");
    }

    return (
        <div className="user-menu" ref={ref}>
            <button className="user-menu__avatar" onClick={() => setOpen(!open)} aria-label="Menú de usuario">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 12c2.7 0 4.8-2.1 4.8-4.8S14.7 2.4 12 2.4 7.2 4.5 7.2 7.2 9.3 12 12 12zm0 2.4c-3.2 0-9.6 1.6-9.6 4.8v2.4h19.2v-2.4c0-3.2-6.4-4.8-9.6-4.8z" />
                </svg>
            </button>

            {open && (
                <div className="user-menu__dropdown">
                    <button onClick={() => { navigate("/perfil"); setOpen(false); }}>👤 Editar perfil</button>
                    <button onClick={() => { navigate("/crear-partida"); setOpen(false); }}>🎮 Crear partida</button>
                    <button onClick={() => { navigate("/unirse-partida"); setOpen(false); }}>🏆 Unirse a partida</button>
                    <hr className="user-menu__divider" />
                    <button className="user-menu__logout" onClick={handleLogout}>🚪 Cerrar sesión</button>
                </div>
            )}
        </div>
    );
}