# Pasos de despliegue pendientes de jUGamiR

Conservar este procedimiento hasta terminar la validación completa. Guardado a petición de Lucas el 4 de noviembre de 2026. Este archivo no autoriza ni ejecuta el despliegue.

1. Revisar y fusionar la PR #5 de símbolos de contraseña: https://github.com/lucasnavaarro/jUGamiR/pull/5. Comprobar su estado actualizado antes de actuar.
2. Preparar, publicar, validar y fusionar una PR con las correcciones de los nueve fallos y de la campaña completa, sus pruebas y Flyway V5–V8. Las ocho decisiones de comportamiento fueron respondidas por Lucas el 5 de noviembre. Comprobar qué cambios locales corresponden a cada trabajo; evitar añadir archivos `.DS_Store` o secretos.
3. Hacer copia de seguridad de PostgreSQL y del volumen de imágenes del servidor. Programar una breve interrupción sin partidas activas. Antes de actualizar, ejecutar en modo de solo lectura las dos consultas siguientes: deben devolver cero filas. Si hay colisiones, resolver sus IDs con el propietario antes de aplicar V7; no fusionar ni borrar cuentas automáticamente.

   ```sql
   SELECT array_agg(id_usuario ORDER BY id_usuario) AS cuentas_en_conflicto
   FROM usuarios GROUP BY lower(btrim(email)) HAVING count(*) > 1;
   SELECT array_agg(id_usuario ORDER BY id_usuario) AS jugadores_en_conflicto
   FROM jugadores GROUP BY lower(btrim(nick)) HAVING count(*) > 1;
   ```
4. Por SSH, en `/opt/andalnet`, comprobar `git status --short`. Con el árbol limpio y las dos PR integradas, usar `git switch main` y `git pull --ff-only origin main`. Validar la configuración y reconstruir/recrear backend y frontend con los dos Compose de producción:

   ```bash
   docker compose -f docker-compose.yml -f docker-compose.managed-proxy.yml config --quiet
   docker compose -f docker-compose.yml -f docker-compose.managed-proxy.yml up -d --build --no-deps backend frontend
   ```

5. Consultar `ps` y `logs --tail=100 backend` usando los mismos archivos Compose. Confirmar arranque correcto y migraciones Flyway V5–V8: progreso a 30, plazas activas y unicidad normalizada e índice de partidas por jugador, sin borrar registros.
6. Probar desde el dominio público salud JSON `UP`, registro con punto en contraseña, login, partida entre navegadores, inicio después de expulsar, quesito con umbral superior a cinco y recuperación del resultado al recargar. Confirmar que el listado público muestra nicks y no correos. Mantener WSS más de 120 segundos y confirmar reconexión tras recrear el backend sin reiniciar el frontend; comprobar que una imagen utilizada devuelve conflicto al intentar borrarla o sobrescribirla. Repetir en el iPhone los cambios Wi-Fi/datos y la suspensión de pantalla con la versión desplegada.

La actualización prevista no exige cambiar `.env`, reimportar preguntas ni eliminar volúmenes. No ejecutar `docker compose down -v`. Revisar este procedimiento si aparecen nuevas migraciones o cambios de configuración durante las pruebas.

## Comprobaciones añadidas tras la revisión del 6 de noviembre

Antes de fusionar, publicar el workflow de regresiones y exigir que los dos trabajos de CI estén aprobados sobre el commit que vaya a desplegarse. La validación local y el borrador de descripción están en `REVISION-FINAL-2026-11-06.md` y `PR-CORRECCIONES-PREPARADA.md`. El CI está publicado y ejecutado en la PR #6; no se han cambiado las comprobaciones obligatorias de la rama.

Durante la ventana de mantenimiento, conservar el SHA anterior, los identificadores de las dos imágenes anteriores y copias verificadas de PostgreSQL y del volumen de imágenes. Evitar nuevas escrituras hasta terminar las comprobaciones iniciales. Si se permite actividad real después, restaurar una copia antigua puede perder esos cambios y exige conciliarlos antes.

Tras desplegar, ejecutar desde fuera del servidor:

```bash
node scripts/qa/public-health.mjs https://jugamir.es/
```

El comprobador solo hace un GET de salud y falla ante HTTP distinto de 200, HTML, JSON sin `UP`, redirecciones o timeout. Revisar también con los dos Compose de producción `ps` y `logs --since=15m --tail=200 backend frontend`, buscando reinicios, errores 5xx y fallos repetidos de turnos o correo. Esta consulta de logs no configura alertas automáticas. Mantener el monitor externo y sus avisos como tarea de puesta en producción; no se ha instalado ninguno durante la revisión local.

Si fallan el arranque, las migraciones o los recorridos críticos, mantener el mantenimiento y revertir la versión de aplicación junto con la copia coherente de BD e imágenes. No basta con cambiar al commit antiguo: su validación Flyway puede rechazar migraciones V5–V8 ya aplicadas. La restauración debe ensayarse sobre una copia aislada antes de usarla en el servidor real, sin borrar volúmenes de producción a ciegas.

La validación equivalente a producción detectó que 256 MiB de `/tmp` no permitían procesar un ZIP válido de 192 MiB. La versión candidata transfiere el spool multipart sin duplicarlo y ambos overlays de producción asignan 512 MiB temporales. Confirmar ese valor al recrear el backend; no requiere modificar `.env`.

Para esta candidata, sustituir la reconstrucción por [las imágenes AMD64 exactas que superaron la aceptación](DESPLEGAR-IMAGENES-VALIDADAS.md). El CI y la carga completa están aprobados en el [informe de pasos 2 y 3](VALIDACION-PASOS-2-3-2026-11-06.md); el despliegue continúa pendiente y es manual.
