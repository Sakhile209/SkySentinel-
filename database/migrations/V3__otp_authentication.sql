ALTER TABLE users ADD COLUMN IF NOT EXISTS cellphone_number VARCHAR(50);

UPDATE users
SET cellphone_number = 'UNSET-' || id
WHERE cellphone_number IS NULL OR cellphone_number = '';

ALTER TABLE users ALTER COLUMN cellphone_number SET NOT NULL;
ALTER TABLE users ALTER COLUMN badge_number SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_cellphone_number ON users(cellphone_number);

CREATE TABLE IF NOT EXISTS otp_challenges (
    id BIGSERIAL PRIMARY KEY,
    challenge_id VARCHAR(64) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    attempts INTEGER NOT NULL DEFAULT 0,
    resend_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    last_sent_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_otp_challenges_user_id ON otp_challenges(user_id);
CREATE INDEX IF NOT EXISTS idx_otp_challenges_expires_at ON otp_challenges(expires_at);
