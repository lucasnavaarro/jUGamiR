# jUGamiR 🩺🎮


jUGamiR es una plataforma web de alto rendimiento diseñada para la preparación del examen MIR (Médico Interno Residente). El proyecto combina una interfaz de usuario dinámica y gamificada con una arquitectura de backend robusta basada en estándares industriales.

## ✨ Características Principales
Ruleta de Especialidades: Sistema de selección aleatoria de preguntas por categorías médicas con animaciones fluidas.

Banco de Preguntas MIR: Gestión de un dataset extenso de preguntas reales, opciones y respuestas correctas.

Dashboard de Estadísticas Avanzado:

Métricas de rendimiento por categoría y tiempo de respuesta.

Visualización de datos mediante componentes dinámicos y barras de progreso.

Diseño 100% Responsive: Optimización total para cualquier dispositivo (Mobile First) utilizando CSS moderno y layouts adaptables.

## 🛠️ Stack Tecnológico
### Backend (Arquitectura Empresarial)
Java 17+: Lenguaje principal para una lógica de negocio sólida.

Spring Boot: Framework core para la creación de la API REST.

Spring Data JPA: Para la persistencia y gestión eficiente de la base de datos.

Maven: Gestión de dependencias y ciclo de vida del proyecto.

Arquitectura Limpia: Separación clara entre Controladores, Servicios y Repositorios.

### Frontend (Interfaz de Usuario)
React.js: Biblioteca principal para la construcción de la SPA (Single Page Application).

CSS3 Moderno: Uso de Variables CSS, Grid Layout, Flexbox y técnicas de truncamiento inteligente.

Fetch API: Comunicación asíncrona con el backend de Spring Boot.


## 🚀 Instalación y Puesta en Marcha

### Requisitos Previos
Docker: Para ejecutar el backend y la base de datos en contenedores.

Node.js 18+: Para lanzar el frontend y el script orquestador.

### ⚡ Lanzar el Proyecto

```bash
# 1. Clonar el repositorio
git clone https://github.com/lucasnavaarro/jUGamiR.git
cd jUGamiR

# 2. Instalar dependencias del orquestador
npm install

# 3. Lanzar todo con un solo comando
npm run dev
```

Esto levanta en paralelo la base de datos PostgreSQL, el backend Spring Boot y el frontend React. La primera vez tarda ~2-3 minutos mientras Maven descarga dependencias y compila el JAR.

### 🌐 Puertos y Acceso

Frontend (React): http://localhost:5173

Backend (API REST): http://localhost:8080

pgAdmin (gestión BD): http://localhost:5050


Autor: Lucas Navarro Moreno
