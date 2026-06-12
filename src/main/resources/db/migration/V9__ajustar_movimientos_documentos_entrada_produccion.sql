ALTER TABLE movimientos_inventario
    ADD COLUMN IF NOT EXISTS guia_serie VARCHAR(20),
    ADD COLUMN IF NOT EXISTS guia_numero VARCHAR(50),
    ADD COLUMN IF NOT EXISTS guia_fecha DATE,
    ADD COLUMN IF NOT EXISTS comprobante_tipo VARCHAR(30),
    ADD COLUMN IF NOT EXISTS comprobante_serie VARCHAR(20),
    ADD COLUMN IF NOT EXISTS comprobante_numero VARCHAR(50),
    ADD COLUMN IF NOT EXISTS comprobante_fecha_emision DATE,
    ADD COLUMN IF NOT EXISTS orden_compra_numero VARCHAR(50),
    ADD COLUMN IF NOT EXISTS observacion_documentaria TEXT;

ALTER TABLE movimientos_inventario
    ALTER COLUMN flete SET DEFAULT 0,
    ALTER COLUMN movilidad SET DEFAULT 0,
    ALTER COLUMN otros_gastos SET DEFAULT 0;

UPDATE movimientos_inventario
SET orden_compra_numero = orden_compra
WHERE orden_compra_numero IS NULL
  AND orden_compra IS NOT NULL;

UPDATE movimientos_inventario
SET comprobante_tipo = tipo_documento,
    comprobante_serie = serie_documento,
    comprobante_numero = numero_documento
WHERE comprobante_tipo IS NULL
  AND tipo_documento IN ('FACTURA', 'BOLETA', 'OTRO')
  AND serie_documento IS NOT NULL
  AND numero_documento IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_movimientos_comprobante_tipo_valido'
    ) THEN
        ALTER TABLE movimientos_inventario
            ADD CONSTRAINT chk_movimientos_comprobante_tipo_valido
            CHECK (
                comprobante_tipo IS NULL
                OR comprobante_tipo IN ('FACTURA', 'BOLETA', 'OTRO')
            );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_movimientos_comprobante_serie_numero'
    ) THEN
        ALTER TABLE movimientos_inventario
            ADD CONSTRAINT chk_movimientos_comprobante_serie_numero
            CHECK (
                (comprobante_serie IS NULL AND comprobante_numero IS NULL)
                OR (comprobante_serie IS NOT NULL AND comprobante_numero IS NOT NULL)
            );
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_movimientos_guia_serie_numero'
    ) THEN
        ALTER TABLE movimientos_inventario
            ADD CONSTRAINT chk_movimientos_guia_serie_numero
            CHECK (
                (guia_serie IS NULL AND guia_numero IS NULL)
                OR (guia_serie IS NOT NULL AND guia_numero IS NOT NULL)
            );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_movimientos_guia
    ON movimientos_inventario (guia_serie, guia_numero);

CREATE INDEX IF NOT EXISTS idx_movimientos_comprobante
    ON movimientos_inventario (comprobante_tipo, comprobante_serie, comprobante_numero);

CREATE INDEX IF NOT EXISTS idx_movimientos_orden_compra_numero
    ON movimientos_inventario (orden_compra_numero);
