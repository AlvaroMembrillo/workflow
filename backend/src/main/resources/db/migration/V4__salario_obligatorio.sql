-- La Directiva (UE) 2023/970 de transparencia salarial obliga a indicar el salario en las ofertas.
-- Si hubiera ofertas sin salario, esta migración falla: hay que completarlas antes de aplicarla.
ALTER TABLE ofertas ALTER COLUMN salario_minimo SET NOT NULL;
ALTER TABLE ofertas ALTER COLUMN salario_maximo SET NOT NULL;

ALTER TABLE ofertas ADD CONSTRAINT ck_ofertas_salario_positivo CHECK (salario_minimo > 0);
