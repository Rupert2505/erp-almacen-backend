INSERT INTO movimiento_correlativos (prefijo, ultimo_correlativo)
VALUES ('DEV', 0)
ON CONFLICT (prefijo) DO NOTHING;

WITH maximo_dev AS (
    SELECT COALESCE(MAX(
        CASE
            WHEN numero ~ '^DEV-[0-9]+$'
                THEN SUBSTRING(numero FROM '^DEV-([0-9]+)$')::BIGINT
            ELSE 0
        END
    ), 0) AS max_correlativo
    FROM movimientos_inventario
)
UPDATE movimiento_correlativos c
SET ultimo_correlativo = GREATEST(c.ultimo_correlativo, m.max_correlativo),
    actualizado_en = CURRENT_TIMESTAMP
FROM maximo_dev m
WHERE c.prefijo = 'DEV';
