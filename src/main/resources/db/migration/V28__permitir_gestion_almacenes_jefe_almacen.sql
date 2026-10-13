INSERT INTO permisos (codigo, descripcion)
VALUES
    ('ALMACEN_CREAR', 'Crear almacenes y ubicaciones'),
    ('ALMACEN_EDITAR', 'Editar almacenes y ubicaciones'),
    ('ALMACEN_DESACTIVAR', 'Activar y desactivar almacenes y ubicaciones')
ON CONFLICT (codigo) DO UPDATE
SET descripcion = EXCLUDED.descripcion,
    actualizado_en = CURRENT_TIMESTAMP;

INSERT INTO rol_permisos (rol_id, permiso_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permisos p
WHERE r.nombre IN ('ADMIN', 'JEFE_ALMACEN')
  AND p.codigo IN (
      'ALMACEN_VER',
      'ALMACEN_CREAR',
      'ALMACEN_EDITAR',
      'ALMACEN_DESACTIVAR'
  )
ON CONFLICT DO NOTHING;
