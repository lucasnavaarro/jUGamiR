-- =========================================================
-- 0) ENUMs
-- =========================================================
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'nivel_pregunta') THEN
    CREATE TYPE nivel_pregunta AS ENUM ('FACIL','MEDIO','DIFICIL');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_pregunta') THEN
    CREATE TYPE estado_pregunta AS ENUM ('BORRADOR','PUBLICADA','ARCHIVADA');
  END IF;

  --IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'modo_juego') THEN
  --  CREATE TYPE modo_juego AS ENUM ('SOLO','MULTI');
  --END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'estado_partida') THEN
    CREATE TYPE estado_partida AS ENUM ('ESPERANDO','EN_CURSO','TERMINADA','CANCELADA');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'resultado_jugador') THEN
    CREATE TYPE resultado_jugador AS ENUM ('VICTORIA','DERROTA','EMPATE','PENDIENTE','ABANDONADA','EXPULSADO');
  END IF;

  IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_partida') THEN
    CREATE TYPE tipo_partida AS ENUM ('PUBLICA', 'PRIVADA');
  END IF;

END $$;

-- =========================================================
-- 1) USERS + subtipos (JUGADOR / PROFESOR / ADMINISTRADOR ⊆ USUARIO)
-- =========================================================
CREATE TABLE IF NOT EXISTS usuarios (
  id_usuario      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre          VARCHAR(120) NOT NULL,
  apellidos       VARCHAR(120) NOT NULL,
  email           VARCHAR(255) NOT NULL UNIQUE,
  dni             VARCHAR(20)  NOT NULL UNIQUE,
  contraseña_hash TEXT NOT NULL,
  es_activo       BOOLEAN NOT NULL DEFAULT TRUE,
  creado_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  ultimo_login    TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS jugadores (
  id_usuario  BIGINT PRIMARY KEY REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
  nick        VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS profesores (
  id_usuario   BIGINT PRIMARY KEY REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
  departamento VARCHAR(120)
);

CREATE TABLE IF NOT EXISTS administradores (
  id_usuario  BIGINT PRIMARY KEY REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- =========================================================
-- 1.1) CODIGOS DE VERIFICACION
-- =========================================================
CREATE TABLE codigos_2fa (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_usuario  BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE UNIQUE,
  codigo      VARCHAR(6) NOT NULL,        -- el código de 6 dígitos
  expira_en   TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '10 minutes'),
  usado       BOOLEAN NOT NULL DEFAULT FALSE
);

-- =========================================================
-- 1.2) Token para reset de contraseña
-- =========================================================
CREATE TABLE password_reset_tokens (
    id        BIGSERIAL PRIMARY KEY,
    token     VARCHAR(255) NOT NULL UNIQUE,
    id_usuario BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    expira_en TIMESTAMP NOT NULL,
    usado     BOOLEAN NOT NULL DEFAULT FALSE
);


-- =========================================================
-- 2) CATEGORIAS + quesitos
-- =========================================================
CREATE TABLE IF NOT EXISTS categorias (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre      VARCHAR(120) NOT NULL UNIQUE,
  color       VARCHAR(30)  NOT NULL UNIQUE,
  descripcion TEXT
);

-- 1:1: un quesito por categoría
CREATE TABLE IF NOT EXISTS quesitos (
  id_categoria BIGINT PRIMARY KEY REFERENCES categorias(id) ON DELETE CASCADE
);

-- =========================================================
-- 3) ASIGNATURAS + profesores_asignaturas
-- =========================================================
CREATE TABLE IF NOT EXISTS asignaturas (
  id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre       VARCHAR(255) NOT NULL UNIQUE,
  categoria_id BIGINT NOT NULL REFERENCES categorias(id) ON DELETE RESTRICT
);

