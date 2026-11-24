-- Plantillas de vitrina que ya dibuja apps/store (además de 'minimal', sembrada en V1).
-- El código 'audaz' se muestra como "Tech". Los códigos deben coincidir con PLANTILLAS_VISIBLES (cms) y PLANTILLAS_DISPONIBLES (store).
-- Idempotente: si en una base ya se creó alguna a mano con el mismo código, se conserva (plantilla_codigo_key).

-- La secuencia de id_plantilla (IDENTITY, V4) podría estar atrasada si se insertaron plantillas con id explícito.
SELECT setval(pg_get_serial_sequence('plantilla', 'id_plantilla'),
              GREATEST((SELECT COALESCE(MAX(id_plantilla), 1) FROM plantilla), 1));

INSERT INTO plantilla (codigo, nombre, activa) VALUES
  ('boutique', 'Boutique', TRUE),
  ('audaz',    'Tech',     TRUE),
  ('gourmet',  'Gourmet',  TRUE),
  ('natural',  'Natural',  TRUE)
ON CONFLICT (codigo) DO NOTHING;
