ALTER TABLE direccion_cliente ALTER COLUMN codigo_ubigeo DROP NOT NULL;
ALTER TABLE direccion_cliente ADD COLUMN departamento VARCHAR(100), ADD COLUMN provincia VARCHAR(100), ADD COLUMN distrito VARCHAR(100), ADD COLUMN place_id VARCHAR(255);
ALTER TABLE pedido ALTER COLUMN ubigeo_entrega DROP NOT NULL, ALTER COLUMN departamento_entrega DROP NOT NULL, ALTER COLUMN provincia_entrega DROP NOT NULL, ALTER COLUMN distrito_entrega DROP NOT NULL;
ALTER TABLE pedido ADD COLUMN place_id_entrega VARCHAR(255);
