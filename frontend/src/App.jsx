import { Routes, Route } from 'react-router-dom';
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

export default function App() {
  return (
    <>
      <Routes>
        {/* ── CON Navbar y Footer ─────────────────── */}
        <Route element={<PublicLayout />}>
          <Route path="/" element={<Home />} />
        </Route>
        {/* ── SIN Navbar y Footer ─────────────────── */}
        <Route element={<CleanLayout />}>
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password" element={<ResetPassword />} />
          <Route path="/password-changed" element={<ChangePasswordConfirmation />} />
          <Route path="/register-confirmation" element={<RegisterConfirmation />} />
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
        </Route>
        <Route path="/partida/:idPartida" element={
          <ProtectedRoute><Lobby /></ProtectedRoute>
        } />
        <Route path="*" element={<NotFound />} />
      </Routes>
    </>

  )
}
