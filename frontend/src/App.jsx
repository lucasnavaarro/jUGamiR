import Navbar from './components/Navbar';
import Hero from './components/Hero';
import Stats from './components/Stats';
import Categories from './components/Categories';
import HowToPlay from './components/HowToPlay';
import Footer from './components/Footer';

export default function App() {
  return (
    <>
      <Navbar />
      <main>
        <Hero />
        <Stats />
        <Categories />
        <HowToPlay />
      </main>
      <Footer />
    </>
  );
}
