# Variantes pendientes por caso

Este archivo enumera el trabajo que todavía falta para completar los casos parciales. No son aprobaciones ni una afirmación de que todo lo restante dependa de buzones reales.

| Caso | Falta completar |
|---|---|
| REG-06 | Completar omisiones de departamento, emails mal formados y todos los mensajes de validación del formulario, además del conjunto API ya ejecutado. |
| AUT-06 | Comprobar visualmente OTP caducado y el reinicio del login; el vencimiento del servidor ya se probó con timestamps de fixture. |
| SES-04 | Completar en navegador las variantes de refresh caducado e invalidado; cookie ausente y limpieza/modal ya están observados. |
| SES-07 | Comprobar cierre visual con cookie revocada; los endpoints con/sin cookie y el cierre normal ya están ejecutados. |
| SES-08 | Completar renovación simultánea de dos pestañas; logout y convergencia de almacenamiento ya se ejecutaron. |
| REC-01 | Abrir el enlace de recuperación en el navegador y completar ese recorrido; envío SMTP capturado, origen y token ya verificados. |
| REC-08 | Completar confirmación de contraseña distinta en UI y comprobar que no consume el enlace; política débil/reintento API ya ejecutados. |
| CRE-02 | Completar el listado gráfico en vivo; creación y dos espectadores STOMP reales ya recibieron las listas correctas. |
| LOB-01 | Completar la secuencia gráfica de entradas en una sala ya abierta; API, emisiones de lobby y tres interfaces de partida ya tienen evidencia. |
| LOB-03 | Completar clic de unión desde la lista pública y su actualización gráfica en otro cliente; API y contador persistido ya probados. |
| LOB-05 | Completar lectura simultánea de la misma sala en dos pestañas del mismo usuario; duplicación de pertenencia por API ya rechazada. |
| RES-10 | Completar instrumentación del reloj del servidor para igualdad exacta con el vencimiento y carrera determinista con el scheduler. Se comprobaron antes/en timestamp fijado/después por SQL y HTTP. |
| FIN-10 | Repetir y registrar como variantes independientes todas las carreras de abandono/vencimiento; se ejecutaron abandono/última respuesta y el escenario de vencimiento dentro de REN-06. |
| ENT-01 | Completar todo el inicio gráfico de entrenamiento de J0; pantalla Sin datos, creación, botón y juego API ya se ejecutaron. |
| ENT-08 | Completar en UI dificultades vacías y extremos inválidos de tiempo/umbral; categorías vacías y límites del backend ya probados. |
| EST-12 | Comparar explícitamente cada contador/unidad/redondeo visible después de una partida; reapertura de estadísticas sin NaN y cálculos API ya verificados. |
| PRO-01 | Contrastar todos los contadores visibles en el listado gráfico; relación y contenido de las listas API ya ejecutados. |
| PRO-02 | Completar creación/edición desde los controles gráficos y recarga de selectores; persistencia API ya verificada. |
| PRO-05 | Completar las variantes de búsqueda en la interfaz; API por identificador/enunciado, mayúsculas y vacío ya ejecutada. |
| PRO-06 | Completar nueva búsqueda desde la última página del componente; paginación API sin duplicados/omisiones ya probada. |
| PRO-08 | Completar recarga gráfica y pregunta futura con todos los campos editados; respuesta HTTP y persistencia de la edición ya tienen evidencia. |
| PRO-12 | Completar la selección de la misma pregunta editada después de desanularla; persistencia de flags y exclusión de anuladas se probaron por separado. |
| CSV-07 | Completar identificadores con un solo año y casos de cambio de siglo, y revisar el mensaje visual del fallback de dificultad. |
| CSV-11 | Completar estado visual/carga del importador mientras termina el lote grande; concurrencia de lote duplicado e importación de 100 preguntas con juego concurrente ya ejecutadas. |
| IMG-06 | Completar una pregunta con imágenes procedentes del ZIP importado; ZIP, URLs y visualización de varias imágenes se verificaron por separado. |
| IMG-10 | Completar impacto sobre dos preguntas que comparten una imagen y definir política de sobrescritura/borrado; operaciones HTTP ya ejecutadas. |
| WS-08 | Completar pérdida de eventos concretos en la interfaz y reconciliación sin recarga manual; reconexión STOMP y estado REST ya probados. |
| WS-09 | La navegación quedó bloqueada antes de completar veinte ciclos. Falta repetir tras resolverlo e instrumentar listeners/timers; veinte sockets directos sí se cerraron. |
| WS-11 | Repetir sesión autorizada a través del proxy externo real de producción; dos minutos de WSS por Nginx TLS QA sí se ejecutaron. |
| WS-12 | Faltan iPhone/Android físicos, cambio Wi-Fi/datos y suspensión real del SO. Se ejecutó móvil táctil/red emulados. |
| API-04 | Completar los payloads HTML en nick, enunciado, opción y errores; nombre de perfil como texto se comprobó en navegador. |
| UI-05 | Falta lector de pantalla real y recorrido completo con teclado; movimiento reducido se activó y los controles se comprobaron. |
| UI-07 | Falta medición de contraste y revisión completa de indicadores. Los resultados con texto/iconos además de color se comprobaron. |
| UI-08 | Completar Enter repetido y navegación mientras se envía; doble clic desde el formulario envía una petición. |
| OPS-04 | Completar restauración de una copia histórica con datos de autenticación anteriores a V2–V4; arranque y V5 con progreso previo sí verificados. |
| OPS-05 | Cerrar el escenario conjunto de pérdida de almacenamiento. BD y SMTP se interrumpieron y restauraron por separado; los errores de permisos se probaron en OPS-06. |
| REN-01 | Completar línea base de dos jugadores equivalente a seis y objetivos acordados en el hardware de destino. Se midieron nueve salas, hasta cincuenta consultas y cincuenta sockets QA. |
| REN-02 | Completar espectadores del listado público al mismo tiempo que las salas bajo carga; salas concurrentes y espectadores se ejercitaron en fases separadas. |
| REN-03 | Falta perfil de heap/listeners/timers para descartar fugas. El ensayo de treinta minutos y las muestras de memoria/conexiones están completos. |
| REN-04 | Completar búsqueda docente sobre banco grande en la misma medición y su impacto en juego. Estadísticas de mil partidas y juego sobre 5000 preguntas sí tienen mediciones. |
