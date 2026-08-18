ALTER TABLE movimientos_inventario
    ADD COLUMN IF NOT EXISTS movimiento_origen_id BIGINT,
    ADD COLUMN IF NOT EXISTS fecha_documento DATE,
    ADD COLUMN IF NOT EXISTS numero_nota_credito VARCHAR(50);

ALTER TABLE movimiento_detalle
    ADD COLUMN IF NOT EXISTS movimiento_detalle_origen_id BIGINT,
    ADD COLUMN IF NOT EXISTS costo_documento_unitario NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS valor_stock NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS valor_documento NUMERIC(18,6),
    ADD COLUMN IF NOT EXISTS diferencia_valorizacion NUMERIC(18,6);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_movimientos_inventario_movimiento_origen'
    ) THEN
        ALTER TABLE movimientos_inventario
            ADD CONSTRAINT fk_movimientos_inventario_movimiento_origen
            FOREIGN KEY (movimiento_origen_id) REFERENCES movimientos_inventario (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_movimiento_detalle_detalle_origen'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT fk_movimiento_detalle_detalle_origen
            FOREIGN KEY (movimiento_detalle_origen_id) REFERENCES movimiento_detalle (id);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_movimiento_detalle_valor_stock_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_valor_stock_no_negativo
            CHECK (valor_stock IS NULL OR valor_stock >= 0);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_movimiento_detalle_valor_documento_no_negativo'
    ) THEN
        ALTER TABLE movimiento_detalle
            ADD CONSTRAINT chk_movimiento_detalle_valor_documento_no_negativo
            CHECK (valor_documento IS NULL OR valor_documento >= 0);
    END IF;
END $$;

ALTER TABLE movimientos_inventario
    DROP CONSTRAINT IF EXISTS chk_movimientos_inventario_tipo;

ALTER TABLE movimientos_inventario
    ADD CONSTRAINT chk_movimientos_inventario_tipo CHECK (
        tipo_movimiento IN (
            'ENTRADA_COMPRA',
            'ENTRADA_AJUSTE',
            'SALIDA_CONSUMO',
            'SALIDA_VENTA',
            'SALIDA_AJUSTE',
            'TRANSFERENCIA',
            'AJUSTE_POSITIVO',
            'AJUSTE_NEGATIVO',
            'DEVOLUCION_PROVEEDOR'
        )
    );

ALTER TABLE kardex
    DROP CONSTRAINT IF EXISTS chk_kardex_tipo_movimiento;

ALTER TABLE kardex
    ADD CONSTRAINT chk_kardex_tipo_movimiento CHECK (
        tipo_movimiento IN (
            'ENTRADA_COMPRA',
            'ENTRADA_AJUSTE',
            'SALIDA_CONSUMO',
            'SALIDA_VENTA',
            'SALIDA_AJUSTE',
            'TRANSFERENCIA',
            'AJUSTE_POSITIVO',
            'AJUSTE_NEGATIVO',
            'DEVOLUCION_PROVEEDOR'
        )
    );

CREATE INDEX IF NOT EXISTS idx_movimientos_inventario_movimiento_origen_id
    ON movimientos_inventario (movimiento_origen_id);

CREATE INDEX IF NOT EXISTS idx_movimientos_inventario_nota_credito
    ON movimientos_inventario (numero_nota_credito);

CREATE INDEX IF NOT EXISTS idx_movimiento_detalle_detalle_origen_id
    ON movimiento_detalle (movimiento_detalle_origen_id);

CREATE INDEX IF NOT EXISTS idx_movimiento_detalle_devolucion_origen
    ON movimiento_detalle (movimiento_detalle_origen_id, movimiento_id);

CREATE INDEX IF NOT EXISTS idx_kardex_producto_almacen_fecha_id
    ON kardex (producto_id, almacen_id, fecha_movimiento, id);

INSERT INTO permisos (codigo, descripcion)
VALUES
    ('INVENTARIO_DEVOLUCION_VER', 'Ver devoluciones a proveedor'),
    ('INVENTARIO_DEVOLUCION_CREAR', 'Crear devoluciones a proveedor'),
    ('INVENTARIO_DEVOLUCION_CONFIRMAR', 'Confirmar devoluciones a proveedor'),
    ('INVENTARIO_DEVOLUCION_ANULAR', 'Anular devoluciones a proveedor')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.nombre IN ('ADMIN', 'JEFE_ALMACEN')
  AND p.codigo IN (
      'INVENTARIO_DEVOLUCION_VER',
      'INVENTARIO_DEVOLUCION_CREAR',
      'INVENTARIO_DEVOLUCION_CONFIRMAR',
      'INVENTARIO_DEVOLUCION_ANULAR'
  )
ON CONFLICT DO NOTHING;
