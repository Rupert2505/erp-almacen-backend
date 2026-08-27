ALTER TABLE movimiento_detalle
    ADD COLUMN IF NOT EXISTS costo_base_comercial_sin_igv NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS recargo_comercial_pct NUMERIC(7,2),
    ADD COLUMN IF NOT EXISTS valor_venta_unitario_sin_igv NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS igv_unitario NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS precio_venta_unitario_con_igv NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS valor_venta_total_sin_igv NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS igv_total NUMERIC(18,2),
    ADD COLUMN IF NOT EXISTS precio_venta_total_con_igv NUMERIC(18,2);

COMMENT ON COLUMN productos.costo_referencial IS
    'Costo base comercial sin IGV. Manual; no afecta stock, kardex ni costo promedio.';

COMMENT ON COLUMN movimiento_detalle.costo_base_comercial_sin_igv IS
    'Snapshot de productos.costo_referencial al confirmar una SALIDA_VENTA.';

COMMENT ON COLUMN movimiento_detalle.recargo_comercial_pct IS
    'Porcentaje de recargo comercial usado para valorizar una SALIDA_VENTA.';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_costo_base_comercial_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_costo_base_comercial_no_negativo
            CHECK (costo_base_comercial_sin_igv IS NULL OR costo_base_comercial_sin_igv >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_recargo_comercial_rango'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_recargo_comercial_rango
            CHECK (
                recargo_comercial_pct IS NULL
                OR (recargo_comercial_pct >= 0 AND recargo_comercial_pct <= 1000.00)
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_valor_venta_unitario_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_valor_venta_unitario_no_negativo
            CHECK (valor_venta_unitario_sin_igv IS NULL OR valor_venta_unitario_sin_igv >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_igv_unitario_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_igv_unitario_no_negativo
            CHECK (igv_unitario IS NULL OR igv_unitario >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_precio_venta_unitario_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_precio_venta_unitario_no_negativo
            CHECK (precio_venta_unitario_con_igv IS NULL OR precio_venta_unitario_con_igv >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_valor_venta_total_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_valor_venta_total_no_negativo
            CHECK (valor_venta_total_sin_igv IS NULL OR valor_venta_total_sin_igv >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_igv_total_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_igv_total_no_negativo
            CHECK (igv_total IS NULL OR igv_total >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_movimiento_detalle_precio_venta_total_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_precio_venta_total_no_negativo
            CHECK (precio_venta_total_con_igv IS NULL OR precio_venta_total_con_igv >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_movimientos_salida_venta_os_fecha
    ON movimientos_inventario (estado, orden_trabajo, fecha_movimiento)
    WHERE tipo_movimiento = 'SALIDA_VENTA';
