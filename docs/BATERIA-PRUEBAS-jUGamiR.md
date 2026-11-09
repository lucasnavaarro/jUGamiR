# Batería de pruebas funcionales de jUGamiR

Fecha de preparación: 30 de octubre de 2026.

Correcciones posteriores del 5 de noviembre: [cambios y verificación de los fallos reproducidos, con las ocho decisiones confirmadas](INFORME-CORRECCIONES-COMPLETAS-2026-11-05.md). El resumen de campaña que sigue es histórico; no representa una aprobación completa de todas las variantes.

Última campaña, 4–5 de noviembre de 2026: [informe y evidencias de los 222 casos](INFORME-BATERIA-COMPLETA-2026-11-05.md). Resultado: 133 OK, 40 con fallo, 40 parciales, 8 bloqueados por criterio de producto y 1 no aplicable. Tener registro de todos los casos no significa haber completado todas sus variantes.

Ejecución automatizada parcial del 4 de noviembre de 2026: consultar [resultados y fallos reproducidos](RESULTADOS-PRUEBAS-2026-11-04.md). La ejecución parcial no aprueba los casos o variantes pendientes.

Los nueve fallos reproducidos se corrigieron posteriormente: [cambios y pruebas de verificación](CORRECCIONES-PRUEBAS-2026-11-04.md).

Esta batería reúne 222 casos en 21 áreas para validar el cliente React, la API Spring, las partidas STOMP y la persistencia PostgreSQL de jUGamiR. Cada caso incluye una acción reproducible y un resultado que se debe comprobar. Es un plan de ejecución; el estado de cada caso está en el informe enlazado y no acredita por sí solo que producción funcione correctamente.

La referencia es el código del directorio de trabajo, incluidos los cambios locales existentes en `JuegoService`, `StatsService`, `JwtAuthFilter` y el esquema. Antes de ejecutar, registrar el commit o imagen desplegada y contrastar sus diferencias con esta referencia. Ninguna batería finita garantiza todas las combinaciones posibles; aquí se cubren las funciones identificadas, sus límites y las interacciones de mayor riesgo.

## Cómo ejecutar y registrar resultados

Prioridades: **P0** bloquea una publicación por pérdida de acceso, datos, integridad del juego o aislamiento entre usuarios; **P1** cubre funciones principales y errores habituales; **P2** cubre presentación y casos secundarios.

Entornos:

- **L**: lectura sobre producción; usar cuentas propias, sin modificaciones.
- **C**: comprobación controlada en producción con cuentas y partidas de prueba identificadas. Escribe datos y puede enviar correo. Ejecutar una vez por variante, sin bucles masivos. Las partidas públicas serán visibles para usuarios reales; preferir su réplica en preproducción.
- **S**: preproducción o instalación local aislada. Incluye importaciones, borrados, manipulación del banco, concurrencia, límites, fallos de infraestructura y carga. Usar PostgreSQL y proxy equivalentes a producción, datos sintéticos y correo capturado.

Todos los casos S se pueden ejecutar en preproducción. No apuntar automatizaciones de escritura a producción por defecto. Antes de un caso C, comprobar que todos los objetos pertenecen a esta ejecución; si afecta al banco compartido o a otro usuario, realizarlo en S.

Para cada ID registrar: estado `PENDIENTE / OK / FALLO / BLOQUEADO / NO APLICA`, versión, fecha, entorno, navegador, cuenta de prueba, pasos concretos, esperado, obtenido, evidencia y defecto asociado. Un caso con varios valores se aprueba solo si pasan todos; anotar cada variante. Guardar capturas de los clientes, peticiones y respuestas REST, eventos STOMP y, cuando proceda, comprobaciones de BD de solo lectura. Ocultar contraseñas, JWT, cookies, OTP y enlaces de recuperación en las evidencias.

Para errores REST se exige rechazo controlado, mensaje utilizable y ausencia de mutaciones. Según los controladores actuales: validación y negocio suelen responder 400, falta de autenticación 401, acceso denegado 403, estado incompatible 409 y límite 429. No convertir cualquier 4xx en éxito: comprobar la causa y el estado final. Un 500 ante una entrada prevista es un fallo. Los bloqueos STOMP se verifican por ausencia de datos y rechazo de la operación, sin exigir códigos HTTP tras el handshake.

## Preparación de cuentas y datos

Crear una ejecución identificada, por ejemplo `QA_20261030_01`, y prefijar con ella los nicks y objetos sintéticos. Registrar los IDs devueltos; no asumir IDs del banco real.

| Recurso | Preparación |
|---|---|
| J1, J2, J3 | Jugadores en perfiles de navegador independientes; J1 será anfitrión inicialmente. |
| J4 a J7 | Jugadores para llenar seis plazas y probar un séptimo acceso en S. |
| J0 | Jugador nuevo sin historial para vacíos y entrenamiento. |
| P1 y P2 | Profesores con buzones controlados cuyo dominio sea exactamente `ugr.es`. En S, capturar el correo; no usar buzones ajenos. |
| X | Jugador autenticado que nunca pertenece a las partidas objetivo. |
| Buzones alternativos | Correo nuevo de jugador y otro `ugr.es` para profesor; ambos bajo control del probador. |
| Banco sintético en S | Categorías A y B, asignaturas QA_A y QA_B, preguntas publicadas de las tres dificultades, borradores y anuladas; opciones con una sola correcta conocida. |
| Preguntas especiales | Una sin imagen, otra con una imagen, otra con varias separadas por ` | `; una categoría sin preguntas elegibles y otra con solo dos preguntas. |
| Archivos | CSV UTF-8 con `;`; PNG/JPEG reales; variantes incorrectas y ZIP construidos solo para la instalación aislada. |
| Partidas base | Privada de J1/J2/J3, pública de prueba, entrenamiento individual; anotar categoría, dificultad, plazo y umbral de quesito. |
| Herramientas | DevTools con Network y WebSocket, dos navegadores y móvil; cliente REST que preserve cookies HTTPS; acceso de solo lectura a BD en S. |

Guardar antes y después los contadores e IDs afectados. Limpiar mediante acciones de la aplicación y únicamente objetos identificados. El borrado de preguntas puede afectar a su historial: reservarlo para S y revisar sus efectos antes de usarlo como limpieza.

## 1 Acceso y navegación

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| NAV-01 | P0 | L | Abrir el dominio HTTPS y consultar `/actuator/health`. | Web servida y salud `UP`; sin detalles internos innecesarios. |
| NAV-02 | P1 | L | Entrar por HTTP en el dominio público. | Redirección a HTTPS con ruta conservada. |
| NAV-03 | P1 | L | Abrir y recargar directamente `/login`, `/register` y `/forgot-password`. | La SPA resuelve cada pantalla, sin 404 del proxy. |
| NAV-04 | P0 | L | Sin sesión, abrir `/jugador`, `/profesor`, `/perfil` y `/partida/ID`. | Acceso protegido; no se muestran datos antes de autenticarse. |
| NAV-05 | P0 | L | Con J1 abrir las rutas de profesor y con P1 las de jugador. | Redirección o denegación coherente con el rol; API también protegida. |
| NAV-06 | P2 | L | Abrir una URL inexistente y usar la vuelta al inicio. | Pantalla de no encontrado y navegación funcional. |
| NAV-07 | P1 | L | Recargar dashboard, perfil y una partida propia mediante URL directa. | Sesión y estado recuperados, sin depender de navegación previa. |
| NAV-08 | P1 | C | Tras logout, usar Atrás y recargar una ruta protegida. | No se recupera acceso autenticado ni se sirven datos nuevos. |

