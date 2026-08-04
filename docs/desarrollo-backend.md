# Desarrollo Backend ERP Almacen

Documento tecnico del estado actual del backend del ERP Almacen.

Ultima actualizacion: 2026-07-24.

## Resumen

El backend implementa una base ERP modular para almacen e inventario con:

- Seguridad JWT.
- Usuarios, roles y permisos.
- Maestros de productos y catalogos relacionados.
- Proveedores.
- Personal.
- Almacenes y ubicaciones.
- Movimientos de inventario.
- Stock actual.
- Kardex.
- Reportes basicos.
- Concurrencia para operaciones criticas.
- Flyway como mecanismo unico de cambios de base de datos.
- Pruebas automatizadas de servicios, seguridad, errores, inventario y concurrencia.

El sistema no usa `ddl-auto=update`. Los perfiles `dev` y `prod` usan:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

## Stack

- Java 21.
- Spring Boot 4.0.6.
- Maven.
- PostgreSQL.
- Spring Data JPA.
- Spring Security.
- JWT con JJWT.
- Flyway.
- Bean Validation.
- Actuator.
- HikariCP.

## Paquete Base

```text
pe.com.ballena.erpalmacen
```

## Arquitectura

La organizacion sigue una arquitectura modular:

```text
src/main/java/pe/com/ballena/erpalmacen
  auth
  usuarios
  maestros
    categorias
    familias
    subfamilias
    marcas
    unidadesmedida
    tiposarticulo
    productos
    proveedores
    personal
  almacen
    almacenes
    ubicaciones
  inventario
    movimientos
    stock
    kardex
    service
    shared
  reportes
  shared
    config
    exception
    response
    security
```

Reglas de arquitectura aplicadas:

- Los controllers no contienen logica de negocio.
- Los services concentran reglas de negocio y transacciones.
- Los repositories solo acceden a datos.
- No se devuelven entidades JPA directamente al frontend.
- Se usan DTOs para request y response.
- Se usan excepciones de negocio y manejo global de errores.
- Los procesos criticos de inventario son transaccionales.
- Las operaciones de stock usan bloqueos pesimistas donde corresponde.

## Configuracion

Archivos principales:

- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`
- `src/main/resources/application-prod.yaml`

Perfil `dev`:

- PostgreSQL local.
- Flyway activo.
- `ddl-auto=validate`.
- HikariCP configurado para desarrollo.
- SQL Hibernate en `debug`.

Perfil `prod`:

- Configuracion por variables de entorno.
- Flyway activo.
- `ddl-auto=validate`.
- HikariCP configurable.
- CORS por variable `CORS_ALLOWED_ORIGINS`.
- JWT secret obligatorio por variable `JWT_SECRET`.

La guia operativa de produccion esta en:

- `docs/operacion-produccion.md`

## Migraciones Flyway

Las migraciones actuales son:

| Version | Archivo | Proposito |
| --- | --- | --- |
| V1 | `V1__crear_tabla_control_sistema.sql` | Tabla inicial `sistema_info`. |
| V2 | `V2__crear_modelo_seguridad.sql` | Usuarios, roles, permisos y relaciones. |
| V3 | `V3__crear_usuario_admin_inicial.sql` | Usuario administrador inicial. |
| V4 | `V4__crear_maestros_base.sql` | Categorias, marcas, unidades, proveedores, almacenes, ubicaciones y productos base. |
| V5 | `V5__crear_inventario_stock_kardex.sql` | Movimientos, detalle, stock actual y kardex. |
| V6 | `V6__crear_familias_subfamilias_y_ajustar_productos.sql` | Familias, subfamilias y ajuste funcional de productos. |
| V7 | `V7__crear_tipos_articulo_y_ajustar_productos.sql` | Tipos de articulo y relacion obligatoria con producto. |
| V8 | `V8__ajustar_movimientos_datos_entrada_y_decimales.sql` | Datos adicionales y precision decimal inicial en movimientos. |
| V9 | `V9__ajustar_movimientos_documentos_entrada_produccion.sql` | Documentos de entrada para uso productivo. |
| V10 | `V10__ajustar_movimientos_campos_operativos_por_tipo.sql` | Campos operativos por tipo de movimiento. |
| V11 | `V11__agregar_cancelacion_movimientos_borrador.sql` | Cancelacion de movimientos en borrador. |
| V12 | `V12__agregar_prefijo_familia_y_correlativo_productos.sql` | Prefijo de familia y correlativos de producto. |
| V13 | `V13__ajustar_correlativos_producto_con_guion.sql` | Codigo de producto con formato `PREFIJO-CORRELATIVO`. |
| V14 | `V14__crear_catalogo_personal.sql` | Catalogo de personal. |

Reglas:

- No modificar migraciones ya ejecutadas.
- Crear siempre una nueva migracion versionada para cambios de estructura.
- No crear tablas manualmente fuera de Flyway.
- No usar `ddl-auto=update` en ambientes controlados.

## Seguridad

### Autenticacion

Endpoints:

- `POST /api/auth/login`
- `GET /api/auth/me`

El login devuelve:

- `accessToken`
- `tokenType`
- `expiresIn`
- datos basicos del usuario
- roles
- permisos

La autenticacion usa:

- Spring Security.
- BCrypt para passwords.
- JWT stateless.
- Filtro JWT.
- `CustomUserDetailsService`.

### Modelo de Seguridad

Tablas:

- `usuarios`
- `roles`
- `permisos`
- `usuario_roles`
- `rol_permisos`

Roles iniciales:

- `ADMIN`
- `JEFE_ALMACEN`
- `OPERADOR_ALMACEN`
- `CONSULTA`

Permisos principales:

- `PRODUCTO_VER`
- `PRODUCTO_CREAR`
- `PRODUCTO_EDITAR`
- `PRODUCTO_DESACTIVAR`
- `PROVEEDOR_VER`
- `PROVEEDOR_CREAR`
- `ALMACEN_VER`
- `INVENTARIO_ENTRADA`
- `INVENTARIO_SALIDA`
- `INVENTARIO_TRANSFERENCIA`
- `INVENTARIO_AJUSTE`
- `STOCK_VER`
- `KARDEX_VER`
- `REPORTE_VER`
- `USUARIO_ADMIN`

### Administracion

Endpoints administrativos:

- `/api/usuarios`
- `/api/roles`
- `/api/permisos`

Reglas:

- No se expone password.
- Usuarios se activan/desactivan, no se eliminan fisicamente.
- Reset de password usa BCrypt.
- Cambio de roles valida existencia de roles.

## Maestros

### Categorias

Mantiene compatibilidad legacy. Producto ya no depende funcionalmente de categoria para el MVP.

Endpoints:

- `GET /api/categorias`
- `GET /api/categorias/{id}`
- `POST /api/categorias`
- `PUT /api/categorias/{id}`
- `PATCH /api/categorias/{id}/activar`
- `PATCH /api/categorias/{id}/desactivar`

### Familias y Subfamilias

Producto usa:

- Familia obligatoria.
- Subfamilia opcional.

Familia tiene `prefijo`, usado para generar codigo de producto.

Endpoints familias:

- `GET /api/familias`
- `GET /api/familias/activas`
- `GET /api/familias/{id}`
- `POST /api/familias`
- `PUT /api/familias/{id}`
- `PATCH /api/familias/{id}/activar`
- `PATCH /api/familias/{id}/desactivar`

Endpoints subfamilias:

- `GET /api/subfamilias`
- `GET /api/subfamilias/activas`
- `GET /api/subfamilias/por-familia/{familiaId}`
- `GET /api/subfamilias/{id}`
- `POST /api/subfamilias`
- `PUT /api/subfamilias/{id}`
- `PATCH /api/subfamilias/{id}/activar`
- `PATCH /api/subfamilias/{id}/desactivar`

### Marcas

Marca es opcional en producto.

Endpoints:

- `GET /api/marcas`
- `GET /api/marcas/{id}`
- `POST /api/marcas`
- `PUT /api/marcas/{id}`
- `PATCH /api/marcas/{id}/activar`
- `PATCH /api/marcas/{id}/desactivar`

### Unidades de Medida

Unidad de medida es obligatoria en producto.

Endpoints:

- `GET /api/unidades-medida`
- `GET /api/unidades-medida/{id}`
- `POST /api/unidades-medida`
- `PUT /api/unidades-medida/{id}`
- `PATCH /api/unidades-medida/{id}/activar`
- `PATCH /api/unidades-medida/{id}/desactivar`

### Tipos de Articulo

Tipo de articulo es obligatorio en producto.

Valores iniciales:

- `ALMACEN`
- `AREA_SERVICIOS`
- `CONSUMIDOS`
- `EPP_PERSONAL`
- `HERRAMIENTA_PERSONAL`
- `MAQUINA_PERSONAL`
- `NO_TERMINADO`
- `VENTA`
- `VENTA_ALMACEN`

Endpoints:

- `GET /api/tipos-articulo`
- `GET /api/tipos-articulo/activos`
- `GET /api/tipos-articulo/{id}`
- `POST /api/tipos-articulo`
- `PUT /api/tipos-articulo/{id}`
- `PATCH /api/tipos-articulo/{id}/activar`
- `PATCH /api/tipos-articulo/{id}/desactivar`

### Proveedores

Endpoints:

- `GET /api/proveedores`
- `GET /api/proveedores/{id}`
- `POST /api/proveedores`
- `PUT /api/proveedores/{id}`
- `PATCH /api/proveedores/{id}/activar`
- `PATCH /api/proveedores/{id}/desactivar`

Busqueda:

- Numero de documento.
- Razon social.
- Nombre comercial.

### Personal

Catalogo administrativo de personal.

Campos:

- `tipoDocumento`
- `numeroDocumento`
- `nombres`
- `apellidos`
- `cargo`
- `area`
- `telefono`
- `email`
- `activo`

Endpoints:

- `GET /api/personal`
- `GET /api/personal/{id}`
- `POST /api/personal`
- `PUT /api/personal/{id}`
- `PATCH /api/personal/{id}/activar`
- `PATCH /api/personal/{id}/desactivar`

Permiso:

- `USUARIO_ADMIN` o `ADMIN`.

## Productos

Producto actualmente usa:

- Codigo autogenerado.
- Tipo de articulo obligatorio.
- Familia obligatoria.
- Subfamilia opcional.
- Marca opcional.
- Unidad de medida obligatoria.
- Costo referencial.
- Stock minimo.
- Stock maximo.
- Controla lote.
- Controla serie.

### Codigo de Producto

El codigo se genera desde backend con:

```text
PREFIJO-CORRELATIVO
```

Ejemplo:

```text
REP-92
REP-1002
```

Reglas:

- El prefijo viene de `familias.prefijo`.
- El correlativo se gestiona por familia en `familia_correlativos`.
- El codigo es unico.
- El codigo no se puede modificar al actualizar producto.
- El backend ignora cualquier codigo enviado al crear producto.
- Existe endpoint para consultar el siguiente codigo sugerido.

Endpoints:

- `GET /api/productos`
- `GET /api/productos/{id}`
- `GET /api/productos/siguiente-codigo?familiaId={id}`
- `POST /api/productos`
- `PUT /api/productos/{id}`
- `PATCH /api/productos/{id}/activar`
- `PATCH /api/productos/{id}/desactivar`

Busqueda:

- Codigo.
- Nombre.
- Descripcion.

Orden por defecto:

- `creadoEn DESC`
- `id DESC`

Si el cliente envia un `sort` explicito, se respeta.

## Almacen

### Almacenes

Endpoints:

- `GET /api/almacenes`
- `GET /api/almacenes/{id}`
- `POST /api/almacenes`
- `PUT /api/almacenes/{id}`
- `PATCH /api/almacenes/{id}/activar`
- `PATCH /api/almacenes/{id}/desactivar`

### Ubicaciones

Endpoints:

- `GET /api/ubicaciones`
- `GET /api/ubicaciones/{id}`
- `GET /api/almacenes/{almacenId}/ubicaciones`
- `POST /api/ubicaciones`
- `PUT /api/ubicaciones/{id}`
- `PATCH /api/ubicaciones/{id}/activar`
- `PATCH /api/ubicaciones/{id}/desactivar`

Reglas:

- La ubicacion pertenece a un almacen.
- El codigo de ubicacion es unico dentro del almacen.
- Se valida que el almacen exista y este activo para crear o actualizar.

## Inventario

### Tipos de Movimiento

- `ENTRADA_COMPRA`
- `ENTRADA_AJUSTE`
- `SALIDA_CONSUMO`
- `SALIDA_VENTA`
- `SALIDA_AJUSTE`
- `TRANSFERENCIA`
- `AJUSTE_POSITIVO`
- `AJUSTE_NEGATIVO`

### Estados

- `BORRADOR`
- `CONFIRMADO`
- `ANULADO`
- `CANCELADO`

Flujos validos:

```text
BORRADOR -> CONFIRMADO -> ANULADO
BORRADOR -> CANCELADO
```

No se permite:

- `CONFIRMADO -> CANCELADO`
- `ANULADO -> CANCELADO`
- `CANCELADO -> CONFIRMADO`
- `CANCELADO -> ANULADO`
- `CANCELADO -> BORRADOR`

### Movimientos

Endpoints:

- `POST /api/inventario/movimientos`
- `GET /api/inventario/movimientos`
- `GET /api/inventario/movimientos/{id}`
- `POST /api/inventario/movimientos/{id}/confirmar`
- `POST /api/inventario/movimientos/{id}/anular`
- `PATCH /api/inventario/movimientos/{id}/cancelar`

Soporta:

- Crear en borrador.
- Crear y confirmar directamente con `confirmar=true`.
- Confirmar borrador.
- Anular confirmado.
- Cancelar borrador.
- Listar con paginacion y filtros.
- Orden por defecto del mas reciente al mas antiguo.

### Reglas de Confirmacion

Reglas generales:

- Solo se confirma `BORRADOR`.
- No se confirma `CONFIRMADO`, `ANULADO` ni `CANCELADO`.
- Debe existir al menos un detalle.
- Cantidades deben ser mayores a cero.
- Productos deben existir y estar activos.
- Almacenes y ubicaciones se validan segun el tipo.
- La operacion es transaccional.
- Si falla una linea, se revierte todo.
- No se permite stock negativo.

Reglas por tipo:

- `ENTRADA_COMPRA`: requiere proveedor, almacen destino y fecha recepcion; aumenta stock.
- `ENTRADA_AJUSTE`: requiere almacen destino, motivo y observacion; aumenta stock.
- `SALIDA_CONSUMO`: requiere almacen origen, motivo y observacion; disminuye stock.
- `SALIDA_VENTA`: requiere almacen origen; disminuye stock.
- `SALIDA_AJUSTE`: requiere almacen origen, motivo y observacion; disminuye stock.
- `TRANSFERENCIA`: requiere origen y destino diferentes; descuenta origen y suma destino.
- `AJUSTE_POSITIVO`: requiere almacen destino, motivo y observacion; aumenta stock.
- `AJUSTE_NEGATIVO`: requiere almacen origen, motivo y observacion; disminuye stock.

### Documentos de Entrada

Para `ENTRADA_COMPRA`, se soportan campos documentarios:

- Guia: serie, numero, fecha.
- Comprobante: tipo, serie, numero, fecha de emision.
- Orden de compra.
- Fecha de pedido.
- Fecha de recepcion.
- Flete.
- Movilidad.
- Otros gastos.
- Observacion documentaria.

Reglas:

- `comprobanteTipo` permitido: `FACTURA`, `BOLETA`, `OTRO`.
- Si se informa guia serie, debe informarse guia numero y viceversa.
- Si se informa comprobante serie, debe informarse comprobante numero y viceversa.
- Los costos adicionales no se prorratean al costo promedio por ahora.

### Stock

Tabla:

- `stock_actual`

Clave logica:

- Producto.
- Almacen.
- Ubicacion opcional.

Reglas:

- Entradas crean stock si no existe.
- Salidas no crean stock; requieren stock suficiente.
- Transferencias bloquean origen y destino.
- Cantidad actual nunca debe quedar negativa.
- Se usa bloqueo pesimista para operaciones criticas.

Endpoints:

- `GET /api/inventario/stock`
- `GET /api/inventario/stock/producto/{productoId}`
- `GET /api/inventario/stock/almacen/{almacenId}`
- `GET /api/inventario/stock/bajo-minimo`

### Kardex

Tabla:

- `kardex`

Registra:

- Movimiento.
- Detalle.
- Producto.
- Almacen.
- Ubicacion.
- Entrada.
- Salida.
- Saldo anterior.
- Saldo final.
- Costo unitario.
- Costo promedio.
- Usuario.

Endpoints:

- `GET /api/inventario/kardex`
- `GET /api/inventario/kardex/producto/{productoId}`
- `GET /api/inventario/kardex/producto/{productoId}/almacen/{almacenId}`
- `GET /api/inventario/kardex/movimiento/{movimientoId}`

### Anulacion

Reglas:

- Solo se anula `CONFIRMADO`.
- La anulacion genera kardex reverso.
- La anulacion actualiza stock en sentido contrario.
- No elimina ni modifica historico de kardex previo.
- Es transaccional.

### Cancelacion

Reglas:

- Solo se cancela `BORRADOR`.
- No afecta stock.
- No genera kardex.
- Guarda motivo, usuario y fecha.
- Un movimiento cancelado no puede confirmarse ni anularse.

## Reportes

Endpoints:

- `GET /api/reportes/inventario/resumen`
- `GET /api/reportes/inventario/movimientos`
- `GET /api/reportes/inventario/stock-valorizado`
- `GET /api/reportes/inventario/entradas-proveedor`
- `GET /api/reportes/inventario/salidas`
- `GET /api/reportes/inventario/productos-bajo-minimo`

Reglas:

- Solo lectura.
- Paginacion donde corresponde.
- Borradores no afectan stock real.
- Stock valorizado usa `cantidadActual * costoPromedio`.
- Si `costoPromedio` es null, el valor se considera cero.

## Estandar de Texto

El backend normaliza campos de negocio al guardar.

Se guardan en mayusculas:

- Codigos.
- Prefijos.
- Nombres.
- Descripciones.
- Razon social.
- Nombre comercial.
- Direcciones.
- Responsables.
- Cargos.
- Areas.

Se guardan en minusculas:

- Emails.

No se transforman:

- Telefonos.
- Passwords.
- Tokens.
- Campos numericos.

La normalizacion se aplica en services al crear o actualizar registros. No se realizo una migracion masiva para cambiar data historica.

## Concurrencia y Multiusuario

Se reforzo el soporte multiusuario en inventario:

- HikariCP configurado por ambiente.
- Operaciones criticas con transacciones.
- Bloqueos pesimistas para movimientos y stock.
- Manejo de conflictos de concurrencia.
- Pruebas de salidas concurrentes.
- Pruebas de confirmacion concurrente.
- Pruebas de transferencia concurrente.

Operaciones protegidas:

- Confirmar movimiento.
- Guardar y confirmar directamente.
- Anular movimiento.
- Cancelar movimiento.
- Actualizar stock.

## Respuestas y Errores

### Respuesta Estandar

Se usa `ApiResponse<T>` para respuestas de exito.

### Errores

Se usa `ApiErrorResponse` y `GlobalExceptionHandler`.

Excepciones principales:

- `BusinessException`
- `ResourceNotFoundException`

Casos manejados:

- Validacion Bean Validation.
- Reglas de negocio.
- Recursos no encontrados.
- Acceso no autorizado.
- Acceso prohibido.
- Conflictos de concurrencia.
- Errores generales controlados.

## Pruebas Automatizadas

Archivos principales:

- `ErpAlmacenApplicationTests`
- `MaestrosServiceIntegrationTest`
- `MovimientoInventarioServiceTest`
- `MovimientoInventarioConcurrencyTest`
- `InventarioConsultaReporteServiceTest`
- `SecurityEndpointIntegrationTest`
- `GlobalExceptionHandlerTest`

Cobertura funcional:

- Arranque de contexto.
- Maestros.
- Productos.
- Codigo autogenerado por familia.
- Orden de productos.
- Personal.
- Normalizacion a mayusculas.
- Proveedores.
- Almacenes y ubicaciones.
- Seguridad y permisos.
- Login JWT.
- Inventario.
- Stock.
- Kardex.
- Reportes.
- Anulacion.
- Cancelacion.
- Concurrencia.
- Manejo global de errores.

Ultima validacion ejecutada:

```powershell
.\mvnw.cmd clean test
```

Resultado:

```text
Tests run: 72, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Tambien se realizo validacion HTTP con perfil `dev`:

