# Correcciones de los casos con fallo y decisiones pendientes

Estado del código probado en QA; no acredita despliegue ni todas las variantes de los 222 casos.

| Caso | Estado anterior | Corrección/criterio aplicado | Verificación |
|---|---|---|---|
| NAV-01 | FALLO | Nginx enruta /actuator/health al backend; JSON UP comprobado en QA. Producción necesita desplegarlo. | CORREGIDO EN QA; 4 variantes |
| REG-09 | BLOQUEADO | Unicidad de email/nick sin mayúsculas ni espacios exteriores; nick conserva su escritura. | CORREGIDO EN QA; 1 variantes |
| REG-10 | FALLO | Validación de límites y nulos; errores 400/404/405/409/413/415 controlados, sin filtrar detalles internos. | CORREGIDO EN QA; 2 variantes |
| AUT-12 | FALLO | Timeouts SMTP, respuesta 503 y recuperación; auth/me no consume el límite Nginx de login, que conserva su 429. | CORREGIDO EN QA; 2 variantes |
| PER-03 | FALLO | Validación de límites y nulos; errores 400/404/405/409/413/415 controlados, sin filtrar detalles internos. | CORREGIDO EN QA; 1 variantes |
| PER-10 | FALLO | Rechazo de negocio al borrar una cuenta con historial/partidas; borrado de cuenta vacía verificado. | CORREGIDO EN QA; 1 variantes |
| CRE-06 | FALLO | Validación de límites y nulos; errores 400/404/405/409/413/415 controlados, sin filtrar detalles internos. | CORREGIDO EN QA; 1 variantes |
| CRE-07 | BLOQUEADO | UI y API admiten 5–600 segundos. | CORREGIDO EN QA; 5 variantes |
| CRE-08 | FALLO | Validación de límites y nulos; errores 400/404/405/409/413/415 controlados, sin filtrar detalles internos. | CORREGIDO EN QA; 1 variantes |
| CRE-10 | FALLO | Una partida competitiva necesita dos participantes para empezar; entrenamiento permite uno. | CORREGIDO EN QA; 1 variantes |
| CRE-11 | BLOQUEADO | Listado público con nick y sin email. | CORREGIDO EN QA; 1 variantes |
| LOB-02 | FALLO | Códigos privados con formato validado y ruta vacía rechazada sin 500. | CORREGIDO EN QA; 1 variantes |
| LOB-06 | FALLO | Unión, inicio y cambios de miembros se serializan con el mismo bloqueo de partida; carreras repetidas cinco veces. | CORREGIDO EN QA; 1 variantes |
| LOB-08 | FALLO | Unión, inicio y cambios de miembros se serializan con el mismo bloqueo de partida; carreras repetidas cinco veces. | CORREGIDO EN QA; 1 variantes |
| LOB-12 | FALLO | Expulsar no marca rollback al autorizar sockets; plazas y orden de unión excluyen/rebasan registros expulsados conservados. | CORREGIDO EN QA; 1 variantes |
| LOB-15 | FALLO | Expulsar no marca rollback al autorizar sockets; plazas y orden de unión excluyen/rebasan registros expulsados conservados. | CORREGIDO EN QA; 1 variantes |
| FIN-05 | FALLO | Reconciliación REST de lobby y estado, cola de eventos hasta la carga inicial y recuperación del ganador al recargar. | CORREGIDO EN QA; 4 variantes |
| ENT-07 | FALLO | Resultados tardíos de una selección anterior no sustituyen el criterio actual de entrenamiento. | CORREGIDO EN QA; 1 variantes |
| ENT-10 | BLOQUEADO | Entrenamiento privado e individual. | CORREGIDO EN QA; 1 variantes |
| EST-05 | BLOQUEADO | Resumen y detalle solo consideran competitivas terminadas. | CORREGIDO EN QA; 1 variantes |
| EST-06 | BLOQUEADO | Abandonos/expulsiones finales cuentan sin victoria; canceladas excluidas. | CORREGIDO EN QA; 1 variantes |
| EST-10 | FALLO | La API declara día/semana/mes; el navegador deja de inferir el período a partir de su zona horaria. | CORREGIDO EN QA; 1 variantes |
| PRO-04 | FALLO | Rechazo de asignatura referenciada, paginación 1–100 y opciones no vacías con una sola correcta antes de mutar. | CORREGIDO EN QA; 1 variantes |
| PRO-07 | FALLO | Rechazo de asignatura referenciada, paginación 1–100 y opciones no vacías con una sola correcta antes de mutar. | CORREGIDO EN QA; 1 variantes |
| PRO-11 | FALLO | Rechazo de asignatura referenciada, paginación 1–100 y opciones no vacías con una sola correcta antes de mutar. | CORREGIDO EN QA; 1 variantes |
| PRO-13 | BLOQUEADO | Todos los profesores gestionan el banco; jugadores sin permiso. | CORREGIDO EN QA; 1 variantes |
| PRO-14 | FALLO | Retirada lógica de preguntas utilizadas; su edición se rechaza sin alterar la solución ni borrar respuestas históricas. Bloqueo y refresco al seleccionarlas. | CORREGIDO EN QA; 1 variantes |
| PRO-15 | FALLO | Retirada lógica de preguntas utilizadas; su edición se rechaza sin alterar la solución ni borrar respuestas históricas. Bloqueo y refresco al seleccionarlas. | CORREGIDO EN QA; 1 variantes |
| CSV-05 | BLOQUEADO | Identificador opcional generado de forma estable, informado y sin duplicación al reintentar. | CORREGIDO EN QA; 1 variantes |
| CSV-06 | FALLO | Parser CSV real con BOM, comillas y multilínea; formato inválido rechazado; pregunta y respuestas en una transacción con savepoint por fila y salida de proceso comprobada. | CORREGIDO EN QA; 5 variantes |
| CSV-09 | FALLO | Parser CSV real con BOM, comillas y multilínea; formato inválido rechazado; pregunta y respuestas en una transacción con savepoint por fila y salida de proceso comprobada. | CORREGIDO EN QA; 2 variantes |
| CSV-10 | FALLO | Parser CSV real con BOM, comillas y multilínea; formato inválido rechazado; pregunta y respuestas en una transacción con savepoint por fila y salida de proceso comprobada. | CORREGIDO EN QA; 2 variantes |
| IMG-02 | FALLO | Rotación EXIF antes de limpiar metadatos, alfa preservado, multipart compatible con límites, errores controlados y rechazo de symlinks ZIP según su directorio central. | CORREGIDO EN QA; 2 variantes |
| IMG-03 | FALLO | Rotación EXIF antes de limpiar metadatos, alfa preservado, multipart compatible con límites, errores controlados y rechazo de symlinks ZIP según su directorio central. | CORREGIDO EN QA; 4 variantes |
| IMG-04 | FALLO | Rotación EXIF antes de limpiar metadatos, alfa preservado, multipart compatible con límites, errores controlados y rechazo de symlinks ZIP según su directorio central. | CORREGIDO EN QA; 7 variantes |
| IMG-07 | FALLO | Rotación EXIF antes de limpiar metadatos, alfa preservado, multipart compatible con límites, errores controlados y rechazo de symlinks ZIP según su directorio central. | CORREGIDO EN QA; 2 variantes |
| IMG-12 | FALLO | Rotación EXIF antes de limpiar metadatos, alfa preservado, multipart compatible con límites, errores controlados y rechazo de symlinks ZIP según su directorio central. | CORREGIDO EN QA; 1 variantes |
| WS-01 | FALLO | Reconciliación REST de lobby y estado, cola de eventos hasta la carga inicial y recuperación del ganador al recargar. | CORREGIDO EN QA; 4 variantes |
| WS-05 | FALLO | Expulsar no marca rollback al autorizar sockets; plazas y orden de unión excluyen/rebasan registros expulsados conservados. | CORREGIDO EN QA; 1 variantes |
| API-06 | FALLO | Validación de límites y nulos; errores 400/404/405/409/413/415 controlados, sin filtrar detalles internos. | CORREGIDO EN QA; 1 variantes |
| API-07 | FALLO | Validación de límites y nulos; errores 400/404/405/409/413/415 controlados, sin filtrar detalles internos. | CORREGIDO EN QA; 1 variantes |
| API-08 | FALLO | Validación de límites y nulos; errores 400/404/405/409/413/415 controlados, sin filtrar detalles internos. | CORREGIDO EN QA; 1 variantes |
| UI-01 | FALLO | Diseño móvil sin desbordamiento reproducido, controles nativos accesibles al teclado, nombres de botones y login reintentable tras error de red. | CORREGIDO EN QA; 4 variantes |
| UI-02 | FALLO | Diseño móvil sin desbordamiento reproducido, controles nativos accesibles al teclado, nombres de botones y login reintentable tras error de red. | CORREGIDO EN QA; 5 variantes |
| UI-03 | FALLO | Diseño móvil sin desbordamiento reproducido, controles nativos accesibles al teclado, nombres de botones y login reintentable tras error de red. | CORREGIDO EN QA; 4 variantes |
| UI-06 | FALLO | Diseño móvil sin desbordamiento reproducido, controles nativos accesibles al teclado, nombres de botones y login reintentable tras error de red. | CORREGIDO EN QA; 4 variantes |
| OPS-07 | FALLO | Publicaciones STOMP únicamente después del commit; un fallo diferido revierte la respuesta sin emitir RESULTADO. | CORREGIDO EN QA; 1 variantes |
| REN-06 | FALLO | Unión, inicio y cambios de miembros se serializan con el mismo bloqueo de partida; carreras repetidas cinco veces. | CORREGIDO EN QA; 1 variantes |