## 2 Registro de jugadores y profesores

Precondición: buzón controlado y correo/nick nuevos en cada alta positiva. Registrar no inicia una sesión completa: el acceso posterior requiere login y código de correo.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| REG-01 | P0 | C | Registrar J0 con nombre, apellidos, email, nick y contraseña válidos. | API 202, confirmación visible y cuenta de jugador única; login posterior posible. |
| REG-02 | P0 | C | Registrar P1 con departamento y correo `usuario@ugr.es` controlado. | API 202 y rol profesor; al entrar exige código del buzón institucional. |
| REG-03 | P0 | S | Registrar profesor con dominio externo, `sub.ugr.es`, `ugr.es.ejemplo` y sin dominio. | Rechazo del backend; no se crea cuenta ni perfil parcial. |
| REG-04 | P1 | S | Repetir un email existente con otro rol o nick. | Error comprensible; una sola cuenta con ese email. |
| REG-05 | P1 | S | Registrar jugador con nick ya usado. | Rechazo sin usuario huérfano. |
| REG-06 | P1 | S | Omitir cada campo obligatorio; enviar blancos, email inválido y campos específicos del rol vacíos. | Validación de cliente y servidor; sin registros incompletos. |
| REG-07 | P1 | S | Probar contraseña de 7 y 8 caracteres; omitir mayúscula, minúscula, número o símbolo por separado. | Solo pasa con mínimo 8 y los cuatro grupos. |
| REG-08 | P1 | S | Probar símbolos imprimibles variados, espacios, salto de línea y letras fuera de A-Z/a-z. | Política consistente: ASCII imprimible sin espacios y requisitos de composición. |
| REG-09 | P1 | S | Variar mayúsculas y espacios en email y nick ya existentes. | Tratamiento consistente entre registro y login; documentar duplicados por normalización como decisión o defecto. |
| REG-10 | P1 | S | Enviar dos altas simultáneas con el mismo email y después con el mismo nick. | Solo una se consolida, sin 500 ni perfiles parciales. |

## 3 Login y código de correo

Precondición: cuenta activa, capturador de correo en S y reloj controlable para caducidades. En C, no agotar límites compartidos por IP.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| AUT-01 | P0 | C | Login válido de J1 y P1; verificar el código recibido. | Login 202 con `challengeId`, verify 200 con JWT y rol correctos; dashboard adecuado. |
| AUT-02 | P0 | S | Login con contraseña errónea y con cuenta inexistente. | Mensaje de credenciales inválidas; no se entrega JWT ni desafío válido. |
| AUT-03 | P0 | S | Verificar sin `challengeId`, con uno inventado y con código de otra cuenta. | Rechazo; ninguna sesión emitida. |
| AUT-04 | P1 | S | Enviar código de longitud/formato inválidos y un código válido que empiece por cero. | Los inválidos fallan; el código se trata como texto y conserva ceros iniciales. |
| AUT-05 | P0 | S | Fallar cinco verificaciones y después aportar el código correcto del mismo desafío. | Desafío inutilizado; los intentos fallidos persisten. |
| AUT-06 | P0 | S | Verificar antes y después de los 10 minutos de vigencia. | Aceptación solo durante la vigencia; el cliente permite reiniciar login. |
| AUT-07 | P0 | S | Reutilizar un código ya consumido; verificarlo simultáneamente en dos clientes. | Un único consumo; las demás solicitudes fallan. |
| AUT-08 | P1 | S | Reenviar antes de 60 s, después de 60 s y una tercera vez tras dos reenvíos. | Reenvío prematuro y exceso rechazados; máximo dos reenvíos por desafío. |
| AUT-09 | P0 | S | Tras un reenvío, usar código anterior y nuevo; comprobar intentos y vencimiento original. | Solo sirve el nuevo; no reinicia los intentos ni prolonga los 10 minutos. |
| AUT-10 | P0 | S | Iniciar otro login de la misma cuenta y usar el desafío anterior. | Desafío anterior invalidado; el nuevo permite autenticarse. |
| AUT-11 | P0 | S | Desactivar una cuenta sintética antes de login o entre login y verify. | No obtiene sesión en ninguno de los dos escenarios. |
| AUT-12 | P1 | S | Simular SMTP caído y exceder límites de envío/login por cuenta e IP. | Error controlado o 429 según causa, sin sesión prematura ni envío ilimitado. |

## 4 Sesiones y renovación

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| SES-01 | P0 | C | Inspeccionar verify por HTTPS y la cookie de renovación. | Refresh solo en cookie `HttpOnly`, `Secure`, `SameSite=Strict`; ausente del JSON y almacenamiento JS. |
| SES-02 | P0 | S | Caducar JWT y abrir perfil/estadísticas con refresh vigente. | Renovación, nuevo JWT y reintento de la petición original con resultado correcto. |
| SES-03 | P0 | S | Con JWT caducado disparar varias peticiones protegidas a la vez en una pestaña. | Renovación compartida por `pendingRefresh`; cada petición termina sin bucles. |
| SES-04 | P0 | S | Eliminar, caducar o invalidar refresh y provocar un 401. | Se limpia sesión y aparece modal de sesión caducada; acceso posterior lleva al inicio. |
| SES-05 | P0 | S | Enviar JWT manipulado, mal formado, expirado o de versión anterior a `/api/auth/me`. | 401, sin acceso ni 500. |
| SES-06 | P0 | C | Entrar en dos navegadores y cerrar sesión en uno. | Cookie local eliminada; JWT y refresh anteriores de ambos dispositivos quedan revocados. |
| SES-07 | P1 | S | Logout sin cookie y logout con cookie ya revocada. | No reaparece sesión; el cliente termina el cierre incluso si el servidor rechaza cookie inválida. |
| SES-08 | P1 | S | Renovar desde dos pestañas simultáneas; cerrar sesión en otra pestaña. | Sin restauración de sesión revocada; las pestañas convergen al estado cerrado. |
| SES-09 | P1 | S | Simular fallo de red durante refresh y repetir al restablecerla. | Sin recursión de reintentos ni spinner indefinido; recuperación o salida comprensible. |
| SES-10 | P0 | S | Desactivar/eliminar cuenta sintética con JWT y refresh vigentes e intentar ambos accesos. | Cuenta inhabilitada no conserva acceso por tokens previos. |

## 5 Recuperación de contraseña

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| REC-01 | P0 | C | Solicitar recuperación de cuenta propia y abrir el enlace recibido. | Respuesta 202, enlace del dominio HTTPS correcto y formulario utilizable. |
| REC-02 | P1 | S | Solicitar recuperación de un email inexistente. | Misma confirmación pública; no se envía correo a otra cuenta. |
| REC-03 | P0 | S | Cambiar con enlace vigente; entrar con contraseña anterior y nueva. | Reset 200, anterior rechazada y nueva aceptada con OTP posterior. |
| REC-04 | P0 | S | Usar enlace inventado, consumido y caducado tras 15 min. | Rechazo sin cambio de contraseña. |
| REC-05 | P0 | S | Consumir el mismo enlace simultáneamente con dos contraseñas distintas. | Solo un cambio se confirma; no hay segundo consumo. |
| REC-06 | P1 | S | Solicitar un nuevo enlace y utilizar el anterior. | Solo el enlace más reciente puede usarse. |
| REC-07 | P0 | S | Mantener JWT, refresh y desafío OTP previos; completar reset. | Todos quedan revocados; también pierde acceso el socket ya suscrito. |
| REC-08 | P1 | S | Enviar contraseña débil o confirmación distinta; repetir sin pasar por UI. | Política de contraseña en servidor; UI rechaza confirmación distinta; sin consumo exitoso con datos inválidos. |

