-- Cuándo aceptó el usuario las condiciones de uso y la política de privacidad al registrarse.
-- Las cuentas anteriores no las aceptaron de forma expresa: se quedan sin fecha.
ALTER TABLE usuarios ADD COLUMN condiciones_aceptadas_en TIMESTAMP WITH TIME ZONE;

-- La limpieza de cuentas que nunca confirmaron su email busca por fecha de alta
CREATE INDEX idx_usuarios_sin_verificar ON usuarios (fecha_creacion) WHERE email_verificado_en IS NULL;
