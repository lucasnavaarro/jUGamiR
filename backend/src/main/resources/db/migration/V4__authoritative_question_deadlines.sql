ALTER TABLE preguntas_partida ADD COLUMN IF NOT EXISTS emitida_en timestamptz;
ALTER TABLE preguntas_partida ADD COLUMN IF NOT EXISTS vence_en timestamptz;
ALTER TABLE preguntas_partida ADD COLUMN IF NOT EXISTS jugador_turno_id bigint REFERENCES jugadores(id_usuario);
-- Historical questions have no trusted deadline and are treated as closed.
ALTER TABLE preguntas_partida ADD COLUMN IF NOT EXISTS cerrada boolean NOT NULL DEFAULT true;
CREATE INDEX IF NOT EXISTS idx_question_deadline ON preguntas_partida(vence_en) WHERE cerrada = false;
