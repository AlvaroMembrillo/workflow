CREATE TABLE candidaturas (
    id                  UUID PRIMARY KEY,
    oferta_id           UUID                     NOT NULL REFERENCES ofertas (id),
    candidato_id        UUID                     NOT NULL REFERENCES usuarios (id),
    carta_presentacion  TEXT,
    estado              VARCHAR(20)              NOT NULL,
    fecha_creacion      TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Un candidato solo puede inscribirse una vez en cada oferta. El índice también sirve para buscar por oferta
    CONSTRAINT uk_candidaturas_oferta_candidato UNIQUE (oferta_id, candidato_id),
    CONSTRAINT ck_candidaturas_estado CHECK (estado IN ('PENDIENTE', 'EN_REVISION', 'ACEPTADA', 'RECHAZADA'))
);

CREATE INDEX idx_candidaturas_candidato ON candidaturas (candidato_id);
