-- =========================================================
-- seed.sql
-- Script para poblar la base de datos con datos iniciales
-- =========================================================

BEGIN;

-- =========================================================
-- 1) Usuarios + subtipos
-- =========================================================

-- María García (profesora)
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('María', 'García López', 'maria.garcia@universidad.es', '11111111A')
ON CONFLICT (email) DO NOTHING;

INSERT INTO profesores (id_usuario, departamento, categoria)
SELECT id_usuario, 'Departamento de Informática', 'Titular'
FROM usuarios WHERE email = 'maria.garcia@universidad.es'
ON CONFLICT (id_usuario) DO NOTHING;

-- Juan Martínez (profesor)
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('Juan', 'Martínez Ruiz', 'juan.martinez@universidad.es', '22222222B')
ON CONFLICT (email) DO NOTHING;

INSERT INTO profesores (id_usuario, departamento, categoria)
SELECT id_usuario, 'Departamento de Matemáticas', 'Asociado'
FROM usuarios WHERE email = 'juan.martinez@universidad.es'
ON CONFLICT (id_usuario) DO NOTHING;

-- Ana Rodríguez (profesora)
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('Ana', 'Rodríguez Pérez', 'ana.rodriguez@universidad.es', '33333333C')
ON CONFLICT (email) DO NOTHING;

INSERT INTO profesores (id_usuario, departamento, categoria)
SELECT id_usuario, 'Departamento de Física', 'Contratada Doctora'
FROM usuarios WHERE email = 'ana.rodriguez@universidad.es'
ON CONFLICT (id_usuario) DO NOTHING;

-- Pedro Sánchez (jugador)
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('Pedro', 'Sánchez Gómez', 'pedro.sanchez@estudiante.es', '44444444D')
ON CONFLICT (email) DO NOTHING;

INSERT INTO jugadores (id_usuario, nick, nivel)
SELECT id_usuario, 'PedroGamer', 0
FROM usuarios WHERE email = 'pedro.sanchez@estudiante.es'
ON CONFLICT (id_usuario) DO NOTHING;

-- Lucía Fernández (jugadora)
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('Lucía', 'Fernández Torres', 'lucia.fernandez@estudiante.es', '55555555E')
ON CONFLICT (email) DO NOTHING;

INSERT INTO jugadores (id_usuario, nick, nivel)
SELECT id_usuario, 'LucyStar', 0
FROM usuarios WHERE email = 'lucia.fernandez@estudiante.es'
ON CONFLICT (id_usuario) DO NOTHING;

-- Carlos López (jugador)
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('Carlos', 'López Martín', 'carlos.lopez@estudiante.es', '66666666F')
ON CONFLICT (email) DO NOTHING;

INSERT INTO jugadores (id_usuario, nick, nivel)
SELECT id_usuario, 'CharlyL', 0
FROM usuarios WHERE email = 'carlos.lopez@estudiante.es'
ON CONFLICT (id_usuario) DO NOTHING;

-- Sofía Moreno (jugadora)
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('Sofía', 'Moreno Jiménez', 'sofia.moreno@estudiante.es', '77777777G')
ON CONFLICT (email) DO NOTHING;

INSERT INTO jugadores (id_usuario, nick, nivel)
SELECT id_usuario, 'SofiPro', 0
FROM usuarios WHERE email = 'sofia.moreno@estudiante.es'
ON CONFLICT (id_usuario) DO NOTHING;

-- Diego Ruiz (jugador)
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('Diego', 'Ruiz Álvarez', 'diego.ruiz@estudiante.es', '88888888H')
ON CONFLICT (email) DO NOTHING;

INSERT INTO jugadores (id_usuario, nick, nivel)
SELECT id_usuario, 'DiegoMaster', 0
FROM usuarios WHERE email = 'diego.ruiz@estudiante.es'
ON CONFLICT (id_usuario) DO NOTHING;

-- Elena Navarro (jugadora)
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('Elena', 'Navarro Castro', 'elena.navarro@estudiante.es', '99999999I')
ON CONFLICT (email) DO NOTHING;

INSERT INTO jugadores (id_usuario, nick, nivel)
SELECT id_usuario, 'ElenaQuiz', 0
FROM usuarios WHERE email = 'elena.navarro@estudiante.es'
ON CONFLICT (id_usuario) DO NOTHING;

-- Administrador del sistema
INSERT INTO usuarios (nombre, apellidos, email, dni)
VALUES ('Admin', 'Sistema', 'admin@jugamir.es', '00000000Z')
ON CONFLICT (email) DO NOTHING;

INSERT INTO administradores (id_usuario, superadmin)
SELECT id_usuario, TRUE
FROM usuarios WHERE email = 'admin@jugamir.es'
ON CONFLICT (id_usuario) DO NOTHING;

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
