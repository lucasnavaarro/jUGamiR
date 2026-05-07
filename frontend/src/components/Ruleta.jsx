import { useEffect, useRef, useState } from 'react';
const ANGULO_COMPLETO = 360;

export default function Ruleta({ categorias, categoriaSeleccionada }) {
    const [rotacion, setRotacion] = useState(0);
    const [animando, setAnimando] = useState(false);
    const rotRef = useRef(0);
    const prevCatIdRef = useRef(null);

    useEffect(() => {

        if (!categoriaSeleccionada) {
            prevCatIdRef.current = null;
            return;
        }
        if (prevCatIdRef.current === categoriaSeleccionada.id) return;
        prevCatIdRef.current = categoriaSeleccionada.id;

        const indice = categorias.findIndex(c => c.id === categoriaSeleccionada.id);
        if (indice === -1) return;
        const sectorAngle = ANGULO_COMPLETO / categorias.length;
        const sectorMid = sectorAngle * indice + sectorAngle / 2;
        const toTop = (ANGULO_COMPLETO - sectorMid + ANGULO_COMPLETO) % ANGULO_COMPLETO;
        const currentMod = rotRef.current % ANGULO_COMPLETO;
        const extra = (toTop - currentMod + ANGULO_COMPLETO) % ANGULO_COMPLETO;
        const final = rotRef.current + 1800 + extra;
        rotRef.current = final;

        setAnimando(true);
        requestAnimationFrame(() => setRotacion(final));

    }, [categoriaSeleccionada]);

    return (
        <div className="ruleta-contenedor">
            <div className="ruleta__flecha">▼</div>
            <div className="ruleta">
                <div
                    className={`ruleta__rueda ${animando ? 'ruleta__rueda--girando' : ''}`}
                    style={{ '--rotacion': `${rotacion}deg` }}
                    onTransitionEnd={() => setAnimando(false)}
                >
                    {categorias.map((cat, i) => {
                        const angulo = (ANGULO_COMPLETO / categorias.length) * i;
                        const seleccionada = categoriaSeleccionada?.id === cat.id;

                        return (
                            <div
                                key={cat.id}
                                className={`ruleta__seccion ${seleccionada ? 'ruleta__seccion--activa' : ''}`}
                                style={{
                                    backgroundColor: cat.color,
                                    transform: `rotate(${angulo}deg) skewY(-${90 - ANGULO_COMPLETO / categorias.length}deg)`
                                }}
                            />
                        );
                    })}
                </div>
            </div>
            {categoriaSeleccionada && !animando && (
                <p className="ruleta__resultado">
                    {categoriaSeleccionada.nombre}
                </p>
            )}
        </div>
    );
}