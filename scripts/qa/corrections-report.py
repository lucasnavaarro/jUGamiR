"""Summarize the fix campaign without rewriting the original acceptance results."""
import collections
import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parents[2]
out = root / "docs/correcciones-completas-2026-11-05"
original = json.loads((root / "docs/ejecucion-completa-2026-11-04/casos.json").read_text())
observations = [json.loads(line) for line in (out / "observaciones.ndjson").read_text().splitlines()]
latest = {(entry["id"], entry["variant"]): entry for entry in observations}
groups = [
    ("NAV-01", "Nginx enruta /actuator/health al backend; JSON UP comprobado en QA. Producción necesita desplegarlo."),
    ("REG-10 PER-03 CRE-06 CRE-08 API-06 API-07 API-08", "Validación de límites y nulos; errores 400/404/405/409/413/415 controlados, sin filtrar detalles internos."),
    ("AUT-12", "Timeouts SMTP, respuesta 503 y recuperación; auth/me no consume el límite Nginx de login, que conserva su 429."),
    ("PER-10", "Rechazo de negocio al borrar una cuenta con historial/partidas; borrado de cuenta vacía verificado."),
    ("CRE-10", "Una partida competitiva necesita dos participantes para empezar; entrenamiento permite uno."),
    ("LOB-02", "Códigos privados con formato validado y ruta vacía rechazada sin 500."),
    ("LOB-06 LOB-08 REN-06", "Unión, inicio y cambios de miembros se serializan con el mismo bloqueo de partida; carreras repetidas cinco veces."),
    ("LOB-12 LOB-15 WS-05", "Expulsar no marca rollback al autorizar sockets; plazas y orden de unión excluyen/rebasan registros expulsados conservados."),
    ("FIN-05 WS-01", "Reconciliación REST de lobby y estado, cola de eventos hasta la carga inicial y recuperación del ganador al recargar."),
    ("ENT-07", "Resultados tardíos de una selección anterior no sustituyen el criterio actual de entrenamiento."),
    ("EST-10", "La API declara día/semana/mes; el navegador deja de inferir el período a partir de su zona horaria."),
    ("PRO-04 PRO-07 PRO-11", "Rechazo de asignatura referenciada, paginación 1–100 y opciones no vacías con una sola correcta antes de mutar."),
    ("PRO-14 PRO-15", "Retirada lógica de preguntas utilizadas; su edición se rechaza sin alterar la solución ni borrar respuestas históricas. Bloqueo y refresco al seleccionarlas."),
    ("CSV-06 CSV-09 CSV-10", "Parser CSV real con BOM, comillas y multilínea; formato inválido rechazado; pregunta y respuestas en una transacción con savepoint por fila y salida de proceso comprobada."),
    ("IMG-02 IMG-03 IMG-04 IMG-07 IMG-12", "Rotación EXIF antes de limpiar metadatos, alfa preservado, multipart compatible con límites, errores controlados y rechazo de symlinks ZIP según su directorio central."),
    ("UI-01 UI-02 UI-03 UI-06", "Diseño móvil sin desbordamiento reproducido, controles nativos accesibles al teclado, nombres de botones y login reintentable tras error de red."),
    ("OPS-07", "Publicaciones STOMP únicamente después del commit; un fallo diferido revierte la respuesta sin emitir RESULTADO."),
]
description = {case: text for cases, text in groups for case in cases.split()}
decisions = {
    "REG-09": "Unicidad de email/nick sin mayúsculas ni espacios exteriores; nick conserva su escritura.",
    "CRE-07": "UI y API admiten 5–600 segundos.",
    "CRE-11": "Listado público con nick y sin email.",
    "ENT-10": "Entrenamiento privado e individual.",
    "EST-05": "Resumen y detalle solo consideran competitivas terminadas.",
    "EST-06": "Abandonos/expulsiones finales cuentan sin victoria; canceladas excluidas.",
    "PRO-13": "Todos los profesores gestionan el banco; jugadores sin permiso.",
    "CSV-05": "Identificador opcional generado de forma estable, informado y sin duplicación al reintentar.",
}
records = []
for case in original:
    if case["status"] not in ("FALLO", "BLOQUEADO"):
        continue
    checks = [entry for entry in latest.values() if entry["id"] == case["id"]]
    passed = bool(checks) and all(entry["result"] == "OK" for entry in checks)
    records.append({"id": case["id"], "previous": case["status"],
                    "status": "CORREGIDO EN QA" if passed else "REVISAR",
                    "change": description.get(case["id"], decisions.get(case["id"])), "checks": checks})
java = collections.Counter()
for xml in (root / "backend/target/surefire-reports").glob("TEST-*.xml"):
    suite = ET.parse(xml).getroot()
    for key in ("tests", "failures", "errors", "skipped"):
        java[key] += int(suite.get(key, 0))
summary = {"casesPreviouslyFailed": sum(record["previous"] == "FALLO" for record in records),
           "productDecisionsResolved": len(decisions), "casesVerified": sum(record["status"] == "CORREGIDO EN QA" for record in records),
           "variants": len(latest), "observationsIncludingReruns": len(observations),
           "latestVariantFailures": sum(entry["result"] != "OK" for entry in latest.values()), "java": dict(java)}
(out / "casos-corregidos.json").write_text(json.dumps(records, ensure_ascii=False, indent=2))
(out / "ultimas-variantes.json").write_text(json.dumps(list(latest.values()), ensure_ascii=False, indent=2))
(out / "resumen.json").write_text(json.dumps(summary, ensure_ascii=False, indent=2))
matrix = ["# Correcciones de los casos con fallo y decisiones pendientes", "",
          "Estado del código probado en QA; no acredita despliegue ni todas las variantes de los 222 casos.", "",
          "| Caso | Estado anterior | Corrección/criterio aplicado | Verificación |", "|---|---|---|---|"]
for record in records:
    matrix.append(f"| {record['id']} | {record['previous']} | {record['change']} | {record['status']}; {len(record['checks'])} variantes |")
(out / "MATRIZ-CORRECCIONES.md").write_text("\n".join(matrix) + "\n")
files = []
for directory in ("backend/src", "frontend/src", "db", "scripts/qa"):
    files.extend(p for p in (root / directory).rglob("*") if p.is_file() and p.suffix in (".java", ".js", ".jsx", ".css", ".sql", ".py", ".mjs", ".yaml"))
files.extend(root / name for name in ("backend/pom.xml", "backend/Dockerfile", "frontend/Dockerfile", "frontend/nginx.conf", "frontend/nginx.production.conf", "frontend/nginx.managed-proxy.conf.template", "frontend/package.json", "frontend/package-lock.json"))
(out / "source-manifest.json").write_text(json.dumps({"head": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip(),
    "dirty": True, "files": {str(p.relative_to(root)): hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(set(files))}}, indent=2))
print(json.dumps(summary, ensure_ascii=False))
