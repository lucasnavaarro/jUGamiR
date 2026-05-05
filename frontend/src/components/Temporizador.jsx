import { useState, useEffect } from 'react';

export default function Temporizador({ segundos, onFin }) {

    const [restante, setRestante] = useState(segundos);

    useEffect(() => {
        setRestante(segundos); //Se hace reset cuando sale una nueva pregunta

        const intervalo = setInterval(() => {
            setRestante(prev => {
                if (prev <= 1) { // Si queda 1 o menos, se para
                    clearInterval(intervalo); // detiene el contador
                    onFin();
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);

        return () => clearInterval(intervalo);
    }, [segundos]);

    const mins = String(Math.floor(restante / 60)).padStart(2, '0');
    const secs = String(restante % 60).padStart(2, '0');

    return (
        <div className={`temporizador ${restante <= 15 ? 'temporizador--urgente' : ''}`}>
            <span className="temporizador__tiempo">{mins}:{secs}</span>
        </div>
    )
} 