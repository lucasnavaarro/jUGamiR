export default function IconoMedico({ nivel }) {
    if (nivel === 'facil') return (
        <svg viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg" width="44" height="44" style={{ flexShrink: 0 }}>
            <path d="M16 64 C16 36, 48 36, 48 64" fill="#F8F9FA" />
            <path d="M26 40 L32 46 L38 40 L38 64 L26 64 Z" fill="#A2D2FF" />
            <circle cx="32" cy="24" r="12" fill="#FFCDB2" />
            <path d="M20 20 C20 8, 44 8, 44 20 C44 24, 20 24, 20 20" fill="#4A4E69" />
            <circle cx="28" cy="25" r="1.5" fill="#1E1E24" />
            <circle cx="36" cy="25" r="1.5" fill="#1E1E24" />
            <path d="M24 45 C24 58, 40 58, 40 45" fill="none" stroke="#4B5563" strokeWidth="2.5" />
            <circle cx="40" cy="45" r="2.5" fill="#4B5563" />
        </svg>
    );

    if (nivel === 'medio') return (
        <svg viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg" width="44" height="44" style={{ flexShrink: 0 }}>
            <path d="M16 64 C16 36, 48 36, 48 64" fill="#4361EE" />
            <path d="M26 38 L32 47 L38 38 Z" fill="#FFCDB2" />
            <circle cx="32" cy="24" r="12" fill="#FFCDB2" />
            <path d="M20 20 C20 8, 44 8, 44 20 C44 24, 20 24, 20 20" fill="#4A4E69" />
            <circle cx="28" cy="23" r="1.5" fill="#1E1E24" />
            <circle cx="36" cy="23" r="1.5" fill="#1E1E24" />
            <path d="M21 28 L43 28 C43 38, 21 38, 21 28 Z" fill="#4CC9F0" />
            <path d="M21 28 L16 24 M43 28 L48 24" stroke="#4CC9F0" strokeWidth="2" />
        </svg>
    );

    if (nivel === 'dificil') return (
        <svg viewBox="0 0 64 64" xmlns="http://www.w3.org/2000/svg" width="44" height="44" style={{ flexShrink: 0 }}>
            <path d="M16 64 C16 36, 48 36, 48 64" fill="#2A9D8F" />
            <path d="M26 38 L32 47 L38 38 Z" fill="#FFCDB2" />
            <circle cx="32" cy="24" r="12" fill="#FFCDB2" />
            <path d="M19 20 C19 5, 45 5, 45 20 Z" fill="#2A9D8F" />
            <circle cx="28" cy="23" r="1.5" fill="#1E1E24" />
            <circle cx="36" cy="23" r="1.5" fill="#1E1E24" />
            <path d="M21 28 L43 28 C43 38, 21 38, 21 28 Z" fill="#A8DADC" />
            <path d="M19 16 C26 19, 38 19, 45 16" fill="none" stroke="#4A4E69" strokeWidth="2" />
            <circle cx="32" cy="14" r="5" fill="#E5E5E5" />
            <circle cx="32" cy="14" r="2" fill="#FCA311" />
        </svg>
    );

    return null;
}
