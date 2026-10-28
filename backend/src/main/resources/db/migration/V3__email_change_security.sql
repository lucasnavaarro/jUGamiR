CREATE TABLE IF NOT EXISTS email_change_challenges (
    usuario_id bigint PRIMARY KEY REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
    new_email varchar(255) NOT NULL,
    code_hash varchar(64) NOT NULL,
    token_version integer NOT NULL,
    expires_at timestamp NOT NULL
);
