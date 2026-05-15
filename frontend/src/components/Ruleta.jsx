import { useEffect, useRef, useState } from 'react';
const ANGULO_COMPLETO = 360;

export default function Ruleta({ categorias, categoriaSeleccionada }) {
    const [rotacion, setRotacion] = useState(0);
    const [animando, setAnimando] = useState(false);
    const rotRef = useRef(0); // Estado para almacenar la rotación acumulada de la rueda
    const prevCatIdRef = useRef(null); // Estado para almacenar la categoría seleccionada, para evitar recalcular si la misma categoría llega dos veces seguidas.

    useEffect(() => {

        if (!categoriaSeleccionada) {
            prevCatIdRef.current = null;
            return;
        }
        //Si la categoría que acaba de llegar es la misma que ya procesamos antes, salimos sin hacer nada.
        if (prevCatIdRef.current === categoriaSeleccionada.id) return;
        //Guardamos la categoría seleccionada.
        prevCatIdRef.current = categoriaSeleccionada.id;

        //Buscamos en qué posición del array está la categoría ganadora
        const indice = categorias.findIndex(c => c.id === categoriaSeleccionada.id);
        if (indice === -1) return;
        //Calcula cuántos grados ocupa cada sector en la ruleta
        const sectorAngle = ANGULO_COMPLETO / categorias.length;
        //Calculamos el punto central (donde apuntaría la flecha) del sector correspondiente a la categoría ganadora
        const sectorMid = sectorAngle * indice + sectorAngle / 2;
        //Calcula cuántos grados hay que rotar para llevar ese punto central hasta el ángulo 0 (arriba, donde está la flecha). 
        const toTop = (ANGULO_COMPLETO - sectorMid + ANGULO_COMPLETO) % ANGULO_COMPLETO;
        //Calcula cuál es la posición actual de la rueda dentro de una vuelta completa. Si la rueda ya lleva 3840 grados acumulados: 3840 % 360 = 240 grados.
        const currentMod = rotRef.current % ANGULO_COMPLETO;
        //Calcula cuántos grados adicionales hay que girar desde la posición actual hasta llegar al ángulo correcto   
        const extra = (toTop - currentMod + ANGULO_COMPLETO) % ANGULO_COMPLETO;
        //Suma 1800 grados (5 vueltas completas) más los grados extra necesarios para quedar en la posición correcta. 
        const final = rotRef.current + 1800 + extra;
        //Guardamos ese ángulo final como el nuevo punto de partida para la próxima vez.
        rotRef.current = final;

        setAnimando(true);
        //Esperamos un frame del navegador antes de actualizar la rotación, para que React haya terminado de renderizar y la transición CSS se active correctamente.
        requestAnimationFrame(() => setRotacion(final));

    }, [categoriaSeleccionada]);

    //Por cada categoria, cat es el objeto e i es su posición en el array
    const gradiente = categorias.map((cat, i) => {
        //Calcula el ángulo donde empieza el sector.
        const start = (ANGULO_COMPLETO / categorias.length) * i;
        //Calcula el ángulo donde termina el sector.
        const end = (ANGULO_COMPLETO / categorias.length) * (i + 1);

        //Ejemplo "#FF0000 120deg 180deg"`, que significa "pinta de rojo desde 120 hasta 180 grados".
        return `${cat.color} ${start}deg ${end}deg`
    }).join(', ');

    return (
        <div className="ruleta-contenedor">
            <div className="ruleta__flecha">▼</div>
            <div className={`ruleta ${!animando && categoriaSeleccionada ? 'ruleta--iluminada' : ''}`}
                style={{ '--cat-color': categoriaSeleccionada?.color }}
            >
                <div
                    className={`ruleta__rueda ${animando ? 'ruleta__rueda--girando' : ''}`}
                    style={{
                        '--rotacion': `${rotacion}deg`,
                        background: `conic-gradient(${gradiente})`
                    }}
                    onTransitionEnd={() => setAnimando(false)}
                />
            </div>
            {categoriaSeleccionada && !animando && (
                <p className="ruleta__resultado">
                    {categoriaSeleccionada.nombre}
                </p>
            )}
        </div>
    );
}