-- Moderación: los usuarios denuncian ofertas y un administrador las retira o desestima la denuncia.

-- Una oferta retirada por moderación no es visible y la empresa no puede volver a abrirla
ALTER TABLE ofertas DROP CONSTRAINT ck_ofertas_estado;
ALTER TABLE ofertas ADD CONSTRAINT ck_ofertas_estado CHECK (estado IN ('ABIERTA', 'CERRADA', 'RETIRADA'));

-- Una cuenta suspendida no puede iniciar sesión
ALTER TABLE usuarios ADD COLUMN suspendido_en TIMESTAMP WITH TIME ZONE;

CREATE TABLE denuncias (
    id               UUID PRIMARY KEY,
    oferta_id        UUID                     NOT NULL REFERENCES ofertas (id) ON DELETE CASCADE,
    -- Si quien denunció borra su cuenta, la denuncia se conserva sin saber de quién era
    denunciante_id   UUID REFERENCES usuarios (id) ON DELETE SET NULL,
    motivo           VARCHAR(30)              NOT NULL,
    detalle          TEXT,
    estado           VARCHAR(20)              NOT NULL,
    fecha_creacion   TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_resolucion TIMESTAMP WITH TIME ZONE,
    -- Cada persona puede denunciar una oferta una sola vez
    CONSTRAINT uk_denuncias_oferta_denunciante UNIQUE (oferta_id, denunciante_id),
    CONSTRAINT ck_denuncias_motivo CHECK (motivo IN ('FRAUDE', 'DISCRIMINACION', 'ENGANOSA', 'OTRO')),
    CONSTRAINT ck_denuncias_estado CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'DESESTIMADA'))
);

-- El panel de moderación lista las pendientes por orden de llegada
CREATE INDEX idx_denuncias_estado_fecha ON denuncias (estado, fecha_creacion);
