CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    nombres VARCHAR(100) NOT NULL,
    apellidos VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuarios_username UNIQUE (username),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_roles_nombre UNIQUE (nombre)
);

CREATE TABLE permisos (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL,
    descripcion VARCHAR(255),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_permisos_codigo UNIQUE (codigo)
);

CREATE TABLE usuario_roles (
    usuario_id BIGINT NOT NULL,
    rol_id BIGINT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_usuario_roles PRIMARY KEY (usuario_id, rol_id),
    CONSTRAINT fk_usuario_roles_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_usuario_roles_rol FOREIGN KEY (rol_id) REFERENCES roles (id)
);

CREATE TABLE rol_permisos (
    rol_id BIGINT NOT NULL,
    permiso_id BIGINT NOT NULL,
    creado_en TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_rol_permisos PRIMARY KEY (rol_id, permiso_id),
    CONSTRAINT fk_rol_permisos_rol FOREIGN KEY (rol_id) REFERENCES roles (id),
    CONSTRAINT fk_rol_permisos_permiso FOREIGN KEY (permiso_id) REFERENCES permisos (id)
);

INSERT INTO roles (nombre, descripcion) VALUES
    ('ADMIN', 'Administrador del sistema'),
    ('JEFE_ALMACEN', 'Responsable de la gestión del almacén'),
    ('OPERADOR_ALMACEN', 'Operador de procesos de almacén'),
    ('CONSULTA', 'Usuario de consulta');

INSERT INTO permisos (codigo, descripcion) VALUES
    ('PRODUCTO_VER', 'Ver productos'),
    ('PRODUCTO_CREAR', 'Crear productos'),
    ('PRODUCTO_EDITAR', 'Editar productos'),
    ('PRODUCTO_DESACTIVAR', 'Desactivar productos'),
    ('PROVEEDOR_VER', 'Ver proveedores'),
    ('PROVEEDOR_CREAR', 'Crear proveedores'),
    ('ALMACEN_VER', 'Ver almacenes'),
    ('INVENTARIO_ENTRADA', 'Registrar entradas de inventario'),
    ('INVENTARIO_SALIDA', 'Registrar salidas de inventario'),
    ('INVENTARIO_TRANSFERENCIA', 'Registrar transferencias de inventario'),
    ('INVENTARIO_AJUSTE', 'Registrar ajustes de inventario'),
    ('STOCK_VER', 'Ver stock'),
    ('KARDEX_VER', 'Ver kardex'),
    ('REPORTE_VER', 'Ver reportes'),
    ('USUARIO_ADMIN', 'Administrar usuarios');

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.nombre = 'ADMIN';
