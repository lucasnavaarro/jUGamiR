# Resultados de pruebas de jUGamiR

Este informe conserva la reproducción inicial. Los nueve problemas se corrigieron después; consultar [correcciones y verificación posterior](CORRECCIONES-PRUEBAS-2026-11-04.md), con 54 pruebas de backend y 8 del cliente correctas.

Ejecución del 4 de noviembre de 2026. **Se ejecutaron 54 pruebas automatizadas: 44 pasan y 10 fallan. Los 10 fallos corresponden a 9 problemas distintos**, porque la selección vacía de dificultades se comprobó tanto en servicio como por HTTP. La compilación Docker del cliente termina correctamente. ESLint falla con 10 errores y 3 avisos, independientes de esos 10 fallos funcionales.

Las pruebas usan el código local: commit `d0010db6c6bb04424bded3f881cdf067a5b6f270` más los cambios de trabajo que ya existían. No se ha conectado a producción ni comprobado que esa sea la versión publicada. La base de datos fue PostgreSQL 16 desechable, el correo estuvo simulado y los endpoints HTTP se probaron contra un servidor Spring real en puerto aleatorio dentro del contenedor de pruebas.

## Resumen de ejecución

| Comprobación | Resultado |
|---|---|
| Suite Java original | 30 pruebas OK antes de añadir regresiones. |
| Suite Java completa tras ampliarla | 46 ejecutadas: 36 OK, 10 fallos de aserción, 0 errores de ejecución y 0 omitidas. |
| Nuevas regresiones de servicios | 8 ejecutadas: 2 OK y 6 fallos. |
| Nuevas regresiones HTTP | 8 ejecutadas: 4 OK y 4 fallos. |
| Suite Node del cliente ampliada | 8 pruebas OK: 2 existentes y 6 nuevas. |
| Esquema y banco inicial | Carga verificada: 5214 preguntas y 23274 respuestas antes de sustituirlas por fixtures aislados. |
| Compilación Docker del frontend | OK con el Dockerfile del proyecto. No se desplegó la imagen. |
| ESLint del frontend | 10 errores y 3 avisos. |
| Compilación directa en macOS | No pudo arrancar por dependencia local ausente `@rollup/rollup-darwin-arm64`; la compilación Docker sí pasó. |

Los resultados de la suite original están incluidos en las 46 pruebas Java de la ejecución final, no sumados dos veces. Los 54 tests no equivalen a 54 casos completos de los 222 del plan: algunos comprueban solo una variante o una capa de un caso.

## Problemas funcionales confirmados

### 1 Los jugadores expulsados reciben un turno al iniciar

**Prioridad P0. Caso LOB-15.**

Reproducción: crear privada, unir otro jugador, expulsarlo e iniciar. El expulsado sigue almacenado, correctamente marcado como EXPULSADO, pero recibe `orden_turno` al asignarse los turnos.

Esperado: ningún expulsado tiene un turno jugable. Observado: un participante EXPULSADO con `orden_turno` no nulo; la prueba esperaba 0 y obtuvo 1. Si el reparto aleatorio le asigna el primer turno, el juego puede quedarse esperando a alguien sin acceso. La prueba confirma la asignación indebida; el bloqueo de la interfaz es una consecuencia a comprobar en navegador.

Origen: `LobbyService.iniciarPartida`, en `backend/src/main/java/com/jugamir/backend/service/LobbyService.java:160`, reparte turnos sin filtrar participantes activos.

Prueba: `FunctionalRegressionTest.LOB15_expelledPlayerMustNeverReceiveATurnOnStart`.

### 2 Configurar 7 aciertos por quesito falla en el sexto acierto

**Prioridad P0. Casos FIN-01 y CRE-03.**

Reproducción: crear una partida con una categoría, dificultad MEDIO, umbral 7 y siete preguntas publicadas distintas. Iniciar y acertar seis. Las ventanas de respuesta se fijan en BD exclusivamente en pruebas para no esperar la animación.

