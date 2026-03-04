import { useState, useEffect } from 'react';
import { healthCheck } from '../services/api';

export default function StatusBadge() {
    const [status, setStatus] = useState('checking'); // 'checking' | 'online' | 'offline'
    const [message, setMessage] = useState('');

    useEffect(() => {
        async function updateStatus() {
            const msg = await healthCheck();
            if (msg) {
                setStatus('online');
                setMessage(msg);
            } else {
                setStatus('offline');
                setMessage('');
            }
        }

        updateStatus();
        const interval = setInterval(updateStatus, 30_000);
        return () => clearInterval(interval);
    }, []);

    const labels = {
        checking: 'Conectando…',
        online: 'Backend online ✓',
        offline: 'Backend offline',
    };

    return (
        <span
            className={`status-badge status-badge--${status}`}
            aria-live="polite"
            title={message}
        >
            <span className="status-dot"></span>
            <span>{labels[status]}</span>
        </span>
    );
}
