# Pasos 2 y 3: automatización y aceptación de la candidata

**Completados el 6 de noviembre de 2026. No se ha fusionado ni desplegado código.**

La [PR #6](https://github.com/lucasnavaarro/jUGamiR/pull/6) está publicada en borrador y apilada sobre la PR #5, que permanece intacta. La candidata aceptada corresponde al commit `6ace3dea2ea98d989290a37830e0f5b1146a463d`. Los cambios posteriores de documentación no sustituyen las imágenes de esa candidata.

## Paso 2: automatización ejecutada en GitHub

El [CI de aceptación](https://github.com/lucasnavaarro/jUGamiR/actions/runs/42192772272) terminó correctamente con tres trabajos: backend, frontend/importación y navegador con la configuración equivalente a producción. Ejecutó 84 pruebas Java sin fallos ni omisiones, 11 del frontend, cinco CSV y dos del propio ejecutor QA. Las 35 observaciones de aceptación de la interfaz, ZIP y carga fueron correctas.

El workflow se dispara al actualizar PR, publicar en `main` o ramas `codex/**`, y manualmente cuando esté integrado en la rama predeterminada. Las regresiones normales incluyen sesiones, juego, teclado, estadísticas, cortes de red y ZIP grande con Chromium fijado por lockfile. La etiqueta `qa:full-load` añade la aceptación de treinta minutos y conserva las imágenes exactas; se retiró tras aprobar la candidata para no repetir la carga por cambios de documentación. En `main` quedará incorporado al fusionar la PR; no se han cambiado sus reglas de protección ni se ha autorizado un despliegue automático.

## Paso 3: candidata validada y conservada

Se comprobó en Linux AMD64, correspondiente al `x86_64` del servidor confirmado por el propietario. La configuración del servidor fue suministrada manualmente mediante un resumen filtrado: perfil de producción, origen HTTPS, backend de solo lectura, frontend en loopback, BD y backend sin puertos públicos, PostgreSQL 16 y la plantilla/entrypoint correctos. El asistente no accedió por SSH ni leyó secretos.

La prueba equivalente a producción encontró y corrigió un problema adicional: un ZIP válido de unos 192 MiB devolvía 503 con el límite temporal de 256 MiB. La candidata transfiere el spool multipart sin duplicarlo y ambos overlays asignan 512 MiB a `/tmp`. El mismo ZIP pasa ahora bajo filesystem de solo lectura y límite de memoria de QA; también se repitieron los bordes de 10 MiB, 16 millones de píxeles, 500 entradas, 200 MiB expandidos y el presupuesto de treinta segundos. Ese cambio de configuración deberá aplicarse al recrear el backend en el paso 4; no se ha aplicado al servidor actual.

V8 añade a instalaciones existentes el índice de consultas por jugador que ya figuraba en el esquema inicial. Se verificó una migración histórica real sin ese índice: V2–V8 conservaron cuenta, pregunta y progreso, y V8 creó el índice.

| Medición AMD64 | Resultado |
|---|---:|
| Duración | 30.06 minutos |
| Jugadores / espectadores públicos | 50 / 3 |
| Peticiones HTTP | 26742 |
| Errores | 0 |
| HTTP p95 | 59.19 ms |
| Eventos STOMP p95 | 16.07 ms |
| Búsqueda docente p95, banco de 5000 preguntas | 12.03 ms |
| Estadísticas p95, historial de 1000 partidas | 99.11 ms |

Todas las respuestas HTTP fueron 200; se cumplieron los objetivos de cero errores, HTTP p95 inferior a 500 ms y eventos p95 inferior a 250 ms. Se midieron por separado partidas de dos y seis jugadores, espectadores públicos y reconexiones. La medición local ARM64 adicional duró 30.06 minutos, con 27143 peticiones, cero errores, HTTP p95 58.07 ms y STOMP p95 13.05 ms.

El artefacto `candidate-linux-amd64` contiene las mismas imágenes que superaron estas pruebas, sus IDs, commit, SHA-256 y resumen de carga. GitHub lo conserva un día; se conserva también una copia local fuera de Git en `.release-candidates/6ace3dea2ea9/`. [Procedimiento para desplegar las imágenes validadas](DESPLEGAR-IMAGENES-VALIDADAS.md). No reconstruir otra imagen y atribuirle estos resultados.

Los [resúmenes técnicos](pasos-2-3-2026-11-06/resumen.json) y los [pasos de despliegue](PASOS-DESPLIEGUE-PENDIENTE.md) distinguen esta candidata de las campañas anteriores. Los logs privados, las capturas y el resumen de configuración proporcionado por el propietario se mantienen localmente.

**Siguen pendientes los pasos 4–6:** copia y despliegue, proxy público/iPhone/suspensión/cambio de red, y vigilancia real posterior. La carga mide QA y no certifica la capacidad del servidor público ni la ausencia de cualquier defecto desconocido. El lector de pantalla continúa excluido por decisión del usuario.
