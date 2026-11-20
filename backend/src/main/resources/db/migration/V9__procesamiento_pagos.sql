-- No inferir ambientes de las cuentas existentes.
ALTER TABLE cuenta_pago_tienda ADD COLUMN ambiente_pago varchar(16) CHECK (ambiente_pago IN ('PRUEBA','PRODUCCION'));
ALTER TABLE cuenta_pago_tienda ADD COLUMN renovacion_dueno uuid;
ALTER TABLE cuenta_pago_tienda ADD COLUMN renovacion_hasta timestamptz;
ALTER TABLE pedido DROP CONSTRAINT pedido_ambiente_pago_check;
ALTER TABLE pedido ADD CONSTRAINT pedido_ambiente_pago_check CHECK (ambiente_pago IN ('PRUEBA','PRODUCCION'));
ALTER TABLE intento_order_mercadopago DROP CONSTRAINT intento_order_mercadopago_ambiente_pago_check;
ALTER TABLE intento_order_mercadopago ADD CONSTRAINT intento_order_mercadopago_ambiente_pago_check CHECK (ambiente_pago IN ('PRUEBA','PRODUCCION'));
ALTER TABLE intento_order_mercadopago ADD COLUMN cuenta_receptora varchar(100);
ALTER TABLE intento_order_mercadopago ADD COLUMN retorno_url text;
ALTER TABLE intento_order_mercadopago ADD COLUMN tarea_dueno uuid;
ALTER TABLE intento_order_mercadopago ADD COLUMN tarea_hasta timestamptz;
ALTER TABLE intento_order_mercadopago ADD COLUMN proximo_intento timestamptz NOT NULL DEFAULT now();
ALTER TABLE intento_order_mercadopago ADD COLUMN intentos integer NOT NULL DEFAULT 0;
ALTER TABLE intento_order_mercadopago ADD COLUMN ultimo_error varchar(100);
ALTER TABLE intento_order_mercadopago ADD COLUMN estado_proveedor varchar(60);
ALTER TABLE intento_order_mercadopago ADD COLUMN clave_cancelacion uuid NOT NULL DEFAULT gen_random_uuid();
CREATE INDEX idx_order_trabajo ON intento_order_mercadopago(proximo_intento);
CREATE TABLE reembolso_order (
 id_pedido uuid PRIMARY KEY REFERENCES pedido(id_pedido),
 clave uuid NOT NULL UNIQUE,
 solicitado_por uuid REFERENCES usuario_plataforma(id_usuario),
 reponer_stock boolean NOT NULL DEFAULT false,
 stock_repuesto boolean NOT NULL DEFAULT false,
 estado varchar(16) NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE','CONFIRMADO')),
 fecha_creacion timestamptz NOT NULL DEFAULT now(),
 fecha_confirmacion timestamptz
);
