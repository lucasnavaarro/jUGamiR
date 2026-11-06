# Título propuesto

Corregir validaciones, sesiones, concurrencia e historial y automatizar regresiones

# Descripción

Corrige fallos reproducidos durante la batería de aceptación de jUGamiR: salas y turnos concurrentes, recuperación de sesión y eventos, conservación de preguntas e imágenes utilizadas, estadísticas competitivas coherentes e importación CSV atómica por fila. Aplica los contratos acordados de email/nick normalizados, tiempo de 5–600 segundos, entrenamiento privado individual y gestión compartida entre profesores. El listado público muestra nicks y el cliente se recupera de cortes de red sin bloquear el juego ni repetir operaciones de otra sesión.

Incluye Flyway V5–V8, regresiones de backend/frontend/CSV, comprobación de errores del ejecutor QA y un workflow de CI sin acceso a producción. V7 se detiene ante identidades existentes que colisionan; requiere comprobar duplicados antes del despliegue. Las asignaturas con preguntas utilizadas conservan su categoría para no alterar el historial.

Validación final local: 84 pruebas Java sin fallos ni omisiones sobre PostgreSQL recién creado; 11 pruebas frontend, cinco CSV, una del ejecutor QA, 30 variantes de navegador, build limpio, imágenes de producción y actionlint correctos. GitHub Actions y la nueva carga AMD64 de treinta minutos están aprobados para la candidata `6ace3dea2ea9`: 26.742 peticiones, cero errores, HTTP p95 59,19 ms y STOMP p95 16,07 ms.

Quedan pendientes el despliegue y la aceptación en el proxy público y dispositivos físicos. Seguir `docs/PASOS-DESPLIEGUE-PENDIENTE.md` con copia de BD/imágenes y ventana de mantenimiento. No incluir `.DS_Store`, credenciales ni copias de datos reales al preparar los commits. La PR publicada es #6, apilada sobre #5. El despliegue permanece pendiente.

Actualización posterior: [pasos 2 y 3 completados y candidata exacta validada](VALIDACION-PASOS-2-3-2026-11-06.md).
