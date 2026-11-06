-- Keep existing spellings and IDs. Stop rather than merge ambiguous accounts.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM usuarios GROUP BY lower(btrim(email)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Emails duplicados tras normalización: resolver las cuentas antes de desplegar V7';
    END IF;
    IF EXISTS (SELECT 1 FROM jugadores GROUP BY lower(btrim(nick)) HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Nicks duplicados tras normalización: resolver las cuentas antes de desplegar V7';
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS usuarios_email_normalized_key ON usuarios (lower(btrim(email)));
CREATE UNIQUE INDEX IF NOT EXISTS jugadores_nick_normalized_key ON jugadores (lower(btrim(nick)));
