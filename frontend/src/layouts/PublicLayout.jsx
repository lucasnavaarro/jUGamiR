import { Outlet } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';


export default function PublicLayout() {
    return (
        <>
            <Navbar />
            <Outlet />   {/* aquí se renderiza la página hija */}
            <Footer />
        </>
    );
}