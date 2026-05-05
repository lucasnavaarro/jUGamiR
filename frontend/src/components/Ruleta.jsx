export default function Ruleta({ categorias, categoriaSeleccionada, girando }) {

    return (
        <div className={`ruleta ${girando ? 'ruleta--girando' : ''}`}>
            <div className="ruleta__rueda">
                {categorias.map((cat, i) => {
                    const angulo = (360 / categorias.length) * i;
                    const seleccionada = categoriaSeleccionada?.id === cat.id;

                    return (
                        <div
                            key={cat.id}
                            className={`ruleta__seccion ${seleccionada ? 'ruleta__seccion--activa' : ''}`}
                            style={{
                                backgroundColor: cat.color,
                                transform: `rotate(${angulo}deg) skewY(-${90 - 360 / categorias.length}deg)`
                            }}
                        />
                    );
                })}
            </div>
            {categoriaSeleccionada && !girando && (
                <p className="ruleta__resultado">
                    {categoriaSeleccionada.nombre}
                </p>
            )}
        </div>
    );
}