-- =========================================================
-- 4) PREGUNTAS + RESPUESTAS
-- =========================================================
CREATE TABLE IF NOT EXISTS preguntas (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  identificador  VARCHAR(60)  UNIQUE,                                          -- ID externo, ej: "MIR 2018, P125"
  titulo_indice  TEXT,                                                         -- Ej: "1. Sistema sanitario y modelo de AP."
  enunciado      TEXT NOT NULL,
  imagen_url     TEXT NULL,
  anio           INT  NULL,
  comentario     TEXT,                                                         -- Explicación/comentario del CSV
  anulada        BOOLEAN NOT NULL DEFAULT FALSE,
  dificultad     nivel_pregunta  NOT NULL DEFAULT 'MEDIO',
  estado         estado_pregunta NOT NULL DEFAULT 'BORRADOR',
  asignatura_id  BIGINT NOT NULL REFERENCES asignaturas(id) ON DELETE RESTRICT,
  creada_por     BIGINT NOT NULL REFERENCES usuarios(id_usuario) ON DELETE RESTRICT,
  creada_el      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  actualizada_el TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_preguntas_imagen_url
    CHECK (imagen_url IS NULL OR length(imagen_url) <= 2048)
);

CREATE INDEX IF NOT EXISTS idx_preguntas_estado      ON preguntas(estado);
CREATE INDEX IF NOT EXISTS idx_preguntas_asignatura  ON preguntas(asignatura_id);

CREATE TABLE IF NOT EXISTS respuestas (
  id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  pregunta_id BIGINT NOT NULL REFERENCES preguntas(id) ON DELETE CASCADE,
  texto_respuesta TEXT NOT NULL,
  es_correcta  BOOLEAN NOT NULL DEFAULT FALSE,
  orden  INT NOT NULL DEFAULT 0,
  CONSTRAINT uq_respuesta_orden UNIQUE (pregunta_id, orden)
);

CREATE INDEX IF NOT EXISTS idx_respuestas_pregunta ON respuestas(pregunta_id);

-- Máximo 1 correcta por pregunta (siempre)
CREATE UNIQUE INDEX IF NOT EXISTS uq_una_correcta_por_pregunta
ON respuestas(pregunta_id)
WHERE es_correcta = TRUE;

-- actualizada_el automático
CREATE OR REPLACE FUNCTION set_preguntas_actualizada_el()
RETURNS TRIGGER AS $$
BEGIN
  NEW.actualizada_el := NOW();
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_preguntas_actualizada_el ON preguntas;
CREATE TRIGGER trg_preguntas_actualizada_el
BEFORE UPDATE ON preguntas
FOR EACH ROW EXECUTE FUNCTION set_preguntas_actualizada_el();

-- =========================================================
-- 5) La clasificación (asignatura → categoría) está garantizada
--    por las FK: preguntas.asignatura_id → asignaturas.categoria_id
--    No se necesita tabla ternaria ni trigger adicional.
-- =========================================================

-- Publicar requiere exactamente 1 correcta
CREATE OR REPLACE FUNCTION asegurar_pregunta_unica_correcta_publicada()
RETURNS TRIGGER AS $$
DECLARE correct_count INT;
BEGIN
  IF (NEW.estado = 'PUBLICADA') AND (OLD.estado IS DISTINCT FROM NEW.estado) THEN
    SELECT COUNT(*) INTO correct_count
    FROM respuestas
    WHERE pregunta_id = NEW.id AND es_correcta = TRUE;

    IF correct_count <> 1 THEN
      RAISE EXCEPTION
        'No se puede publicar la pregunta %: debe tener exactamente 1 respuesta correcta (tiene %).',
        NEW.id, correct_count;
    END IF;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_asegurar_pregunta_unica_correcta_publicada ON preguntas;
CREATE TRIGGER trg_asegurar_pregunta_unica_correcta_publicada
BEFORE UPDATE OF estado ON preguntas
FOR EACH ROW EXECUTE FUNCTION asegurar_pregunta_unica_correcta_publicada();

-- Mantener exactamente 1 correcta mientras esté publicada
CREATE OR REPLACE FUNCTION asegurar_pregunta_unica_correcta_YA_publicada()
RETURNS TRIGGER AS $$
DECLARE
  pid BIGINT;
  pstatus estado_pregunta;
  correct_count INT;
