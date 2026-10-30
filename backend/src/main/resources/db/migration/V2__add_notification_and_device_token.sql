-- ============================================================
-- V2 — Notification & Device Token Tables
-- ============================================================

CREATE TABLE IF NOT EXISTS notification (
    id                  SERIAL       PRIMARY KEY,
    recipient_user_id   INTEGER      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    complaint_id        INTEGER      NOT NULL REFERENCES complaint(id) ON DELETE CASCADE,
    type                VARCHAR(30)  NOT NULL CHECK (type IN (
                            'STATUS_CHANGE','AWAITING_INFO','RESOLVED','ESCALATED','VENDOR_UPDATE'
                        )),
    title               VARCHAR(150) NOT NULL,
    body                TEXT         NOT NULL,
    status_at_send      VARCHAR(40),
    is_read             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient
    ON notification(recipient_user_id, is_read, created_at DESC);

-- ============================================================

CREATE TABLE IF NOT EXISTS device_token (
    id          SERIAL       PRIMARY KEY,
    user_id     INTEGER      NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    fcm_token   VARCHAR(300) NOT NULL,
    platform    VARCHAR(20)  NOT NULL DEFAULT 'ANDROID',
    updated_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_device_fcm_token UNIQUE (fcm_token)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_device_token_fcm
    ON device_token(fcm_token);
