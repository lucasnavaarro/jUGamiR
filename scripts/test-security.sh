#!/bin/sh
# Runs only against a fresh, disposable PostgreSQL container; never reads .env.
set -eu
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
test_name="jugamir-security-test-$$"
cleanup() {
    docker rm -f "$test_name" >/dev/null 2>&1 || true
    docker network rm "$test_name" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM
docker network create "$test_name" >/dev/null
docker run -d --rm --name "$test_name" --network "$test_name" \
    -e POSTGRES_USER=audit_test -e POSTGRES_PASSWORD=disposable-test-only -e POSTGRES_DB=audit_test \
    -v "$project_dir/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro" \
    -v "$project_dir/db/data.sql:/docker-entrypoint-initdb.d/02-data.sql:ro" postgres:16-alpine >/dev/null
attempt=0
until docker exec "$test_name" pg_isready -h 127.0.0.1 -U audit_test -d audit_test >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -gt 45 ]; then echo 'PostgreSQL de pruebas no arrancó' >&2; exit 1; fi
    sleep 1
done
expected_questions=$(awk '/^INSERT INTO public.preguntas / {n++} END {print n+0}' "$project_dir/db/data.sql")
expected_answers=$(awk '/^INSERT INTO public.respuestas / {n++} END {print n+0}' "$project_dir/db/data.sql")
actual_questions=$(docker exec "$test_name" psql -X -U audit_test -d audit_test -Atc 'SELECT count(*) FROM preguntas')
actual_answers=$(docker exec "$test_name" psql -X -U audit_test -d audit_test -Atc 'SELECT count(*) FROM respuestas')
test "$expected_questions" -gt 0
test "$expected_questions" -eq "$actual_questions"
test "$expected_answers" -eq "$actual_answers"
docker exec -i "$test_name" psql -X -U audit_test -d audit_test -q -v ON_ERROR_STOP=1 <<'SQL'
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM usuarios WHERE id_usuario=2 AND es_activo=false AND "contraseña_hash"='!disabled-content-owner')
       OR EXISTS (SELECT 1 FROM profesores WHERE id_usuario=2)
       OR EXISTS (SELECT 1 FROM jugadores WHERE id_usuario=2)
       OR EXISTS (SELECT 1 FROM preguntas p LEFT JOIN usuarios u ON u.id_usuario=p.creada_por WHERE u.id_usuario IS NULL)
       OR EXISTS (SELECT 1 FROM respuestas r LEFT JOIN preguntas p ON p.id=r.pregunta_id WHERE p.id IS NULL) THEN
        RAISE EXCEPTION 'La carga inicial no cumple las comprobaciones de integridad';
    END IF;
END $$;
SQL
echo "Carga inicial verificada: $actual_questions preguntas y $actual_answers respuestas"
docker run --rm --network "$test_name" \
    -e DB_HOST="$test_name" -e DB_PORT=5432 -e DB_NAME=audit_test \
    -e DB_USER=audit_test -e DB_PASSWORD=disposable-test-only \
    -e MAIL_USERNAME=test@example.invalid -e MAIL_PASSWORD=unused-test \
    -e JWT_SECRET=disposable-test-key-for-security-tests-only-64-characters-long-0001 \
    -e APP_URL=https://game.example -e JUGAMIR_DISPOSABLE_DB_TEST=true -e SHOW_SQL=false \
    -e LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_BOOT_SECURITY_AUTOCONFIGURE=ERROR \
    -v "$project_dir/backend:/work" -v jugamir_security_maven:/root/.m2 -w /work \
    maven:3.9.9-eclipse-temurin-17 mvn -B -ntp verify