BEGIN
  --En insert o update existe NEW y en delete OLD, COALESCE devuelve el primer valor no nulo
  pid := COALESCE(NEW.pregunta_id, OLD.pregunta_id); 
  SELECT estado INTO pstatus FROM preguntas WHERE id = pid;

  IF pstatus = 'PUBLICADA' THEN
    SELECT COUNT(*) INTO correct_count
    FROM respuestas
    WHERE pregunta_id = pid AND es_correcta = TRUE;

    IF correct_count <> 1 THEN
      RAISE EXCEPTION
        'La pregunta % esta publicada y debe mantener exactamente 1 respuesta correcta (tiene %).',
        pid, correct_count;
    END IF;
  END IF;

  RETURN NULL;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_comprobar_respuesta_pregunta_ins ON respuestas;
DROP TRIGGER IF EXISTS trg_comprobar_respuesta_pregunta_upd ON respuestas;
DROP TRIGGER IF EXISTS trg_comprobar_respuesta_pregunta_del ON respuestas;

CREATE TRIGGER trg_comprobar_respuesta_pregunta_ins
AFTER INSERT ON respuestas
FOR EACH ROW EXECUTE FUNCTION asegurar_pregunta_unica_correcta_YA_publicada();

CREATE TRIGGER trg_comprobar_respuesta_pregunta_upd
AFTER UPDATE OF es_correcta ON respuestas
FOR EACH ROW EXECUTE FUNCTION asegurar_pregunta_unica_correcta_YA_publicada();

CREATE TRIGGER trg_comprobar_respuesta_pregunta_del
AFTER DELETE ON respuestas
FOR EACH ROW EXECUTE FUNCTION asegurar_pregunta_unica_correcta_YA_publicada();

-- =========================================================
-- 6) PARTIDAS + participación + preguntas en partida (codigo_union, max_jugadores=6, no repetir)
-- =========================================================
CREATE TABLE IF NOT EXISTS partidas (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  codigo_union       VARCHAR(12) UNIQUE,
  max_jugadores      INT NOT NULL DEFAULT 6,
  tipo               tipo_partida NOT NULL DEFAULT 'PUBLICA',  -- PUBLICA | PRIVADA
  dificultad         nivel_pregunta NOT NULL DEFAULT 'MEDIO',    -- FÁCIL | MEDIO | DIFÍCIL
  tiempo_respuesta   INT NOT NULL DEFAULT 30,                 -- segundos por pregunta
  --modo               modo_juego NOT NULL DEFAULT 'MULTI',
  estado             estado_partida NOT NULL DEFAULT 'ESPERANDO',
  duracion           INT CHECK (duracion IS NULL OR duracion > 0),
  total_preguntas    INT NOT NULL DEFAULT 0 CHECK (total_preguntas >= 0),
  empezada_en        TIMESTAMPTZ,
  terminada_en       TIMESTAMPTZ,
  turno_actual       INT NOT NULL DEFAULT 1,
  creada_por         BIGINT REFERENCES usuarios(id_usuario) ON DELETE SET NULL,
  creada_en          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_partidas_max_jugadores CHECK (max_jugadores BETWEEN 2 AND 6),
  CONSTRAINT chk_partidas_codigo_union_len CHECK (codigo_union IS NULL OR char_length(codigo_union) BETWEEN 4 AND 12)
);

CREATE INDEX IF NOT EXISTS idx_partidas_estado ON partidas(estado);
CREATE INDEX IF NOT EXISTS idx_partidas_codigo_union ON partidas(codigo_union);