## 6 Perfil y eliminación de cuenta

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| PER-01 | P1 | L | Consultar perfil de J1 y P1. | Datos propios; nick para jugador, departamento para profesor; sin hashes/tokens. |
| PER-02 | P1 | C | Cambiar nombre/apellidos y nick propio o departamento; recargar. | Cambios persistentes y visibles en las pantallas que los usan. |
| PER-03 | P1 | S | Cambiar a nick/email ocupado y enviar campos vacíos o excesivos. | Validación controlada y sin cambios parciales. |
| PER-04 | P0 | S | Solicitar código de cambio de email con contraseña incorrecta. | No se emite código válido; límites por cuenta aplicados. |
| PER-05 | P0 | C | Solicitar código al nuevo buzón propio y confirmar con contraseña actual. | Código llega al nuevo correo; cambio válido exige ambos factores y devuelve `reauthenticate=true`. |
| PER-06 | P0 | S | Cambiar email sin código, con código erróneo, caducado o emitido para otro email/usuario. | Rechazo; email original conservado. |
| PER-07 | P0 | S | Tras cambiar email, usar JWT, refresh, OTP y enlace de reset anteriores. | Revocados; nuevo login usa email nuevo. |
| PER-08 | P0 | S | P1 solicita cambiar correo a dominio externo y después a otro `ugr.es` verificado. | Externo rechazado; institucional controlado aceptado. |
| PER-09 | P1 | S | Emitir segundo código de email e intentar el primero; dos confirmaciones simultáneas. | Primer código sustituido; una sola confirmación efectiva. |
| PER-10 | P0 | S | Eliminar jugador desechable sin historial, con historial y dentro de partida; repetir por variante. | Borrado o rechazo de negocio coherente, sin 500, huérfanos ni partida bloqueada. No asumir que el borrado con historial está soportado. |
| PER-11 | P1 | S | Eliminar profesor sin preguntas y otro con preguntas propias. | Sin preguntas: 204; con preguntas: rechazo y cuenta conservada. |
| PER-12 | P0 | S | Tras borrar cuenta con éxito, reutilizar tokens y abrir sockets existentes. | No conserva acceso; referencias restantes siguen siendo válidas. |

## 7 Crear y descubrir partidas

Precondición: J1 autenticado y categorías elegibles conocidas. En API verificar la configuración persistida a través del estado y, en S, de BD.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| CRE-01 | P0 | C | Crear privada con categorías/dificultades concretas, 30 s, dos plazas y un acierto por quesito. | Devuelve ID y código; anfitrión unido una sola vez; estado ESPERANDO. |
| CRE-02 | P1 | C | Crear pública y observar listado en otro cliente autenticado. | Aparece con plazas y dificultades correctas; privada ausente. |
| CRE-03 | P1 | S | Crear desde UI con 1 y 6 jugadores, 30 y 300 s, umbrales 1 y 30. | Extremos de los controles aceptados y configuración respetada. |
| CRE-04 | P1 | S | Vaciar categorías o dificultades en UI y enviar listas vacías por API. | Rechazo sin partida incompleta; no basta con bloquear solo el formulario. |
| CRE-05 | P1 | S | Enviar por API categorías inexistentes, mezcla válida/inválida y duplicadas. | No se ignoran silenciosamente selecciones inválidas ni se duplican categorías. |
| CRE-06 | P1 | S | Enviar jugadores 0, -1, 7; umbral 0, -1, 31; valores nulos o enormes. | Rechazo controlado según contrato aprobado; no crear configuraciones imposibles. |
| CRE-07 | P1 | S | Enviar tiempo 4, 5, 29, 30, 300, 301, 600 y 601 directamente a API. | DTO actual admite 5–600; UI admite 30–300. Registrar la diferencia y fijar contrato antes de cerrar el caso. |
| CRE-08 | P1 | S | Enviar enum desconocido, JSON mal formado, tipos incorrectos y campos requeridos ausentes. | 400 controlado; no se persiste partida. |
| CRE-09 | P1 | S | Doble clic al crear y reenvío tras timeout sin saber si se confirmó. | UI evita duplicación accidental; registrar si API genera dos partidas al repetir petición. |
| CRE-10 | P1 | S | Crear privada de una plaza, iniciar y jugar. | El servidor admite inicio con un jugador; no exige dos para esta configuración. |
| CRE-11 | P1 | L | Revisar datos visibles del listado público para cuentas propias. | Solo datos aprobados para ese listado; revisar expresamente la exposición actual de email del anfitrión. |

## 8 Lobby y pertenencia

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| LOB-01 | P0 | C | J2 y J3 entran en privada de J1 con su código. | Todos ven miembros, anfitrión y plazas actualizados sin recargar. |
| LOB-02 | P1 | S | Introducir código inexistente, vacío, con espacios y minúsculas. | Inválidos rechazados; normalización consistente con la UI; sin acceso a otra partida. |
| LOB-03 | P1 | C | Unirse a pública desde el listado. | Entrada confirmada y contador actualizado en los otros clientes. |
| LOB-04 | P0 | S | Usar `/unirse/publica/{id}` contra una privada. | Rechazo; no se elude el código mediante su ID. |
| LOB-05 | P1 | S | Repetir unión de J2 y abrir la misma sala en dos pestañas. | Una sola pertenencia; repetición no duplica plazas. |
| LOB-06 | P0 | S | Llenar seis plazas e intentar J7; después dos jugadores compiten por la última plaza. | Capacidad nunca superada; exactamente uno consigue la última. |
| LOB-07 | P0 | C | J2 intenta iniciar; después J1 inicia. | Solo anfitrión inicia; todos pasan a EN_CURSO y comparten orden de turnos. |
| LOB-08 | P0 | S | Dos inicios simultáneos y unión al mismo tiempo que inicio. | Un único inicio/orden; no entran miembros fuera del estado permitido. |
| LOB-09 | P0 | S | Unirse a partida EN_CURSO, TERMINADA o CANCELADA. | Rechazo sin nueva pertenencia. |
| LOB-10 | P1 | C | J2 abandona en espera; J1 abandona quedando J3. | Se libera plaza; anfitrión pasa al participante activo más antiguo. |
| LOB-11 | P1 | C | Último miembro abandona en espera. | Partida CANCELADA; desaparece del listado público si corresponde. |
| LOB-12 | P0 | S | J1 expulsa J2; J2 vuelve por código/API y conserva socket abierto. | Estado EXPULSADO, modal/expulsión de UI, no puede reingresar ni seguir recibiendo datos. |
| LOB-13 | P0 | S | J2 expulsa a J3; J1 se expulsa a sí mismo; expulsión en EN_CURSO. | Las tres operaciones se rechazan sin cambios. |
| LOB-14 | P0 | S | X consulta lobby/estado por ID y se suscribe al topic privado. | Acceso denegado sin datos de miembros ni preguntas. |
| LOB-15 | P0 | S | Expulsar a un jugador y después iniciar o transferir anfitrión. | Expulsado excluido del juego, turnos y elección de anfitrión; no consume plaza activa. |

