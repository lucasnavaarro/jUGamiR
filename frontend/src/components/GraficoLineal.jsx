import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';

export default function GraficoLineal({ datos, dataKey, color, titulo, formatter, mensaje }) {

    if (!datos || datos.length < 2) {
        return <p className="stats__grafico-vacio">{mensaje ?? "No hay estadisticas suficientes para generar el gráfico"}</p>
    }

    return (
        <div className="stats__grafico">
            {titulo && <p className="stats__grafico-titulo">{titulo}</p>}
            <ResponsiveContainer width="100%" height={200}>
                <LineChart data={datos} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#33333322" />
                    <XAxis
                        dataKey="fecha"
                        tick={{ fontSize: 11 }}
                    />
                    <YAxis
                        tickFormatter={formatter}
                        tick={{ fontSize: 11 }}
                        width={48}
                    />
                    <Tooltip
                        formatter={(val) => formatter(val)}
                    />
                    <Line
                        type="monotone"
                        dataKey={dataKey}
                        stroke={color}
                        strokeWidth={2}
                        dot={datos.length <= 24}
                        activeDot={{ r: 2 }}
                    />
                </LineChart>
            </ResponsiveContainer>
        </div>
    );
}
