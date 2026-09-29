CREATE TABLE empresas (
    id             UUID PRIMARY KEY,
    usuario_id     UUID                     NOT NULL REFERENCES usuarios (id),
    nombre         VARCHAR(150)             NOT NULL,
    descripcion    TEXT,
    sitio_web      VARCHAR(255),
    ubicacion      VARCHAR(150),
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_empresas_usuario UNIQUE (usuario_id)
);

CREATE TABLE ofertas (
    id                  UUID PRIMARY KEY,
    empresa_id          UUID                     NOT NULL REFERENCES empresas (id),
    titulo              VARCHAR(150)             NOT NULL,
    descripcion         TEXT                     NOT NULL,
    ubicacion           VARCHAR(150)             NOT NULL,
    modalidad           VARCHAR(20)              NOT NULL,
    tipo_contrato       VARCHAR(20)              NOT NULL,
    salario_minimo      INTEGER,
    salario_maximo      INTEGER,
    estado              VARCHAR(20)              NOT NULL,
    fecha_creacion      TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_actualizacion TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_ofertas_modalidad CHECK (modalidad IN ('PRESENCIAL', 'REMOTO', 'HIBRIDO')),
    CONSTRAINT ck_ofertas_tipo_contrato CHECK (tipo_contrato IN ('INDEFINIDO', 'TEMPORAL', 'PRACTICAS', 'FREELANCE')),
    CONSTRAINT ck_ofertas_estado CHECK (estado IN ('ABIERTA', 'CERRADA')),
    CONSTRAINT ck_ofertas_salario CHECK (salario_minimo IS NULL OR salario_maximo IS NULL OR salario_minimo <= salario_maximo)
);

CREATE INDEX idx_ofertas_empresa ON ofertas (empresa_id);
-- El listado público filtra por estado y ordena por fecha
CREATE INDEX idx_ofertas_estado_fecha ON ofertas (estado, fecha_creacion DESC);
