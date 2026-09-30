-- Currículum del candidato: un PDF por cuenta, que ven las empresas en las que se inscribe.
-- Se guarda en la base de datos (máximo 5 MB) para que las copias de seguridad lo incluyan.
CREATE TABLE curriculos (
    usuario_id     UUID PRIMARY KEY REFERENCES usuarios (id) ON DELETE CASCADE,
    nombre_fichero VARCHAR(255)             NOT NULL,
    tamano         INTEGER                  NOT NULL,
    contenido      BYTEA                    NOT NULL,
    fecha_subida   TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_curriculos_tamano CHECK (tamano > 0)
);
