-- La dirección se conserva como identificador de la solicitud, sin impedir su eliminación.
CREATE TABLE checkout_idempotencia (
 id_usuario UUID NOT NULL REFERENCES usuario_plataforma(id_usuario),
 id_tienda UUID NOT NULL REFERENCES tienda(id_tienda),
 clave VARCHAR(100) NOT NULL,
 id_direccion UUID NOT NULL,
 total_esperado NUMERIC(12,2) NOT NULL CHECK (total_esperado > 0),
 id_pedido UUID NOT NULL UNIQUE REFERENCES pedido(id_pedido),
 fecha_creacion TIMESTAMPTZ NOT NULL DEFAULT now(),
 PRIMARY KEY (id_usuario, id_tienda, clave)
);
