-- Verificación del email. Las cuentas anteriores a esta migración no pudieron verificarlo:
-- se dan por verificadas con su fecha de alta para no dejarlas a medias.
ALTER TABLE usuarios ADD COLUMN email_verificado_en TIMESTAMP WITH TIME ZONE;
UPDATE usuarios SET email_verificado_en = fecha_creacion;

-- Avisos por correo (nueva candidatura, cambio de estado). El usuario puede desactivarlos
ALTER TABLE usuarios ADD COLUMN avisos_por_correo BOOLEAN NOT NULL DEFAULT TRUE;

-- Enlaces de un solo uso que se envían por correo. Solo se guarda el hash del token:
-- quien lea la base de datos no puede usar los enlaces pendientes.
CREATE TABLE tokens_de_un_uso (
    id              UUID PRIMARY KEY,
    usuario_id      UUID                     NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    tipo            VARCHAR(30)              NOT NULL,
    hash            VARCHAR(64)              NOT NULL,
    fecha_creacion  TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_caducidad TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_uso       TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_tokens_de_un_uso_hash UNIQUE (hash),
    CONSTRAINT ck_tokens_de_un_uso_tipo CHECK (tipo IN ('VERIFICAR_EMAIL', 'RESTABLECER_PASSWORD'))
);

CREATE INDEX idx_tokens_de_un_uso_usuario ON tokens_de_un_uso (usuario_id, tipo);
-- La limpieza diaria borra por fecha de caducidad
CREATE INDEX idx_tokens_de_un_uso_caducidad ON tokens_de_un_uso (fecha_caducidad);
