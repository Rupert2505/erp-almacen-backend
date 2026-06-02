CREATE TABLE movimientos_inventario (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(30) NOT NULL,
    tipo_movimiento VARCHAR(40) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'BORRADOR',
    fecha_movimiento TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    proveedor_id BIGINT,
    almacen_origen_id BIGINT,
    almacen_destino_id BIGINT,
    documento_referencia VARCHAR(100),
    observacion VARCHAR(500),
    usuario_id BIGINT NOT NULL,
    confirmado_en TIMESTAMP,
    anulado_en TIMESTAMP,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_movimientos_inventario_numero UNIQUE (numero),
    CONSTRAINT fk_movimientos_inventario_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores (id),
    CONSTRAINT fk_movimientos_inventario_almacen_origen FOREIGN KEY (almacen_origen_id) REFERENCES almacenes (id),
    CONSTRAINT fk_movimientos_inventario_almacen_destino FOREIGN KEY (almacen_destino_id) REFERENCES almacenes (id),
    CONSTRAINT fk_movimientos_inventario_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT chk_movimientos_inventario_tipo CHECK (
        tipo_movimiento IN (
            'ENTRADA_COMPRA',
            'ENTRADA_AJUSTE',
            'SALIDA_CONSUMO',
            'SALIDA_VENTA',
            'SALIDA_AJUSTE',
            'TRANSFERENCIA',
            'AJUSTE_POSITIVO',
            'AJUSTE_NEGATIVO'
        )
    ),
    CONSTRAINT chk_movimientos_inventario_estado CHECK (estado IN ('BORRADOR', 'CONFIRMADO', 'ANULADO'))
);

CREATE TABLE movimiento_detalle (
    id BIGSERIAL PRIMARY KEY,
    movimiento_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    ubicacion_origen_id BIGINT,
    ubicacion_destino_id BIGINT,
    cantidad NUMERIC(18,4) NOT NULL,
    costo_unitario NUMERIC(18,4),
    total_linea NUMERIC(18,4),
    observacion VARCHAR(255),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT fk_movimiento_detalle_movimiento FOREIGN KEY (movimiento_id) REFERENCES movimientos_inventario (id),
    CONSTRAINT fk_movimiento_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_movimiento_detalle_ubicacion_origen FOREIGN KEY (ubicacion_origen_id) REFERENCES ubicaciones_almacen (id),
    CONSTRAINT fk_movimiento_detalle_ubicacion_destino FOREIGN KEY (ubicacion_destino_id) REFERENCES ubicaciones_almacen (id),
    CONSTRAINT chk_movimiento_detalle_cantidad_positiva CHECK (cantidad > 0),
    CONSTRAINT chk_movimiento_detalle_costo_unitario_no_negativo CHECK (costo_unitario IS NULL OR costo_unitario >= 0),
    CONSTRAINT chk_movimiento_detalle_total_linea_no_negativo CHECK (total_linea IS NULL OR total_linea >= 0)
);

CREATE TABLE stock_actual (
    id BIGSERIAL PRIMARY KEY,
    producto_id BIGINT NOT NULL,
    almacen_id BIGINT NOT NULL,
    ubicacion_id BIGINT,
    cantidad_actual NUMERIC(18,4) NOT NULL DEFAULT 0,
    costo_promedio NUMERIC(18,4),
    fecha_ultimo_movimiento TIMESTAMP,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT fk_stock_actual_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_stock_actual_almacen FOREIGN KEY (almacen_id) REFERENCES almacenes (id),
    CONSTRAINT fk_stock_actual_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones_almacen (id),
    CONSTRAINT chk_stock_actual_cantidad_no_negativa CHECK (cantidad_actual >= 0),
    CONSTRAINT chk_stock_actual_costo_promedio_no_negativo CHECK (costo_promedio IS NULL OR costo_promedio >= 0)
);

