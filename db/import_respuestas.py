#!/usr/bin/env python3
"""
import_respuestas.py
====================
Importa las respuestas de preguntas_mir_final.csv a la tabla `respuestas`
y publica las preguntas que tienen una respuesta correcta definida.

Uso:
    python3 db/import_respuestas.py

Variables de entorno opcionales:
    DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
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

#CSV_PATH = os.path.join(os.path.dirname(__file__), "..", "preguntas_mir_final.csv")
CSV_PATH = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), "..", "preguntas_mir_final.csv")


# ── Main ──────────────────────────────────────────────────────────────────────

def main():
    # Leer CSV
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

    # ── Cargar caché de preguntas { identificador → id } ─────────────────────
    cur.execute("SELECT identificador, id FROM preguntas WHERE identificador IS NOT NULL")
    preg_cache: dict[str, int] = {ident: id_ for ident, id_ in cur.fetchall()}
    print(f"Preguntas en BD con identificador: {len(preg_cache)}")

    # ── Contadores ────────────────────────────────────────────────────────────
    respuestas_insertadas = 0
    preguntas_publicadas  = 0
    preguntas_anuladas    = 0   # con respuesta ANULADA/IMPUGNADA
    duplicadas            = 0
    errores               = []

    for i, row in enumerate(filas, start=2):

        identificador = (row.get("Identificador") or "").strip()
        if not identificador or identificador == "--":
            continue

        opciones_raw  = (row.get("Opciones") or "").strip()
        resp_correcta = (row.get("RespuestaCorrecta") or "").strip().upper()

        # Buscar pregunta en BD
        pregunta_id = preg_cache.get(identificador)
        if pregunta_id is None:
            errores.append(
                f"Fila {i} ({identificador}): no encontrada en BD, omitida."
            )
            continue

        # Si no hay opciones, no hay nada que insertar
        if not opciones_raw or opciones_raw == "--":
            continue

        # Parsear opciones: "1. Texto A. | 2. Texto B. | ..."
        opciones = [o.strip() for o in opciones_raw.split(" | ") if o.strip()]
        if not opciones:
            continue

        # Determinar cuál es la correcta (0 = ninguna, para ANULADA/IMPUGNADA)
        es_anulada = resp_correcta in ("ANULADA", "IMPUGNADA")
        try:
            correcta_num = int(resp_correcta) if not es_anulada else 0
        except ValueError:
            correcta_num = 0

        # ── Insertar respuestas ───────────────────────────────────────────────
        cur.execute("SAVEPOINT pregunta")
        hubo_error = False
        resp_esta_fila = 0

        for orden, texto in enumerate(opciones, start=1):
            es_correcta_bool = (orden == correcta_num)
            try:
                cur.execute(
                    """
                    INSERT INTO respuestas
                        (pregunta_id, texto_respuesta, es_correcta, orden)
                    VALUES (%s, %s, %s, %s)
                    ON CONFLICT (pregunta_id, orden) DO NOTHING
                    """,
                    (pregunta_id, texto, es_correcta_bool, orden),
                )
                if cur.rowcount > 0:
                    resp_esta_fila += 1
                else:
                    duplicadas += 1
            except Exception as e:
                cur.execute("ROLLBACK TO SAVEPOINT pregunta")
                errores.append(
                    f"Fila {i} ({identificador}), opción {orden}: {e}"
                )
                hubo_error = True
                break

        if hubo_error:
            continue

        respuestas_insertadas += resp_esta_fila

        # ── Publicar la pregunta si tiene una correcta válida ─────────────────
        if not es_anulada and correcta_num > 0:
            try:
                cur.execute(
                    "UPDATE preguntas SET estado = 'PUBLICADA' WHERE id = %s AND estado = 'BORRADOR'",
                    (pregunta_id,),
                )
                if cur.rowcount > 0:
                    preguntas_publicadas += 1
            except Exception as e:
                cur.execute("ROLLBACK TO SAVEPOINT pregunta")
                errores.append(
                    f"Fila {i} ({identificador}): error al publicar: {e}"
                )
                continue
        else:
            preguntas_anuladas += 1

        cur.execute("RELEASE SAVEPOINT pregunta")

        # Commit parcial cada 500 preguntas procesadas
        if (preguntas_publicadas + preguntas_anuladas) % 500 == 0:
            conn.commit()
            print(f"  … {preguntas_publicadas} publicadas, {preguntas_anuladas} anuladas …")

    conn.commit()
    cur.close()
    conn.close()

    # ── Resumen ───────────────────────────────────────────────────────────────
    print("\n=== IMPORTACIÓN DE RESPUESTAS COMPLETADA ===")
    print(f"  Respuestas insertadas:    {respuestas_insertadas}")
    print(f"  Respuestas ya existían:   {duplicadas}")
    print(f"  Preguntas publicadas:     {preguntas_publicadas}")
    print(f"  Preguntas ANULADAS/IMPL.: {preguntas_anuladas}  (se quedan en BORRADOR)")
    print(f"  Errores:                  {len(errores)}")
    if errores:
        print(f"\n  ⚠  Primeros errores:")
        for e in errores[:20]:
            print(f"     {e}")
        if len(errores) > 20:
            print(f"     … y {len(errores) - 20} más.")


if __name__ == "__main__":
    main()