CREATE TABLE IF NOT EXISTS jugadores_partida (
  id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  partida_id     BIGINT NOT NULL REFERENCES partidas(id) ON DELETE CASCADE,
  jugador_id     BIGINT NOT NULL REFERENCES jugadores(id_usuario) ON DELETE CASCADE,
  orden_union    INT,
  orden_turno    INT,  -- orden de juego asignado al iniciar la partida (1 = primero)
  puntos         INT NOT NULL DEFAULT 0,
  num_acertadas  INT NOT NULL DEFAULT 0 CHECK (num_acertadas >= 0),
  num_falladas   INT NOT NULL DEFAULT 0 CHECK (num_falladas >= 0),
  tiempo_total   INT NOT NULL DEFAULT 0 CHECK (tiempo_total >= 0),
  resultado      resultado_jugador NOT NULL DEFAULT 'PENDIENTE',
  unido_en       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  --Evitar que mismo jugador se una dos veces a la misma partida
  CONSTRAINT uq_jugadores_partida UNIQUE (partida_id, jugador_id)
);

CREATE INDEX IF NOT EXISTS idx_jugadores_partida_partida ON jugadores_partida(partida_id);

-- =========================================================
-- Trigger: impedir más de 6 jugadores en una partida
-- (con control de concurrencia)
-- =========================================================
CREATE OR REPLACE FUNCTION impedir_mas_6_jugadores()
RETURNS TRIGGER AS $$
DECLARE
  v_max INT;
  v_count INT;
BEGIN
  -- Bloqueo para evitar carreras: dos inserts simultáneos
  SELECT max_jugadores INTO v_max
  FROM partidas
  WHERE id = NEW.partida_id
  FOR UPDATE;

  IF v_max IS NULL THEN
    RAISE EXCEPTION 'Partida % no existe.', NEW.partida_id;
  END IF;

  SELECT COUNT(*) INTO v_count
  FROM jugadores_partida
  WHERE partida_id = NEW.partida_id;

  IF v_count >= v_max THEN
    RAISE EXCEPTION 'La partida % ya está completa (máximo % jugadores).', NEW.partida_id, v_max;
  END IF;

  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_impedir_mas_6_jugadores ON jugadores_partida;
CREATE TRIGGER trg_impedir_mas_6_jugadores
BEFORE INSERT ON jugadores_partida
FOR EACH ROW
EXECUTE FUNCTION impedir_mas_6_jugadores();

-- también en UPDATE de partida_id
DROP TRIGGER IF EXISTS trg_impedir_mas_6_jugadores_upd ON jugadores_partida;
CREATE TRIGGER trg_impedir_mas_6_jugadores_upd
BEFORE UPDATE OF partida_id ON jugadores_partida
FOR EACH ROW
EXECUTE FUNCTION impedir_mas_6_jugadores();

-- =========================================================
-- Preguntas en partida 
-- =========================================================
CREATE TABLE IF NOT EXISTS preguntas_partida (
  id                 BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  partida_id         BIGINT NOT NULL REFERENCES partidas(id) ON DELETE CASCADE,
  pregunta_id        BIGINT NOT NULL REFERENCES preguntas(id) ON DELETE RESTRICT,
  orden_pregunta     INT NOT NULL CHECK (orden_pregunta > 0),
  CONSTRAINT uq_pregunta_partida_orden UNIQUE (partida_id, orden_pregunta),
  CONSTRAINT uq_partida_pregunta_no_repetida UNIQUE (partida_id, pregunta_id)
);

CREATE INDEX IF NOT EXISTS idx_preguntas_partida ON preguntas_partida(partida_id);

-- =========================================================
-- 7) Respuestas en partida (validar respuesta pertenece a la pregunta)
-- =========================================================
CREATE TABLE IF NOT EXISTS respuestas_jugador (
  id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  jugador_partida_id  BIGINT NOT NULL REFERENCES jugadores_partida(id) ON DELETE CASCADE,
  pregunta_partida_id BIGINT NOT NULL REFERENCES preguntas_partida(id) ON DELETE CASCADE,
  respuesta_id        BIGINT NOT NULL REFERENCES respuestas(id) ON DELETE RESTRICT,
  es_correcta         BOOLEAN NOT NULL DEFAULT FALSE,
  tiempo_ms           INT NOT NULL DEFAULT 0 CHECK (tiempo_ms >= 0),
  respondida_el       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_una_respuesta_por_jugador UNIQUE (jugador_partida_id, pregunta_partida_id)
);