## 9 Ruleta y selección de preguntas

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| RUL-01 | P0 | C | Jugador de turno gira; observar tres clientes. | Una categoría/pregunta común; solo el jugador de turno puede actuar. |
| RUL-02 | P0 | S | X y jugador fuera de turno llaman a `/girar`. | Rechazo sin pregunta ni avance de turno. |
| RUL-03 | P0 | S | Girar dos veces durante la animación o con pregunta pendiente, también simultáneamente. | Solo una pregunta activa del turno; segunda acción rechazada. |
| RUL-04 | P1 | S | Jugar con cada dificultad y con combinaciones de dificultades/categorías. | Todas las preguntas pertenecen a las categorías y dificultades seleccionadas. |
| RUL-05 | P0 | S | Incluir en banco borrador y anulada con el resto de filtros iguales. | Nunca aparecen en una partida; solo publicadas no anuladas. |
| RUL-06 | P1 | S | Consumir el banco reducido de dos preguntas de una categoría. | No se repiten dentro de la partida; agotamiento produce error comprensible sin pregunta corrupta. |
| RUL-07 | P1 | S | Girar con categoría seleccionada sin preguntas elegibles. | Error controlado; UI permite recuperarse o salir. Registrar bloqueo si no hay salida útil. |
| RUL-08 | P0 | S | Inspeccionar respuesta REST de girar, evento PREGUNTA y estado recuperado. | No incluyen `esCorrecta`, solución ni comentario que revele respuesta antes de contestar. |
| RUL-09 | P1 | C | Preguntas sin imagen, con una y varias; revisar opciones. | Imágenes y orden de opciones correctos, enunciado completo y sin romper diseño. |
| RUL-10 | P0 | S | Responder por API durante los 4,2 s previos a `emitidaEn`. | Rechazo; animación no permite contestación anticipada. |

## 10 Responder y pasar turno

Usar preguntas de solución conocida en S. Cada intento debe llevar el `preguntaPartidaId` recibido por STOMP o estado, además del `respuestaId` al contestar.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| RES-01 | P0 | C | Elegir respuesta correcta dentro del plazo. | Un acierto, progreso correspondiente y mismo jugador conserva el turno; resultado común. |
| RES-02 | P0 | C | Elegir respuesta incorrecta dentro del plazo. | Un fallo, solución mostrada y turno al siguiente activo. |
| RES-03 | P0 | C | Pasar una pregunta activa antes de vencer. | Una no respondida, pregunta cerrada y siguiente turno; no se registra fallo adicional. |
| RES-04 | P0 | S | No responder hasta vencer con todos los clientes conectados. | Servidor cierra una sola vez y avanza; no depende de que el cliente llame a pasar. |
| RES-05 | P0 | S | Enviar opción de otra pregunta o ID de respuesta inexistente. | Rechazo, pregunta actual sigue utilizable hasta su vencimiento. |
| RES-06 | P0 | S | Enviar `preguntaPartidaId` anterior, de otra sala, nulo o inventado. | Rechazo sin modificar la pregunta vigente ni sus contadores. |
| RES-07 | P0 | S | Repetir respuesta o pasar sobre una pregunta cerrada. | Ninguna segunda respuesta, contador ni avance adicional. |
| RES-08 | P0 | S | Responder simultáneamente desde dos pestañas del jugador de turno. | Una sola respuesta consolidada y un único resultado lógico. |
| RES-09 | P0 | S | Responder y pasar simultáneamente. | Solo una operación cierra la pregunta; contadores mutuamente coherentes. |
| RES-10 | P0 | S | Responder justo antes, en y después de `venceEn`; competir con scheduler. | Aceptada solo si el servidor recibe/procesa dentro de la ventana; cierre exactamente una vez. |
| RES-11 | P0 | S | Cambiar reloj local y añadir un supuesto tiempo cliente al JSON. | El servidor calcula tiempo desde `emitidaEn`; no confía en el valor cliente. |
| RES-12 | P0 | S | J2 o X responde/pasa pregunta de J1; actuar tras terminar/cancelar. | Rechazo en todos los escenarios sin cambios de turno. |
| RES-13 | P1 | S | Cortar respuesta HTTP después del envío y consultar estado al recuperar red. | La UI reconcilia el resultado real sin repetir puntuación ni quedar atascada. |

## 11 Quesitos y finalización

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| FIN-01 | P0 | S | Con umbral 3 acertar dos y después la tercera en categoría A. | Antes no hay quesito; a la tercera aparece uno y progreso reinicia a cero. |
| FIN-02 | P1 | S | Intercalar aciertos en A/B y errores, usando umbral 3. | Progreso independiente por categoría; error no borra aciertos acumulados según servicio actual. |
| FIN-03 | P0 | S | Seguir acertando una categoría cuyo quesito ya se ganó. | No se duplica quesito ni hay error por restricciones del progreso. |
| FIN-04 | P0 | S | Ganar todos los quesitos seleccionados, con una y varias categorías. | TERMINADA, fecha de fin, una VICTORIA y DERROTA de los rivales activos. |
| FIN-05 | P0 | C | Ver final en todos los clientes, recargar y volver a entrar. | Mismo ganador/quesitos/estado final; no vuelve a permitir girar o responder. |
| FIN-06 | P0 | S | En partida de tres, abandona el jugador de turno con pregunta abierta. | Quedan dos, siguiente activo juega; pregunta anterior no bloquea ni avanza después el turno nuevo. |
| FIN-07 | P0 | S | En partida de tres, abandona uno fuera de turno. | Turno actual conservado; el abandonado se omite en ciclos siguientes. |
| FIN-08 | P0 | S | En partida de dos, uno abandona. | Restante gana y partida termina; abandono no figura como derrota ordinaria. |
| FIN-09 | P0 | S | Abandonar partida individual/entrenamiento y hacer dos abandonos simultáneos. | Sin ganador inexistente, 500 ni partida EN_CURSO sin activos; estado final coherente. |
| FIN-10 | P0 | S | Abandonar al mismo tiempo que responder, vencer o ganar último quesito. | Un desenlace consistente, sin doble victoria ni doble cierre. |
| FIN-11 | P1 | S | Intentar abandonar una partida terminada/cancelada y repetir abandono ya realizado. | Rechazo o efecto único; historial final no cambia. |

## 12 Entrenamiento adaptado

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| ENT-01 | P0 | C | J0 abre entrenamiento sin historial y lo inicia. | Categorías marcadas como Sin datos; privada individual con `modoEntrenamiento=true` y juego utilizable. |
| ENT-02 | P1 | S | Elegir TIEMPO con MEJORAR y AFIANZAR sobre datos conocidos. | MEJORAR ordena lentas primero; AFIANZAR rápidas primero. |
| ENT-03 | P1 | S | Elegir ACIERTOS con MEJORAR y AFIANZAR. | MEJORAR ordena menor porcentaje primero; AFIANZAR mayor primero. |
| ENT-04 | P1 | S | Seleccionar mezcla de categorías con y sin historial y revisar POST de creación. | Con historial reciben pesos n, n-1, etc. según orden; sin datos reciben 1; solo seleccionadas. |
| ENT-05 | P1 | S | Crear entrenamiento de dos categorías con pesos 3 y 1; usar muestreo aislado. | Selección compatible con probabilidades 75/25, sin exigir esa proporción exacta en pocas tiradas. |
| ENT-06 | P0 | S | Enviar pesos negativos, todos cero, enormes, categorías inexistentes o no seleccionadas. | Rechazo controlado; no sortea categorías ajenas ni provoca 500/desbordamiento. |
| ENT-07 | P1 | S | Alternar rápidamente criterio/dirección con respuestas de red retrasadas. | Lista, etiquetas, barras y pesos corresponden a la última selección. |
| ENT-08 | P1 | S | Quitar todas las categorías/dificultades y exceder rangos de tiempo/umbral. | Mensajes de validación; no inicia entrenamiento inválido. |
| ENT-09 | P0 | S | Guardar estadísticas antes/después de terminar entrenamiento con aciertos/fallos/pases. | Entrenamiento no modifica estadísticas competitivas ni sus gráficas/progreso por asignatura. |
| ENT-10 | P1 | S | Intentar unión de J2 a entrenamiento individual y crear entrenamiento público/multijugador por API. | La variante individual no admite segundo jugador; configuraciones alternativas requieren contrato explícito y validación. |

