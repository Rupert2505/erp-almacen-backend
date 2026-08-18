ALTER TABLE movimiento_detalle
    ADD COLUMN IF NOT EXISTS valor_unitario_sin_igv NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS tasa_igv NUMERIC(7,4),
    ADD COLUMN IF NOT EXISTS monto_igv_unitario NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS monto_igv_total NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS precio_unitario_con_igv NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS igv_recuperable BOOLEAN,
    ADD COLUMN IF NOT EXISTS costo_unitario_inventariable NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS total_inventariable NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS total_con_igv NUMERIC(18,6);

UPDATE movimiento_detalle
SET costo_unitario_inventariable = costo_unitario,
    total_inventariable = total_linea
WHERE costo_unitario_inventariable IS NULL
  AND costo_unitario IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_valor_sin_igv_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_valor_sin_igv_no_negativo
            CHECK (valor_unitario_sin_igv IS NULL OR valor_unitario_sin_igv >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_tasa_igv_no_negativa'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_tasa_igv_no_negativa
            CHECK (tasa_igv IS NULL OR tasa_igv >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_monto_igv_unitario_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_monto_igv_unitario_no_negativo
            CHECK (monto_igv_unitario IS NULL OR monto_igv_unitario >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_monto_igv_total_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_monto_igv_total_no_negativo
            CHECK (monto_igv_total IS NULL OR monto_igv_total >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_precio_con_igv_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_precio_con_igv_no_negativo
            CHECK (precio_unitario_con_igv IS NULL OR precio_unitario_con_igv >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_costo_inventariable_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_costo_inventariable_no_negativo
            CHECK (costo_unitario_inventariable IS NULL OR costo_unitario_inventariable >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_total_inventariable_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_total_inventariable_no_negativo
            CHECK (total_inventariable IS NULL OR total_inventariable >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_total_con_igv_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_total_con_igv_no_negativo
            CHECK (total_con_igv IS NULL OR total_con_igv >= 0);
    END IF;
END $$;
