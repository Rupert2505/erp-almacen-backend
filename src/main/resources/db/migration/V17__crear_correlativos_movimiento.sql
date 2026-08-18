CREATE TABLE IF NOT EXISTS movimiento_correlativos (
    id BIGSERIAL PRIMARY KEY,
    prefijo VARCHAR(10) NOT NULL,
    ultimo_correlativo BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_movimiento_correlativos_prefijo UNIQUE (prefijo),
    CONSTRAINT chk_movimiento_correlativos_prefijo_no_vacio CHECK (BTRIM(prefijo) <> ''),
    CONSTRAINT chk_movimiento_correlativos_ultimo_no_negativo CHECK (ultimo_correlativo >= 0)
);

INSERT INTO movimiento_correlativos (prefijo, ultimo_correlativo)
VALUES
    ('MOV', 0),
    ('DEV', 0)
ON CONFLICT (prefijo) DO NOTHING;

WITH maximos AS (
    SELECT
        p.prefijo,
        COALESCE(MAX(
            CASE
                WHEN m.numero ~ ('^' || p.prefijo || '-[0-9]+$')
                    THEN SUBSTRING(m.numero FROM ('^' || p.prefijo || '-([0-9]+)$'))::BIGINT
                ELSE 0
            END
        ), 0) AS max_correlativo
    FROM (VALUES ('MOV'), ('DEV')) AS p(prefijo)
    LEFT JOIN movimientos_inventario m
        ON m.numero ~ ('^' || p.prefijo || '-[0-9]+$')
    GROUP BY p.prefijo
)
UPDATE movimiento_correlativos c
SET ultimo_correlativo = GREATEST(c.ultimo_correlativo, m.max_correlativo),
    actualizado_en = CURRENT_TIMESTAMP
FROM maximos m
WHERE c.prefijo = m.prefijo;
