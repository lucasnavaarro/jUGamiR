import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from csv_rows import read_rows


class CsvRowsTests(unittest.TestCase):
    def read(self, content):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "questions.csv"
            path.write_text(content, encoding="utf-8")
            return read_rows(path)

    def test_bom_quotes_semicolon_and_multiline(self):
        rows = self.read('\ufeffAsignatura;Enunciado;Opciones;RespuestaCorrecta\nAnatomía;"Texto; con\nsalto y ""comillas""";A | B;1\n')
        self.assertEqual(rows[0]["Enunciado"], 'Texto; con\nsalto y "comillas"')
        self.assertEqual(rows[0]["Asignatura"], "Anatomía")

    def test_identifier_is_optional(self):
        self.assertEqual(len(self.read("Asignatura;Enunciado;Opciones;RespuestaCorrecta\nA;Q;A | B;1\n")), 1)

    def test_missing_required_header_is_rejected(self):
        with self.assertRaises(ValueError):
            self.read("Asignatura;Enunciado\nA;Q\n")

    def test_short_and_extra_rows_are_rejected(self):
        for row in ["A;Q;A | B", "A;Q;A | B;1;extra"]:
            with self.subTest(row=row), self.assertRaises(ValueError):
                self.read("Asignatura;Enunciado;Opciones;RespuestaCorrecta\n" + row + "\n")

    def test_unclosed_quotes_are_rejected(self):
        import csv
        with self.assertRaises(csv.Error):
            self.read('Asignatura;Enunciado;Opciones;RespuestaCorrecta\nA;"unfinished\n')


if __name__ == "__main__":
    unittest.main()
