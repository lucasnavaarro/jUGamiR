import Hero from "../components/Hero";
import Stats from "../components/Stats";
import Categories from "../components/Categories";
import HowToPlay from "../components/HowToPlay";

export default function Home() {
    return (
        <main>
            <Hero />
            <Stats />
            <Categories />
            <HowToPlay />
        </main>
    );
}