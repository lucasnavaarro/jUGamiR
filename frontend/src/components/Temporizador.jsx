import { useState, useEffect } from 'react';

export default function Temporizador({ venceEn }) {
    const [restante, setRestante] = useState(0);
    useEffect(() => {
        const update = () => setRestante(Math.max(0, Math.ceil((Date.parse(venceEn) - Date.now()) / 1000)));
        update();
        const interval = setInterval(update, 250);
        return () => clearInterval(interval);
    }, [venceEn]);
    const mins = String(Math.floor(restante / 60)).padStart(2, '0');
    const secs = String(restante % 60).padStart(2, '0');
    return <div className={`temporizador ${restante <= 15 ? 'temporizador--urgente' : ''}`}>
        <span className="temporizador__tiempo">{mins}:{secs}</span>
    </div>;
}
