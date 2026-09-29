-- Fecha de cada paso, para que el candidato vea cuándo avanzó su candidatura
ALTER TABLE candidaturas ADD COLUMN fecha_revision TIMESTAMP WITH TIME ZONE;
ALTER TABLE candidaturas ADD COLUMN fecha_resolucion TIMESTAMP WITH TIME ZONE;

-- Las candidaturas ya resueltas toman como fecha de resolución la de su último cambio
UPDATE candidaturas
SET fecha_resolucion = fecha_actualizacion
WHERE estado IN ('ACEPTADA', 'RECHAZADA');

-- Nuevo estado final: el candidato retira su candidatura
ALTER TABLE candidaturas DROP CONSTRAINT ck_candidaturas_estado;
ALTER TABLE candidaturas ADD CONSTRAINT ck_candidaturas_estado
    CHECK (estado IN ('PENDIENTE', 'EN_REVISION', 'ACEPTADA', 'RECHAZADA', 'RETIRADA'));

-- Las ofertas sin actividad se cierran solas: el proceso busca por estado y fecha de actualización
CREATE INDEX idx_ofertas_estado_actualizacion ON ofertas (estado, fecha_actualizacion);
