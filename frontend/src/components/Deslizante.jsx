import { useId } from 'react';

export default function Deslizante({ label, min, max, step = 1, value, text, onSliderChange, onTextChange }) {
    const id = useId();
    return (
        <div className="crear-partida__field">
            <label htmlFor={id} className="crear-partida__group-title">{label}</label>
            <div className="crear-partida__range-row">
                <input
                    id={id} type="range" min={min} max={max} step={step} value={value}
                    onChange={e => onSliderChange(Number(e.target.value))}
                    className="crear-partida__range"
                />
                <input
                    type="number" aria-label={`${label} (valor)`} min={min} max={max} value={text}
                    onChange={e => onTextChange(e.target.value)}
                    className="crear-partida__number-input"
                />
            </div>
        </div>
    );
}
