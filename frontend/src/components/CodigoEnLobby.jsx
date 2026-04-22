export default function CodigoEnLobby({ codigo }) {
    return (
        <div className="lobby__code-boxes">
            {codigo.split('').map((char, i) => (
                <div key={i} className="join-card__box join-card__box--filled lobby__code-box">
                    {char}
                </div>
            ))}
        </div>
    );
}