--
-- PostgreSQL database dump
--

-- Dumped from database version 16.14 (Debian 16.14-1.pgdg13+1)
-- Dumped by pg_dump version 16.14 (Debian 16.14-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: estado_partida; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estado_partida AS ENUM (
    'ESPERANDO',
    'EN_CURSO',
    'TERMINADA',
    'CANCELADA'
);


--
-- Name: estado_pregunta; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.estado_pregunta AS ENUM (
    'BORRADOR',
    'PUBLICADA',
    'ARCHIVADA'
);


--
-- Name: nivel_pregunta; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.nivel_pregunta AS ENUM (
    'FACIL',
    'MEDIO',
    'DIFICIL'
);


--
-- Name: resultado_jugador; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.resultado_jugador AS ENUM (
    'VICTORIA',
    'DERROTA',
    'EMPATE',
    'PENDIENTE',
    'ABANDONADA',
    'EXPULSADO'
);


--
-- Name: tipo_partida; Type: TYPE; Schema: public; Owner: -
--

CREATE TYPE public.tipo_partida AS ENUM (
    'PUBLICA',
    'PRIVADA'
);


--
-- Name: asegurar_pregunta_unica_correcta_publicada(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.asegurar_pregunta_unica_correcta_publicada() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: asegurar_pregunta_unica_correcta_ya_publicada(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.asegurar_pregunta_unica_correcta_ya_publicada() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


--
-- Name: impedir_mas_6_jugadores(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.impedir_mas_6_jugadores() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
  v_max INT;
  v_count INT;
BEGIN
  -- En UPDATE, solo comprobar si partida_id realmente cambia
  IF TG_OP = 'UPDATE' AND NEW.partida_id = OLD.partida_id THEN
    RETURN NEW;
  END IF;
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
$$;


--
-- Name: set_preguntas_actualizada_el(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.set_preguntas_actualizada_el() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
  NEW.actualizada_el := NOW();
  RETURN NEW;
END;
$$;


--
-- Name: validar_respuesta_jugador(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE FUNCTION public.validar_respuesta_jugador() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
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
$$;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: administradores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.administradores (
    id_usuario bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: asignaturas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.asignaturas (
    id bigint NOT NULL,
    nombre character varying(255) NOT NULL,
    categoria_id bigint NOT NULL
);


--
-- Name: asignaturas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.asignaturas ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.asignaturas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: categorias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categorias (
    id bigint NOT NULL,
    nombre character varying(120) NOT NULL,
    color character varying(30) NOT NULL,
    descripcion text
);


--
-- Name: categorias_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.categorias ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.categorias_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: codigos_2fa; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.codigos_2fa (
    id bigint NOT NULL,
    id_usuario bigint NOT NULL,
    codigo character varying(6) NOT NULL,
    expira_en timestamp with time zone DEFAULT (now() + '00:10:00'::interval) NOT NULL,
    usado boolean DEFAULT false NOT NULL
);


--
-- Name: codigos_2fa_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.codigos_2fa ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.codigos_2fa_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: jugadores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jugadores (
    id_usuario bigint NOT NULL,
    nick character varying(80) NOT NULL
);


--
-- Name: jugadores_partida; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.jugadores_partida (
    id bigint NOT NULL,
    partida_id bigint NOT NULL,
    jugador_id bigint NOT NULL,
    orden_union integer,
    puntos integer DEFAULT 0 NOT NULL,
    num_acertadas integer DEFAULT 0 NOT NULL,
    num_falladas integer DEFAULT 0 NOT NULL,
    tiempo_total integer DEFAULT 0 NOT NULL,
    resultado public.resultado_jugador DEFAULT 'PENDIENTE'::public.resultado_jugador NOT NULL,
    unido_en timestamp with time zone DEFAULT now() NOT NULL,
    orden_turno integer,
    num_no_respondidas integer DEFAULT 0 NOT NULL,
    CONSTRAINT jugadores_partida_num_acertadas_check CHECK ((num_acertadas >= 0)),
    CONSTRAINT jugadores_partida_num_falladas_check CHECK ((num_falladas >= 0)),
    CONSTRAINT jugadores_partida_num_no_respondidas_check CHECK ((num_no_respondidas >= 0)),
    CONSTRAINT jugadores_partida_tiempo_total_check CHECK ((tiempo_total >= 0))
);


--
-- Name: jugadores_partida_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.jugadores_partida ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.jugadores_partida_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: partida_categoria_pesos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.partida_categoria_pesos (
    id bigint NOT NULL,
    partida_id bigint NOT NULL,
    categoria_id bigint NOT NULL,
    peso integer NOT NULL
);


--
-- Name: partida_categoria_pesos_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.partida_categoria_pesos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: partida_categoria_pesos_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.partida_categoria_pesos_id_seq OWNED BY public.partida_categoria_pesos.id;


--
-- Name: partida_dificultades; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.partida_dificultades (
    partida_id bigint NOT NULL,
    dificultad character varying(10) NOT NULL
);


--
-- Name: partidas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.partidas (
    id bigint NOT NULL,
    codigo_union character varying(12),
    max_jugadores integer DEFAULT 6 NOT NULL,
    estado public.estado_partida DEFAULT 'ESPERANDO'::public.estado_partida NOT NULL,
    duracion integer,
    empezada_en timestamp with time zone,
    terminada_en timestamp with time zone,
    creada_por bigint,
    creada_en timestamp with time zone DEFAULT now() NOT NULL,
    tipo public.tipo_partida DEFAULT 'PUBLICA'::public.tipo_partida NOT NULL,
    tiempo_respuesta integer DEFAULT 30 NOT NULL,
    turno_actual integer DEFAULT 1 NOT NULL,
    aciertos_para_quesito integer DEFAULT 5 NOT NULL,
    modo_entrenamiento boolean DEFAULT false NOT NULL,
    CONSTRAINT chk_partidas_codigo_union_len CHECK (((codigo_union IS NULL) OR ((char_length((codigo_union)::text) >= 4) AND (char_length((codigo_union)::text) <= 12)))),
    CONSTRAINT chk_partidas_max_jugadores CHECK (((max_jugadores >= 1) AND (max_jugadores <= 6))),
    CONSTRAINT partidas_duracion_check CHECK (((duracion IS NULL) OR (duracion > 0)))
);


--
-- Name: partidas_categorias; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.partidas_categorias (
    partida_id bigint NOT NULL,
    categoria_id bigint NOT NULL
);


--
-- Name: partidas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.partidas ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.partidas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: password_reset_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.password_reset_tokens (
    id bigint NOT NULL,
    token character varying(255) NOT NULL,
    id_usuario bigint NOT NULL,
    expira_en timestamp without time zone NOT NULL,
    usado boolean DEFAULT false NOT NULL
);


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.password_reset_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: password_reset_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.password_reset_tokens_id_seq OWNED BY public.password_reset_tokens.id;


--
-- Name: preguntas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.preguntas (
    id bigint NOT NULL,
    identificador character varying(60),
    titulo_indice text,
    enunciado text NOT NULL,
    imagen_url text,
    anio integer,
    comentario text,
    anulada boolean DEFAULT false NOT NULL,
    dificultad public.nivel_pregunta DEFAULT 'MEDIO'::public.nivel_pregunta NOT NULL,
    estado public.estado_pregunta DEFAULT 'BORRADOR'::public.estado_pregunta NOT NULL,
    asignatura_id bigint NOT NULL,
    creada_por bigint NOT NULL,
    creada_el timestamp with time zone DEFAULT now() NOT NULL,
    actualizada_el timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT chk_preguntas_imagen_url CHECK (((imagen_url IS NULL) OR (length(imagen_url) <= 2048)))
);


--
-- Name: preguntas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.preguntas ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.preguntas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: preguntas_partida; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.preguntas_partida (
    id bigint NOT NULL,
    partida_id bigint NOT NULL,
    pregunta_id bigint NOT NULL,
    orden_pregunta integer NOT NULL,
    CONSTRAINT preguntas_partida_orden_pregunta_check CHECK ((orden_pregunta > 0))
);


--
-- Name: preguntas_partida_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.preguntas_partida ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.preguntas_partida_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: profesores; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.profesores (
    id_usuario bigint NOT NULL,
    departamento character varying(120)
);


--
-- Name: progreso_categoria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.progreso_categoria (
    id bigint NOT NULL,
    jugador_partida_id bigint NOT NULL,
    categoria_id bigint NOT NULL,
    aciertos integer DEFAULT 0 NOT NULL,
    CONSTRAINT progreso_categoria_aciertos_check CHECK (((aciertos >= 0) AND (aciertos <= 5)))
);


--
-- Name: progreso_categoria_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.progreso_categoria_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: progreso_categoria_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.progreso_categoria_id_seq OWNED BY public.progreso_categoria.id;


--
-- Name: quesitos_ganados; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.quesitos_ganados (
    jugador_partida_id bigint NOT NULL,
    categoria_id bigint NOT NULL,
    ganado_el timestamp with time zone DEFAULT now() NOT NULL
);


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    token character varying(255) NOT NULL,
    usuario_id bigint NOT NULL,
    expira_en timestamp without time zone NOT NULL
);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;


--
-- Name: respuestas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.respuestas (
    id bigint NOT NULL,
    pregunta_id bigint NOT NULL,
    texto_respuesta text NOT NULL,
    es_correcta boolean DEFAULT false NOT NULL,
    orden integer DEFAULT 0 NOT NULL
);


--
-- Name: respuestas_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.respuestas ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.respuestas_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: respuestas_jugador; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.respuestas_jugador (
    id bigint NOT NULL,
    jugador_partida_id bigint NOT NULL,
    pregunta_partida_id bigint NOT NULL,
    respuesta_id bigint NOT NULL,
    es_correcta boolean DEFAULT false NOT NULL,
    tiempo_ms integer DEFAULT 0 NOT NULL,
    respondida_el timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT respuestas_jugador_tiempo_ms_check CHECK ((tiempo_ms >= 0))
);


--
-- Name: respuestas_jugador_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.respuestas_jugador ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.respuestas_jugador_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: usuarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.usuarios (
    id_usuario bigint NOT NULL,
    nombre character varying(120) NOT NULL,
    apellidos character varying(120) NOT NULL,
    email character varying(255) NOT NULL,
    "contraseña_hash" text NOT NULL,
    es_activo boolean DEFAULT true NOT NULL,
    creado_en timestamp with time zone DEFAULT now() NOT NULL,
    ultimo_login timestamp with time zone,
    token_version integer DEFAULT 1 NOT NULL
);


--
-- Name: usuarios_id_usuario_seq; Type: SEQUENCE; Schema: public; Owner: -
--

ALTER TABLE public.usuarios ALTER COLUMN id_usuario ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.usuarios_id_usuario_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- Name: v_codigos_2fa; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_codigos_2fa AS
 SELECT c.id,
    c.id_usuario,
    u.nombre,
    u.apellidos,
    c.codigo,
    c.expira_en,
    c.usado
   FROM (public.codigos_2fa c
     JOIN public.usuarios u ON ((c.id_usuario = u.id_usuario)));


--
-- Name: v_usuarios_jugadores; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_usuarios_jugadores AS
 SELECT u.id_usuario,
    u.nombre,
    u.apellidos,
    u.email,
    u."contraseña_hash",
    u.es_activo,
    u.creado_en,
    u.ultimo_login,
    u.token_version,
    j.nick
   FROM (public.usuarios u
     JOIN public.jugadores j ON ((u.id_usuario = j.id_usuario)));


--
-- Name: v_usuarios_profesores; Type: VIEW; Schema: public; Owner: -
--

CREATE VIEW public.v_usuarios_profesores AS
 SELECT u.id_usuario,
    u.nombre,
    u.apellidos,
    u.email,
    u."contraseña_hash",
    u.es_activo,
    u.creado_en,
    u.ultimo_login,
    u.token_version,
    p.departamento
   FROM (public.usuarios u
     JOIN public.profesores p ON ((u.id_usuario = p.id_usuario)));


--
-- Name: partida_categoria_pesos id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partida_categoria_pesos ALTER COLUMN id SET DEFAULT nextval('public.partida_categoria_pesos_id_seq'::regclass);


--
-- Name: password_reset_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens ALTER COLUMN id SET DEFAULT nextval('public.password_reset_tokens_id_seq'::regclass);


--
-- Name: progreso_categoria id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progreso_categoria ALTER COLUMN id SET DEFAULT nextval('public.progreso_categoria_id_seq'::regclass);


--
-- Name: refresh_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);


--
-- Name: administradores administradores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administradores
    ADD CONSTRAINT administradores_pkey PRIMARY KEY (id_usuario);


--
-- Name: asignaturas asignaturas_nombre_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaturas
    ADD CONSTRAINT asignaturas_nombre_key UNIQUE (nombre);


--
-- Name: asignaturas asignaturas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaturas
    ADD CONSTRAINT asignaturas_pkey PRIMARY KEY (id);


--
-- Name: categorias categorias_nombre_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorias
    ADD CONSTRAINT categorias_nombre_key UNIQUE (nombre);


--
-- Name: categorias categorias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorias
    ADD CONSTRAINT categorias_pkey PRIMARY KEY (id);


--
-- Name: codigos_2fa codigos_2fa_id_usuario_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.codigos_2fa
    ADD CONSTRAINT codigos_2fa_id_usuario_key UNIQUE (id_usuario);


--
-- Name: codigos_2fa codigos_2fa_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.codigos_2fa
    ADD CONSTRAINT codigos_2fa_pkey PRIMARY KEY (id);


--
-- Name: jugadores jugadores_nick_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jugadores
    ADD CONSTRAINT jugadores_nick_key UNIQUE (nick);


--
-- Name: jugadores_partida jugadores_partida_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jugadores_partida
    ADD CONSTRAINT jugadores_partida_pkey PRIMARY KEY (id);


--
-- Name: jugadores jugadores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jugadores
    ADD CONSTRAINT jugadores_pkey PRIMARY KEY (id_usuario);


--
-- Name: partida_categoria_pesos partida_categoria_pesos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partida_categoria_pesos
    ADD CONSTRAINT partida_categoria_pesos_pkey PRIMARY KEY (id);


--
-- Name: partidas_categorias partidas_categorias_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partidas_categorias
    ADD CONSTRAINT partidas_categorias_pkey PRIMARY KEY (partida_id, categoria_id);


--
-- Name: partidas partidas_codigo_union_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partidas
    ADD CONSTRAINT partidas_codigo_union_key UNIQUE (codigo_union);


--
-- Name: partidas partidas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partidas
    ADD CONSTRAINT partidas_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_pkey PRIMARY KEY (id);


--
-- Name: password_reset_tokens password_reset_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_token_key UNIQUE (token);


--
-- Name: preguntas preguntas_identificador_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preguntas
    ADD CONSTRAINT preguntas_identificador_key UNIQUE (identificador);


--
-- Name: preguntas_partida preguntas_partida_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preguntas_partida
    ADD CONSTRAINT preguntas_partida_pkey PRIMARY KEY (id);


--
-- Name: preguntas preguntas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preguntas
    ADD CONSTRAINT preguntas_pkey PRIMARY KEY (id);


--
-- Name: profesores profesores_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profesores
    ADD CONSTRAINT profesores_pkey PRIMARY KEY (id_usuario);


--
-- Name: progreso_categoria progreso_categoria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progreso_categoria
    ADD CONSTRAINT progreso_categoria_pkey PRIMARY KEY (id);


--
-- Name: quesitos_ganados quesitos_ganados_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quesitos_ganados
    ADD CONSTRAINT quesitos_ganados_pkey PRIMARY KEY (jugador_partida_id, categoria_id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);


--
-- Name: respuestas_jugador respuestas_jugador_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.respuestas_jugador
    ADD CONSTRAINT respuestas_jugador_pkey PRIMARY KEY (id);


--
-- Name: respuestas respuestas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.respuestas
    ADD CONSTRAINT respuestas_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens uk_usuario; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT uk_usuario UNIQUE (usuario_id);


--
-- Name: categorias uq_categorias_color; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorias
    ADD CONSTRAINT uq_categorias_color UNIQUE (color);


--
-- Name: jugadores_partida uq_jugadores_partida; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jugadores_partida
    ADD CONSTRAINT uq_jugadores_partida UNIQUE (partida_id, jugador_id);


--
-- Name: preguntas_partida uq_partida_pregunta_no_repetida; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preguntas_partida
    ADD CONSTRAINT uq_partida_pregunta_no_repetida UNIQUE (partida_id, pregunta_id);


--
-- Name: preguntas_partida uq_pregunta_partida_orden; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preguntas_partida
    ADD CONSTRAINT uq_pregunta_partida_orden UNIQUE (partida_id, orden_pregunta);


--
-- Name: progreso_categoria uq_progreso; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progreso_categoria
    ADD CONSTRAINT uq_progreso UNIQUE (jugador_partida_id, categoria_id);


--
-- Name: respuestas uq_respuesta_orden; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.respuestas
    ADD CONSTRAINT uq_respuesta_orden UNIQUE (pregunta_id, orden);


--
-- Name: respuestas uq_una_correcta_por_pregunta; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.respuestas
    ADD CONSTRAINT uq_una_correcta_por_pregunta EXCLUDE USING btree (pregunta_id WITH =) WHERE ((es_correcta = true)) DEFERRABLE INITIALLY DEFERRED;


--
-- Name: respuestas_jugador uq_una_respuesta_por_jugador; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.respuestas_jugador
    ADD CONSTRAINT uq_una_respuesta_por_jugador UNIQUE (jugador_partida_id, pregunta_partida_id);


--
-- Name: usuarios usuarios_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_email_key UNIQUE (email);


--
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id_usuario);


--
-- Name: idx_jugadores_partida_partida; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_jugadores_partida_partida ON public.jugadores_partida USING btree (partida_id);


--
-- Name: idx_partidas_codigo_union; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partidas_codigo_union ON public.partidas USING btree (codigo_union);


--
-- Name: idx_partidas_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_partidas_estado ON public.partidas USING btree (estado);


--
-- Name: idx_preguntas_asignatura; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_preguntas_asignatura ON public.preguntas USING btree (asignatura_id);


--
-- Name: idx_preguntas_estado; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_preguntas_estado ON public.preguntas USING btree (estado);


--
-- Name: idx_preguntas_partida; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_preguntas_partida ON public.preguntas_partida USING btree (partida_id);


--
-- Name: idx_quesitos_ganados_categoria; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_quesitos_ganados_categoria ON public.quesitos_ganados USING btree (categoria_id);


--
-- Name: idx_respuestas_jugador_jugador; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_respuestas_jugador_jugador ON public.respuestas_jugador USING btree (jugador_partida_id);


--
-- Name: idx_respuestas_jugador_pregunta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_respuestas_jugador_pregunta ON public.respuestas_jugador USING btree (pregunta_partida_id);


--
-- Name: idx_respuestas_pregunta; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_respuestas_pregunta ON public.respuestas USING btree (pregunta_id);


--
-- Name: preguntas trg_asegurar_pregunta_unica_correcta_publicada; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_asegurar_pregunta_unica_correcta_publicada BEFORE UPDATE OF estado ON public.preguntas FOR EACH ROW EXECUTE FUNCTION public.asegurar_pregunta_unica_correcta_publicada();


--
-- Name: respuestas trg_comprobar_respuesta_pregunta_del; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_comprobar_respuesta_pregunta_del AFTER DELETE ON public.respuestas FOR EACH ROW EXECUTE FUNCTION public.asegurar_pregunta_unica_correcta_ya_publicada();


--
-- Name: respuestas trg_comprobar_respuesta_pregunta_ins; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_comprobar_respuesta_pregunta_ins AFTER INSERT ON public.respuestas FOR EACH ROW EXECUTE FUNCTION public.asegurar_pregunta_unica_correcta_ya_publicada();


--
-- Name: respuestas trg_comprobar_respuesta_pregunta_upd; Type: TRIGGER; Schema: public; Owner: -
--

CREATE CONSTRAINT TRIGGER trg_comprobar_respuesta_pregunta_upd AFTER UPDATE OF es_correcta ON public.respuestas DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION public.asegurar_pregunta_unica_correcta_ya_publicada();


--
-- Name: jugadores_partida trg_impedir_mas_6_jugadores; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_impedir_mas_6_jugadores BEFORE INSERT ON public.jugadores_partida FOR EACH ROW EXECUTE FUNCTION public.impedir_mas_6_jugadores();


--
-- Name: jugadores_partida trg_impedir_mas_6_jugadores_upd; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_impedir_mas_6_jugadores_upd BEFORE UPDATE OF partida_id ON public.jugadores_partida FOR EACH ROW EXECUTE FUNCTION public.impedir_mas_6_jugadores();


--
-- Name: preguntas trg_preguntas_actualizada_el; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_preguntas_actualizada_el BEFORE UPDATE ON public.preguntas FOR EACH ROW EXECUTE FUNCTION public.set_preguntas_actualizada_el();


--
-- Name: respuestas_jugador trg_validar_respuesta_jugador; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trg_validar_respuesta_jugador BEFORE INSERT OR UPDATE OF respuesta_id, pregunta_partida_id ON public.respuestas_jugador FOR EACH ROW EXECUTE FUNCTION public.validar_respuesta_jugador();


--
-- Name: administradores administradores_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administradores
    ADD CONSTRAINT administradores_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: asignaturas asignaturas_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.asignaturas
    ADD CONSTRAINT asignaturas_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categorias(id) ON DELETE RESTRICT;


--
-- Name: codigos_2fa codigos_2fa_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.codigos_2fa
    ADD CONSTRAINT codigos_2fa_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: jugadores jugadores_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jugadores
    ADD CONSTRAINT jugadores_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: jugadores_partida jugadores_partida_jugador_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jugadores_partida
    ADD CONSTRAINT jugadores_partida_jugador_id_fkey FOREIGN KEY (jugador_id) REFERENCES public.jugadores(id_usuario) ON DELETE CASCADE;


--
-- Name: jugadores_partida jugadores_partida_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.jugadores_partida
    ADD CONSTRAINT jugadores_partida_partida_id_fkey FOREIGN KEY (partida_id) REFERENCES public.partidas(id) ON DELETE CASCADE;


--
-- Name: partida_categoria_pesos partida_categoria_pesos_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partida_categoria_pesos
    ADD CONSTRAINT partida_categoria_pesos_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categorias(id);


--
-- Name: partida_categoria_pesos partida_categoria_pesos_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partida_categoria_pesos
    ADD CONSTRAINT partida_categoria_pesos_partida_id_fkey FOREIGN KEY (partida_id) REFERENCES public.partidas(id) ON DELETE CASCADE;


--
-- Name: partida_dificultades partida_dificultades_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partida_dificultades
    ADD CONSTRAINT partida_dificultades_partida_id_fkey FOREIGN KEY (partida_id) REFERENCES public.partidas(id) ON DELETE CASCADE;


--
-- Name: partidas_categorias partidas_categorias_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partidas_categorias
    ADD CONSTRAINT partidas_categorias_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categorias(id) ON DELETE RESTRICT;


--
-- Name: partidas_categorias partidas_categorias_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partidas_categorias
    ADD CONSTRAINT partidas_categorias_partida_id_fkey FOREIGN KEY (partida_id) REFERENCES public.partidas(id) ON DELETE CASCADE;


--
-- Name: partidas partidas_creada_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.partidas
    ADD CONSTRAINT partidas_creada_por_fkey FOREIGN KEY (creada_por) REFERENCES public.usuarios(id_usuario) ON DELETE SET NULL;


--
-- Name: password_reset_tokens password_reset_tokens_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.password_reset_tokens
    ADD CONSTRAINT password_reset_tokens_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: preguntas preguntas_asignatura_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preguntas
    ADD CONSTRAINT preguntas_asignatura_id_fkey FOREIGN KEY (asignatura_id) REFERENCES public.asignaturas(id) ON DELETE RESTRICT;


--
-- Name: preguntas preguntas_creada_por_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preguntas
    ADD CONSTRAINT preguntas_creada_por_fkey FOREIGN KEY (creada_por) REFERENCES public.usuarios(id_usuario) ON DELETE RESTRICT;


--
-- Name: preguntas_partida preguntas_partida_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preguntas_partida
    ADD CONSTRAINT preguntas_partida_partida_id_fkey FOREIGN KEY (partida_id) REFERENCES public.partidas(id) ON DELETE CASCADE;


--
-- Name: preguntas_partida preguntas_partida_pregunta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.preguntas_partida
    ADD CONSTRAINT preguntas_partida_pregunta_id_fkey FOREIGN KEY (pregunta_id) REFERENCES public.preguntas(id) ON DELETE RESTRICT;


--
-- Name: profesores profesores_id_usuario_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profesores
    ADD CONSTRAINT profesores_id_usuario_fkey FOREIGN KEY (id_usuario) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: progreso_categoria progreso_categoria_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progreso_categoria
    ADD CONSTRAINT progreso_categoria_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categorias(id);


--
-- Name: progreso_categoria progreso_categoria_jugador_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.progreso_categoria
    ADD CONSTRAINT progreso_categoria_jugador_partida_id_fkey FOREIGN KEY (jugador_partida_id) REFERENCES public.jugadores_partida(id) ON DELETE CASCADE;


--
-- Name: quesitos_ganados quesitos_ganados_categoria_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quesitos_ganados
    ADD CONSTRAINT quesitos_ganados_categoria_id_fkey FOREIGN KEY (categoria_id) REFERENCES public.categorias(id) ON DELETE RESTRICT;


--
-- Name: quesitos_ganados quesitos_ganados_jugador_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.quesitos_ganados
    ADD CONSTRAINT quesitos_ganados_jugador_partida_id_fkey FOREIGN KEY (jugador_partida_id) REFERENCES public.jugadores_partida(id) ON DELETE CASCADE;


--
-- Name: refresh_tokens refresh_tokens_usuario_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_usuario_id_fkey FOREIGN KEY (usuario_id) REFERENCES public.usuarios(id_usuario) ON DELETE CASCADE;


--
-- Name: respuestas_jugador respuestas_jugador_jugador_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.respuestas_jugador
    ADD CONSTRAINT respuestas_jugador_jugador_partida_id_fkey FOREIGN KEY (jugador_partida_id) REFERENCES public.jugadores_partida(id) ON DELETE CASCADE;


--
-- Name: respuestas_jugador respuestas_jugador_pregunta_partida_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.respuestas_jugador
    ADD CONSTRAINT respuestas_jugador_pregunta_partida_id_fkey FOREIGN KEY (pregunta_partida_id) REFERENCES public.preguntas_partida(id) ON DELETE CASCADE;


--
-- Name: respuestas_jugador respuestas_jugador_respuesta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.respuestas_jugador
    ADD CONSTRAINT respuestas_jugador_respuesta_id_fkey FOREIGN KEY (respuesta_id) REFERENCES public.respuestas(id) ON DELETE RESTRICT;


--
-- Name: respuestas respuestas_pregunta_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.respuestas
    ADD CONSTRAINT respuestas_pregunta_id_fkey FOREIGN KEY (pregunta_id) REFERENCES public.preguntas(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

