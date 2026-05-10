export default function Deslizante({ label, min, max, step = 1, value, text, onSliderChange, onTextChange }) {
    return (
        <div className="crear-partida__field">
            <label className="crear-partida__group-title">{label}</label>
            <div className="crear-partida__range-row">
                <input
                    type="range" min={min} max={max} step={step} value={value}
                    onChange={e => onSliderChange(Number(e.target.value))}
                    className="crear-partida__range"
                />
                <input
                    type="number" value={text}
                    onChange={e => onTextChange(e.target.value)}
                    className="crear-partida__number-input"
                />
            </div>
        </div>
    );
}
