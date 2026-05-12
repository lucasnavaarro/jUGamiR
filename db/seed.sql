-- =========================================================
-- seed.sql
-- Script para poblar la base de datos con datos iniciales
-- =========================================================

BEGIN;

-- =========================================================
-- 2) Categorías
-- =========================================================
INSERT INTO categorias (nombre, color, descripcion)
VALUES
  ('Metabolismo y Mente',           '#FFFF00', 'Preguntas sobre DIGESTIVO, CIRUGIA ENDOCRINA Y PSIQUIATRIA'),
  ('Cardio y Sangre',               '#804000', 'Preguntas sobre CARDIOLOGIA, HEMATOLOGIA Y VASCULAR'),
  ('Neuro, Renal e Infecciosas',    '#0000FF', 'Preguntas sobre UROLOGIA. NEFROLOGIA, ENF.INFECCIOSAS Y NEUROLOGIA'),
  ('Respiratorio y Reproducción',   '#FF8000', 'Preguntas sobre OBSTETRICIA, GINECOLOGIA Y RESPIRATORIO'),
  ('Locomotor, Piel y Defensas',    '#08A94E', 'Preguntas sobre REUMATOLOGIA, TRAUMATOLOGIA, DERMATOLOGIA E INMUNOLOGIA'),
  ('Pediatría y Primaria',          '#F03687', 'Preguntas sobre PEDIATRIA I, PEDIATRIA II Y ATENCION PRIMARIA')
ON CONFLICT (nombre) DO NOTHING;

-- =========================================================
-- 3) Quesitos (1:1 con categorías)
-- =========================================================
INSERT INTO quesitos (id_categoria)
VALUES (1), (2), (3), (4), (5), (6)
ON CONFLICT (id_categoria) DO NOTHING;

-- =========================================================
-- Las asignaturas y preguntas se importan desde el CSV MIR.
-- Ejecutar: python3 db/import_csv.py
-- =========================================================

COMMIT;

-- =========================================================
-- Verificación de datos insertados
-- =========================================================
SELECT 'Usuarios:',        COUNT(*) FROM usuarios;
SELECT 'Jugadores:',       COUNT(*) FROM jugadores;
SELECT 'Profesores:',      COUNT(*) FROM profesores;
SELECT 'Administradores:', COUNT(*) FROM administradores;
SELECT 'Categorías:',      COUNT(*) FROM categorias;
SELECT 'Asignaturas:',     COUNT(*) FROM asignaturas;
SELECT 'Preguntas:',       COUNT(*) FROM preguntas;
SELECT 'Respuestas:',      COUNT(*) FROM respuestas;