Esperado: seis aciertos acumulados, todavía sin quesito. Observado: el sexto acierto lanza `DataIntegrityViolationException`, por `progreso_categoria_aciertos_check`. La restricción SQL limita progreso a 5, mientras el formulario permite umbrales hasta 30. El avance hasta el sexto acierto fue ejecutado, no inferido.

Origen: `db/schema.sql:566` y la acumulación en `JuegoService.actualizarProgreso`. El rechazo de BD deja ese intento sin consolidar; no se probó cómo lo presenta cada navegador.

Prueba: `FunctionalRegressionTest.FIN01_thresholdSevenMustAllowSixCorrectAnswersBeforeAwardingWedge`.

### 3 Editar una pregunta permite alterar una respuesta de otra pregunta

**Prioridad P0. Caso PRO-10.**

Reproducción: crear preguntas A y B con opciones propias. Editar A incluyendo en `respuestas` el ID de una opción incorrecta de B y un texto nuevo.

Esperado: rechazo o conservación de la opción de B. Observado: el texto de B cambia de `Wrong` a `Unexpected foreign edit` aunque la operación se dirige a A.

Origen: `ProfesorService.editarPregunta`, en `backend/src/main/java/com/jugamir/backend/service/ProfesorService.java:203` y `:213`, busca opciones únicamente por su ID y no comprueba la pregunta propietaria. Reproducción en servicio real con PostgreSQL; no se ha realizado la misma mutación desde la interfaz docente.

Prueba: `FunctionalRegressionTest.PRO10_editingAQuestionMustNotModifyAnotherQuestionsOption`.

### 4 Una cuenta desactivada mantiene acceso HTTP con su JWT anterior

**Prioridad P0. Caso SES-10.**

Reproducción: emitir un JWT válido, comprobar `/api/auth/me`, desactivar la cuenta en la BD de pruebas y repetir la petición con el mismo JWT.

Esperado: 401. Observado: 200 después de establecer `es_activo=false`.

Origen: `backend/src/main/java/com/jugamir/backend/security/JwtAuthFilter.java:53` comprueba versión de token pero no exige cuenta activa antes de autenticar. La suite original verifica que el login/OTP de cuentas desactivadas falla; esa comprobación no cubría JWT ya emitidos. Esta prueba comprueba el JWT, no el refresh de una cuenta desactivada.

Prueba: `HttpFunctionalRegressionTest.SES10_disabledAccountMustNotUsePreviouslyIssuedJwtOverHttp`.

### 5 Se pueden crear partidas sin dificultades

**Prioridad P1. Caso CRE-04.**

Reproducción HTTP: `POST /api/lobby/crear` con datos válidos y `dificultades: []`.

Esperado: 400 sin crear partida. Observado: 200 con `idPartida` y `codigoUnion`, persistiendo una configuración sin dificultad elegible. También se reprodujo directamente contra el servicio. El frontend impide normalmente esta entrada; la API no la valida.

Origen: `CrearPartidaRequest` exige lista no nula pero no que contenga elementos, y `LobbyService.crearPartida` tampoco lo comprueba.

Pruebas: `FunctionalRegressionTest.CRE04_emptyDifficultySelectionMustBeRejected` y `HttpFunctionalRegressionTest.CRE04_emptyDifficultiesMustBeRejectedAtHttpBoundary`. Son dos aserciones fallidas sobre un mismo problema.

### 6 Las categorías inexistentes se descartan sin informar

**Prioridad P1. Caso CRE-05.**

Reproducción: crear partida con una categoría válida y otra inexistente.

Esperado por la batería: rechazo de configuración inválida. Observado: el servicio crea la partida con la categoría válida y no señala la selección descartada. No se produjo la excepción esperada.

