import psycopg2
import csv

conn = psycopg2.connect(
    host="localhost", port=5432,
    dbname="jugamirdb", user="jugamir", password="jugamir_pw"
)
cur = conn.cursor()

with open('/home/lucas/Escritorio/TFG/jUGamiR/preguntas_mir_final.csv', encoding='utf-8') as f:
    reader = csv.reader(f, delimiter=';')
    next(reader)  # saltar cabecera
    count = 0
    for row in reader:
        if len(row) < 4:
            continue
        identificador = row[1].strip()
        enunciado = row[3].strip()
        if 'respuesta correcta:' in enunciado:
            cur.execute(
                "UPDATE preguntas SET enunciado = %s WHERE identificador = %s",
                (enunciado, identificador)
            )
            count += cur.rowcount

conn.commit()
cur.close()
conn.close()
print(f"Actualizadas: {count} preguntas")
