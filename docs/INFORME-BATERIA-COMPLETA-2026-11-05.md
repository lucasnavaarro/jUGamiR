# Resultados de la batería de jUGamiR

Actualización posterior: [correcciones de los 40 casos con fallo y de las ocho decisiones pendientes](INFORME-CORRECCIONES-COMPLETAS-2026-11-05.md). Los resultados siguientes conservan el estado observado antes de esas correcciones.

Campaña iniciada el 4 y revisada el 5 de noviembre de 2026. **Los 222 casos tienen registro y evidencia, pero no quedan todos completados ni aprobados.** Se distinguen funcionalidad observada, variantes pendientes y criterios de producto que todavía no permiten emitir un pasa/falla.

| Resultado | Casos |
|---|---:|
| OK en los entornos y variantes documentados | 133 |
| FALLO reproducido | 40 |
| PARCIAL, con variantes o capas pendientes | 40 |
| BLOQUEADO para decidir el criterio de producto | 8 |
| NO APLICA por una función inexistente en la aplicación | 1 |
| **Total** | **222** |

Los 40 casos fallidos **no son necesariamente 40 errores independientes**: por ejemplo, el tratamiento genérico de excepciones provoca 500 en varios casos distintos. PARCIAL y BLOQUEADO no se cuentan como aprobados. No sería correcto afirmar que se han ejecutado absolutamente todas las variantes de la batería.

[Matriz de los 222 casos](ejecucion-completa-2026-11-04/MATRIZ-222-CASOS.md) · [Detalles y evidencias por caso](ejecucion-completa-2026-11-04/casos.json) · [Resumen estructurado](ejecucion-completa-2026-11-04/resumen.json).

## Qué se ejecutó

Se registraron **584 variantes diferenciadas y 784 observaciones incluyendo las repeticiones**. Las repeticiones corrigieron precondiciones del arnés, como payloads incompletos, selectores de controles ocultos, esperas de carga y fixtures que infringían restricciones de la base. Se conserva el historial y el resultado final utiliza la última ejecución de una misma variante.

- **Producción:** navegación y consultas públicas de solo lectura en `https://jugamir.es/`, sin crear cuentas, enviar correos ni modificar datos. El endpoint de salud se volvió a consultar el 5 de noviembre.
- **QA:** aplicación real empaquetada en Docker, PostgreSQL propio, Nginx con HTTPS/WSS y certificado local, y SMTP capturado con Mailpit. No son simulaciones de los controladores o repositorios: se ejercitaron HTTP, STOMP, SQL y procesos Python reales.
- **Cuentas:** jugadores y profesores con correos ficticios, incluidos dominios `@ugr.es`. Se leyeron códigos en PostgreSQL y se contrastó su entrega en el capturador SMTP. No hacen falta múltiples buzones institucionales reales para estas pruebas funcionales.
- **Navegadores:** Chrome 157 y Firefox de escritorio, y WebKit de Playwright. Se probaron tamaños públicos y autenticados de 360, 390, 768 y 1440 px. Hubo una partida entre tres contextos independientes, uno con viewport y tacto móvil emulados.
- **Infraestructura:** reinicio/recreación de servicios, vencimiento persistido de preguntas, caída y recuperación de PostgreSQL/SMTP, errores de permisos, copia/restauración de BD e imágenes en otra instalación y fallo forzado al confirmar una respuesta.
- **Pruebas de código:** 59 Java y 8 del cliente correctas. Las Java incluyen límites exactos de imágenes/ZIP y una comprobación real del presupuesto de 30 segundos. Estas suites verdes no invalidan los fallos de los recorridos completos.

La referencia sigue siendo `d0010db6c6bb04424bded3f881cdf067a5b6f270` más los cambios locales, incluidas las nueve correcciones y V5. [Manifiesto de archivos](ejecucion-completa-2026-11-04/source-manifest.json). No se ha publicado esa versión ni se ha supuesto que sea la instalada en producción.

## Fallos de mayor impacto

