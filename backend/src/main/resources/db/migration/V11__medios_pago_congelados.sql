-- Congelar las exclusiones con la compra para conservar el cuerpo de los reintentos.
ALTER TABLE intento_order_mercadopago ADD COLUMN tipos_excluidos text;
