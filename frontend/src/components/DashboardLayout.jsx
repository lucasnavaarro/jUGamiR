import ActionCard from './ActionCard';

export default function DashboardLayout({ nombreUsuario, subtitulo, acciones, gridClass = '' }) {
    return (
        <main>
            <section className="dashboard__hero">
                <div className="container dashboard__hero-inner">
                    <h1 className="dashboard__title">
                        Bienvenido, <span>{nombreUsuario}</span>
                    </h1>
                    <p className="dashboard__subtitle">{subtitulo}</p>
                </div>
            </section>

            <section className="dashboard__actions">
                <div className="container">
                    <div className={`dashboard__grid ${gridClass}`}>
                        {acciones.map((accion) => (
                            <ActionCard key={accion.id} {...accion} />
                        ))}
                    </div>
                </div>
            </section>
        </main>
    );
}
