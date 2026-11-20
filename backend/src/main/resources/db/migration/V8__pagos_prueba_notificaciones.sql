-- Los registros anteriores no se clasifican como prueba automáticamente.
ALTER TABLE pedido ADD COLUMN ambiente_pago VARCHAR(16)
    CHECK (ambiente_pago IS NULL OR ambiente_pago = 'PRUEBA');
ALTER TABLE intento_order_mercadopago ADD COLUMN ambiente_pago VARCHAR(16)
    CHECK (ambiente_pago IS NULL OR ambiente_pago = 'PRUEBA');

-- La notificación solo solicita reconciliación: nunca confirma un pago por sí sola.
CREATE TABLE notificacion_order_mercadopago (
 id_order VARCHAR(128) PRIMARY KEY,
 primera_recepcion TIMESTAMPTZ NOT NULL DEFAULT now(),
 ultima_recepcion TIMESTAMPTZ NOT NULL DEFAULT now(),
 version BIGINT NOT NULL DEFAULT 1,
 version_procesada BIGINT NOT NULL DEFAULT 0,
 CHECK (version > 0 AND version_procesada >= 0 AND version_procesada <= version)
);
