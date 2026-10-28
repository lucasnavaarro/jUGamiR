import { useState } from 'react';
import LoginForm from '../components/LoginForm';
import VerifyForm from '../components/VerifyForm';

export default function Login() {
    const [step, setStep] = useState('login'); // 'login' | 'verify'
    const [email, setEmail] = useState('');
    const [challengeId, setChallengeId] = useState('');

    return (
        <main className="login-page">
            {step === 'login'
                ? <LoginForm onEmailSent={(e, id) => { setEmail(e); setChallengeId(id); setStep('verify'); }} />
                : <VerifyForm email={email} challengeId={challengeId} />
            }
        </main>
    );
}
