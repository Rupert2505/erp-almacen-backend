CREATE TABLE categorias (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_categorias_codigo UNIQUE (codigo)
);

CREATE TABLE marcas (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_marcas_codigo UNIQUE (codigo)
);

CREATE TABLE unidades_medida (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    abreviatura VARCHAR(20) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_unidades_medida_codigo UNIQUE (codigo)
);

CREATE TABLE proveedores (
    id BIGSERIAL PRIMARY KEY,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(20) NOT NULL,
    razon_social VARCHAR(200) NOT NULL,
    nombre_comercial VARCHAR(200),
    direccion VARCHAR(255),
    telefono VARCHAR(50),
    email VARCHAR(150),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_proveedores_numero_documento UNIQUE (numero_documento)
);

CREATE TABLE almacenes (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    direccion VARCHAR(255),
    responsable VARCHAR(150),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_almacenes_codigo UNIQUE (codigo)
);

CREATE TABLE ubicaciones_almacen (
    id BIGSERIAL PRIMARY KEY,
    almacen_id BIGINT NOT NULL,
    codigo VARCHAR(30) NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_ubicaciones_almacen_codigo UNIQUE (almacen_id, codigo),
    CONSTRAINT fk_ubicaciones_almacen_almacen FOREIGN KEY (almacen_id) REFERENCES almacenes (id)
);

CREATE TABLE productos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL,
    nombre VARCHAR(200) NOT NULL,
    descripcion VARCHAR(255),
    categoria_id BIGINT,
    marca_id BIGINT,
    unidad_medida_id BIGINT NOT NULL,
    stock_minimo NUMERIC(18,4) NOT NULL DEFAULT 0,
    stock_maximo NUMERIC(18,4),
    costo_referencial NUMERIC(18,4),
    controla_lote BOOLEAN NOT NULL DEFAULT FALSE,
    controla_serie BOOLEAN NOT NULL DEFAULT FALSE,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP,
    CONSTRAINT uk_productos_codigo UNIQUE (codigo),
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_productos_marca FOREIGN KEY (marca_id) REFERENCES marcas (id),
    CONSTRAINT fk_productos_unidad_medida FOREIGN KEY (unidad_medida_id) REFERENCES unidades_medida (id),
    CONSTRAINT chk_productos_stock_minimo_no_negativo CHECK (stock_minimo >= 0),
    CONSTRAINT chk_productos_stock_maximo_no_negativo CHECK (stock_maximo IS NULL OR stock_maximo >= 0),
    CONSTRAINT chk_productos_costo_referencial_no_negativo CHECK (costo_referencial IS NULL OR costo_referencial >= 0),
    CONSTRAINT chk_productos_stock_maximo_mayor_igual_minimo CHECK (stock_maximo IS NULL OR stock_maximo >= stock_minimo)
);

CREATE INDEX idx_categorias_codigo ON categorias (codigo);
CREATE INDEX idx_categorias_nombre ON categorias (nombre);
CREATE INDEX idx_categorias_activo ON categorias (activo);

CREATE INDEX idx_marcas_codigo ON marcas (codigo);
CREATE INDEX idx_marcas_nombre ON marcas (nombre);
CREATE INDEX idx_marcas_activo ON marcas (activo);

CREATE INDEX idx_unidades_medida_codigo ON unidades_medida (codigo);
CREATE INDEX idx_unidades_medida_nombre ON unidades_medida (nombre);
CREATE INDEX idx_unidades_medida_activo ON unidades_medida (activo);

CREATE INDEX idx_proveedores_numero_documento ON proveedores (numero_documento);
CREATE INDEX idx_proveedores_razon_social ON proveedores (razon_social);
CREATE INDEX idx_proveedores_activo ON proveedores (activo);

CREATE INDEX idx_almacenes_codigo ON almacenes (codigo);
CREATE INDEX idx_almacenes_nombre ON almacenes (nombre);
CREATE INDEX idx_almacenes_activo ON almacenes (activo);

CREATE INDEX idx_ubicaciones_almacen_almacen_id ON ubicaciones_almacen (almacen_id);
CREATE INDEX idx_ubicaciones_almacen_codigo ON ubicaciones_almacen (codigo);
CREATE INDEX idx_ubicaciones_almacen_nombre ON ubicaciones_almacen (nombre);
CREATE INDEX idx_ubicaciones_almacen_activo ON ubicaciones_almacen (activo);

CREATE INDEX idx_productos_codigo ON productos (codigo);
CREATE INDEX idx_productos_nombre ON productos (nombre);
CREATE INDEX idx_productos_activo ON productos (activo);
CREATE INDEX idx_productos_categoria_id ON productos (categoria_id);
CREATE INDEX idx_productos_marca_id ON productos (marca_id);
CREATE INDEX idx_productos_unidad_medida_id ON productos (unidad_medida_id);

INSERT INTO unidades_medida (codigo, nombre, abreviatura) VALUES
    ('UND', 'Unidad', 'UND'),
    ('CJ', 'Caja', 'CJ'),
    ('PAQ', 'Paquete', 'PAQ'),
    ('KG', 'Kilogramo', 'KG'),
    ('LT', 'Litro', 'LT');
