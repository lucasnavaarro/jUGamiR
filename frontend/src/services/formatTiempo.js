export function formatTiempo(ms, decimales = 1) {
    const totalSec = ms / 1000;
    if (totalSec < 60) return totalSec.toFixed(decimales) + 's';
    const mins = Math.floor(totalSec / 60);
    const secs = Math.round(totalSec % 60);
    return `${mins}min ${secs}s`;
}
