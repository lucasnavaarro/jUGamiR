import { useState, useEffect } from 'react';
import { apiFetch } from '../services/api';

const iconMap = {
    'Sentidos y Metabolismo': '🧠',
    'Cardio, Sangre y Piel': '❤️',
    'Riñón, Infección y Autoinmunidad': '🦴',
    'Pediatría, Primaria y Reproducción': '👶',
    'Cuerpo y Mente': '🫁',
    'Ciencias Básicas': '🧬',
};

export default function Categories() {

    const [categorias, setCategorias] = useState([]);

    useEffect(() => {
        apiFetch('/api/categorias/con-asignaturas')
            .then(res => res.json())
            .then(setCategorias)
            .catch(() => { });
    }, []);
    return (
        <section className="categories" id="categories" aria-label="Categorías del juego">
            <div className="container">
                <div className="section-header">
                    <h2>Elige tu especialidad</h2>
                    <p>Cada partida pone a prueba tus conocimientos en estas 6 categorías</p>
                </div>

                <div className="categories__grid">
                    {categorias.map((cat) => (
                        <article className="cat-card" tabIndex={0} aria-label={`Categoría ${cat.nombre}`} key={cat.id}>
                            <div className="cat-card__icon">{iconMap[cat.nombre] || '📚'}</div>
                            <h3 className="cat-card__name">{cat.nombre}</h3>
                            <p className="cat-card__info">
                                {cat.asignaturas
                                    .map(a => a.toLowerCase()
                                        .split(' ')
                                        .map(word => word.split('-').map((part, i) => {
                                            if (/^[ivxlcdm]+$/.test(part)) return part.toUpperCase();
                                            return part.charAt(0).toUpperCase() + part.slice(1);
                                        }).join('-'))
                                        .join(' ')
                                    )
                                    .join(' · ')}
                            </p>
                            <div className="cat-card__bar"></div>
                        </article>
                    ))}
                </div>
            </div>
        </section>
    );
}