## 13 Estadísticas y gráficas

Preparar en S una cuenta sin historial, otra con una partida competitiva finalizada con 2 aciertos, 1 fallo y 1 pase, y dos respuestas correctas en 2 y 4 s y una incorrecta en 3 s. Total: 3 contestadas, 1 no respondida, 50 % de acierto global incluyendo pase, 3 s de media de respuestas. Por categoría/asignatura, si todas pertenecen a A, porcentaje sobre respuestas registradas: 2/3 = 66,67 %. El pase no genera `RespuestaJugador`. No exigir que ambas métricas coincidan: deben estar etiquetadas y documentadas correctamente.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| EST-01 | P1 | L | J0 abre estadísticas y progreso sin datos. | Ceros/listas vacías y estado vacío útil; no NaN, infinito ni gráfica rota. |
| EST-02 | P0 | S | Consultar resumen de la cuenta sintética anterior y contrastar contadores. | 2 aciertos, 1 fallo, 1 no respondida, 3 contestadas, 50 % global y media 3000 ms con tolerancia del fixture. |
| EST-03 | P1 | S | Preparar una victoria y una derrota competitivas. | Dos partidas, una victoria y 50 % de victorias. |
| EST-04 | P1 | S | Consultar categoría A y sus asignaturas; cambiar a B. | Solo preguntas correspondientes; aciertos, fallos, porcentajes y tiempos correctos por grupo. |
| EST-05 | P0 | S | Mantener una partida competitiva en curso con respuestas registradas. | Contrastar resumen y detalle: el código filtra finalizadas para contadores y admite respuestas en curso para ciertos detalles. Resolver/rotular esa diferencia antes de aprobar coherencia. |
| EST-06 | P1 | S | Preparar resultados ABANDONADA, EXPULSADO y CANCELADA. | Revisar denominador de partidas/victorias y etiquetas; no asumir que todos equivalen a una partida jugada normal. |
| EST-07 | P1 | S | Preparar historial que abarque exactamente 14, 15, 60 y 61 días. | Progreso agrupa por día hasta 14, semana hasta 60 y mes a partir de 61; orden cronológico. |
| EST-08 | P1 | S | Preparar dos partidas de un período con distinto número de respuestas/tiempos. | Gráfica global usa promedio de porcentajes y tiempos medios por partida según implementación; no confundir con promedio ponderado del resumen. |
| EST-09 | P1 | S | Consultar progreso por categoría con categoría válida sin datos y con ID inválido. | Vacío o error controlado según contrato; no mezcla categorías ni produce 500. |
| EST-10 | P1 | S | Terminar partida cerca de medianoche/cambio de semana y cambiar zona del cliente. | Fechas y períodos consistentes; documentar zona usada por servidor y presentación. |
| EST-11 | P0 | S | X intenta obtener estadísticas de J1 manipulando IDs/query; comparar J1 y J2. | La identidad procede del usuario autenticado; ningún dato privado ajeno. |
| EST-12 | P1 | C | Terminar una partida propia y volver a estadísticas sin borrar almacenamiento. | Datos actualizados, unidades ms/s correctas, decimales y leyendas comprensibles. |

## 14 Gestión de asignaturas y preguntas

Precondición: profesores y banco sintético en S. Los endpoints actuales de profesor no limitan las operaciones al autor; verificar explícitamente si la gestión compartida por P1/P2 es el comportamiento aprobado.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| PRO-01 | P1 | L | P1 lista asignaturas y categorías con asignaturas. | Nombres, relación con categoría y contadores visibles coherentes. |
| PRO-02 | P1 | S | Crear QA_A, editar nombre/categoría y recargar. | Cambios persistentes y visibles en búsqueda y selección. |
| PRO-03 | P1 | S | Crear/renombrar a duplicado; nombre vacío; categoría inexistente. | Error controlado y sin duplicado ni modificación parcial. |
| PRO-04 | P0 | S | Borrar asignatura vacía y después otra con preguntas/historial. | Vacía: 204; con referencias: operación coherente con política, sin huérfanos ni 500. Registrar política pendiente. |
| PRO-05 | P1 | S | Buscar por fragmento de identificador y enunciado, mayúsculas y sin coincidencias. | Coincidencias correctas, orden consistente y vacío bien presentado. |
| PRO-06 | P1 | S | Paginar 36 resultados con tamaño 35; cambiar búsqueda desde última página. | Sin omisiones/repeticiones indebidas; totales correctos y página reiniciada. |
| PRO-07 | P1 | S | API de búsqueda con página -1, tamaño 0, negativo y excesivo. | Rechazo/límite controlado; no 500 ni consumo sin cota. |
| PRO-08 | P1 | S | Editar identificador, título, enunciado, comentario, dificultad, asignatura e imágenes. | Cada campo persiste y se refleja en búsqueda y preguntas futuras. |
| PRO-09 | P1 | S | Duplicar identificador/enunciado; enviar asignatura inválida. | Rechazo sin cambios parciales. |
| PRO-10 | P0 | S | Editar textos y correcta usando IDs de opciones de otra pregunta. | Nunca modifica opciones ajenas; rechazo controlado. |
| PRO-11 | P0 | S | Publicada con ninguna/dos correctas, opciones vacías, órdenes duplicados o lista nula. | No consolida pregunta jugable incoherente; mostrar error útil. |
| PRO-12 | P1 | S | Marcar anulada, comprobar exclusión; desmarcar y volver a probar. | Solo vuelve a ser elegible si cumple estado publicado y filtros. |
| PRO-13 | P0 | S | P2 edita/borra pregunta de P1; J1 invoca los mismos endpoints. | Jugador recibe 403. Entre profesores, resultado coincide con política de gestión compartida que se apruebe. |
| PRO-14 | P0 | S | Borrar pregunta nueva y otra usada en partida/historial; comparar BD y estadísticas. | Nueva eliminada; para usada, verificar impacto explícito. El borrado actual elimina respuestas históricas y preguntas de partida: no aprobar conservación histórica si desaparecen. |
| PRO-15 | P0 | S | Editar/eliminar pregunta o su respuesta correcta mientras está activa en partida. | Política consistente, sin 500 ni bloqueo ni adjudicación silenciosa incorrecta del resultado. |

## 15 Importación CSV

Usar el formato real del proyecto, no una plantilla CSV genérica. Los importadores dividen por líneas y por `;`, y las opciones por ` | `; la compatibilidad con comillas, separadores incrustados y multilínea debe probarse expresamente.

Cabecera de referencia para preparar fixtures (contrastar con el CSV de la versión desplegada):

```text
Asignatura;Identificador;TituloIndice;Enunciado;Opciones;RespuestaCorrecta;Comentario;Anulada;Imagen;Dificultad
```

