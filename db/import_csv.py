#!/usr/bin/env python3
"""
import_csv.py
============
Importa preguntas_mir.csv a las tablas: asignaturas, preguntas, respuestas.

Uso:
    # Con Docker corriendo:
    python3 db/import_csv.py

    # Con variables de entorno personalizadas:
    DB_HOST=localhost DB_PORT=5432 DB_NAME=jugamirdb DB_USER=jugamir DB_PASSWORD=jugamir_pw \
        python3 db/import_csv.py

Requisitos:
    pip install psycopg2-binary
"""

import csv
import os
import sys
import re
import psycopg2
from psycopg2.extras import execute_batch

# ---------------------------------------------------------------------------
# Configuración de conexión (lee variables de entorno o usa defaults de Docker)
# ---------------------------------------------------------------------------
DB_CONFIG = {
    "host":     os.getenv("DB_HOST",     "localhost"),
    "port":     int(os.getenv("DB_PORT", "5432")),
    "dbname":   os.getenv("DB_NAME",     "jugamirdb"),
    "user":     os.getenv("DB_USER",     "jugamir"),
    "password": os.getenv("DB_PASSWORD", "jugamir_pw"),
}

CSV_PATH       = os.path.join(os.path.dirname(__file__), "..", "preguntas_mir.csv")
IMG_PREFIX_OLD = "imagenes/"
IMG_PREFIX_NEW = "/home/lucas/Escritorio/TFG/jUGamiR/imagenes/"

# ---------------------------------------------------------------------------
# Mapping asignatura (nombre en CSV) → categoría (nombre en BD)
# ---------------------------------------------------------------------------
ASIGNATURA_CATEGORIA: dict = {
    "DIGESTIVO":                 "Metabolismo y Mente",
    "CIRUGIA ENDOCRINA":         "Metabolismo y Mente",
    "PSIQUIATRIA":               "Metabolismo y Mente",
    "CARDIOLOGIA":               "Cardio y Sangre",
    "HEMATOLOGÍA":               "Cardio y Sangre",
    "VASCULAR":                  "Cardio y Sangre",
    "UROLOGIA":                  "Neuro, Renal e Infecciosas",
    "NEFROLOGIA":                "Neuro, Renal e Infecciosas",
    "INFECCIOSAS":               "Neuro, Renal e Infecciosas",
    "NEUROLOGIA Y NEUROCIRUGIA": "Neuro, Renal e Infecciosas",
    "OBSTETRICIA":               "Respiratorio y Reproducción",
    "GINECOLOGIA":               "Respiratorio y Reproducción",
    "RESPIRATORIO":              "Respiratorio y Reproducción",
    "REUMATOLOGIA Y SISTEMICAS": "Locomotor, Piel y Defensas",
    "TRAUMATOLOGIA":             "Locomotor, Piel y Defensas",
    "DERMATOLOGIA":              "Locomotor, Piel y Defensas",
    "INMUNOLOGIA":               "Locomotor, Piel y Defensas",
    "PEDIATRIA-I":               "Pediatría y Primaria",
    "PEDIATRIA-II":              "Pediatría y Primaria",
    "ATENCION PRIMARIA":         "Pediatría y Primaria",
}

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def null_if_empty(value):
    """Devuelve None si el valor es None, vacío, '--' o solo espacios."""
    if value is None:
        return None
    v = value.strip()
    return None if (not v or v == "--") else v


def fix_imagen_url(raw) -> str | None:
    """Corrige el prefijo de la ruta de imagen. Devuelve None si no hay imagen."""
    v = null_if_empty(raw)
    if v is None:
        return None
    # Pueden venir varias imágenes separadas por ' | '
    # Tomamos solo la primera
    first = v.split(" | ")[0].strip()
    if first.startswith(IMG_PREFIX_OLD):
        return IMG_PREFIX_NEW + first[len(IMG_PREFIX_OLD):]
    # Si ya es ruta absoluta o tiene otro formato, la devolvemos tal cual
    return first