Origen: `LobbyService.crearPartida`, `backend/src/main/java/com/jugamir/backend/service/LobbyService.java:40`, solo comprueba que el resultado de `findAllById` no esté vacío. Es una discrepancia de contrato/configuración; no demuestra acceso a datos ajenos.

Prueba: `FunctionalRegressionTest.CRE05_unknownSelectedCategoryMustNotBeSilentlyIgnored`.

### 7 Entrenamiento acepta pesos para categorías no seleccionadas

**Prioridad P1. Caso ENT-06.**

Reproducción: seleccionar exclusivamente categoría A y enviar `categoriaPesos` con categoría B existente.

Esperado: rechazar pesos ajenos a la selección. Observado: se consolida la configuración sin excepción. La selección ponderada usa esas filas; debe restringirse al conjunto de categorías permitido. El test confirma la aceptación, no mide frecuencias de la ruleta.

Origen: `LobbyService.crearPartida`, `backend/src/main/java/com/jugamir/backend/service/LobbyService.java:84`, valida existencia de cada categoría ponderada pero no su pertenencia a la selección.

Prueba: `FunctionalRegressionTest.ENT06_trainingWeightsMustNotSelectAnUnselectedCategory`.

### 8 Siete plazas provocan 500 en lugar de validación

**Prioridad P1. Caso CRE-06.**

Reproducción: `POST /api/lobby/crear` con `maxJugadores: 7` y el resto válido.

Esperado: 400 de validación. Observado: 500 con `{"error":"Error interno del servidor"}`, al alcanzar la restricción de BD `chk_partidas_max_jugadores`. El fallo es la ausencia de validación y el error recibido; no se ha demostrado que se cree una partida de siete plazas.

Origen: `backend/src/main/java/com/jugamir/backend/dto/CrearPartidaRequest.java:13`, sin límites de validación para capacidad.

Prueba: `HttpFunctionalRegressionTest.CRE06_outOfRangeCapacityMustReturnValidationErrorRatherThan500`.

### 9 Se permite umbral cero para conseguir quesito

**Prioridad P1. Caso CRE-06.**

Reproducción: crear partida por HTTP con `aciertosParaQuesito: 0`.

Esperado: 400 sin crear partida. Observado: 200 con ID/código y partida persistida. No se ejecutó una partida completa con esa configuración inválida.

Origen: `backend/src/main/java/com/jugamir/backend/dto/CrearPartidaRequest.java:15` y creación del servicio sin comprobación del rango 1–30.

Prueba: `HttpFunctionalRegressionTest.CRE06_zeroWedgeThresholdMustNotCreatePlayableRoom`.

## Comprobaciones que pasan

La suite original pasa en su totalidad: dominio institucional de profesor, desafíos de login, persistencia de intentos OTP, reenvíos, consumo concurrente de códigos y reset, revocación tras reset/cambio de email, pertenencia a partidas, ocultación de la solución, cierre de vencimientos y cálculo del tiempo de respuesta; también políticas de cookies/contraseñas, STOMP y validaciones de imágenes/ZIP en los supuestos existentes.

Las nuevas pruebas añaden resultados positivos para:

- Victoria individual con un quesito, persistencia del final y contabilización en estadísticas competitivas.
- Entrenamiento finalizado que no se añade al resumen ni al progreso competitivo.
- Denegación HTTP sin sesión para seis familias de rutas y separación HTTP entre roles.
- Login y OTP por HTTP, cookie de refresh, renovación, `/me` y revocación tras logout. El cliente HTTP de prueba entrega manualmente la cookie: no valida comportamiento de cookies en un navegador HTTPS.
- Creación e inicio de privada por HTTP con configuración válida.
- Cliente JS: health UP/DOWN y error de red, envío de JWT/cuerpo, reintento tras refresh, una sola renovación con peticiones concurrentes y ausencia de refresh automático ante 403.

## Resultado de ESLint

