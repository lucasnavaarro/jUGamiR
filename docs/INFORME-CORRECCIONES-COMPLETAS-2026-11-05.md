# Correcciones de la campaña completa de jUGamiR

Se corrigieron en el directorio de trabajo los defectos reproducidos de los **40 casos con fallo** y se aplicaron las **ocho decisiones confirmadas por Lucas**. Los cambios se verifican en QA aislado; no se ha modificado producción ni publicado una PR con este trabajo.

La correspondencia entre cada caso, su corrección y sus comprobaciones está en la [matriz de correcciones](correcciones-completas-2026-11-05/MATRIZ-CORRECCIONES.md), con [evidencias estructuradas](correcciones-completas-2026-11-05/casos-corregidos.json) y [resumen de ejecución](correcciones-completas-2026-11-05/resumen.json). Los 40 casos no representan necesariamente 40 causas independientes.

## Comportamientos aplicados

- Email y nick sin duplicados por mayúsculas o espacios exteriores; el nick mantiene su escritura para mostrarlo.
- Tiempo por pregunta de **5–600 segundos** en API e interfaz.
- Listado público con el nick del anfitrión, sin su email.
- Entrenamiento privado e individual.
- Resumen, detalle y progreso competitivo basados en partidas terminadas. Abandonos y expulsiones cuentan como jugadas sin victoria cuando la partida termina; las canceladas y los entrenamientos quedan fuera.
- Banco compartido entre todos los profesores; jugadores sin permiso de gestión.
- Identificador CSV opcional, generado de forma estable cuando falta e indicado en el resultado. Reintentar el mismo contenido no duplica su pregunta.

[Decisiones guardadas](correcciones-completas-2026-11-05/DECISIONES.md).

## Cambios de mayor impacto

**Partidas y persistencia.** Unión, inicio y cambios de participantes usan el mismo bloqueo. La capacidad y el orden de unión permiten sustituir a un expulsado conservando su registro. Los eventos STOMP se publican después de confirmar la transacción: el fallo real inyectado al commit revierte la respuesta sin emitir `RESULTADO`. El control de acceso de un socket expulsado no provoca rollback de la expulsión.

**Historial y banco.** Eliminar una pregunta ya utilizada la retira de futuras selecciones sin borrar sus respuestas ni sus instancias de partida. Se rechaza editar su contenido/solución. La selección bloquea y refresca la pregunta para evitar leer una versión anterior a una edición concurrente. Las opciones se validan antes de modificar datos. La asignatura referenciada y la cuenta con historial reciben un rechazo de negocio, y la paginación está acotada a 100 resultados.

**Importaciones e imágenes.** El CSV usa un parser que respeta comillas, separadores dentro del texto, BOM y saltos de línea. El flujo HTTP importa pregunta y respuestas en una transacción, con savepoint por fila; comprueba la salida y el tiempo del proceso. Un error de opciones no deja una pregunta huérfana. Los JPEG aplican orientación EXIF antes de eliminar metadatos, el PNG conserva alfa y se rechazan atributos de enlace simbólico de ZIP. Los límites de multipart y Nginx permiten las imágenes admitidas por el servicio y el presupuesto de ZIP, sin confundirlos con el antiguo límite HTTP de 1 MiB.

**Cliente y errores.** El lobby reconcilia eventos perdidos mediante REST, borra errores de red al recuperarse y la partida espera a cargar el estado antes de aplicar eventos. Recargar una partida terminada recupera su ganador. El login puede reintentarse tras un error de red. Se corrigieron selección de categorías por teclado, nombres accesibles y desbordamiento reproducido a 360 px. El entrenamiento descarta respuestas tardías de un criterio anterior y los períodos de estadísticas llegan explícitos del servidor.

**Operación.** `/actuator/health` se dirige al backend. `auth/me` queda fuera del limitador de intentos de login; la protección de login se conserva. SMTP tiene timeouts y una respuesta controlada de indisponibilidad. JSON, tipos, métodos, conflictos y recursos ausentes reciben respuestas HTTP específicas sin filtrar detalles internos.

## Verificación y límites

Se verificaron **97 variantes** y se guardaron **155 observaciones incluyendo repeticiones**, con cero fallos en las últimas ejecuciones de cada variante. Los recorridos de interfaz finales pasaron en **Chrome, Firefox y WebKit**.

La suite Java final pasa **73 pruebas**, sin fallos, errores ni omisiones; el cliente pasa **8 pruebas Node**. Se reconstruyeron backend y frontend con sus Dockerfiles. Las regresiones incluyen PostgreSQL real, HTTP, STOMP, SMTP capturado, fallos controlados de proceso/BD/SMTP, subidas reales y navegadores aislados.

Se repitieron cinco veces las carreras de última plaza y de unión frente a inicio. También se comprobó por un socket real que un fallo diferido de commit no publica el resultado. Se verificaron CSV con comillas y multilínea, imágenes de 10 MiB y 16 millones de píxeles a ambos lados de sus límites, orientación y alfa, y rechazo de ZIP con enlaces. Los límites de 500 entradas, 200 MiB y presupuesto de procesamiento de ZIP siguen cubiertos por Java.

El historial de ejecuciones conserva los errores del arnés y las repeticiones: por ejemplo, un selector inicial apuntaba al control de jugadores en vez del tiempo y un contador CSV tenía un nombre distinto en la API. Se corrigieron las precondiciones antes de repetirlos. La ampliación a WebKit detectó además un error persistente de red en el lobby, que se corrigió y se volvió a comprobar.

Esto **no convierte los 222 casos originales en una aprobación completa**. Las variantes parciales de la campaña anterior siguen documentadas; no se ejecutó un lector de pantalla físico ni la matriz de dispositivos iOS/Android, ni se probó entrega a buzones reales. WebKit no acredita Safari físico. Tampoco se repitió el ensayo anterior de carga de 30 minutos con esta versión; se repitieron las carreras relacionadas con los defectos corregidos.

## Despliegue pendiente

Las correcciones incluyen Flyway **V6** para contar plazas activas y **V7** para unicidad normalizada, además de la V5 de las nueve correcciones previas. V7 se detiene si hay cuentas existentes que colisionan; **no fusiona ni borra usuarios**. El procedimiento guardado incluye consultas de solo lectura para comprobarlo antes de actualizar.

El endpoint de salud y los demás cambios seguirán con el comportamiento anterior en `jugamir.es` hasta desplegar la versión corregida. No se han creado usuarios ficticios ni leído OTP en la base de producción: todas las escrituras y fallos inducidos pertenecen a QA.

Al finalizar se guardaron logs e identificaciones de imágenes y se retiraron los contenedores y la red de `jugamir-fullqa-20261104`, conservando los volúmenes de pruebas.

[Pasos 1–6 de despliegue actualizados](PASOS-DESPLIEGUE-PENDIENTE.md) · [Informe original de la campaña](INFORME-BATERIA-COMPLETA-2026-11-05.md) · [Pendientes originales por caso](ejecucion-completa-2026-11-04/PENDIENTES-POR-CASO.md).

Ampliación posterior: [variantes pendientes, nuevas correcciones y límites externos](INFORME-AMPLIACION-PRUEBAS-2026-11-05.md).