| Casos | Reproducción y resultado observado |
|---|---|
| **LOB-08** | Una unión simultánea al inicio puede dejar un miembro dentro de una partida EN_CURSO sin `orden_turno`. Se reprodujo en cinco intentos de carrera. |
| **OPS-07** | Un trigger QA hizo fallar el commit de una respuesta. La BD revirtió la respuesta, pero un cliente ya suscrito recibió `RESULTADO`. El evento anuncia un resultado que no quedó persistido. |
| **PRO-14** | Borrar una pregunta utilizada elimina sus registros en `respuestas_jugador` y `preguntas_partida`, perdiendo evidencia histórica. Se verificó que una respuesta existente desaparecía tras el borrado. |
| **PRO-15** | Cambiar la opción correcta durante una pregunta activa cambia el resultado de quien responde según la solución original. El test terminó registrando esa respuesta como incorrecta. |
| **LOB-12, LOB-15, WS-05** | Se obtuvieron 500 en variantes de expulsión con un socket abierto y al ocupar una plaza liberada por expulsión. Esto es adicional al reparto de turnos de expulsados que ya se corrigió; requiere revisar transacción/notificación y orden de unión. |
| **WS-01** | Al empezar inmediatamente, la BD queda EN_CURSO pero la interfaz puede seguir mostrando el lobby. Esperando a que las tres interfaces estén suscritas, el inicio y el juego sí funcionan. Hay evidencia de ambas variantes. |
| **FIN-05** | Tras finalizar y recargar la URL de la partida, no se recupera la pantalla de resultado final esperada; se vuelve a representar el flujo del lobby. |
| **UI-06** | Al abortar la petición de login, el botón permanece deshabilitado. El usuario no puede volver a enviarla desde ese formulario. |
| **ENT-07** | Una respuesta tardía de TIEMPO reemplaza la lista de categorías de ACIERTOS elegida después. Se usaron estadísticas con órdenes diferentes para distinguir la lista correcta de la obsoleta. |
| **EST-10** | El mismo progreso semanal se titula «por semana» en UTC y «por día» en America/Los_Angeles. Cambia la etiqueta del período con la zona del navegador, aunque los datos del servidor sean los mismos. |

No se ha corregido código funcional adicional durante esta pasada. Las regresiones de las nueve correcciones anteriores siguen pasando; estos escenarios amplían las combinaciones y capas probadas.

## Otros resultados que requieren corrección o revisión

| Casos | Observación |
|---|---|
| REG-10, LOB-06, REN-06 | Altas duplicadas concurrentes y carreras por la última plaza producen 500. En las carreras de plaza observadas, PostgreSQL impidió superar la capacidad; el fallo comprobado es la respuesta no controlada al cliente que pierde. |
| CRE-06, CRE-08, API-06/07/08, PER-03 | JSON mal formado, enums/tipos inválidos, ciertos nulos, IDs fuera del rango Long, métodos no admitidos o valores demasiado largos acaban en 500. Valores ordinarios fuera de rango como siete plazas ya devuelven 400 tras las correcciones anteriores. |
| CRE-10 | El servidor permite iniciar una partida normal de una plaza; la interfaz exige dos jugadores y deshabilita el botón. Entrenamiento utiliza otra condición. |
| PRO-04, PER-10 | Borrar una asignatura referenciada o determinados jugadores con historial provoca 500 en lugar de rechazo de negocio o borrado coherente. |
| PRO-07 | La búsqueda acepta `size=1000000` sin un límite de paginación documentado/aplicado. |
| PRO-11 | Ninguna o dos respuestas correctas causan 500; un texto de opción vacío puede guardarse. Lista nula y órdenes no modificados se comprobaron en fixtures independientes para evitar atribuirles fallos de una variante anterior. |
| CSV-06 | El parser basado en `split` trunca campos CSV entre comillas que contienen `;` o saltos de línea. |
| CSV-09/10 | Contenido no CSV puede presentarse como éxito con cero errores. Al retirar temporalmente el segundo importador en QA, la API respondió 200 con añadidas y cero errores aunque no se habían importado las respuestas. El reintento recuperó la coherencia, pero la primera confirmación era engañosa. |
| IMG-03/04/12 | Imágenes truncadas y errores de multipart reciben 500. Una subida válida de 2 MiB o próxima a 10 MiB falla en HTTP aunque el validador de servicio admita el límite documentado. Los recursos ausentes también devolvieron 500 en vez de 404. |
| IMG-02 | La recodificación elimina EXIF, pero no aplica la rotación de `Orientation=6`: la muestra que debía visualizarse como 24×32 sigue teniendo píxeles 32×24. El canal alfa del PNG sí se conservó. |
| IMG-07 | Se aceptó como PNG regular un ZIP cuyo atributo Unix indicaba symlink. No se observó creación de enlace simbólico ni escritura fuera del almacén: es una diferencia frente al rechazo exigido por la batería, no una demostración de escape del directorio. |
| NAV-01 | En producción, `/actuator/health` respondió **200 `text/html` con el index de la SPA**, no el JSON de salud. Se confirmó en los tres motores y mediante consulta final el 5 de noviembre. |
| UI-01/02/03 | Las estadísticas desbordan 11 px a 360 px; hay botones sin nombre accesible y los selectores de categoría ocultos no reciben foco de teclado ni a través de su etiqueta. |
| AUT-12 | Al detener SMTP en QA, el login no respondió en los 15 segundos del timeout fijado por el arnés. No se afirma que nunca termine: el resultado comprobado es superar ese límite sin respuesta. |
| WS-09 | La variante de navegación repetida quedó bloqueada esperando una pantalla después de acumular peticiones de autenticación. No se ha demostrado una fuga de memoria; se mantuvo el caso como parcial en vez de adjudicarle una fuga. |

