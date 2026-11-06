# Matriz de los 222 casos de jUGamiR

Campaña iniciada el 4 y revisada el 5 de noviembre de 2026. Los estados se refieren a las variantes y entornos documentados. **PARCIAL y BLOQUEADO no son aprobaciones.** Los casos compuestos no se aprueban por superar solo una variante.

| Estado | Casos |
|---|---:|
| OK | 133 |
| FALLO | 40 |
| PARCIAL | 40 |
| BLOQUEADO | 8 |
| NO APLICA | 1 |

Variantes diferenciadas: 584. Observaciones incluidas las repeticiones: 784.

[Informe de conclusiones](../INFORME-BATERIA-COMPLETA-2026-11-05.md). [Detalles completos por caso](casos.json). [Historial de observaciones](observaciones.ndjson).

| Caso | Prioridad | Estado | Variantes | Evidencia y límite |
|---|---|---|---:|---|
| NAV-01 | P0 | FALLO | 3 | health devuelve text/html |
| NAV-02 | P1 | OK | 3 | WebKit HTTP conserva /login |
| NAV-03 | P1 | OK | 9 | WebKit acceso directo/recarga /forgot-password |
| NAV-04 | P0 | OK | 12 | WebKit anónimo /partida/999999 |
| NAV-05 | P0 | OK | 3 | WebKit separación de roles |
| NAV-06 | P2 | OK | 3 | WebKit 404 y retorno |
| NAV-07 | P1 | OK | 6 | WebKit recarga /perfil |
| NAV-08 | P1 | OK | 1 | logout, Atrás y recarga protegida |
| REG-01 | P0 | OK | 2 | alta completa mediante formulario jugador |
| REG-02 | P0 | OK | 2 | alta completa mediante formulario profesor |
| REG-03 | P0 | OK | 4 | teacher |
| REG-04 | P1 | OK | 2 | email duplicado rol profesor |
| REG-05 | P1 | OK | 1 | nick duplicado no deja usuario huérfano |
| REG-06 | P1 | PARCIAL | 15 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| REG-07 | P1 | OK | 8 | formulario rechazo contraseña variante "Aa1aaaaa" |
| REG-08 | P1 | OK | 5 | formulario rechazo contraseña variante "Aa1!aaa\n" |
| REG-09 | P1 | BLOQUEADO | 1 | Se probó la normalización; falta decidir si email/nick deben ser insensibles a mayúsculas y espacios. |
| REG-10 | P1 | FALLO | 2 | Expected values to be strictly deep-equal: + actual - expected    [     202, +   500 -   400   ]  |
| AUT-01 | P0 | OK | 4 | login y OTP por formulario profesor |
| AUT-02 | P0 | OK | 2 | missing |
| AUT-03 | P0 | OK | 3 | código de otra cuenta |
| AUT-04 | P1 | OK | 5 | código válido con ceros iniciales |
| AUT-05 | P0 | OK | 1 | cinco intentos y código correcto posterior |
| AUT-06 | P0 | PARCIAL | 2 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| AUT-07 | P0 | OK | 1 | consumo concurrente y repetido OTP |
| AUT-08 | P1 | OK | 1 | intervalo de 60 segundos y dos reenvíos |
| AUT-09 | P0 | OK | 1 | reenvío conserva intentos y vencimiento |
| AUT-10 | P0 | OK | 1 | nuevo login invalida desafío anterior |
| AUT-11 | P0 | OK | 2 | desactivada between |
| AUT-12 | P1 | FALLO | 3 | The operation was aborted due to timeout |
| SES-01 | P0 | OK | 3 | cookie navegador profesor |
| SES-02 | P0 | OK | 1 | JWT expirado renueva y carga perfil |
| SES-03 | P0 | OK | 1 | dos consultas reciben 401 y comparten refresh pendiente |
| SES-04 | P0 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| SES-05 | P0 | OK | 2 | versión antigua del JWT |
| SES-06 | P0 | OK | 1 | logout revoca dos dispositivos HTTP |
| SES-07 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| SES-08 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| SES-09 | P1 | OK | 1 | fallo de red durante refresh permite salir sin spinner eterno |
| SES-10 | P0 | OK | 2 | deleted con JWT/refresh |
| REC-01 | P0 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| REC-02 | P1 | OK | 1 | email inexistente mantiene confirmación 202 |
| REC-03 | P0 | OK | 1 | contraseña anterior falla y nueva accede con OTP |
| REC-04 | P0 | OK | 3 | expired |
| REC-05 | P0 | OK | 1 | dos reset concurrentes un solo consumo |
| REC-06 | P1 | OK | 1 | nuevo enlace invalida anterior |
| REC-07 | P0 | OK | 1 | Falta socket ya suscrito; variante STOMP se añade en otra área. |
| REC-08 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| PER-01 | P1 | OK | 2 | perfil PROFESOR |
| PER-02 | P1 | OK | 7 | perfil cambia campos comunes y específicos PROFESOR |
| PER-03 | P1 | FALLO | 4 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |
| PER-04 | P0 | OK | 1 | contraseña incorrecta no emite desafío email |
| PER-05 | P0 | OK | 1 | cambio a buzón verificado revoca sesión |
| PER-06 | P0 | OK | 5 | different-user |
| PER-07 | P0 | OK | 1 | La revocación de OTP anterior se valida en suite Java; pendiente socket en este caso. |
| PER-08 | P0 | OK | 1 | profesor rechaza externo y acepta otro institucional |
| PER-09 | P1 | OK | 1 | nuevo código sustituye al anterior y consumo concurrente |
| PER-10 | P0 | FALLO | 3 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |
| PER-11 | P1 | OK | 1 | profesor vacío elimina y con preguntas se conserva |
| PER-12 | P0 | OK | 1 | cuenta borrada pierde REST refresh y socket |
| CRE-01 | P0 | OK | 1 | configuración y pertenencia de anfitrión |
| CRE-02 | P1 | PARCIAL | 2 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| CRE-03 | P1 | OK | 16 | formulario límites 6/300/30 |
| CRE-04 | P1 | OK | 4 | formulario selección vacía dificultades |
| CRE-05 | P1 | OK | 3 | {"error":"No puedes seleccionar categorías duplicadas"} |
| CRE-06 | P1 | FALLO | 10 | HTTP 500, expected 400; {"error":"Error interno del servidor"} |
| CRE-07 | P1 | BLOQUEADO | 8 | Se probaron 4,5,29,30,300,301,600,601; falta aprobar API 5–600 frente a UI 30–300. |
| CRE-08 | P1 | FALLO | 4 | HTTP 500, expected 400; {"error":"Error interno del servidor"} |
| CRE-09 | P1 | OK | 1 | Se crean 2 partidas al repetir la petición; API sin idempotencia. |
| CRE-10 | P1 | FALLO | 4 | API admite 1 pero UI exige 2 jugadores  true !== false  |
| CRE-11 | P1 | BLOQUEADO | 1 | El listado público devuelve email del anfitrión; política de datos públicos sin aprobar. |
| LOB-01 | P0 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| LOB-02 | P1 | FALLO | 4 | Código vacío responde 500 |
| LOB-03 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| LOB-04 | P0 | OK | 1 | no elude código privado por ID público |
| LOB-05 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| LOB-06 | P0 | FALLO | 13 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |
| LOB-07 | P0 | OK | 1 | solo anfitrión inicia y turnos son únicos |
| LOB-08 | P0 | FALLO | 5 | Se incorporó un miembro sin turno durante inicio |
| LOB-09 | P0 | OK | 3 | CANCELADA |
| LOB-10 | P1 | OK | 1 | abandono en espera y transferencia al más antiguo |
| LOB-11 | P1 | OK | 1 | último abandono cancela pública y desaparece |
| LOB-12 | P0 | FALLO | 1 | HTTP 500, expected 200; {"error":"Error interno del servidor"} |
| LOB-13 | P0 | OK | 1 | expulsión por no anfitrión, propia y en curso |
| LOB-14 | P0 | OK | 1 | outsider no accede REST ni topic |
| LOB-15 | P0 | FALLO | 1 | HTTP 500, expected 200; {"error":"Error interno del servidor"} |
| RUL-01 | P0 | OK | 1 | PREGUNTA idéntica en tres sockets y pregunta única |
| RUL-02 | P0 | OK | 1 | fuera de turno y outsider no giran |
| RUL-03 | P0 | OK | 1 | dos giros concurrentes y giro con pregunta abierta |
| RUL-04 | P1 | OK | 4 | FACIL,DIFICIL |
| RUL-05 | P0 | OK | 1 | borradores y anuladas excluidos |
| RUL-06 | P1 | OK | 1 | no repite dos preguntas y comunica agotamiento |
| RUL-07 | P1 | OK | 2 | categoría vacía permite abandonar y muestra error comprensible |
| RUL-08 | P0 | OK | 1 | REST, estado y evento no filtran solución |
| RUL-09 | P1 | OK | 2 | DOM e imágenes reales en tres contextos; no teléfonos físicos. {"observedImageCounts":[1,2,0]} |
| RUL-10 | P0 | OK | 1 | rechazo antes del fin de animación |
| RES-01 | P0 | OK | 2 | Navegadores reales con móvil emulado; no dispositivos físicos. |
| RES-02 | P0 | OK | 2 | Navegadores reales con móvil emulado; no dispositivos físicos. |
| RES-03 | P0 | OK | 2 | Navegadores reales con móvil emulado; no dispositivos físicos. |
| RES-04 | P0 | OK | 2 | vencimiento real con navegador conectado |
| RES-05 | P0 | OK | 2 | missing |
| RES-06 | P0 | OK | 3 | id -1 |
| RES-07 | P0 | OK | 1 | respuesta y pase no se repiten |
| RES-08 | P0 | OK | 5 | carrera answer,answer 5 |
| RES-09 | P0 | OK | 5 | carrera answer,pass 5 |
| RES-10 | P0 | PARCIAL | 3 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| RES-11 | P0 | OK | 2 | reloj navegador adelantado diez minutos, tiempo servidor intacto |
| RES-12 | P0 | OK | 1 | respuestas fuera de turno, outsider y finalizada |
| RES-13 | P1 | OK | 1 | perder respuesta HTTP después de que se confirme |
| FIN-01 | P0 | OK | 1 | umbral tres acumula dos y tercero concede quesito |
| FIN-02 | P1 | OK | 2 | A correcto B correcto A incorrecto A correcto, progreso separado |
| FIN-03 | P0 | OK | 1 | seguir acertando categoría con quesito no duplica |
| FIN-04 | P0 | OK | 1 | victoria una y varias categorías con rivales |
| FIN-05 | P0 | FALLO | 1 | locator.waitFor: Timeout 7000ms exceeded. Call log:   - waiting for getByText(/Has ganado/ha finalizado/).first() to be visible  |
| FIN-06 | P0 | OK | 1 | abandono turno actual |
| FIN-07 | P0 | OK | 1 | abandono fuera turno |
| FIN-08 | P0 | OK | 1 | abandono uno de dos concede victoria restante |
| FIN-09 | P0 | OK | 3 | dos abandonos concurrentes |
| FIN-10 | P0 | PARCIAL | 5 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| FIN-11 | P1 | OK | 2 | CANCELADA |
| ENT-01 | P0 | PARCIAL | 4 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| ENT-02 | P1 | OK | 4 | interfaz con datos conocidos Tiempo/Afianzar |
| ENT-03 | P1 | OK | 4 | interfaz con datos conocidos Aciertos/Afianzar |
| ENT-04 | P1 | OK | 2 | pesos exactos con categorías con y sin historial |
| ENT-05 | P1 | OK | 1 | {"1":148,"2":52} |
| ENT-06 | P0 | OK | 4 | pesos {"999999":1} |
| ENT-07 | P1 | FALLO | 2 | Llegó tarde la lista de TIEMPO y sustituyó la selección de ACIERTOS + actual - expected  + 'QA Deep 1793865388019 0' - 'QA Deep 1793865388019 1'                          ^  |
| ENT-08 | P1 | PARCIAL | 3 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| ENT-09 | P0 | OK | 1 | entrenamiento con acierto fallo y pase no cambia ninguna estadística |
| ENT-10 | P1 | BLOQUEADO | 1 | El individual rechaza un segundo jugador; la API acepta variantes públicas/multijugador pendientes de política. |
| EST-01 | P1 | OK | 1 | API sin historial devuelve ceros/listas vacías |
| EST-02 | P0 | OK | 1 | fixture 2 aciertos, 1 fallo, 1 pase, media 3 s |
| EST-03 | P1 | OK | 1 | una victoria una derrota |
| EST-04 | P1 | OK | 1 | detalle por categoría/asignatura |
| EST-05 | P0 | BLOQUEADO | 1 | Se observó diferencia de población entre resumen y detalle; falta aprobar/rotular esa semántica. |
| EST-06 | P1 | BLOQUEADO | 2 | Se midió qué resultados entran en denominadores; falta aprobar tratamiento de expulsión/abandono/cancelación. |
| EST-07 | P1 | OK | 4 | historial 61 días |
| EST-08 | P1 | OK | 1 | promedio por partida frente al ponderado |
| EST-09 | P1 | OK | 2 | categoría sin datos/id 999999 |
| EST-10 | P1 | FALLO | 2 | ["% de victorias por semana","% de victorias por día"] + actual - expected  + '% de victorias por semana' - '% de victorias por día'                       ^  |
| EST-11 | P0 | OK | 1 | IDs query no permiten leer otro jugador |
| EST-12 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| PRO-01 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| PRO-02 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| PRO-03 | P1 | OK | 3 | asignatura inválida {"nombre":"QA Invalid","categoriaId":999999} |
| PRO-04 | P0 | FALLO | 1 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |
| PRO-05 | P1 | PARCIAL | 3 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| PRO-06 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| PRO-07 | P1 | FALLO | 4 | No hay límite de tamaño: HTTP 200 |
| PRO-08 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| PRO-09 | P1 | OK | 3 | edición inválida asignaturaId |
| PRO-10 | P0 | OK | 1 | opción ajena por HTTP se rechaza sin mutación |
| PRO-11 | P0 | FALLO | 5 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |
| PRO-12 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| PRO-13 | P0 | BLOQUEADO | 1 | Se verificó gestión compartida entre profesores; falta aprobar restricción por autor. |
| PRO-14 | P0 | FALLO | 1 | Borrar pregunta eliminó respuestas históricas  '0' !== '1'  |
| PRO-15 | P0 | FALLO | 1 | Solución cambió durante pregunta ya emitida  'f' !== 't'  |
| CSV-01 | P0 | OK | 1 | CSV válido tres dificultades y opciones |
| CSV-02 | P1 | OK | 1 | reimportación idempotente |
| CSV-03 | P1 | OK | 1 | mezcla nueva duplicada e inválida |
| CSV-04 | P1 | OK | 4 | incomplete-header |
| CSV-05 | P1 | BLOQUEADO | 3 | El importador admite identificador ausente; decidir si ese campo es obligatorio en el contrato del CSV. |
| CSV-06 | P1 | FALLO | 5 | Expected values to be strictly equal: + actual - expected  + '"texto' - 'texto; con separador'  |
| CSV-07 | P1 | PARCIAL | 2 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| CSV-08 | P0 | OK | 4 | correcta IMPUGNADA |
| CSV-09 | P1 | FALLO | 2 | HTTP 200, {"añadidas":0,"yaExistentes":0,"errores":0,"detalleErrores":["Filas leídas del CSV: 0\nAsignaturas en BD: 2\ncreada_por = usuario 3\n\n=== IMPORTACIÓN DE PREGUNTAS COMPLETADA ===\n |
| CSV-10 | P0 | FALLO | 1 | Proceso de respuestas falló pero API devolvió HTTP 200, añadidas=1, errores=0 |
| CSV-11 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| IMG-01 | P1 | OK | 3 | alpha.png |
| IMG-02 | P1 | FALLO | 2 | Se eliminó EXIF Orientation=6 sin aplicar su rotación a los píxeles + actual - expected    [ +   32,     24, -   32   ]  |
| IMG-03 | P0 | FALLO | 4 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |
| IMG-04 | P1 | FALLO | 7 | HTTP 500, expected 200; {"error":"Error interno del servidor"} |
| IMG-05 | P0 | OK | 4 | other/qa.png |
| IMG-06 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| IMG-07 | P0 | FALLO | 2 | Expected controlled 4xx, got 200: "" |
| IMG-08 | P1 | OK | 5 | Prueba dinámica ya ejecutada; evidencia JUnit conservada en java-resumen y suite de 59 pruebas. |
| IMG-09 | P1 | OK | 2 | Prueba dinámica ya ejecutada; evidencia JUnit conservada en java-resumen y suite de 59 pruebas. |
| IMG-10 | P0 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| IMG-11 | P1 | OK | 2 | {"classpathRoute":"imagenes/ANATOMIA_ordenadas_por_temas_(hasta_2025)/1._Neuroanatomia/MIR_2015-16%2C_P24_p7_1.png","bytes":177591} |
| IMG-12 | P1 | FALLO | 1 | Expected values to be strictly equal:  500 !== 404  |
| WS-01 | P0 | FALLO | 2 | locator.waitFor: Timeout 7000ms exceeded. Call log:   - waiting for locator('main.juego') to be visible  |
| WS-02 | P0 | OK | 4 | CONNECT expired |
| WS-03 | P0 | OK | 3 | /topic/invented |
| WS-04 | P0 | OK | 1 | SEND de cliente no publica resultado inventado |
| WS-05 | P0 | FALLO | 4 | HTTP 500, expected 200; {"error":"Error interno del servidor"} |
| WS-06 | P0 | OK | 1 | vence con cliente desconectado y plazo real de cinco segundos |
| WS-07 | P0 | OK | 2 | recarga de pregunta pendiente mantiene plazo y pregunta |
| WS-08 | P0 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| WS-09 | P1 | PARCIAL | 2 | Se cerraron 20 sockets directos. La navegación repetida de UI quedó bloqueada por el limitador del proxy; no se ha probado fuga de memoria, timers o listeners. |
| WS-10 | P0 | OK | 1 | JWT vence con WSS abierto y se renueva por temporizador |
| WS-11 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| WS-12 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| API-01 | P0 | OK | 6 | roles API |
| API-02 | P0 | OK | 3 | WebKit rol local manipulado no cambia API |
| API-03 | P1 | OK | 3 | handshake WebSocket de origen de otro puerto rechazado |
| API-04 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| API-05 | P1 | OK | 1 | comillas y SQL en búsqueda son datos |
| API-06 | P1 | FALLO | 12 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |
| API-07 | P1 | FALLO | 2 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |
| API-08 | P1 | FALLO | 1 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |
| UI-01 | P1 | FALLO | 60 | /jugador/estadisticas: overflow 11px |
| UI-02 | P1 | FALLO | 2 | No se puede enfocar categoría por teclado: {"inputFocused":false,"labelFocused":false,"display":"none"} |
| UI-03 | P1 | FALLO | 3 | Botón sin nombre accesible  1 !== 0  |
| UI-04 | P1 | NO APLICA | 1 | La aplicación representa la imagen sin control de ampliación/modal. La batería suponía una función no implementada; no se adjudica un fallo de ejecución a ese control inexistente. |
| UI-05 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| UI-06 | P1 | FALLO | 1 | Login queda deshabilitado tras fallo de red  true !== false  |
| UI-07 | P2 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| UI-08 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| OPS-01 | P0 | OK | 1 | reinicio recupera pregunta persistida y vence sin ampliar plazo |
| OPS-02 | P0 | OK | 1 | recrear frontend/backend conserva cuentas, banco e imagen |
| OPS-03 | P0 | OK | 2 | Aplicación independiente con audit_restore y volumen restaurado: login OTP, perfil, imagen y Flyway v5 correctos. |
| OPS-04 | P0 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| OPS-05 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| OPS-06 | P1 | OK | 2 | importador sin permiso para crear temporales y recuperación |
| OPS-07 | P0 | FALLO | 1 | Un RESULTADO fue recibido aunque la transacción se revirtió  1 !== 0  |
| OPS-08 | P1 | OK | 1 | scheduler cierra más de cien preguntas vencidas por lotes |
| REN-01 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| REN-02 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| REN-03 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| REN-04 | P1 | PARCIAL | 1 | Hay evidencia dinámica, pero faltan variantes/capas indicadas en las notas. QA no certifica correo real, dispositivos físicos ni sesiones de producción. |
| REN-05 | P1 | OK | 1 | {"importMs":1252.3085,"gameMaxMs":19.861541999999645} |
| REN-06 | P0 | FALLO | 1 | Expected controlled 4xx, got 500: {"error":"Error interno del servidor"} |

## Interpretación y restricciones

Las pruebas de escritura utilizaron dos bases QA y correo capturado; los códigos se leyeron en PostgreSQL y se comprobó su presencia en SMTP. En producción se hicieron consultas y navegación pública de solo lectura sobre https://jugamir.es/. No se crearon cuentas ni se borraron datos en producción.

Chrome y Firefox fueron motores de escritorio reales; WebKit fue el motor de Playwright, no Safari nativo. Los viewports/tacto móviles son emulación. No hay certificación de iPhone/Android físicos, lector de pantalla o entrega a un buzón institucional real.

Se conservan las ejecuciones iniciales del arnés y sus repeticiones corregidas. Se ajustaron payloads, selectores, esperas y fixtures que no respetaban restricciones del esquema; el resultado final utiliza la última observación de la misma variante. No se modificó código de producción para ocultar errores durante esta pasada.

El fallo de IMG-07 con un ZIP marcado como symlink significa que se aceptó el atributo como imagen regular recodificada; no se ha demostrado creación de enlace simbólico ni escape del directorio. WS-09 no demuestra una fuga de memoria: la variante de navegación quedó bloqueada por el limitador. Las diferencias de política se identifican como BLOQUEADO para decisión de producto.
