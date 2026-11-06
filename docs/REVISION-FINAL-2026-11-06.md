# Revisión final de cambios de jUGamiR

Se revisó el conjunto local de cambios de autenticación, validaciones, lobby, juego, estadísticas, edición/importación docente, imágenes, migraciones, interfaz y proxies. Las comprobaciones se realizaron sobre QA aislado y una segunda base PostgreSQL recién creada. No se ha desplegado ni publicado código, ni modificado datos de producción.

## Defectos adicionales corregidos

| Área | Problema y comportamiento final |
|---|---|
| Imágenes utilizadas | Una ruta equivalente como `imagenes/carpeta//foto.png` podía eludir la comparación de referencias. La protección compara ahora rutas canónicas. Los ZIP también detectan entradas que representan la misma ruta. |
| Cambio de sesión | Una renovación tardía podía reemplazar el JWT de un login posterior. Ahora solo guarda el token si sigue vigente la sesión que inició la renovación. |
| Peticiones antiguas | Una escritura que devolviera 401 después de cambiar de sesión podía repetirse con las credenciales de otra cuenta. Ahora se devuelve el rechazo sin repetirla ni borrar la sesión nueva. |
| Ruleta y respuestas | Los fallos de red podían dejar la ruleta o las opciones bloqueadas. Ahora se recupera el estado persistido y se permite continuar. Se comprobó tanto el corte anterior al envío como la pérdida de respuesta después del commit. |
| Abandono | Un rechazo HTTP podía llevar al jugador al inicio aunque siguiera en la partida. Solo navega después de recibir confirmación; si falla, mantiene la partida y muestra el error. |
| Historial por categoría | Editar la categoría de una asignatura con preguntas utilizadas modificaba también sus estadísticas antiguas. Ahora conserva la categoría y permite seguir editando el nombre. El sorteo bloquea y vuelve a comprobar la asignatura, categoría y dificultad frente a cambios simultáneos. |
| Temporizadores | Se cancelan las transiciones de resultado y notificación al reconciliar, recibir una nueva pregunta o desmontar el juego. |
| Ejecutor de QA | Registrar un caso fallido no hacía fallar el proceso. Ahora conserva las observaciones posteriores pero termina con código de error, evitando un resultado automático falsamente aprobado. |

Los fallos de rutas, cambio de sesión, petición antigua, historial y código de salida del ejecutor se reprodujeron con pruebas que fallaron antes de corregirlos. Los logs de ambos resultados se conservan en la carpeta de evidencias.

## Validación

- **84 pruebas Java: cero fallos, errores u omisiones**, con `mvn clean verify`, PostgreSQL 16 vacío, esquema inicial y migraciones Flyway. Un comprobador adicional exige que existan las suites de regresión y rechaza cualquier prueba omitida.
- **11 pruebas del frontend**, instalación limpia con Node 22 y build de producción correctos. Las imágenes reales de backend y frontend también se reconstruyeron correctamente.
- **Cinco pruebas Python** del lector CSV: BOM, comillas, punto y coma, saltos de línea, columnas obligatorias y filas incompletas.
- **Una prueba del propio ejecutor de QA**, que exige código de salida 1 y que se registren también los casos posteriores.
- **30 variantes de navegador diferentes** verificadas en esta revisión, incluyendo autenticación, OTP, refresh y logout entre pestañas, errores 429/500, recuperación de contraseña, partida con teclado, estadísticas y los cuatro escenarios de fallo de red del juego.
- ESLint de los módulos modificados de juego y API, `git diff --check` y validación del workflow con actionlint correctos.

La evidencia está en [revision-final-2026-11-06](revision-final-2026-11-06/). El manifiesto identifica los fuentes mediante SHA-256. El build conserva el aviso de Vite sobre el tamaño del bundle principal; no impide compilar y no se presenta como un error funcional.

## Automatización preparada

El workflow `.github/workflows/regression.yml` ejecutará las regresiones en PR, pushes a `main` o `codex/**`, y manualmente. Usa PostgreSQL desechable, Java 17, Node 22, fixtures ficticios y permisos de lectura del repositorio; no necesita secretos de producción ni despliega. Conserva los informes de Maven durante siete días.

Se validó la sintaxis con actionlint y se reprodujeron localmente los pasos de BD, Maven, instalación limpia, tests y build. **Todavía no se ha ejecutado en GitHub**, porque estos cambios siguen sin publicarse. La batería completa de navegador y la carga de treinta minutos no están incluidas en cada push: siguen siendo comprobaciones de aceptación antes de publicar una versión. Tampoco se han configurado reglas de protección de ramas en GitHub.

Las acciones se contrastaron con su documentación oficial: [checkout](https://github.com/actions/checkout), [setup-java](https://github.com/actions/setup-java), [setup-node](https://github.com/actions/setup-node) y [upload-artifact](https://github.com/actions/upload-artifact).

## Despliegue y límites

Se mantienen los [seis pasos de despliegue](PASOS-DESPLIEGUE-PENDIENTE.md), ampliados con observación y reversión. Hay un comprobador de salud de solo lectura en `scripts/qa/public-health.mjs`; se verificó contra QA. No se ha instalado un monitor externo ni programado alertas.

Los resultados de carga del 5 de noviembre siguen correspondiendo al código de aquella campaña; no se ha repetido la carga de treinta minutos sobre esta revisión. No se debe atribuir esa medición a estas imágenes nuevas.

Siguen pendientes el despliegue, la repetición de esta versión a través del proxy público y las pruebas físicas de suspensión y cambio Wi-Fi/datos. El iPhone solo puede acceder a producción; no hay Android físico. VoiceOver continúa excluido por decisión del usuario. SMTP se verifica mediante captura en QA, no mediante entrega a un buzón externo real.

Esta revisión reduce riesgos y deja regresiones reproducibles. No demuestra que la aplicación carezca de cualquier fallo desconocido.
