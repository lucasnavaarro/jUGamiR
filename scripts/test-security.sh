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
    -v "$project_dir/db/schema.sql:/docker-entrypoint-initdb.d/01-schema.sql:ro" postgres:16-alpine >/dev/null
attempt=0
until docker exec "$test_name" pg_isready -U audit_test -d audit_test >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ "$attempt" -gt 45 ]; then echo 'PostgreSQL de pruebas no arrancó' >&2; exit 1; fi
    sleep 1
done
docker run --rm --network "$test_name" \
    -e DB_HOST="$test_name" -e DB_PORT=5432 -e DB_NAME=audit_test \
    -e DB_USER=audit_test -e DB_PASSWORD=disposable-test-only \
    -e MAIL_USERNAME=test@example.invalid -e MAIL_PASSWORD=unused-test \
    -e JWT_SECRET=disposable-test-key-for-security-tests-only-64-characters-long-0001 \
    -e APP_URL=https://game.example -e JUGAMIR_DISPOSABLE_DB_TEST=true -e SHOW_SQL=false \
    -e LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_BOOT_SECURITY_AUTOCONFIGURE=ERROR \
    -v "$project_dir/backend:/work" -v jugamir_security_maven:/root/.m2 -w /work \
    maven:3.9.9-eclipse-temurin-17 mvn -B -ntp verify
