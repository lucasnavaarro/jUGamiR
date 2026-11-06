# Título propuesto

Corregir validaciones, sesiones, concurrencia e historial y automatizar regresiones

# Descripción

Corrige fallos reproducidos durante la batería de aceptación de jUGamiR: salas y turnos concurrentes, recuperación de sesión y eventos, conservación de preguntas e imágenes utilizadas, estadísticas competitivas coherentes e importación CSV atómica por fila. Aplica los contratos acordados de email/nick normalizados, tiempo de 5–600 segundos, entrenamiento privado individual y gestión compartida entre profesores. El listado público muestra nicks y el cliente se recupera de cortes de red sin bloquear el juego ni repetir operaciones de otra sesión.

Incluye Flyway V5–V7, regresiones de backend/frontend/CSV, comprobación de errores del ejecutor QA y un workflow de CI sin acceso a producción. V7 se detiene ante identidades existentes que colisionan; requiere comprobar duplicados antes del despliegue. Las asignaturas con preguntas utilizadas conservan su categoría para no alterar el historial.

Validación final local: 84 pruebas Java sin fallos ni omisiones sobre PostgreSQL recién creado; 11 pruebas frontend, cinco CSV, una del ejecutor QA, 30 variantes de navegador, build limpio, imágenes de producción y actionlint correctos. GitHub Actions queda pendiente de la publicación de la PR. La carga de treinta minutos de la campaña anterior no se atribuye a esta revisión nueva.

Quedan pendientes el despliegue y la aceptación en el proxy público y dispositivos físicos. Seguir `docs/PASOS-DESPLIEGUE-PENDIENTE.md` con copia de BD/imágenes y ventana de mantenimiento. No incluir `.DS_Store`, credenciales ni copias de datos reales al preparar los commits. Este archivo es un borrador local: no representa una PR publicada.
