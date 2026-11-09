BEGIN;
CREATE TABLE cuenta_pago_historial (
    id_historial BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id_tienda UUID NOT NULL REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
    id_usuario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
    estado_anterior VARCHAR(20) NOT NULL CHECK (estado_anterior IN ('PENDIENTE','ACTIVA','DESCONECTADA','ERROR')),
    estado_nuevo VARCHAR(20) NOT NULL CHECK (estado_nuevo = 'DESCONECTADA'),
    fecha_cambio TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_cuenta_pago_historial_tienda_fecha ON cuenta_pago_historial(id_tienda, fecha_cambio);
COMMIT;