[Consulta final de salud de producción](ejecucion-completa-2026-11-04/produccion-health-final-20261105-headers.txt) y [diagnóstico de tramas WSS sin JWT](ejecucion-completa-2026-11-04/wss-diagnostic.json).

## Carga y persistencia

El ensayo aislado duró **30,05 minutos**, con 50 sockets, nueve salas y 426 ciclos de juego/consulta/reconexión. Se escaló el número de consultas desde 2 a 10, 25 y 50 clientes; los 50 sockets estaban preparados desde el inicio. El ciclo tenía una espera de unos cuatro segundos, por lo que no representa una prueba de saturación máxima.

| Medida | Observado |
|---|---:|
| Peticiones HTTP | 27.857 |
| Errores de conexión o respuestas 5xx del ensayo | 0 |
| HTTP p95 | 45,85 ms |
| Propagación STOMP p95 en localhost | 9,84 ms |
| Eventos STOMP medidos | 6.912 |
| Sockets abiertos en las muestras finales | 50 |
| Memoria del backend, primera/última muestra | 365,4 / 588,8 MiB |

La memoria subió durante el calentamiento y se mantuvo aproximadamente entre 587 y 589 MiB en las últimas muestras. Esto no demuestra ausencia de fugas: no se instrumentaron todos los listeners/timers del frontend ni se hizo un perfil de heap. Tampoco acredita la capacidad del servidor de producción ni latencias de red de Internet.

Se probaron estadísticas sobre mil partidas, importación de cien preguntas mientras otro usuario consultaba una partida, cierre de más de cien vencimientos y restauración en `audit_restore` con un volumen de imágenes nuevo y un backend independiente. Login/OTP, perfil, imagen y Flyway v5 funcionaron sobre la copia restaurada.

[Mediciones de carga](ejecucion-completa-2026-11-04/load-results.json) · [Resumen Java](ejecucion-completa-2026-11-04/java-resumen.txt) · [Pruebas del cliente](ejecucion-completa-2026-11-04/node-tests.txt).

## Qué impide dar los 222 por completados

Los 40 parciales tienen evidencia dinámica, pero todavía faltan variantes concretas: se enumeran en [pendientes por caso](ejecucion-completa-2026-11-04/PENDIENTES-POR-CASO.md). Incluyen recorridos de interfaz adicionales, algunas combinaciones concurrentes, instrumentación de listeners/timers y condiciones externas. No se atribuyen todos los parciales a falta de cuentas institucionales.

La matriz física de Safari/iOS/Android, un lector de pantalla real, la entrega efectiva a un buzón real y sesiones autenticadas atravesando el despliegue público no se sustituyen por WebKit, viewports móviles ni lectura SQL del OTP. Los correos ficticios y el capturador cubren la lógica de la aplicación y los mensajes SMTP generados, pero no la recepción final en un buzón de producción.

Los ocho bloqueados son criterios de producto observados, no operaciones que hayan quedado sin intentar: normalización de email/nick, límites diferentes de tiempo API/UI, email del anfitrión público, entrenamiento público/multijugador, poblaciones de estadísticas, denominadores de abandono/expulsión, gestión compartida entre profesores e identificadores CSV opcionales. La matriz documenta el comportamiento actual; hace falta decidir qué resultado se desea exigir antes de etiquetar esas diferencias como correctas o defectuosas.

UI-04 se marcó NO APLICA: la batería que preparé suponía un control de ampliación de imágenes, pero la interfaz implementa imágenes sin ese modal. No debe confundirse la ausencia de esa función con un fallo al ejecutarla.

## Archivos y continuación

- [Batería original](BATERIA-PRUEBAS-jUGamiR.md).
- [Matriz de 222 casos](ejecucion-completa-2026-11-04/MATRIZ-222-CASOS.md).
- [Observaciones y repeticiones](ejecucion-completa-2026-11-04/observaciones.ndjson).
- [Capturas de navegador](ejecucion-completa-2026-11-04/screenshots).
- [Pasos 1–6 de despliegue guardados](PASOS-DESPLIEGUE-PENDIENTE.md).

Se añadieron scripts y fixtures de QA y cinco pruebas Java de límites; las modificaciones funcionales de las nueve correcciones se conservaron. Los datos de prueba son propios de QA. Las evidencias persistidas omiten JWT y credenciales.

Al cerrar la campaña se guardaron los logs y las identificaciones de imágenes y se retiraron únicamente los contenedores y la red `jugamir-fullqa-20261104`. Se conservaron los volúmenes QA para poder investigar y repetir pruebas. No se modificó el despliegue de producción.

La publicación sigue pendiente. Antes de aplicar los pasos de despliegue guardados, conviene resolver los fallos de integridad, concurrencia y recuperación que ahora están reproducidos y repetir sus casos; una suite Java verde por sí sola no certifica los recorridos que fallaron.
