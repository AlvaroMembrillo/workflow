CREATE TABLE usuarios (
    id             UUID PRIMARY KEY,
    email          VARCHAR(255)             NOT NULL,
    password_hash  VARCHAR(100)             NOT NULL,
    nombre         VARCHAR(100)             NOT NULL,
    rol            VARCHAR(20)              NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('CANDIDATO', 'EMPRESA', 'ADMIN'))
);
