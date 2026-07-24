INSERT INTO familia_correlativos (familia_id, ultimo_correlativo)
SELECT f.id, 0
FROM familias f
WHERE NOT EXISTS (
    SELECT 1
    FROM familia_correlativos c
    WHERE c.familia_id = f.id
);

WITH maximos AS (
    SELECT
        f.id AS familia_id,
        COALESCE(MAX(
            CASE
                WHEN p.codigo ~ ('^' || f.prefijo || '-[0-9]+$')
                    THEN SUBSTRING(p.codigo FROM ('^' || f.prefijo || '-([0-9]+)$'))::BIGINT
                WHEN p.codigo ~ ('^' || f.prefijo || '[0-9]+$')
                    THEN SUBSTRING(p.codigo FROM ('^' || f.prefijo || '([0-9]+)$'))::BIGINT
                ELSE 0
            END
        ), 0) AS max_correlativo
    FROM familias f
    LEFT JOIN productos p ON p.familia_id = f.id
    GROUP BY f.id
)
UPDATE familia_correlativos c
SET ultimo_correlativo = GREATEST(c.ultimo_correlativo, m.max_correlativo),
    actualizado_en = CURRENT_TIMESTAMP
FROM maximos m
WHERE c.familia_id = m.familia_id;
