import { useRef, useState } from 'react'; //para poder acceder a los inputs del DOM
import { useNavigate, Link } from 'react-router-dom';
import { apiFetch } from '../services/api';


const LONGITUD_CODIGO = 6;

export default function UnirsePartidaForm() {

    const [digits, setDigits] = useState(Array(LONGITUD_CODIGO).fill('')); // Array de 6 inputs para el código, primero vacío
    const [error, setError] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    // Refs para los inputs
    const inputs = useRef([]);
    const navigate = useNavigate();
    const volverA = localStorage.getItem('rol') === 'JUGADOR' ? '/jugador' : '/profesor';

    //Función que se ejecuta cada vez que el usuario escribe en un cuadradito
    function handleChange(index, value) {

        const char = value.replace(/[^0-9]/g, '').slice(-1); //Elimina todo lo que no sea un número y se queda con el último carácter
        const next = [...digits]; //Copia del array
        next[index] = char;
        setDigits(next);
        setError('');
        if (char && index < LONGITUD_CODIGO - 1) {
            inputs.current[index + 1].focus(); //Pasa al siguiente cuadrado
        }

    }

    function handleKeyDown(index, e) {
        //Si la tecla es borrar
        if (e.key === 'Backspace') {
            //Si el cuadrado tiene un digito, lo borra
            if (digits[index]) {
                const next = [...digits];
                next[index] = '';
                setDigits(next);
                //Si el cuadrado está vacío, pasa al anterior
            } else if (index > 0) {
                inputs.current[index - 1].focus();
            }
        }
    }

    function handlePaste(e) {
        e.preventDefault(); //Para que no intente meter todo lo copiado en un solo cuadrado
        const pasted = e.clipboardData.getData('text').replace(/[^0-9]/g, '');
        if (pasted.length === 0) return;
        const next = Array(LONGITUD_CODIGO).fill('');
        for (let i = 0; i < LONGITUD_CODIGO && i < pasted.length; i++) {
            next[i] = pasted[i];
        }
        setDigits(next);
        const lastFilled = Math.min(pasted.length, LONGITUD_CODIGO - 1); //Calcula cual es el último cuadrado que se ha rellenado
        inputs.current[lastFilled].focus();

    }

    async function handleSubmit(e) {
        e.preventDefault(); //Para que el navegador no recargue la página
        const codigo = digits.join(''); //Une los digitos del array en un string
        if (codigo.length < LONGITUD_CODIGO) {
            setError('Introduce el código completo');
            return;
        }
        setIsLoading(true);
        const res = await apiFetch(`/api/lobby/unirse/privada/${codigo}`, {
            method: 'POST',
        });
        setIsLoading(false);
        if (res && res.ok) {
            const data = await res.json();
            navigate(`/partida/${data.idPartida}`);
        } else if (res && res.status === 404) {
            setError('Código de partida no encontrado');
        } else {
            setError('Error al unirse a la partida');
        }
    }

    return (
        <div className="join-card">
            <button className="join-card__back" onClick={() => navigate(volverA)} aria-label="Volver"> Volver </button>
            <div className="join-card__header">
                <div className="join-card__icon">🎮</div>
                <h1 className="join-card__title">Unirse a partida</h1>
                <p className="join-card__subtitle">Introduce el código de partida</p>
            </div>
            <form onSubmit={handleSubmit} noValidate>
                <div className="join-card__inputs" onPaste={handlePaste}>
                    {digits.map((digit, i) => (
                        <input
                            key={i}
                            ref={(el) => (inputs.current[i] = el)} //Guarda la referencia del input
                            className={`join-card__box${digit ? ' join-card__box--filled' : ''}`} //Cambia el color del input si tiene un dígito
                            type="text"
                            inputMode="numeric" //Muestra el teclado numérico en móviles
                            maxLength={1}
                            value={digit}
                            onChange={(e) => handleChange(i, e.target.value)}
                            onKeyDown={(e) => handleKeyDown(i, e)}
                            autoFocus={i === 0}
                            aria-label={`Dígito ${i + 1}`}
                            id={`codigo-box-${i}`}
                        />
                    ))}
                </div>
                {error && <p className="join-card__error">{error}</p>}
                <button
                    type="submit"
                    className="btn btn--primary btn--full join-card__submit"
                    disabled={isLoading}
                    id="btn-unirse-submit"
                >
                    {isLoading ? 'Uniéndose...' : 'Unirse'}
                </button>
                <div className="join-card__divider">
                    <span>o</span>
                </div>
                <Link to="/unirse/partida/publica" className="btn btn--outline btn--lg2 join-card__submit">
                    Unirse a partida pública
                </Link>
            </form>
        </div>
    );
}