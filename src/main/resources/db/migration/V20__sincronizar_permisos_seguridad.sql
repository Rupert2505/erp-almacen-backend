INSERT INTO permisos (codigo, descripcion)
VALUES
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
    ('INVENTARIO_DEVOLUCION_VER', 'Ver devoluciones a proveedor'),
    ('INVENTARIO_DEVOLUCION_CREAR', 'Crear devoluciones a proveedor'),
    ('INVENTARIO_DEVOLUCION_CONFIRMAR', 'Confirmar devoluciones a proveedor'),
    ('INVENTARIO_DEVOLUCION_ANULAR', 'Anular devoluciones a proveedor'),
    ('STOCK_VER', 'Ver stock'),
    ('KARDEX_VER', 'Ver kardex'),
    ('REPORTE_VER', 'Ver reportes'),
    ('REPORTE_COSTOS_VER', 'Ver costos y valorizaciones en reportes'),
    ('REPORTE_AUDITORIA_VER', 'Ver reportes de auditoria'),
    ('REPORTE_EXPORTAR', 'Exportar reportes'),
    ('USUARIO_ADMIN', 'Administrar usuarios')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.nombre = 'ADMIN'
  AND p.codigo IN (
      'PRODUCTO_VER',
      'PRODUCTO_CREAR',
      'PRODUCTO_EDITAR',
      'PRODUCTO_DESACTIVAR',
      'PROVEEDOR_VER',
      'PROVEEDOR_CREAR',
      'ALMACEN_VER',
      'INVENTARIO_ENTRADA',
      'INVENTARIO_SALIDA',
      'INVENTARIO_TRANSFERENCIA',
      'INVENTARIO_AJUSTE',
      'INVENTARIO_DEVOLUCION_VER',
      'INVENTARIO_DEVOLUCION_CREAR',
      'INVENTARIO_DEVOLUCION_CONFIRMAR',
      'INVENTARIO_DEVOLUCION_ANULAR',
      'STOCK_VER',
      'KARDEX_VER',
      'REPORTE_VER',
      'REPORTE_COSTOS_VER',
      'REPORTE_AUDITORIA_VER',
      'REPORTE_EXPORTAR',
      'USUARIO_ADMIN'
  )
ON CONFLICT DO NOTHING;
