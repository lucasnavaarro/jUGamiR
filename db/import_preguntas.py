#!/usr/bin/env python3
"""
import_preguntas.py
===================
Importa las preguntas de preguntas_mir_final.csv a la tabla `preguntas`.
Las respuestas se insertan en un paso separado.

Uso:
    python3 db/import_preguntas.py

Variables de entorno opcionales:
    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
    CREADA_POR_ID   (id_usuario del usuario que figura como autor; por defecto
                     se selecciona automáticamente el primer usuario de la BD)
"""

import os
import re
import sys
import unicodedata
import psycopg2

# ── Conexión ──────────────────────────────────────────────────────────────────
DB_CONFIG = {
    "host":     os.getenv("DB_HOST",     "localhost"),
    "port":     int(os.getenv("DB_PORT", "5432")),
    "dbname":   os.getenv("DB_NAME",     "jugamirdb"),
    "user":     os.getenv("DB_USER",     "jugamir"),
    "password": os.getenv("DB_PASSWORD", "jugamir_pw"),
}

CSV_PATH = os.path.join(os.path.dirname(__file__), "..", "preguntas_mir_final.csv")

# ── Helpers ───────────────────────────────────────────────────────────────────

def null_if_dash(value: str | None) -> str | None:
    """Devuelve None si el valor es None, vacío o '--'."""
    if value is None:
        return None
    v = value.strip()
    return None if (not v or v == "--") else v


def extract_anio(identificador: str) -> int | None:
    """
    Extrae el segundo año del identificador MIR.
      "MIR 2024-25, P32"   → 2025
      "MIR 1999-2000, P1"  → 2000
      "MIR 2025, P23"      → 2025
    """
    m = re.search(r'MIR\s+(\d{4})(?:-(\d{2,4}))?', identificador)
    if not m:
        return None
    first  = m.group(1)      # p.ej. "2024" o "1999"
    second = m.group(2)      # p.ej. "25", "2000" o None
    if second:
        if len(second) == 2:
            return int(first[:2] + second)   # "20" + "25" = 2025
        else:
            return int(second)               # "2000"
    return int(first)


def parse_anulada(anulada_col: str, resp_col: str) -> bool:
    """
    La pregunta se considera anulada si:
      - La columna Anulada es 'SI', o
      - RespuestaCorrecta es 'ANULADA' o 'IMPUGNADA'
    """
    return (
        anulada_col.strip().upper() == "SI"
        or resp_col.strip().upper() in ("ANULADA", "IMPUGNADA")
    )


def parse_dificultad(valor: str) -> str:
    """Normaliza el valor de dificultad al enum de la BD."""
    v = valor.strip().upper()
    if v in ("FACIL", "MEDIO", "DIFICIL"):
        return v
    return "MEDIO"   # fallback

# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    # Leer CSV completo (separador ';')
    with open(CSV_PATH, encoding="utf-8") as f:
        raw = f.read().splitlines()

    if not raw:
        print("El CSV está vacío.")
        sys.exit(1)

    header = [h.strip() for h in raw[0].split(";")]
    filas  = [dict(zip(header, line.split(";"))) for line in raw[1:] if line.strip()]
    print(f"Filas leídas del CSV: {len(filas)}")

    # Conectar a la BD
    try:
        conn = psycopg2.connect(**DB_CONFIG)
    except Exception as e:
        print(f"Error de conexión: {e}")
        sys.exit(1)

    conn.autocommit = False
    cur = conn.cursor()

    # ── Cargar caché de asignaturas { nombre → id } ───────────────────────────
    cur.execute("SELECT id, nombre FROM asignaturas")
    asig_cache: dict[str, int] = {nombre: id_ for id_, nombre in cur.fetchall()}
    print(f"Asignaturas en BD: {len(asig_cache)}")

    # ── Obtener creada_por (primer usuario disponible o variable de entorno) ──
    creada_por_env = os.getenv("CREADA_POR_ID")
    if creada_por_env:
        creada_por_id = int(creada_por_env)
    else:
        cur.execute("SELECT id_usuario FROM usuarios ORDER BY id_usuario LIMIT 1")
        row = cur.fetchone()
        if not row:
            print("No hay usuarios en la BD. Crea al menos uno antes de importar.")
            sys.exit(1)
        creada_por_id = row[0]

    print(f"creada_por = usuario {creada_por_id}")

    # ── Recorrer filas e insertar preguntas ───────────────────────────────────
    insertadas  = 0
    duplicadas  = 0
    errores     = []

    for i, row in enumerate(filas, start=2):   # start=2 porque la fila 1 es la cabecera

        asignatura_nombre = unicodedata.normalize("NFC", row.get("Asignatura", "").strip())
        identificador     = null_if_dash(row.get("Identificador"))
        titulo_indice     = null_if_dash(row.get("TituloIndice"))
        enunciado         = null_if_dash(row.get("Enunciado"))
        comentario        = null_if_dash(row.get("Comentario"))
        imagen_url        = null_if_dash(row.get("Imagen"))
        dificultad        = parse_dificultad(row.get("Dificultad", "MEDIO"))
        anulada           = parse_anulada(
                                row.get("Anulada", "NO"),
                                row.get("RespuestaCorrecta", "")
                            )
        anio              = extract_anio(identificador or "")

        # Validaciones mínimas
        if not enunciado:
            errores.append(f"Fila {i} ({identificador}): enunciado vacío, omitida.")
            continue

        asignatura_id = asig_cache.get(asignatura_nombre)
        if asignatura_id is None:
            errores.append(
                f"Fila {i} ({identificador}): asignatura '{asignatura_nombre}' "
                f"no encontrada en BD, omitida."
            )
            continue

        # Insertar con savepoint para continuar ante errores puntuales
        cur.execute("SAVEPOINT fila")
        try:
            cur.execute(
                """
                INSERT INTO preguntas
                    (identificador, titulo_indice, enunciado, imagen_url,
                     anio, comentario, anulada, dificultad, estado,
                     asignatura_id, creada_por, creada_el, actualizada_el)
                VALUES
                    (%s, %s, %s, %s,
                     %s, %s, %s, %s, 'BORRADOR',
                     %s, %s, NOW(), NOW())
                ON CONFLICT (identificador) DO NOTHING
                RETURNING id
                """,
                (
                    identificador, titulo_indice, enunciado, imagen_url,
                    anio, comentario, anulada, dificultad,
                    asignatura_id, creada_por_id,
                ),
            )
        except Exception as e:
            cur.execute("ROLLBACK TO SAVEPOINT fila")
            errores.append(f"Fila {i} ({identificador}): {e}")
            continue

        resultado = cur.fetchone()
        if resultado is None:
            # ON CONFLICT DO NOTHING → ya existía
            cur.execute("RELEASE SAVEPOINT fila")
            duplicadas += 1
            continue

        cur.execute("RELEASE SAVEPOINT fila")
        insertadas += 1

        # Commit parcial cada 500 preguntas
        if insertadas % 500 == 0:
            conn.commit()
            print(f"  … {insertadas} preguntas insertadas …")

    conn.commit()
    cur.close()
    conn.close()

    # ── Resumen ───────────────────────────────────────────────────────────────
    print("\n=== IMPORTACIÓN DE PREGUNTAS COMPLETADA ===")
    print(f"  Insertadas :  {insertadas}")
    print(f"  Duplicadas :  {duplicadas}")
    print(f"  Omitidas   :  {len(errores)}")
    if errores:
        print(f"\n  ⚠  Primeros errores:")
        for e in errores[:20]:
            print(f"     {e}")
        if len(errores) > 20:
            print(f"     … y {len(errores) - 20} más.")


if __name__ == "__main__":
    main()
