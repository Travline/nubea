-- Ejecutar una sola vez sobre una base existente, antes de habilitar OAuth.
BEGIN;
CREATE TABLE oauth_mercado_pago_intento (
    id_intento UUID PRIMARY KEY,
    id_tienda UUID NOT NULL REFERENCES tienda(id_tienda) ON DELETE RESTRICT,
    id_usuario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario) ON DELETE RESTRICT,
    state_hash BYTEA NOT NULL UNIQUE CHECK (octet_length(state_hash) = 32),
    verifier_cifrado TEXT NOT NULL,
    retorno_panel TEXT NOT NULL,
    fecha_creacion TIMESTAMPTZ NOT NULL,
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'CONSUMIDO', 'INVALIDADO')),
    CHECK (fecha_expiracion > fecha_creacion)
);
CREATE INDEX idx_oauth_mp_intento_tienda ON oauth_mercado_pago_intento(id_tienda, fecha_creacion DESC);
CREATE UNIQUE INDEX uq_oauth_mp_pendiente_tienda ON oauth_mercado_pago_intento(id_tienda) WHERE estado = 'PENDIENTE';
COMMIT;
