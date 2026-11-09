"""Strict CSV decoding shared by the CLI importers and the HTTP import."""
import csv


def read_rows(path):
    with open(path, encoding="utf-8-sig", newline="") as source:
        reader = csv.DictReader(source, delimiter=";", strict=True)
        if reader.fieldnames is None:
            raise ValueError("El CSV está vacío")
        reader.fieldnames = [name.strip() for name in reader.fieldnames]
        required = {"Asignatura", "Enunciado", "Opciones", "RespuestaCorrecta"}
        if not required.issubset(reader.fieldnames):
            raise ValueError("Faltan columnas obligatorias: " + ", ".join(sorted(required - set(reader.fieldnames))))
        rows = list(reader)
        if any(None in row or any(value is None for value in row.values()) for row in rows):
            raise ValueError("Una fila tiene un número de columnas distinto a la cabecera")
        return rows