ESLint señala 10 errores y 3 avisos. Estos son problemas de análisis estático y mantenimiento; no todos equivalen a un fallo visible reproducido en navegador.

| Archivo | Resultado |
|---|---|
| `components/Categories.jsx:40` | Variable `i` sin usar. |
| `components/JuegoEnCurso.jsx:47` | Regla de hooks/immutability por acceso a `handleEvent` antes de su declaración; además, dependencias del efecto incompletas. No se ha demostrado un ReferenceError: es una función declarada. |
| `components/LobbyEspera.jsx:19` y `:41` | Parámetros `error` sin usar. |
| `components/LoginForm.jsx:2` | Import `useNavigate` sin usar. |
| `components/Navbar.jsx:1` | Import `useState` sin usar. |
| `components/ProtectedRoute.jsx:25` | Aviso de dependencias de efecto incompletas. |
| `components/Ruleta.jsx:39` | Actualización síncrona de estado dentro de efecto; aviso adicional de dependencias incompletas. |
| `pages/EditarPerfil.jsx:55` | Parámetro `err` sin usar. |
| `pages/Entrenamiento.jsx:25` | Parámetro `err` sin usar. |
| `pages/Lobby.jsx:22` | Actualización síncrona de estado dentro de efecto. |

El archivo de pruebas nuevo del cliente pasa su propio lint. Las incidencias anteriores no se han corregido en esta ejecución.

## Evidencias y reproducción

- [Resultados por prueba Java y mensajes de fallo](resultados-pruebas-2026-11-04/backend-resumen.txt).
- [Salida de las ocho pruebas Node](resultados-pruebas-2026-11-04/frontend-tests.txt).
- [Salida de ESLint](resultados-pruebas-2026-11-04/frontend-lint.txt).
- [Salida del build Docker](resultados-pruebas-2026-11-04/frontend-build-docker.txt).

Los informes completos de la última ejecución Java permanecen en `backend/target/surefire-reports`; se sobrescribirán con ejecuciones posteriores. Los resúmenes anteriores quedan guardados con fecha.

Desde la raíz del repositorio, con Docker disponible:

```bash
# Base nueva audit_test, datos sintéticos y correo simulado.
# Actualmente termina con código 1 por los fallos confirmados.
sh scripts/test-security.sh

# En un entorno con Node/npm instalado, incluye las nuevas pruebas del cliente.
npm --prefix frontend test

# Build aislado del frontend; no publica ni arranca el servicio.
docker build -f frontend/Dockerfile -t jugamir-qa-frontend:20261104 .
```

Los nuevos tests están protegidos por `JUGAMIR_DISPOSABLE_DB_TEST=true` y comprueban que la base se llama `audit_test` antes de truncar fixtures. Usarlos mediante el script desechable; no activar esa variable sobre una instalación compartida. Los contenedores y redes de BD se eliminan al terminar. La imagen de build de pruebas y la caché Maven permanecen disponibles.

## Cambios realizados y cobertura pendiente

Se añadieron `FunctionalRegressionTest.java`, `HttpFunctionalRegressionTest.java` y `frontend/src/services/api.test.js`. El comando `npm test` del frontend se amplió para incluir las pruebas de servicios. No se modificaron servicios, controladores ni esquema para ocultar los fallos; las regresiones permanecen rojas y servirán para verificar su corrección.

**No se han ejecutado los 222 casos completos.** Quedan pendientes recorridos de navegador, dispositivos reales, entrega de correo real, proxy/HTTPS/WSS de producción, carreras de última plaza y abandono, importación completa con fallos intermedios, cambios de infraestructura, restauración y carga. Las pruebas STOMP existentes prueban autorización y no sustituyen una partida completa por WebSocket entre navegadores.

La batería original y este informe deben usarse juntos. Los fallos encontrados ya justifican corregir y repetir las regresiones antes de ampliar la ejecución; los casos pendientes no quedan aprobados por el resultado de las pruebas automatizadas.
