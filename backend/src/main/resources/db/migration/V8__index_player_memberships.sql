-- Apply the lookup index to existing installations as well as fresh seed schemas.
-- Deploy during the documented maintenance window: this is a regular index build.
CREATE INDEX IF NOT EXISTS idx_jugadores_partida_jugador ON public.jugadores_partida (jugador_id);
