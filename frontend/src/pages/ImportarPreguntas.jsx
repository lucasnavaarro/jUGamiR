import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiFetch } from '../services/api';

export default function ImportarPreguntas() {
    const navigate = useNavigate();
    const [archivo, setArchivo] = useState(null);
    const [arrastrando, setArrastrando] = useState(false);
    const [cargando, setCargando] = useState(false);
    const [resultado, setResultado] = useState(null);
    const [error, setError] = useState(null);
    const [archivoZip, setArchivoZip] = useState(null);
    const [arrastandoZip, setArrastandoZip] = useState(false);
    const inputZipRef = useRef(null);
    const inputRef = useRef(null);

    useEffect(() => {
        if (error) {
            const timer = setTimeout(() => {
                setError('');
            }, 5000);
            return () => clearTimeout(timer);
        }
    }, [error]);

    function handleArchivoSeleccionado(file) {
        if (!file) return;
        if (!file.name.endsWith('.csv')) {
            setError('Solo se permiten archivos .csv');
            return;
        }
        setArchivo(file);
        setError('');
        setResultado(null);
    }

    async function handleImportar() {
        if (!archivo) return;
        setCargando(true);
        setError('');
        setResultado(null);
        const formData = new FormData();
        formData.append('file', archivo);

        try {
            const res = await apiFetch(`/api/profesor/preguntas/importar`, {
                method: 'POST',
                body: formData
            });
            if (!res.ok) throw new Error('Error al importar');
            setResultado(await res.json());
            if (archivoZip) {
                const formDataZip = new FormData();
                formDataZip.append('file', archivoZip);
                await apiFetch('/api/profesor/imagenes/zip', { method: 'POST', body: formDataZip });
            }
        } catch {
            setError('Error al importar las preguntas. Comprueba el formato del CSV.');
        } finally {
            setCargando(false);
        }
    }

    return (
        <main className="gestion">
            <div className="gestion__container">
                <div style={{ textAlign: 'left' }}>
                    <button className="crear-partida__back" onClick={() => navigate('/profesor')}>Volver</button>
                </div>
                <h1 className="gestion__titulo">Importar preguntas</h1>
                <p style={{ color: 'var(--text-muted)' }}>
                    El archivo CSV debe tener el mismo formato que <code>preguntas_mir_final.csv</code>
                </p>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginTop: 'var(--space-xs)' }}>
                    Formato de columnas: <code>Asignatura;Identificador;TituloIndice;Enunciado;Opciones;RespuestaCorrecta;Comentario;Anulada;Imagen;Dificultad</code>
                </p>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginTop: 'var(--space-xs)' }}>
                    Si no tiene comentario o imagen, escribe <code>--</code> en ese campo. Si tiene más de una imagen, sepáralas con <code>|</code>. En Dificultad escribe <code>FACIL</code>, <code>MEDIO</code> o <code>DIFICIL</code>.
                </p>
                <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', marginTop: 'var(--space-xs)', wordBreak: 'break-all' }}>
                    Ejemplo: <code>ANATOMIA;MIR 2024-25, P32;1. Neuroanatomía;Enunciado de la pregunta...;1. Opción A. | 2. Opción B. | 3. Opción C. | 4. Opción D.;1;--;NO;--;FACIL</code>
                </p>



                <div
                    className={`importar__zona ${arrastrando ? 'importar__zona--activa' : ''} ${archivo ? 'importar__zona--archivo' : ''}`}
                    onDragOver={e => { e.preventDefault(); setArrastrando(true) }}
                    onDragLeave={() => setArrastrando(false)}
                    onDrop={e => { e.preventDefault(); setArrastrando(false); handleArchivoSeleccionado(e.dataTransfer.files[0]); }}
                    onClick={() => inputRef.current.click()}
                >
                    <input ref={inputRef} type="file" accept=".csv" style={{ display: "none" }}
                        onChange={e => handleArchivoSeleccionado(e.target.files[0])} />
                    <div className="importar__icono">{archivo ? '📄' : '📂'}</div>
                    {archivo ? (
                        <>
                            <p className="importar__nombre">{archivo.name}</p>
                            <p className="importar__hint">Haz clic para cambiar el archivo</p>
                            <button
                                className="btn btn--danger btn--sm"
                                style={{ marginTop: 'var(--space-sm)' }}
                                //El e.stopPropagation() es importante para que al hacer clic en el botón no se abra también el selector de archivos.
                                onClick={e => { e.stopPropagation(); setArchivo(null); if (inputRef.current) inputRef.current.value = ''; }}>
                                Quitar archivo
                            </button>
                        </>
                    ) : (
                        <>
                            <p className="importar__texto">Arrastra un archivo CSV aquí</p>
                            <p className="importar__hint">o haz clic para seleccionarlo</p>
                        </>
                    )}
                </div>

                <div
                    className={`importar__zona importar__zona--sm ${arrastandoZip ? 'importar__zona--activa' : ''} ${archivoZip ? 'importar__zona--archivo' : ''}`}
                    onClick={() => inputZipRef.current.click()}
                    onDragOver={e => { e.preventDefault(); setArrastandoZip(true); }}
                    onDragLeave={() => setArrastandoZip(false)}
                    onDrop={e => {
                        e.preventDefault();
                        setArrastandoZip(false);
                        const f = e.dataTransfer.files[0];
                        if (f && f.name.endsWith('.zip')) setArchivoZip(f);
                        else if (f) setError('Solo se permiten archivos .zip');
                    }}
                >
                    <input ref={inputZipRef} type="file" accept=".zip" style={{ display: 'none' }}
                        onChange={e => {
                            const f = e.target.files[0];
                            if (f && f.name.endsWith('.zip')) setArchivoZip(f);
                        }} />
                    <div className="importar__icono">{archivoZip ? '🗜️' : '🖼️'}</div>
                    {archivoZip ? (
                        <>
                            <p className="importar__nombre">{archivoZip.name}</p>
                            <p className="importar__hint">Haz clic para cambiar</p>
                            <button className="btn btn--danger btn--sm" style={{ marginTop: 'var(--space-sm)' }}
                                onClick={e => { e.stopPropagation(); setArchivoZip(null); if (inputZipRef.current) inputZipRef.current.value = ''; }}>
                                Quitar
                            </button>
                        </>
                    ) : (
                        <>
                            <p className="importar__texto">ZIP de imágenes (opcional)</p>
                            <p className="importar__hint">Solo si el CSV incluye imágenes</p>
                        </>
                    )}
                </div>

                <p className="imporat__hint" style={{ marginTop: 'var(--space-xs)' }}>
                    La carpeta raíz del ZIP debe coincidir con las rutas del CSV. Por ejemplo, si el CSV tiene <code>imagenes/anatomia/pulmon.png</code>, el ZIP debe contener la carpeta <code>imagenes/anatomia</code> con el archivo dentro.
                </p>

                {error && <p className="gestion__error">{error}</p>}

                {archivo && !cargando && !resultado && (
                    <button className="btn btn--primary btn--lg" onClick={handleImportar}>
                        Importar preguntas
                    </button>
                )}

                {cargando && (
                    <div className="importar__cargando">
                        <div className="importar__spinner" />
                        <p>Importando... esto puede tardar unos segundos</p>
                    </div>
                )}

                {resultado && (
                    <div className="importar__resultado">
                        <h2 className="gestion__subtitulo">Resultado</h2>
                        <div className="importar__stats">
                            <div className="importar__stat importar__stat--ok">
                                <span className="importar__stat-num">{resultado.añadidas}</span>
                                <span className="importar__stat-label">Añadidas</span>
                            </div>
                            <div className="importar__stat importar__stat--dup">
                                <span className="importar__stat-num">{resultado.yaExistentes}</span>
                                <span className="importar__stat-label">Ya existían</span>
                            </div>
                            <div className="importar__stat importar__stat--err">
                                <span className="importar__stat-num">{resultado.errores}</span>
                                <span className="importar__stat-label">Errores</span>
                            </div>
                        </div>
                        {resultado.detalleErrores?.length > 0 && (
                            <details className="importar__detalle">
                                <summary>Ver detalle</summary>
                                <pre className="importar__log">{resultado.detalleErrores.join('\n\n')}</pre>
                            </details>
                        )}
                        <button className="btn btn--outline" onClick={() => { setArchivo(null); setArchivoZip(null); setResultado(null); }}>
                            Importar otro archivo
                        </button>
                    </div>
                )}
            </div>
        </main>
    );
}