-- Match the supported 1..30 correct answers per wedge, preserving existing progress.
ALTER TABLE progreso_categoria DROP CONSTRAINT progreso_categoria_aciertos_check;
ALTER TABLE progreso_categoria ADD CONSTRAINT progreso_categoria_aciertos_check
    CHECK (aciertos >= 0 AND aciertos <= 30);