CREATE INDEX IF NOT EXISTS idx_respuestas_jugador_jugador ON respuestas_jugador(jugador_partida_id);
CREATE INDEX IF NOT EXISTS idx_respuestas_jugador_pregunta ON respuestas_jugador(pregunta_partida_id);

CREATE OR REPLACE FUNCTION validar_respuesta_jugador()
RETURNS TRIGGER AS $$
DECLARE
  pid         BIGINT;
  rid_pid     BIGINT;
  es_correcta BOOLEAN;
BEGIN
  -- Obtener pregunta_id de preguntas_partida
  SELECT pp.pregunta_id INTO pid
  FROM preguntas_partida pp
  WHERE pp.id = NEW.pregunta_partida_id;

  -- Obtener a qué pregunta pertenece la respuesta elegida, y si es correcta
  SELECT r.pregunta_id, r.es_correcta INTO rid_pid, es_correcta
  FROM respuestas r
  WHERE r.id = NEW.respuesta_id;

  IF pid IS NULL OR rid_pid IS NULL THEN
    RAISE EXCEPTION 'pregunta_partida_id o respuesta_id no válido.';
  END IF;

  IF rid_pid <> pid THEN
    RAISE EXCEPTION
      'La respuesta % no pertenece a la pregunta % (pregunta_partida %).',
      NEW.respuesta_id, pid, NEW.pregunta_partida_id;
  END IF;

  NEW.es_correcta := es_correcta;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validar_respuesta_jugador ON respuestas_jugador;
CREATE TRIGGER trg_validar_respuesta_jugador
BEFORE INSERT OR UPDATE OF respuesta_id, pregunta_partida_id ON respuestas_jugador
FOR EACH ROW EXECUTE FUNCTION validar_respuesta_jugador();

-- =========================================================
-- 8) Quesitos ganados por jugador en una partida concreta
-- =========================================================
CREATE TABLE IF NOT EXISTS quesitos_ganados (
  jugador_partida_id    BIGINT NOT NULL REFERENCES jugadores_partida(id) ON DELETE CASCADE,
  categoria_id BIGINT NOT NULL REFERENCES categorias(id) ON DELETE RESTRICT,
  ganado_el         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  PRIMARY KEY (jugador_partida_id, categoria_id)
);

CREATE INDEX IF NOT EXISTS idx_quesitos_ganados_categoria ON quesitos_ganados(categoria_id);

-- =========================================================
-- 9) Progreso por categoría (aciertos hacia el quesito, 0-4)
--    Cuando llega a 5 se crea la fila en quesitos_ganados y se resetea.
-- =========================================================
CREATE TABLE IF NOT EXISTS progreso_categoria (
  id                 BIGSERIAL PRIMARY KEY,
  jugador_partida_id BIGINT NOT NULL REFERENCES jugadores_partida(id) ON DELETE CASCADE,
  categoria_id       BIGINT NOT NULL REFERENCES categorias(id),
  aciertos           INT NOT NULL DEFAULT 0 CHECK (aciertos BETWEEN 0 AND 5),
  CONSTRAINT uq_progreso UNIQUE (jugador_partida_id, categoria_id)
);

CREATE INDEX IF NOT EXISTS idx_progreso_jugador ON progreso_categoria(jugador_partida_id);

-- =========================================================
-- 10) Vistas
-- =========================================================

CREATE VIEW v_usuarios_profesores AS
SELECT 
	u.*,
	p.departamento
FROM Usuarios u
JOIN Profesores p ON u.id_usuario = p.id_usuario;

CREATE VIEW v_usuarios_jugadores AS
SELECT 
	u.*,
	j.nick
FROM Usuarios u
JOIN Jugadores j ON u.id_usuario = j.id_usuario;

CREATE VIEW v_codigos_2fa AS
SELECT 
    c.id,
    c.id_usuario,
    u.nombre,
    u.apellidos,
    c.codigo,
    c.expira_en,
    c.usado
FROM codigos_2fa c
JOIN usuarios u ON c.id_usuario = u.id_usuario;