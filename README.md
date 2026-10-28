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

## 📁 Estructura del Proyecto

```
.
├── backend
│   ├── src
│   │   ├── main
│   │   │   ├── java
│   │   │   │   └── com
│   │   │   │       └── jugamir
│   │   │   │           └── backend
│   │   │   │               ├── config
│   │   │   │               ├── controller
│   │   │   │               ├── dto
│   │   │   │               ├── exception
│   │   │   │               ├── model
│   │   │   │               │   └── enums
│   │   │   │               ├── repository
│   │   │   │               ├── security
│   │   │   │               └── service
│   │   │   └── resources
│   │   │       ├── static
│   │   │       └── templates
│   │   └── test
│   ├── Dockerfile
│   ├── mvnw
│   ├── mvnw.cmd
│   └── pom.xml
├── db
│   ├── backup_completo.sql
│   ├── data.sql
│   ├── import_preguntas.py
│   ├── import_respuestas.py
│   └── schema.sql
├── frontend
│   ├── node_modules
│   ├── public
│   │   └── favicon.png
│   ├── src
│   │   ├── components
│   │   ├── layouts
│   │   ├── pages
│   │   ├── services
│   │   ├── App.jsx
│   │   ├── main.jsx
│   │   └── style.css
│   ├── Dockerfile
│   ├── eslint.config.js
│   ├── index.html
│   ├── nginx.conf
│   ├── package.json
│   ├── package-lock.json
│   ├── README.md
│   └── vite.config.js
├── node_modules
├── scripts
│   ├── fix_preguntas2.py
│   ├── fix_preguntas.py
│   ├── fix_preguntas_vascular.py
│   └── test_partidas.mjs
├── docker-compose.yml
├── package.json
├── package-lock.json
└── README.md
```

## 🚀 Instalación y Puesta en Marcha

### Requisitos Previos
- **Docker** y **Docker Compose**

### ⚡ Lanzar el Proyecto

```bash
# 1. Clonar el repositorio
git clone https://github.com/lucasnavaarro/jUGamiR.git
cd jUGamiR

# 2. Crear el fichero de variables de entorno
Crear un fichero .env en la raíz del proyecto con las variables necesitadas.
```

El fichero `.env` debe contener las siguientes variables:

```dotenv
# Base de datos
DB_USER=jugamir
DB_PASSWORD=<contraseña-aleatoria>

# JWT (clave generada con: openssl rand -base64 64)
JWT_SECRET=

# Gmail (desde donde se envían los correos)
MAIL_USERNAME=
MAIL_PASSWORD=

# pgAdmin
PGADMIN_EMAIL=admin@jugamir.com
PGADMIN_PASSWORD=<contraseña-aleatoria>
```

```bash
# 3. Levantar todos los servicios
docker compose up --build
```

Para que el envío de correos funcione, la cuenta de Gmail usada en `MAIL_USERNAME` debe tener activada la [verificación en dos pasos](https://myaccount.google.com/security). Una vez activada, hay que generar una [contraseña de aplicación](https://myaccount.google.com/apppasswords), que es el valor que debe ir en el campo `MAIL_PASSWORD` (no la contraseña habitual de la cuenta).

### 🌐 Puertos y Acceso

| Servicio | URL |
|---|---|
| Frontend (React) | http://localhost:3000 |
| Backend (API REST) | http://localhost:8080 |
| pgAdmin (gestión BD) | http://localhost:5050 |

## Configuración y actualización de seguridad

El Compose base es para desarrollo local: publica los puertos únicamente en `127.0.0.1`
y activa el perfil `local`. Fuera de Docker, usar `SPRING_PROFILES_ACTIVE=local` para
HTTP en localhost. Las cookies de renovación son `HttpOnly`, `SameSite=Strict` y
`Secure` por defecto; la excepción HTTP solo funciona con ese perfil y una URL de loopback.

Para producción, Docker Compose 2.24.4 o superior:

```bash
# APP_URL debe ser el origen HTTPS exacto, sin barra final.
# TLS_CERTS_DIR contiene fullchain.pem y privkey.pem de tu dominio.
docker compose -f docker-compose.yml -f docker-compose.production.yml up --build -d
```

El proxy redirige HTTP a HTTPS, limita autenticación por IP y no publica PostgreSQL
ni el backend. Si hay otro proxy delante de Nginx, configurar los rangos de proxies
confiables para recuperar la IP real; no aceptar cabeceras de IP arbitrarias.
La renovación de certificados corresponde al despliegue.

- El registro de profesores está abierto a cualquier correo cuyo dominio sea exactamente
  `ugr.es`, sin lista de autorización ni configuración adicional en `.env`. El backend
  aplica esta regla también al cambiar el correo de una cuenta de profesor.
  Para acceder se exige el código enviado al buzón institucional; esto acredita acceso
  al correo y no verifica por separado la condición de profesor.
- El login devuelve `challengeId` tras comprobar la contraseña. Verificar y reenviar
  requieren ese identificador. Cada desafío caduca en 10 minutos, admite cinco intentos
  y dos reenvíos separados por al menos 60 segundos. Hay límites adicionales por cuenta.
- Cambiar correo exige contraseña actual y código enviado al nuevo correo. Tanto el
  cambio de correo como la recuperación de contraseña revocan sesiones anteriores.
- Las partidas exigen pertenencia en HTTP y STOMP. El servidor fija y guarda el plazo
  de cada pregunta, calcula el tiempo y cierra los turnos vencidos aunque el cliente se desconecte.
- Solo se aceptan PNG/JPEG reales, reencodificados sin metadatos, hasta 10 MiB y 16 millones
  de píxeles. Los ZIP deben usar rutas `imagenes/...`: máximo 500 entradas, 200 MiB
  descomprimidos y recodificados, con un presupuesto de procesamiento de 30 segundos
  comprobado entre operaciones (no un timeout de CPU estricto), sin traversal ni enlaces simbólicos.

Flyway aplica V2–V4 sobre el esquema existente, baselining en V1. Hacer una copia de
seguridad antes de actualizar. V2 invalida códigos de login antiguos; V4 cierra
preguntas anteriores que no tenían un plazo fiable. No es necesario borrar la base.
En una instalación vacía, el Compose carga primero `db/schema.sql`.
Después importa `db/data.sql` en una transacción. El banco inicial usa un autor
interno deshabilitado, sin perfil de profesor/jugador y sin contraseña utilizable;
las cuentas reales se crean desde el registro. No se incluyen cuentas de acceso
de demostración. El chequeo de salud de PostgreSQL
espera al servidor TCP definitivo para no dar por terminada la importación durante
el servidor temporal de inicialización.

Las nuevas imágenes se guardan en el volumen `imagenes_data`; los originales del
classpath siguen disponibles. Si hay imágenes antiguas en el bind mount local,
volver a subir las necesarias mediante el importador validado. No copiar a ciegas
archivos antiguos al volumen ni eliminar el volumen de PostgreSQL durante la actualización.
El contenedor backend usa un usuario sin privilegios y los scripts de importación son de solo lectura.

### Pruebas de regresión de seguridad

```bash
sh scripts/test-security.sh
docker build -f frontend/Dockerfile -t jugamir-frontend:check .
```

El script crea una base temporal sin puertos públicos, comprueba la carga del esquema
y del banco de preguntas, ejecuta las pruebas con correo simulado y elimina la base
y su red al terminar. No lee `.env`. Requiere Docker y las imágenes PostgreSQL 16 y Maven/Java 17.

---

Autor: Lucas Navarro Moreno
