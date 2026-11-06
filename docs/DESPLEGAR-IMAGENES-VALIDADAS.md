# Desplegar las imágenes que superaron la aceptación

Este procedimiento pertenece al paso 4 y no se ha ejecutado. Primero revisar y fusionar las PR #5 y #6, hacer las copias de BD e imágenes y abrir la ventana de mantenimiento indicada en `PASOS-DESPLIEGUE-PENDIENTE.md`. El acceso SSH lo realiza exclusivamente el propietario.

El CI de la PR #6 conserva `candidate-linux-amd64` únicamente cuando está activada la etiqueta `qa:full-load` y han pasado backend, frontend, navegador, ZIP grande y treinta minutos reales de carga. El artefacto contiene las imágenes probadas, su commit, identificadores, SHA-256 y resultados de carga. GitHub lo conserva un día; descargarlo antes de que expire. La copia local de esta sesión se guardará fuera de Git en `.release-candidates/`.

Descargar el artefacto y transferir manualmente su contenido al servidor. No compartir claves privadas. En el directorio donde se hayan copiado los archivos, comprobar e importar:

```bash
sha256sum -c SHA256SUMS
docker load -i images-linux-amd64.tar.gz
```

En `/opt/andalnet`, actualizar `main` solo después de fusionar ambas PR y comprobar que no hay cambios locales que se perderían. Conservar antes las imágenes anteriores y las copias coherentes de BD e imágenes. Comparar los archivos de despliegue con `deployment-files.sha256` del artefacto, ejecutando la comprobación desde la raíz del repositorio:

```bash
sha256sum -c /RUTA/AL/ARTEFACTO/deployment-files.sha256
```

Comprobar también que el código de aplicación de `main` coincide con el commit del artefacto. El fetch permite encontrar el commit incluso si la PR se fusionó mediante squash; no cambia el checkout:

```bash
candidate_sha=$(cat /RUTA/AL/ARTEFACTO/source-commit.txt)
git fetch origin refs/pull/6/head
git diff --exit-code "$candidate_sha" HEAD -- backend/src/main backend/pom.xml backend/Dockerfile frontend db docker-compose.yml docker-compose.managed-proxy.yml docker-compose.candidate.yml
```

La comparación debe terminar sin diferencias. Si cambió código o configuración desde la aceptación, repetirla sobre la candidata nueva; no atribuir a las imágenes antiguas las pruebas de otra versión.

Si hay diferencias, detenerse y revisarlas. No omitir la comprobación ni reconstruir imágenes para sustituir las probadas. Obtener el sufijo de las etiquetas a partir del commit conservado en el artefacto:

```bash
candidate_tag=$(cut -c1-12 /RUTA/AL/ARTEFACTO/source-commit.txt)
export JUGAMIR_BACKEND_IMAGE="jugamir-backend:candidate-$candidate_tag"
export JUGAMIR_FRONTEND_IMAGE="jugamir-frontend:candidate-$candidate_tag"
docker image inspect "$JUGAMIR_BACKEND_IMAGE" "$JUGAMIR_FRONTEND_IMAGE" --format '{{.Id}} {{.Os}}/{{.Architecture}}'
```

Los IDs deben coincidir con `images.txt` y la arquitectura debe ser `linux/amd64`, correspondiente al `x86_64` confirmado por el propietario. Con esas variables en la misma terminal:

```bash
docker compose -f docker-compose.yml -f docker-compose.managed-proxy.yml -f docker-compose.candidate.yml config --quiet
docker compose -f docker-compose.yml -f docker-compose.managed-proxy.yml -f docker-compose.candidate.yml up -d --no-build --no-deps backend frontend
```

Esta variante sustituye la reconstrucción del paso 4 por la importación de las imágenes ya validadas. Usa el mismo origen HTTPS, volumen de imágenes, proxy y secretos del servidor. El overlay actualizado aumenta `/tmp` de 256 a 512 MiB; no requiere editar `.env`. Confirmar Flyway V5–V8 y continuar con salud pública, observación y pruebas externas del procedimiento guardado. No eliminar volúmenes de producción.

Si el artefacto expiró y no se conservó ninguna copia verificada, ejecutar de nuevo la aceptación completa antes de desplegar nuevas imágenes. La carga de CI mide ese entorno de QA; no certifica la capacidad del servidor público ni reemplaza las pruebas del proxy real y del iPhone.
