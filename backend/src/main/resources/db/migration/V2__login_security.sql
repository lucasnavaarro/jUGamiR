ALTER TABLE codigos_2fa ADD COLUMN IF NOT EXISTS challenge_id varchar(36);
ALTER TABLE codigos_2fa ADD COLUMN IF NOT EXISTS attempts integer NOT NULL DEFAULT 0;
ALTER TABLE codigos_2fa ADD COLUMN IF NOT EXISTS resends integer NOT NULL DEFAULT 0;
ALTER TABLE codigos_2fa ADD COLUMN IF NOT EXISTS last_sent timestamp;
-- Challenges issued before this migration must not be usable without a first factor.
DELETE FROM codigos_2fa WHERE challenge_id IS NULL;
ALTER TABLE codigos_2fa ALTER COLUMN challenge_id SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_login_challenge ON codigos_2fa(challenge_id);
CREATE TABLE IF NOT EXISTS security_rate_limits (
    bucket_key varchar(64) PRIMARY KEY,
    hits integer NOT NULL,
    resets_at timestamp NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_rate_limits_expiry ON security_rate_limits(resets_at);