- `/actuator/health`: `UP`.
- Login admin: correcto.
- `/api/familias`: correcto.
- `/api/productos`: correcto.
- `/api/personal`: correcto.

## Comandos Utiles

Ejecutar pruebas:

```powershell
.\mvnw.cmd clean test
```

Levantar backend dev:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Health:

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

Login:

```powershell
Invoke-RestMethod `
  -Uri http://localhost:8080/api/auth/login `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"admin","password":"Admin123*"}'
```

## Consideraciones para Frontend

Cambios backend que el frontend debe considerar:

- Existe nuevo modulo `Personal`.
- Productos se ordenan por defecto del mas reciente al mas antiguo.
- Codigo de producto es autogenerado y no editable.
- Familia requiere prefijo.
- Producto puede consultar siguiente codigo por familia.
- Campos de negocio se guardan en mayusculas desde backend.
- Emails se guardan en minusculas.

## Pendientes Recomendados

Antes de despliegue productivo:

- Ejecutar pruebas con datos reales migrados en staging.
- Hacer backup y restauracion de prueba de PostgreSQL.
- Revisar `JWT_SECRET` y variables productivas.
- Medir tiempos de reportes con volumen real.
- Evaluar indices adicionales despues de pruebas de carga.
- Definir si se migrara data historica a mayusculas.
- Completar documentacion de frontend cuando se cierren las fases visuales.

