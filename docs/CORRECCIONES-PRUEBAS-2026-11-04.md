# Correcciones de los nueve fallos de jUGamiR

Verificación del 4 de noviembre de 2026: **54 pruebas de backend y 8 del cliente, todas correctas**. Maven verify compila y empaqueta el backend. Las diez aserciones que fallaban en el informe inicial ahora pasan, sin omitirlas ni relajar su resultado esperado.

| Fallo | Corrección |
|---|---|
| Expulsados con turno al iniciar | Solo los participantes PENDIENTE reciben turno. Los expulsados tampoco cuentan al decidir la victoria por abandono de los demás. |
| Sexto acierto con umbral 7 | Esquema y migración Flyway V5 amplían el progreso permitido de 5 a 30. Se conserva el progreso existente. |
| Opciones de otra pregunta editables | Se valida la pertenencia de todas las opciones antes de modificar la pregunta. IDs ajenos, inexistentes, nulos o repetidos se rechazan. |
| JWT de cuenta desactivada aceptado | El filtro HTTP exige cuenta activa. La renovación también rechaza cuentas desactivadas. |
| Partidas sin dificultades | Lista no vacía y sin elementos nulos, validada tanto en el DTO HTTP como en el servicio. |
| Categorías inexistentes ignoradas | Se exige que existan todas las categorías solicitadas; también se rechazan IDs nulos, no positivos o repetidos. |
| Pesos para categorías no seleccionadas | Solo se admiten pesos positivos para categorías seleccionadas; la suma se valida para evitar desbordamientos. |
| Siete plazas causaban 500 | Capacidad validada entre 1 y 6 antes de persistir; la API devuelve 400 para valores fuera de rango. |
| Umbral cero aceptado | Umbral validado entre 1 y 30 en DTO y servicio; los valores inválidos devuelven 400. |

El rango HTTP existente de tiempo, 5–600 segundos, se conserva. La interfaz ofrece 30–300 segundos, como antes.

## Verificación ejecutada

- Las 30 pruebas Java originales siguen pasando.
- Las regresiones funcionales de servicio son ahora 14; incluyen la finalización con umbrales 7 y 30, edición válida de opciones, rechazo de configuraciones y victoria tras abandono con un expulsado en el historial.
- Las pruebas HTTP son ahora 10; comprueban 400 para configuración inválida, bloqueo de JWT/refresh de cuentas desactivadas y los recorridos válidos de sesión y partida.
- La prueba de V5 reconstruye la restricción antigua sobre un registro con progreso 4, aplica el SQL de la migración, comprueba que el registro conserva ese valor y admite progreso 29; sigue rechazando 31.
- Las 8 pruebas del cliente pasan mediante `npm test` en Node Docker sin red.

[Resultados de las 54 pruebas Java](resultados-correcciones-2026-11-04/backend-resumen.txt) y [salida de las 8 pruebas del cliente](resultados-correcciones-2026-11-04/frontend-tests.txt).

Comandos desde la raíz del repositorio:

```bash
sh scripts/test-security.sh
npm --prefix frontend test
```

El primer comando crea una base PostgreSQL desechable, valida la carga inicial, ejecuta Maven verify y elimina el contenedor y la red temporal. No lee `.env` ni utiliza correo real.

## Aplicación al despliegue

Los cambios están en el directorio local; **no se ha actualizado producción**. Para llevarlos al servidor, desplegar el backend actualizado junto con `V5__expand_category_progress_limit.sql` mediante el procedimiento habitual, con una copia de seguridad previa de la base de datos. Flyway aplica V5 automáticamente al arrancar el backend y registra la versión 5. La migración cambia una restricción, sin borrar tablas ni registros.

En instalaciones existentes no basta con actualizar `db/schema.sql`, porque los scripts de inicialización no se vuelven a ejecutar sobre volúmenes existentes. V5 es la que corrige esas bases. No hay que eliminar volúmenes ni reimportar el banco de preguntas. El cambio de restricción requiere un bloqueo de tabla durante la migración; programar la actualización en un momento de baja actividad.

Después de desplegar, comprobar la versión de Flyway, login/renovación, inicio tras expulsar y una partida con umbral superior a 5. El comportamiento del proxy, dispositivos reales y los demás casos pendientes de la batería completa no quedan certificados por esta ejecución local.

Los errores y avisos de ESLint del informe inicial son independientes de estos nueve fallos; no se han modificado componentes del frontend para resolverlos.