def parse_opciones(raw: str) -> list[str]:
    """
    Convierte el campo Opciones en una lista de textos de respuesta.
    Formato: '1. Texto uno | 2. Texto dos | 3. Texto tres | 4. Texto cuatro'
    Elimina el prefijo numérico '1. ', '2. ', etc.
    """
    parts = [p.strip() for p in raw.split(" | ") if p.strip()]
    cleaned = []
    for part in parts:
        # Quitar prefijo '1. ', '2. ', …  (uno o dos dígitos + punto + espacio)
        cleaned.append(re.sub(r"^\d{1,2}\.\s*", "", part))
    return cleaned


def parse_respuesta_correcta(raw: str) -> int | None:
    """Devuelve el índice (1-based) de la respuesta correcta, o None si es inválido."""
    v = raw.strip()
    try:
        n = int(v)
        return n if 1 <= n <= 10 else None
    except ValueError:
        return None


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------

def main():
    csv_path = os.path.abspath(CSV_PATH)
    if not os.path.exists(csv_path):
        print(f"ERROR: No se encuentra el CSV en {csv_path}", file=sys.stderr)
        sys.exit(1)

    print(f"Conectando a PostgreSQL {DB_CONFIG['host']}:{DB_CONFIG['port']} / {DB_CONFIG['dbname']} …")
    try:
        conn = psycopg2.connect(**DB_CONFIG)
    except Exception as e:
        print(f"ERROR de conexión: {e}", file=sys.stderr)
        sys.exit(1)

    cur = conn.cursor()

    # ------------------------------------------------------------------
    # Lectura del CSV
    # ------------------------------------------------------------------
    rows = []
    with open(csv_path, encoding="utf-8") as f:
        reader = csv.DictReader(f, delimiter=";")
        for row in reader:
            rows.append(row)

    print(f"Filas leídas del CSV: {len(rows)}")

    # ------------------------------------------------------------------
    # 1) Insertar asignaturas con su categoria_id
    # ------------------------------------------------------------------
    asignaturas_set = sorted({r["Asignatura"].strip() for r in rows if r["Asignatura"].strip()})
    print(f"Asignaturas encontradas: {len(asignaturas_set)}")

    # Obtener mapa nombre_categoria → id desde la BD
    cur.execute("SELECT id, nombre FROM categorias")
    categoria_ids: dict = {nombre: cid for cid, nombre in cur.fetchall()}
    if not categoria_ids:
        print("AVISO: la tabla categorias está vacía. Ejecuta seed.sql antes de este script.", file=sys.stderr)

    for asig_nombre in asignaturas_set:
        cat_nombre = ASIGNATURA_CATEGORIA.get(asig_nombre)
        cat_id = categoria_ids.get(cat_nombre) if cat_nombre else None
        if cat_id is None:
            print(f"  AVISO: asignatura '{asig_nombre}' sin categoría asignada (categoria_id=NULL)")
        cur.execute(
            """
            INSERT INTO asignaturas (nombre, categoria_id)
            VALUES (%s, %s)
            ON CONFLICT (nombre) DO UPDATE SET categoria_id = EXCLUDED.categoria_id
            """,
            (asig_nombre, cat_id),
        )

    # Obtener mapa nombre → id
    cur.execute("SELECT id, nombre FROM asignaturas")
    asignatura_ids: dict = {nombre: aid for aid, nombre in cur.fetchall()}

    # ------------------------------------------------------------------
    # 2) Insertar preguntas + respuestas
    # ------------------------------------------------------------------
    preguntas_insertadas   = 0
    preguntas_duplicadas   = 0
    respuestas_insertadas  = 0
    errores                = []

    for i, row in enumerate(rows, start=1):
        asignatura_nombre = row["Asignatura"].strip()
        identificador     = null_if_empty(row["Identificador"])
        titulo_indice     = null_if_empty(row["TituloIndice"])
        enunciado         = row["Enunciado"].strip()
        opciones_raw      = row["Opciones"].strip()
        respuesta_correcta_num = parse_respuesta_correcta(row["RespuestaCorrecta"])
        comentario        = null_if_empty(row["Comentario"])
        anulada           = (row.get("Anulada") or "").strip().upper() == "SI"
        imagen_url        = fix_imagen_url(row["Imagen"])
        asignatura_id     = asignatura_ids.get(asignatura_nombre)

        if not enunciado:
            errores.append(f"Fila {i}: enunciado vacío, ignorada.")
            continue

        if asignatura_id is None:
            errores.append(f"Fila {i}: asignatura '{asignatura_nombre}' no encontrada, ignorada.")
            continue

        opciones = parse_opciones(opciones_raw)
        if not opciones:
            errores.append(f"Fila {i}: sin opciones, ignorada.")
            continue

        if respuesta_correcta_num is None or respuesta_correcta_num > len(opciones):
            errores.append(
                f"Fila {i} ({identificador}): respuesta correcta '{row['RespuestaCorrecta']}' "
                f"inválida (hay {len(opciones)} opciones), ignorada."
            )
            continue

        # Insertar pregunta como BORRADOR para no disparar el trigger de correctas
        cur.execute("SAVEPOINT fila")
        try:
            cur.execute(
                """
                INSERT INTO preguntas
                    (identificador, titulo_indice, enunciado, imagen_url,
                     comentario, anulada, dificultad, estado, asignatura_id)
                VALUES (%s, %s, %s, %s, %s, %s, 'MEDIO', 'BORRADOR', %s)
                ON CONFLICT (identificador) DO NOTHING
                RETURNING id
                """,
                (identificador, titulo_indice, enunciado, imagen_url,
                 comentario, anulada, asignatura_id),
            )
        except Exception as e:
            cur.execute("ROLLBACK TO SAVEPOINT fila")
            errores.append(f"Fila {i} ({identificador}): error al insertar pregunta: {e}")
            continue

        result = cur.fetchone()
        if result is None:
            # ON CONFLICT DO NOTHING → pregunta ya existía
            cur.execute("RELEASE SAVEPOINT fila")
            preguntas_duplicadas += 1
            continue

        pregunta_id = result[0]

        # Insertar respuestas (la pregunta está en BORRADOR → no dispara trigger)
        resp_ok = True
        for orden, texto in enumerate(opciones, start=1):
            es_correcta = (orden == respuesta_correcta_num)
            try:
                cur.execute(
                    """
                    INSERT INTO respuestas (pregunta_id, texto_respuesta, es_correcta, orden)
                    VALUES (%s, %s, %s, %s)
                    ON CONFLICT (pregunta_id, orden) DO NOTHING
                    """,
                    (pregunta_id, texto, es_correcta, orden),
                )
                respuestas_insertadas += 1
            except Exception as e:
                cur.execute("ROLLBACK TO SAVEPOINT fila")
                errores.append(f"Fila {i}, opción {orden}: error al insertar respuesta: {e}")
                resp_ok = False
                break

        if not resp_ok:
            continue

        # Ahora publicar: el trigger comprobará que haya exactamente 1 correcta
        try:
            cur.execute(
                "UPDATE preguntas SET estado = 'PUBLICADA' WHERE id = %s",
                (pregunta_id,),
            )
        except Exception as e:
            cur.execute("ROLLBACK TO SAVEPOINT fila")
            errores.append(f"Fila {i} ({identificador}): error al publicar pregunta: {e}")
            continue

        cur.execute("RELEASE SAVEPOINT fila")
        preguntas_insertadas += 1

        # Commit cada 200 preguntas para no acumular demasiada transacción
        if preguntas_insertadas % 200 == 0:
            conn.commit()
            print(f"  … {preguntas_insertadas} preguntas insertadas hasta ahora …")

    conn.commit()
    cur.close()
    conn.close()

    # ------------------------------------------------------------------
    # Resumen
    # ------------------------------------------------------------------
    print("\n=== IMPORTACIÓN COMPLETADA ===")
    print(f"  Asignaturas insertadas : {len(asignaturas_set)}")
    print(f"  Preguntas insertadas   : {preguntas_insertadas}")
    print(f"  Preguntas ya existían  : {preguntas_duplicadas}")
    print(f"  Respuestas insertadas  : {respuestas_insertadas}")
    if errores:
        print(f"\n  ⚠  {len(errores)} advertencias:")
        for e in errores[:20]:   # mostrar máx. 20
            print(f"     {e}")
        if len(errores) > 20:
            print(f"     … y {len(errores) - 20} más.")


if __name__ == "__main__":
    main()
