import { Routes, Route } from 'react-router-dom';
import { useState, useEffect } from 'react';
import Home from './pages/Home';
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Register from './pages/Register';
import ChangePasswordConfirmation from './pages/ChangePasswordConfirmation';
import RegisterConfirmation from './pages/RegisterConfirmation';
import DashboardJugador from './pages/DashboardJugador';
import DashboardProfesor from './pages/DashboardProfesor';
import UnirsePartida from './pages/UnirsePartida';
import ProtectedRoute from './components/ProtectedRoute';
import Lobby from './pages/Lobby';
import NotFound from './pages/NotFound';
import PublicLayout from './layouts/PublicLayout';
import CleanLayout from './layouts/CleanLayout';
import AppLayout from './layouts/AppLayout';
import PartidasPublicas from './pages/PartidasPublicas';
import PublicRoute from './components/PublicRoute';
import SessionExpiredModal from './components/SessionExpiredModal';
import CrearPartida from './pages/CrearPartida';
import EstadisticasJugador from './pages/EstadisticasJugador';
import Entrenamiento from './pages/Entrenamiento';

export default function App() {

  const [sessionExpired, setSessionExpired] = useState(false);

  useEffect(() => {

    //Escucha el custom event (otro navegador)
    const handleSessionExpired = () => {
      setSessionExpired(true);
    };
    window.addEventListener('session-expired', handleSessionExpired);

    // Escucha el storage event (otra pestaña del mismo navegador)
    const handleStorage = (event) => {
      // console.log('storage event recibido:', event.key, event.newValue);
      if (event.key === 'jwt' && event.newValue === null) {
        setSessionExpired(true);
      }
    };
    window.addEventListener('storage', handleStorage);

    return () => {
      window.removeEventListener('session-expired', handleSessionExpired);
      window.removeEventListener('storage', handleStorage);
    };
  }, []);

  function handleSessionExpirada() {
    setSessionExpired(false);
    window.location.href = "/";
  }

  return (
    <>
      {sessionExpired && <SessionExpiredModal onAceptar={handleSessionExpirada} />}
      <Routes>
        {/* ── CON Navbar y Footer ─────────────────── */}
        <Route element={<PublicLayout />}>
          <Route path="/" element={<PublicRoute><Home /></PublicRoute>} />
        </Route>
        {/* ── SIN Navbar y Footer ─────────────────── */}
        <Route element={<CleanLayout />}>
          <Route path="/login" element={<PublicRoute><Login /></PublicRoute>} />
          <Route path="/register" element={<PublicRoute><Register /></PublicRoute>} />
          <Route path="/forgot-password" element={<PublicRoute><ForgotPassword /></PublicRoute>} />
          <Route path="/reset-password" element={<PublicRoute><ResetPassword /></PublicRoute>} />
          <Route path="/password-changed" element={<PublicRoute><ChangePasswordConfirmation /></PublicRoute>} />
          <Route path="/register-confirmation" element={<PublicRoute><RegisterConfirmation /></PublicRoute>} />
        </Route>
        {/* ── CON Navbar SIN Footer ─────────────────── */}
        <Route element={<AppLayout />}>
          <Route path="/jugador" element={
            <ProtectedRoute rolRequerido="JUGADOR"><DashboardJugador /></ProtectedRoute>
          } />
          <Route path="/profesor" element={
            <ProtectedRoute rolRequerido="PROFESOR"><DashboardProfesor /></ProtectedRoute>
          } />
          <Route path="/unirse/partida/privada" element={
            <ProtectedRoute><UnirsePartida /></ProtectedRoute>
          } />
          <Route path="/unirse/partida/publica" element={
            <ProtectedRoute><PartidasPublicas /></ProtectedRoute>
          } />
          <Route path="/crear/partida" element={
            <ProtectedRoute rolRequerido="JUGADOR"><CrearPartida /></ProtectedRoute>
          } />
          <Route path="/jugador/estadisticas" element={
            <ProtectedRoute rolRequerido="JUGADOR"><EstadisticasJugador /></ProtectedRoute>
          } />
          <Route path="/jugador/entrenamiento" element={
            <ProtectedRoute rolRequerido="JUGADOR"><Entrenamiento /></ProtectedRoute>
          } />
        </Route>
        <Route path="/partida/:idPartida" element={
          <ProtectedRoute><Lobby /></ProtectedRoute>
        } />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </>

  )
}