`Opciones` usa, por ejemplo, `1. Opción A | 2. Opción B | 3. Opción C | 4. Opción D`; `RespuestaCorrecta` indica el ordinal. La asignatura debe existir. Crear identificadores y enunciados exclusivos por ejecución.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| CSV-01 | P0 | S | Importar CSV pequeño válido con tres dificultades y preguntas de A/B. | Contadores de añadidas/duplicadas/errores verificables; preguntas, opciones, autor y estado coherentes. |
| CSV-02 | P1 | S | Reimportar el mismo CSV. | No duplica preguntas ni opciones; contador de existentes comprensible. |
| CSV-03 | P1 | S | Mezclar nuevas, duplicadas y filas inválidas. | Se identifican filas omitidas; totales coinciden con lo efectivamente persistido. |
| CSV-04 | P1 | S | Archivo vacío, solo cabecera, cabecera ausente e incompleta. | Error o cero filas explícito; no falso éxito con pérdida silenciosa. |
| CSV-05 | P1 | S | Asignatura inexistente y enunciado/identificador ausente. | Omitida o rechazada según contrato; detalle explica la causa y no deja opciones huérfanas. |
| CSV-06 | P1 | S | Acentos/ñ, UTF-8 con BOM, CRLF, comillas, `;` en texto y texto multilínea. | Texto fiel si formato soportado; si no, rechazo/diagnóstico explícito sin desplazar columnas silenciosamente. |
| CSV-07 | P1 | S | Dificultad desconocida o vacía; identificadores MIR de uno/dos años. | Normalización actual a MEDIO para dificultad desconocida y año extraído correctamente; UI informa del contrato. |
| CSV-08 | P0 | S | RespuestaCorrecta fuera de rango, no numérica, ANULADA/IMPUGNADA o sin opciones. | No queda publicada una pregunta normal sin exactamente una correcta; anuladas no se juegan. |
| CSV-09 | P1 | S | Subir extensión incorrecta y contenido no CSV con nombre `.csv`. | Rechazo/errores claros; extensión sola no implica importación válida. |
| CSV-10 | P0 | S | Interrumpir el proceso entre importar preguntas y respuestas; después reintentar. | No deja preguntas jugables incompletas; reintento recupera coherencia e informa del resultado real. |
| CSV-11 | P1 | S | Importar simultáneamente mismo lote desde P1/P2 y un lote grande acotado. | Sin duplicados ni bloqueo permanente; límites y timeout observables; interfaz no declara éxito antes de terminar. |

## 16 Imágenes individuales y ZIP

Límites de referencia: PNG/JPEG reales, 10 MiB y 16 millones de píxeles por imagen; ZIP hasta 500 entradas y 200 MiB descomprimidos y recodificados. Presupuesto de ZIP de 30 s comprobado entre operaciones. El límite de multipart/proxy puede rechazar antes que la validación de imagen. Probar los bordes sin atribuir al backend un rechazo del proxy.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| IMG-01 | P1 | S | Subir PNG/JPEG válidos en ruta `imagenes/QA_EJECUCION/...`. | URL devuelta accesible; imagen recodificada y utilizable en pregunta. |
| IMG-02 | P1 | S | Subir JPEG con EXIF/orientación y PNG transparente. | Metadatos eliminados; revisar orientación, legibilidad y transparencia esperada. |
| IMG-03 | P0 | S | Subir SVG/GIF/HTML/script renombrado `.png`, imagen truncada y MIME discordante. | Rechazo del contenido no admitido; no se publica contenido ejecutable. |
| IMG-04 | P1 | S | Probar tamaño justo debajo/en/encima de 10 MiB y 16 millones de píxeles. | Respeta límites de imagen y da error comprensible; sin memoria agotada. |
| IMG-05 | P0 | S | Usar ruta absoluta, `../`, variante codificada y fuera de `imagenes/`. | No escribe/borra fuera del directorio permitido. |
| IMG-06 | P1 | S | ZIP válido con varios PNG/JPEG bajo `imagenes/` y carpetas. | Extracción y URLs correctas; preguntas con varias imágenes se muestran. |
| IMG-07 | P0 | S | ZIP con traversal, enlace simbólico y nombres/rutas no permitidos. | Rechazo y ningún archivo fuera del almacenamiento autorizado. |
| IMG-08 | P1 | S | ZIP de 500/501 entradas y justo debajo/encima de presupuesto de 200 MiB. | Límites respetados, sin agotar disco/memoria; inspeccionar archivos que hayan quedado tras rechazo. |
| IMG-09 | P1 | S | ZIP con imagen inválida tras imágenes válidas y procesamiento superior al presupuesto. | Error controlado; documentar si quedan archivos parciales y cómo recuperarlos. No exigir timeout de CPU estricto de 30 s. |
| IMG-10 | P0 | S | Sobrescribir ruta usada por otra pregunta; borrar una imagen compartida y una inexistente. | Comportamiento compartido explícito, sin borrar archivos ajenos al ámbito permitido ni corrupción silenciosa. |
| IMG-11 | P1 | S | Reiniciar contenedor tras subir imagen y consultar una imagen semilla y otra subida. | Ambas siguen disponibles por sus mecanismos de persistencia. |
| IMG-12 | P1 | L | Pedir imagen inexistente y ruta con extensión no servida. | Sin listado del directorio ni detalles del sistema; la UI tolera imagen ausente. |

## 17 WebSocket y recuperación de conexión

Precondición: clientes independientes dentro/fuera de una sala. En HTTPS comprobar `wss://DOMINIO/ws`, CONNECT con Authorization y eventos de `/topic/lobby/{id}`, `/topic/juego/{id}` y `/topic/partidas-publicas`.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| WS-01 | P0 | C | Jugar desde dos navegadores y móvil a través del proxy real. | Handshake y STOMP operativos; lobby, pregunta, resultado y turno se sincronizan. |
| WS-02 | P0 | S | CONNECT sin JWT, JWT inválido/expirado o revocado. | No obtiene suscripción ni datos protegidos. |
| WS-03 | P0 | S | X se suscribe a lobby/juego ajenos y a un destino inventado. | Rechazo; únicamente destinos permitidos y salas propias. |
| WS-04 | P0 | S | Cliente intenta publicar con SEND resultados/preguntas a topics. | Publicación rechazada; solo el servidor emite estado de juego. |
| WS-05 | P0 | S | Revocar sesión/cambiar email/expulsar tras SUBSCRIBE, sin cerrar socket manualmente. | No recibe eventos posteriores a la pérdida de autorización. |
| WS-06 | P0 | S | Desconectar al jugador durante pregunta hasta superar `venceEn`. | Scheduler cierra aunque no haya clientes; al volver ve turno/estado actual. |
| WS-07 | P0 | C | Recargar con pregunta abierta antes de vencer. | REST restaura `preguntaActual`, IDs y plazo original; no concede tiempo nuevo. |
| WS-08 | P0 | S | Perder PREGUNTA o RESULTADO durante corte y reconectar. | Cliente recupera estado vigente sin tener que esperar al siguiente evento. |
| WS-09 | P1 | S | Cortar/redescubrir red repetidamente y navegar entrando/saliendo de sala. | Sin listeners duplicados, contadores dobles ni sockets/timers acumulados. |
| WS-10 | P0 | S | Caducar JWT con socket abierto y refresh vigente. | Renueva/reconecta con token actual; no pierde acceso legítimo ni conserva permisos revocados. |
| WS-11 | P1 | S | Mantener conexión ociosa y probar detrás de ambos proxies durante varios minutos. | Heartbeats y timeout compatibles; sin desconexión permanente. |
| WS-12 | P1 | S | Alta latencia, cambio Wi-Fi/datos y móvil en segundo plano. | Al volver, resincroniza reloj/estado; botones no permiten actuar sobre turno obsoleto. |

## 18 Contrato de API y aislamiento