CREATE TABLE kardex (
    id BIGSERIAL PRIMARY KEY,
    movimiento_id BIGINT NOT NULL,
    movimiento_detalle_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    almacen_id BIGINT NOT NULL,
    ubicacion_id BIGINT,
    fecha_movimiento TIMESTAMP NOT NULL,
    tipo_movimiento VARCHAR(40) NOT NULL,
    numero_movimiento VARCHAR(30) NOT NULL,
    entrada NUMERIC(18,4) NOT NULL DEFAULT 0,
    salida NUMERIC(18,4) NOT NULL DEFAULT 0,
    saldo_anterior NUMERIC(18,4) NOT NULL DEFAULT 0,
    saldo_final NUMERIC(18,4) NOT NULL DEFAULT 0,
    costo_unitario NUMERIC(18,4),
    costo_promedio NUMERIC(18,4),
    usuario_id BIGINT NOT NULL,
    observacion VARCHAR(500),
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_kardex_movimiento FOREIGN KEY (movimiento_id) REFERENCES movimientos_inventario (id),
    CONSTRAINT fk_kardex_movimiento_detalle FOREIGN KEY (movimiento_detalle_id) REFERENCES movimiento_detalle (id),
    CONSTRAINT fk_kardex_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_kardex_almacen FOREIGN KEY (almacen_id) REFERENCES almacenes (id),
    CONSTRAINT fk_kardex_ubicacion FOREIGN KEY (ubicacion_id) REFERENCES ubicaciones_almacen (id),
    CONSTRAINT fk_kardex_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT chk_kardex_tipo_movimiento CHECK (
        tipo_movimiento IN (
            'ENTRADA_COMPRA',
            'ENTRADA_AJUSTE',
            'SALIDA_CONSUMO',
            'SALIDA_VENTA',
            'SALIDA_AJUSTE',
            'TRANSFERENCIA',
            'AJUSTE_POSITIVO',
            'AJUSTE_NEGATIVO'
        )
    ),
    CONSTRAINT chk_kardex_entrada_no_negativa CHECK (entrada >= 0),
    CONSTRAINT chk_kardex_salida_no_negativa CHECK (salida >= 0),
    CONSTRAINT chk_kardex_saldo_anterior_no_negativo CHECK (saldo_anterior >= 0),
    CONSTRAINT chk_kardex_saldo_final_no_negativo CHECK (saldo_final >= 0),
    CONSTRAINT chk_kardex_costo_unitario_no_negativo CHECK (costo_unitario IS NULL OR costo_unitario >= 0),
    CONSTRAINT chk_kardex_costo_promedio_no_negativo CHECK (costo_promedio IS NULL OR costo_promedio >= 0),
    CONSTRAINT chk_kardex_entrada_salida_exclusiva CHECK (
        (entrada > 0 AND salida = 0)
        OR (entrada = 0 AND salida > 0)
    )
);

CREATE INDEX idx_movimientos_inventario_numero ON movimientos_inventario (numero);
CREATE INDEX idx_movimientos_inventario_tipo_movimiento ON movimientos_inventario (tipo_movimiento);
CREATE INDEX idx_movimientos_inventario_estado ON movimientos_inventario (estado);
CREATE INDEX idx_movimientos_inventario_fecha_movimiento ON movimientos_inventario (fecha_movimiento);
CREATE INDEX idx_movimientos_inventario_proveedor_id ON movimientos_inventario (proveedor_id);
CREATE INDEX idx_movimientos_inventario_almacen_origen_id ON movimientos_inventario (almacen_origen_id);
CREATE INDEX idx_movimientos_inventario_almacen_destino_id ON movimientos_inventario (almacen_destino_id);

CREATE INDEX idx_movimiento_detalle_movimiento_id ON movimiento_detalle (movimiento_id);
CREATE INDEX idx_movimiento_detalle_producto_id ON movimiento_detalle (producto_id);

CREATE INDEX idx_stock_actual_producto_id ON stock_actual (producto_id);
CREATE INDEX idx_stock_actual_almacen_id ON stock_actual (almacen_id);
CREATE INDEX idx_stock_actual_ubicacion_id ON stock_actual (ubicacion_id);
CREATE UNIQUE INDEX uk_stock_actual_producto_almacen_ubicacion
    ON stock_actual (producto_id, almacen_id, ubicacion_id)
    WHERE ubicacion_id IS NOT NULL;
CREATE UNIQUE INDEX uk_stock_actual_producto_almacen_sin_ubicacion
    ON stock_actual (producto_id, almacen_id)
    WHERE ubicacion_id IS NULL;

CREATE INDEX idx_kardex_producto_id ON kardex (producto_id);
CREATE INDEX idx_kardex_almacen_id ON kardex (almacen_id);
CREATE INDEX idx_kardex_ubicacion_id ON kardex (ubicacion_id);
CREATE INDEX idx_kardex_fecha_movimiento ON kardex (fecha_movimiento);
CREATE INDEX idx_kardex_movimiento_id ON kardex (movimiento_id);
