-- El receptor sugerido solo enruta la consulta. La respuesta autenticada autoriza la asociación.
ALTER TABLE notificacion_order_mercadopago ADD COLUMN receptor_sugerido varchar(100);
ALTER TABLE notificacion_order_mercadopago ADD COLUMN proxima_consulta timestamptz NOT NULL DEFAULT now();
