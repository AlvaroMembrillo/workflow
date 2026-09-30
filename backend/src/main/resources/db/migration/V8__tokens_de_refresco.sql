-- Sesiones largas: cada inicio de sesión abre una "familia" de tokens de refresco. Al renovar, el token
-- usado se marca y se entrega otro de la misma familia. Solo se guarda el hash de cada token.
CREATE TABLE tokens_de_refresco (
    id               UUID PRIMARY KEY,
    usuario_id       UUID                     NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    familia          UUID                     NOT NULL,
    hash             VARCHAR(64)              NOT NULL,
    fecha_creacion   TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_caducidad  TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Cuándo se canjeó por el siguiente token de la familia
    fecha_uso        TIMESTAMP WITH TIME ZONE,
    -- Cierre de sesión, cambio de contraseña o reutilización sospechosa
    fecha_revocacion TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_tokens_de_refresco_hash UNIQUE (hash)
);

CREATE INDEX idx_tokens_de_refresco_usuario ON tokens_de_refresco (usuario_id);
CREATE INDEX idx_tokens_de_refresco_familia ON tokens_de_refresco (familia);
CREATE INDEX idx_tokens_de_refresco_caducidad ON tokens_de_refresco (fecha_caducidad);
