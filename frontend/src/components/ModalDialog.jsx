import { useEffect, useId, useRef } from 'react';

export default function ModalDialog({ title, children }) {
    const titleId = useId();
    const dialog = useRef(null);
    useEffect(() => {
        const previous = document.activeElement;
        const controls = () => [...dialog.current.querySelectorAll('button, a[href], input, select, textarea, [tabindex="0"]')]
            .filter(element => !element.disabled);
        controls()[0]?.focus();
        const trap = event => {
            if (event.key !== 'Tab') return;
            const elements = controls(), first = elements[0], last = elements.at(-1);
            if (!first) return;
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        };
        const element = dialog.current;
        element.addEventListener('keydown', trap);
        return () => { element.removeEventListener('keydown', trap); if (previous?.isConnected) previous.focus(); };
    }, []);
    return <div className="session-modal-overlay"><div ref={dialog} className="session-modal" role="dialog" aria-modal="true" aria-labelledby={titleId}>
        <h2 id={titleId} className="session-modal__title">{title}</h2>{children}
    </div></div>;
}
