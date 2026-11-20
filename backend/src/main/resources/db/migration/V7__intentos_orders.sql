-- Un intento durable por pedido; la clave del proveedor no cambia en los reintentos.
CREATE TABLE intento_order_mercadopago (
 id_intento UUID PRIMARY KEY,
 id_pedido UUID NOT NULL UNIQUE REFERENCES pedido(id_pedido),
 clave_proveedor UUID NOT NULL UNIQUE,
 id_order VARCHAR(128) UNIQUE,
 checkout_url TEXT,
 estado VARCHAR(30) NOT NULL DEFAULT 'PREPARADO'
   CHECK (estado IN ('PREPARADO','CREACION_INCIERTA','CREADA','FINALIZADA','REEMBOLSO_PENDIENTE','REEMBOLSADA')),
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 fecha_actualizacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 CONSTRAINT intento_order_datos_creada CHECK (
   estado NOT IN ('CREADA','FINALIZADA','REEMBOLSO_PENDIENTE','REEMBOLSADA') OR id_order IS NOT NULL
 )
);

-- Conservar pedidos registrados por el núcleo de checkout antes de esta migración.
INSERT INTO intento_order_mercadopago(id_intento,id_pedido,clave_proveedor)
SELECT gen_random_uuid(),id_pedido,gen_random_uuid() FROM checkout_idempotencia;
