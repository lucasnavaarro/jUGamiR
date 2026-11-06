-- Expelled participants retain history but do not occupy an active seat.
CREATE OR REPLACE FUNCTION public.impedir_mas_6_jugadores() RETURNS trigger
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
  WHERE partida_id = NEW.partida_id AND resultado = 'PENDIENTE';
  IF v_count >= v_max THEN
    RAISE EXCEPTION 'La partida % ya está completa (máximo % jugadores).', NEW.partida_id, v_max;
  END IF;
  RETURN NEW;
END;
$$;
