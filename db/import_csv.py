#!/usr/bin/env python3
"""Import questions and answers together, with one savepoint per complete row."""
import os
import hashlib
import sys
import unicodedata
import psycopg2
from csv_rows import read_rows
from import_preguntas import DB_CONFIG, extract_anio, null_if_dash, parse_dificultad, parse_anulada


def main():
    rows = read_rows(sys.argv[1])
    added = duplicates = 0
    errors = []
    generated = 0
    with psycopg2.connect(**DB_CONFIG) as connection:
        with connection.cursor() as cursor:
            cursor.execute("SELECT nombre, id FROM asignaturas")
            subjects = {unicodedata.normalize("NFC", name): value for name, value in cursor.fetchall()}
            for line, row in enumerate(rows, start=2):
                cursor.execute("SAVEPOINT csv_row")
                try:
                    subject = subjects.get(unicodedata.normalize("NFC", row["Asignatura"].strip()))
                    statement = null_if_dash(row["Enunciado"])
                    identifier = null_if_dash(row.get("Identificador"))
                    generated_identifier = not identifier
                    if generated_identifier:
                        # Stable across retries; identical complete content gets the same identifier.
                        identity = "\0".join([str(subject), statement or "", row["Opciones"], row["RespuestaCorrecta"]])
                        identifier = "CSV-" + hashlib.sha256(identity.encode("utf-8")).hexdigest()[:40]
                    if not subject or not statement:
                        raise ValueError("Asignatura inexistente o enunciado vacío")
                    if identifier and len(identifier) > 60:
                        raise ValueError("Identificador demasiado largo")
                    options = [value.strip() for value in row["Opciones"].split(" | ") if value.strip()]
                    annulled = parse_anulada(row.get("Anulada", "NO"), row["RespuestaCorrecta"])
                    try:
                        correct = int(row["RespuestaCorrecta"])
                    except ValueError:
                        correct = 0
                    if len(options) < 2 or not annulled and not 1 <= correct <= len(options):
                        raise ValueError("Opciones o respuesta correcta inválidas")
                    cursor.execute("""INSERT INTO preguntas
                        (identificador,titulo_indice,enunciado,imagen_url,anio,comentario,anulada,dificultad,estado,asignatura_id,creada_por)
                        VALUES (%s,%s,%s,%s,%s,%s,%s,%s,'BORRADOR',%s,%s)
                        ON CONFLICT (identificador) DO NOTHING RETURNING id""",
                        (identifier, null_if_dash(row.get("TituloIndice")), statement,
                         null_if_dash(row.get("Imagen")), extract_anio(identifier or ""),
                         null_if_dash(row.get("Comentario")), annulled,
                         parse_dificultad(row.get("Dificultad", "MEDIO")), subject, int(os.environ["CREADA_POR_ID"])))
                    inserted = cursor.fetchone()
                    if inserted is None:
                        duplicates += 1
                    else:
                        question = inserted[0]
                        for order, text in enumerate(options, start=1):
                            cursor.execute("INSERT INTO respuestas(pregunta_id,texto_respuesta,es_correcta,orden) VALUES (%s,%s,%s,%s)",
                                           (question, text, order == correct, order))
                        if not annulled:
                            cursor.execute("UPDATE preguntas SET estado='PUBLICADA' WHERE id=%s", (question,))
                        added += 1
                        if generated_identifier: generated += 1
                except (ValueError, psycopg2.Error) as error:
                    cursor.execute("ROLLBACK TO SAVEPOINT csv_row")
                    # Avoid exposing SQL/connection details in HTTP responses.
                    reason = str(error) if isinstance(error, ValueError) else "Datos incompatibles con el banco de preguntas"
                    errors.append(f"Fila {line}: {reason}; omitida")
                finally:
                    cursor.execute("RELEASE SAVEPOINT csv_row")
    print(f"Insertadas: {added}\nDuplicadas: {duplicates}\nOmitidas: {len(errors)}")
    print(f"Identificadores generados: {generated}")
    for error in errors[:50]:
        print(error)


if __name__ == "__main__":
    try:
        main()
    except Exception:
        print("No se pudo importar el CSV. Revisa sus columnas, codificación y disponibilidad del servicio.")
        sys.exit(1)
