import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import Hero from './components/Hero';
import Stats from './components/Stats';
import Categories from './components/Categories';
import HowToPlay from './components/HowToPlay';
import Footer from './components/Footer';
import Login from './pages/Login';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Register from './pages/Register';
import ChangePasswordConfirmation from './pages/ChangePasswordConfirmation';
import RegisterConfirmation from './pages/RegisterConfirmation';
import DashboardJugador from './pages/DashboardJugador';
import DashboardProfesor from './pages/DashboardProfesor';

function Home() {
  return (
    <main>
      <Hero />
      <Stats />
      <Categories />
      <HowToPlay />
    </main>
  );
}

export default function App() {
  return (
    <>
      <Navbar />
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/login" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
        <Route path="/register" element={<Register />} />
        <Route path="/password-changed" element={<ChangePasswordConfirmation />} />
        <Route path="/register-confirmation" element={<RegisterConfirmation />} />
        <Route path="/jugador" element={<DashboardJugador />} />
        <Route path="/profesor" element={<DashboardProfesor />} />
      </Routes>
      <Footer />
    </>

  )
}
