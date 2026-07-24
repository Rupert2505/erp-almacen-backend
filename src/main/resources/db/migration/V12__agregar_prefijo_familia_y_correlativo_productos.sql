ALTER TABLE familias
    ADD COLUMN IF NOT EXISTS prefijo VARCHAR(20);

UPDATE familias
SET prefijo = LEFT(
        COALESCE(NULLIF(REGEXP_REPLACE(UPPER(nombre), '[^A-Z0-9]', '', 'g'), ''), 'FAM'),
        GREATEST(1, 20 - LENGTH(id::TEXT))
    ) || id::TEXT
WHERE prefijo IS NULL;

ALTER TABLE familias
    ALTER COLUMN prefijo SET NOT NULL;

ALTER TABLE familias
    ADD CONSTRAINT uk_familias_prefijo UNIQUE (prefijo);

ALTER TABLE familias
    ADD CONSTRAINT chk_familias_prefijo_no_vacio CHECK (BTRIM(prefijo) <> '');

ALTER TABLE familias
    ADD CONSTRAINT chk_familias_prefijo_formato CHECK (prefijo ~ '^[A-Z0-9]+$');

CREATE TABLE IF NOT EXISTS familia_correlativos (
    id BIGSERIAL PRIMARY KEY,
    familia_id BIGINT NOT NULL,
    ultimo_correlativo BIGINT NOT NULL DEFAULT 0,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NULL,
    CONSTRAINT fk_familia_correlativos_familia
        FOREIGN KEY (familia_id) REFERENCES familias(id),
    CONSTRAINT uk_familia_correlativos_familia UNIQUE (familia_id),
    CONSTRAINT chk_familia_correlativos_ultimo_no_negativo CHECK (ultimo_correlativo >= 0)
);

INSERT INTO familia_correlativos (familia_id, ultimo_correlativo)
SELECT
    f.id,
    COALESCE(MAX(
        CASE
            WHEN p.codigo ~ ('^' || f.prefijo || '[0-9]+$')
                THEN SUBSTRING(p.codigo FROM ('^' || f.prefijo || '([0-9]+)$'))::BIGINT
            ELSE 0
        END
    ), 0)
FROM familias f
LEFT JOIN productos p ON p.familia_id = f.id
GROUP BY f.id
ON CONFLICT (familia_id) DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_familia_correlativos_familia_id
    ON familia_correlativos(familia_id);