Estos son casos de validación funcional de permisos y entradas. No sustituyen una auditoría de seguridad.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| API-01 | P0 | S | Sin sesión invocar perfil/lobby/juego/stats/profesor; repetir como J1/P1 según rol. | 401 sin sesión; 403 para profesor/estadísticas/juego de rol incompatible; ningún efecto lateral. |
| API-02 | P0 | S | Cambiar en localStorage el rol/ID mostrado y usar JWT real sin alterar. | Backend mantiene identidad y permisos del token/cuenta. |
| API-03 | P1 | S | REST y handshake desde origen distinto al configurado. | Navegador no obtiene acceso CORS/WebSocket no permitido; no se refleja cualquier origen. |
| API-04 | P1 | S | Payload con etiquetas HTML en nombre, nick, enunciado, opción y errores. | Se representa como texto o se sanea; no ejecuta código en otros clientes. |
| API-05 | P1 | S | Texto con comillas y caracteres de consulta en búsqueda/edición. | Se procesa como dato sin alterar consultas ni exponer errores internos. |
| API-06 | P1 | S | IDs negativos, cero, inexistentes y fuera de rango en cada familia de endpoints. | Rechazos controlados; no 500 por ausencia o conversión prevista. |
| API-07 | P1 | S | Método HTTP incorrecto y tipos de contenido no admitidos. | Respuesta controlada sin ejecutar la operación. |
| API-08 | P1 | S | Provocar validación, conflicto y error interno controlado. | Contrato de error utilizable por UI; ningún SQL, stack trace o secreto en respuesta. |

## 19 Compatibilidad y accesibilidad de la interfaz

Repetir NAV-03, AUT-01, CRE-01, LOB-01, RES-01/02/03, WS-07 y PER-02 en Chrome/Firefox de escritorio, Safari macOS/iOS y Chrome Android disponibles en el proyecto. Registrar versión exacta. Probar 360, 390, 768 y 1440 px y orientación móvil horizontal/vertical.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| UI-01 | P1 | L | Recorrer pantallas en tamaños anteriores y zoom 200 %. | Sin controles inaccesibles ni solapes que impidan operar; enunciados/opciones largos legibles. |
| UI-02 | P1 | C | Completar login, código, creación, respuesta y perfil solo con teclado. | Foco visible y orden lógico; botones y errores accesibles. |
| UI-03 | P1 | L | Revisar formularios y modales con lector de pantalla. | Campos etiquetados, errores asociados, foco controlado y salida del modal posible. |
| UI-04 | P1 | C | Abrir imagen ampliada, cerrar y responder desde móvil. | Se puede cerrar, no bloquea scroll ni selección, vuelve foco apropiado. |
| UI-05 | P1 | S | Desactivar animaciones del sistema y jugar con lector/teclado. | Estado comprensible; temporización no depende del fin de una animación visual. |
| UI-06 | P1 | S | Retrasar o fallar cada carga inicial de listas, perfil y estadísticas. | Carga/error/vacío diferenciados; reintento o navegación posible, sin pantalla en blanco. |
| UI-07 | P2 | L | Revisar contraste, categorías y respuestas correctas/incorrectas. | Color acompañado por texto/icono o indicación comprensible. |
| UI-08 | P1 | S | Doble clic, Enter repetido y navegación durante envío de formulario. | Botones reflejan estado de envío y no generan mutaciones accidentales repetidas. |

## 20 Persistencia y operación cliente servidor

Ejecutar en S con copias y volúmenes aislados. Los reinicios de producción requieren una ventana de mantenimiento específica y quedan fuera de esta ejecución documental.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| OPS-01 | P0 | S | Reiniciar backend con pregunta pendiente; volver antes/después del plazo. | Plazos persistidos; recuperación o cierre único tras arranque, sin reiniciar tiempo. |
| OPS-02 | P0 | S | Recrear frontend/backend conservando volúmenes. | Usuarios, partidas, preguntas e imágenes persisten; proxy vuelve a enrutar. |
| OPS-03 | P0 | S | Restaurar copia de BD y volumen de imágenes en una instalación aparte. | Login, banco, imágenes y referencias utilizables; versión de migraciones compatible. |
| OPS-04 | P0 | S | Arranque desde cero y actualización de una copia previa con Flyway V2–V4. | Semillas coherentes; migraciones sin pérdida inesperada; contenido base no crea cuenta accesible de demostración. |
| OPS-05 | P1 | S | Cortar BD/SMTP/almacenamiento por separado y restaurarlos. | Errores controlados, transacciones coherentes y recuperación sin procesos bloqueados. |
| OPS-06 | P1 | S | Simular fallo de disco o permisos al subir imágenes/importar. | No se anuncia éxito falso; referencias y archivos parciales identificables. |
| OPS-07 | P0 | S | Responder y forzar rollback/fallo de BD antes de commit, con otro cliente suscrito. | Ningún cliente queda consolidando un RESULTADO que no se persistió; recuperación REST corrige cualquier evento anticipado. |
| OPS-08 | P1 | S | Verificar reinicio del scheduler con más de 100 preguntas vencidas. | Cierra todos los lotes eventualmente, sin doble conteo ni starvation. |

## 21 Rendimiento y concurrencia sostenida

Medir primero un escenario de 2 clientes y después 10/25/50 usuarios sintéticos, sin superar la capacidad acordada de preproducción. Son escalones iniciales propuestos, no capacidad acreditada. No ejecutar carga sobre producción. Separar tiempos de red/servidor, render y espera de animación de 4,2 s.

Como criterio inicial a acordar antes de medir: p95 REST inferior a 1 s para operaciones ordinarias y propagación STOMP p95 inferior a 1 s en la red de pruebas, sin 5xx ni divergencias; importaciones y builds se miden aparte. Guardar CPU, RAM, conexiones BD, colas, latencia y errores por endpoint.

| ID | Prioridad | Entorno | Acción | Resultado esperado |
|---|---|---|---|---|
| REN-01 | P1 | S | Mantener salas de 2 y 6 jugadores con varios turnos completos. | Latencias dentro del objetivo acordado y mismo estado final en todos los clientes. |
| REN-02 | P1 | S | Ejecutar varias salas simultáneas y espectadores del listado. | Sin eventos cruzados ni degradación desproporcionada; recursos estables. |
| REN-03 | P1 | S | Ejecutar 30 min de juego/reconexiones y revisar recursos antes/después. | Sin crecimiento sostenido de sockets, timers, memoria o conexiones BD. |
| REN-04 | P1 | S | Consultar estadísticas/búsqueda con historial y banco grandes. | Respuesta dentro del presupuesto; paginación/carga no bloquean partidas. |
| REN-05 | P1 | S | Importar lote acotado mientras otras cuentas juegan. | Operaciones interactivas mantienen integridad y latencia aceptable; detectar bloqueos BD. |
| REN-06 | P0 | S | Repetir carreras de última plaza, respuesta/pase y abandono/vencimiento. | Invariantes siempre válidas; registrar frecuencia e interleaving de cualquier fallo. |

## Casos prioritarios por discrepancias del código actual

Estas observaciones proceden de lectura estática y orientan la ejecución; no son fallos confirmados en la versión publicada.

