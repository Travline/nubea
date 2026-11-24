-- Número corto de pedido por tienda (#1001, #1002…).
-- Se asigna en la BD con un trigger BEFORE INSERT que incrementa un contador en la fila de la tienda:
--   * el checkout ya bloquea esa fila (SELECT … FOR UPDATE), así que los pedidos de una tienda se numeran en serie;
--   * si la transacción se revierte, el contador también: no quedan huecos;
--   * una instancia antigua del backend (despliegue gradual) sigue pudiendo insertar pedidos.
-- Toda la migración corre en una transacción (Flyway + PostgreSQL): o se aplica completa o no se aplica.

ALTER TABLE tienda ADD COLUMN ultimo_numero_pedido INTEGER NOT NULL DEFAULT 1000
  CONSTRAINT chk_tienda_ultimo_numero_pedido CHECK (ultimo_numero_pedido >= 0);

ALTER TABLE pedido ADD COLUMN numero INTEGER;

-- Pedidos existentes: numerar por tienda en orden de creación, empezando en 1001.
-- trg_pedido_transicion no lo impide: no cambia estado, importes ni reserva.
WITH numerados AS (
  SELECT id_pedido,
         1000 + row_number() OVER (PARTITION BY id_tienda ORDER BY fecha_creacion, id_pedido) AS numero
  FROM pedido
)
UPDATE pedido p SET numero = n.numero FROM numerados n WHERE p.id_pedido = n.id_pedido;

UPDATE tienda t SET ultimo_numero_pedido = COALESCE(
  (SELECT max(p.numero) FROM pedido p WHERE p.id_tienda = t.id_tienda), 1000);

ALTER TABLE pedido
  ALTER COLUMN numero SET NOT NULL,
  ADD CONSTRAINT chk_pedido_numero CHECK (numero > 0),
  ADD CONSTRAINT uq_pedido_numero_tienda UNIQUE (id_tienda, numero);

CREATE OR REPLACE FUNCTION nubea_asignar_numero_pedido() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.numero IS NULL THEN
    UPDATE tienda SET ultimo_numero_pedido = ultimo_numero_pedido + 1
     WHERE id_tienda = NEW.id_tienda
    RETURNING ultimo_numero_pedido INTO NEW.numero;
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER trg_pedido_numero BEFORE INSERT ON pedido
  FOR EACH ROW EXECUTE FUNCTION nubea_asignar_numero_pedido();

-- El número es estable: no se puede cambiar después de asignarlo.
CREATE OR REPLACE FUNCTION nubea_proteger_numero_pedido() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
  IF NEW.numero IS DISTINCT FROM OLD.numero THEN
    RAISE EXCEPTION 'El número del pedido % no se puede cambiar', OLD.id_pedido;
  END IF;
  RETURN NEW;
END $$;

CREATE TRIGGER trg_pedido_numero_fijo BEFORE UPDATE OF numero ON pedido
  FOR EACH ROW EXECUTE FUNCTION nubea_proteger_numero_pedido();
