# jUGamiR 🩺🎮

jUGamiR es una plataforma web de alto rendimiento diseñada para la preparación del examen MIR (Médico Interno Residente). El proyecto combina una interfaz de usuario dinámica y gamificada con una arquitectura de backend robusta basada en estándares industriales.

## ✨ Características Principales

- **Ruleta de Especialidades**: Sistema de selección aleatoria de preguntas por categorías médicas con animaciones fluidas.
- **Banco de Preguntas MIR**: Gestión de un dataset extenso de preguntas reales, opciones y respuestas correctas.
- **Partidas multijugador**: Competición en tiempo real entre jugadores mediante WebSockets.
- **Dashboard de Estadísticas**: Métricas de rendimiento por categoría y tiempo de respuesta con visualización de datos dinámica.
- **Diseño 100% Responsive**: Optimización total para cualquier dispositivo (Mobile First) utilizando CSS moderno y layouts adaptables.

## 🛠️ Stack Tecnológico

### Backend
- **Java 17** — Lenguaje principal.
- **Spring Boot 4.0.2** — Framework core para la API REST.
- **Spring Security + JWT (JJWT 0.12.6)** — Autenticación y autorización.
- **Spring Data JPA** — Persistencia y gestión de la base de datos.
- **Spring WebSocket + STOMP** — Comunicación en tiempo real para partidas multijugador.
- **Spring Mail** — Envío de correos electrónicos.
- **Flyway** — Migraciones de base de datos.
- **Lombok** — Reducción de boilerplate.
- **Maven 3.9.9** — Gestión de dependencias y build.

### Frontend
- **React 19.2** — Biblioteca principal para la SPA.
- **React Router DOM 7.13** — Enrutado del lado del cliente.
- **Recharts 3.8** — Visualización de datos y gráficas.
- **@stomp/stompjs 7.3** — Cliente WebSocket para partidas en tiempo real.
- **Vite 7.3** — Bundler y herramienta de build.
- **Nginx** — Servidor estático en producción con proxy inverso al backend.

### Base de datos
- **PostgreSQL 16** — Base de datos principal.
- **pgAdmin 4** — Interfaz de administración de la base de datos.

### Infraestructura
- **Docker + Docker Compose** — Contenerización y orquestación de todos los servicios.

## 🚀 Instalación y Puesta en Marcha

### Requisitos Previos
- **Docker** y **Docker Compose**

### ⚡ Lanzar el Proyecto

```bash
# 1. Clonar el repositorio
git clone https://github.com/lucasnavaarro/jUGamiR.git
cd jUGamiR

# 2. Crear el fichero de variables de entorno
# Crear un fichero .env en la raíz del proyecto con las variables necesitadas.

# 3. Levantar todos los servicios
docker compose up --build
```

La primera vez tarda ~2-3 minutos mientras Maven descarga dependencias y compila el JAR.

### 🌐 Puertos y Acceso

| Servicio | URL |
|---|---|
| Frontend (React) | http://localhost:3000 |
| Backend (API REST) | http://localhost:8080 |
| pgAdmin (gestión BD) | http://localhost:5050 |

---

Autor: Lucas Navarro Moreno