| Casos | Observación a resolver |
|---|---|
| CRE-04 a CRE-07, ENT-06 | DTO de creación valida tiempo 5–600 y nulidad de listas, mientras UI restringe 30–300, 1–6 jugadores y 1–30 aciertos. Revisar validación efectiva del servidor, listas vacías y pesos. |
| LOB-06, LOB-08 | La unión comprueba plazas y estado sin el mismo bloqueo explícito de partida usado para iniciar. Ejecutar carreras con última plaza e inicio. |
| LOB-15 | El inicio toma la lista de participantes sin filtrar expresamente expulsados al asignar orden de turno. Verificar que un expulsado no bloquea la partida. |
| EST-05, EST-06, EST-08 | Resumen, detalle y gráficas tienen filtros y promedios diferentes. Aprobar una semántica visible antes de validar solo cifras aisladas. |
| PRO-10, PRO-11, PRO-15 | La edición de respuestas toma IDs del payload; revisar pertenencia a pregunta y consistencia al editar preguntas jugadas. |
| PRO-14 | El borrado de preguntas elimina registros de respuestas y preguntas de partida; puede cambiar el historial de otros jugadores. |
| CSV-06, CSV-10 | Parser basado en `split` y dos procesos de importación separados; probar delimitadores, errores intermedios y reintento. |
| FIN-06, OPS-07 | Revisar pregunta pendiente al abandonar y eventos enviados antes de que una transacción termine de confirmar. |
| CRE-11, PRO-13 | El listado público devuelve email de anfitrión y la gestión docente es compartida según controles actuales. Confirmar política de producto. |

## Automatización disponible y siguiente cobertura

Existen pruebas Java para políticas de contraseña, cookies, dominio institucional, STOMP, imágenes y ZIP; `SecurityIntegrationTest` cubre desafíos OTP, revocación, cambios de correo, pertenencia a partidas, ocultación de solución y plazos/respuestas. Su presencia no significa que se hayan ejecutado ni sustituye los escenarios de navegador y proxy.

Desde la raíz del repositorio:

```bash
# Prueba unitaria existente de política de contraseña del cliente
npm --prefix frontend test

# Integración existente con PostgreSQL desechable y correo simulado
# Requiere Docker y las imágenes indicadas en README
sh scripts/test-security.sh

# Verificación de compilación del cliente, complementaria a las pruebas
npm --prefix frontend run build
```

`test-security.sh` crea BD/red temporales, carga esquema/banco y ejecuta Maven verify; no lee `.env`. Crea recursos Docker y puede descargar dependencias. Revisar el resumen de pruebas, incluidas las omitidas; un build correcto no acredita por sí solo cobertura de integración.

**No ejecutar `scripts/test_partidas.mjs` sobre producción.** Conserva cuentas/credenciales incrustadas, envía verify con contrato anterior sin `challengeId` y contiene una limpieza SQL de todas las partidas EN_CURSO, sin filtrar por ejecución. Antes de reutilizarlo debe adaptarse a cuentas sintéticas, contrato vigente y limpieza por IDs propios, con rechazo explícito de destinos de producción. Esta batería no modifica ni ejecuta ese script.

Orden propuesto para ampliar la automatización:

1. Integración Java con PostgreSQL real aislado: CRE-04/06, LOB-06/08/15, RES-08/09/10, FIN-06/10 y EST-02/05/09. Controlar reloj/fixtures para no esperar minutos reales.
2. E2E de navegador con contextos separados J1/J2/X/P1: acceso, OTP mediante correo de pruebas, creación privada, respuesta, recarga, reconexión y permisos. Playwright es una opción de implementación, aún no incorporada aquí.
3. Importador e imágenes: fixtures mínimos válidos e inválidos, aserciones sobre BD/archivos y aislamiento de cada ejecución.
4. Carga por HTTP/STOMP en preproducción con cuentas y salas sintéticas. Conservar resultados de invariantes, además de percentiles.

## Trazabilidad con las interfaces implementadas

| Interfaces | Familias de casos |
|---|---|
| `POST /api/auth/register/jugador`, `/register/profesor` | REG |
| `POST /api/auth/login`, `/verify`, `/reenviar-codigo` | AUT |
| `POST /api/auth/refresh`, `/logout`; `GET /api/auth/me` | SES, WS |
| `POST /api/auth/forgot-password`, `/reset-password` | REC |
| `GET/PUT/DELETE /api/usuario/perfil`; `POST /api/usuario/email/codigo` | PER |
| `GET /api/categorias`, `/con-asignaturas` | CRE, ENT, PRO |
| `POST /api/lobby/crear`; `GET /api/lobby/publicas` | CRE, ENT |
| `POST /api/lobby/unirse/publica/{id}`, `/unirse/privada/{codigo}` | LOB |
| `POST /api/lobby/iniciar/{id}`; `GET /api/lobby/{id}` | LOB, WS |
| `DELETE /api/lobby/abandonar/{id}`, `/expulsar/{id}/{jugadorId}` | LOB, FIN |
| `POST /api/juego/{id}/girar`, `/responder`, `/pasarse`; `GET /api/juego/{id}/estado` | RUL, RES, FIN, WS |
| `GET /api/stats/jugador`, `/categoria/{id}`, `/progreso`, `/progreso/categoria/{id}`, `/categorias-entrenamiento` | EST, ENT |
| `GET/POST /api/profesor/asignaturas`; `PUT/DELETE /api/profesor/asignaturas/{id}` | PRO |
| `POST /api/profesor/preguntas/importar`; `GET /api/profesor/preguntas/buscar`; `PUT/DELETE /api/profesor/preguntas/{id}` | PRO, CSV |
| `POST/DELETE /api/profesor/imagenes`; `POST /api/profesor/imagenes/zip`; `GET /imagenes/**` | IMG |
| `/ws`, `/topic/partidas-publicas`, `/topic/lobby/{id}`, `/topic/juego/{id}` | LOB, RES, FIN, WS |
| `/actuator/health`, rutas SPA, proxy y volúmenes | NAV, OPS, UI |

Referencias del repositorio: controladores y servicios en `backend/src/main/java/com/jugamir/backend`, validaciones DTO y `config/SecurityConfig`, `frontend/src/App.jsx`, formularios de creación/entrenamiento, `frontend/src/services/api.js`, `socket.js`, scripts de importación en `db`, pruebas existentes y README de despliegue.

## Orden recomendado y criterio de aceptación

1. **Comprobación tras despliegue, unos 20–40 min según correo y partida:** NAV-01/03/07, AUT-01 para ambos roles, SES-01, PER-01, CRE-01, LOB-01/07, RUL-01, RES-01/02/03, FIN-05, EST-12 y WS-01/07. Usar partida privada y cuentas propias; hacerla corta para poder completar FIN-05.
2. **Regresión de preproducción:** todos los P0 y P1, empezando por discrepancias del código. Tiempo según fixtures y automatización; no dar por cubiertas importación/borrado/caducidades con el recorrido corto.
3. **Compatibilidad y operación:** matriz de dispositivos, P2, restauración y carga antes de cambios relevantes de cliente, proxy, esquema o infraestructura.

Aceptar una versión cuando todos los P0 estén OK, cada P1 tenga resultado y los fallos restantes estén evaluados; no considerar OK un caso bloqueado o una diferencia de contrato pendiente. Una ejecución corta de producción acredita únicamente ese recorrido, no la regresión completa.

### Plantilla de acta de ejecución

```text
Ejecución:
Versión local de referencia:
Commit o imagen desplegada:
Entorno y URL:
Fecha y responsable:
Navegador y dispositivo:
Cuenta y objetos sintéticos usados:

ID y variante:
Estado: PENDIENTE / OK / FALLO / BLOQUEADO / NO APLICA
Precondiciones y pasos concretos:
Resultado esperado:
Resultado observado:
Evidencia sin secretos:
Defecto y prioridad:
Limpieza realizada y objetos que permanecen:
```

Ampliación posterior: [variantes pendientes, nuevas correcciones y límites externos](INFORME-AMPLIACION-PRUEBAS-2026-11-05.md).
