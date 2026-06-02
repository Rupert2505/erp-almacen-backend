package pe.com.ballena.erpalmacen.reportes.inventario.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.stock.entity.StockActualEntity;
import pe.com.ballena.erpalmacen.maestros.categorias.entity.CategoriaEntity;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.proveedores.entity.ProveedorEntity;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.EntradaProveedorResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.MovimientoInventarioReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ProductoBajoMinimoReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ResumenInventarioResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaInventarioResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.StockValorizadoResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReporteInventarioService {

    private static final EnumSet<TipoMovimientoInventario> TIPOS_SALIDA = EnumSet.of(
            TipoMovimientoInventario.SALIDA_CONSUMO,
            TipoMovimientoInventario.SALIDA_VENTA,
            TipoMovimientoInventario.SALIDA_AJUSTE,
            TipoMovimientoInventario.AJUSTE_NEGATIVO
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public ResumenInventarioResponse obtenerResumen() {
        Long totalProductos = singleResult(
                "select count(p.id) from ProductoEntity p",
                Long.class
        );
        Long productosActivos = singleResult(
                "select count(p.id) from ProductoEntity p where p.activo = true",
                Long.class
        );
        Long totalAlmacenes = singleResult(
                "select count(a.id) from AlmacenEntity a",
                Long.class
        );
        Long totalMovimientos = singleResult(
                "select count(m.id) from MovimientoInventarioEntity m",
                Long.class
        );
        Long movimientosConfirmados = contarMovimientosPorEstado(EstadoMovimientoInventario.CONFIRMADO);
        Long movimientosBorrador = contarMovimientosPorEstado(EstadoMovimientoInventario.BORRADOR);
        Long movimientosAnulados = contarMovimientosPorEstado(EstadoMovimientoInventario.ANULADO);
        BigDecimal totalStockActual = sumBigDecimal(
                "select coalesce(sum(s.cantidadActual), 0) from StockActualEntity s"
        );
        Long productosBajoMinimo = singleResult("""
                select count(s.id)
                from StockActualEntity s
                where s.producto.stockMinimo > 0
                  and s.cantidadActual < s.producto.stockMinimo
                """, Long.class);
        BigDecimal valorInventarioEstimado = sumBigDecimal("""
                select coalesce(sum(s.cantidadActual * coalesce(s.costoPromedio, 0)), 0)
                from StockActualEntity s
                """);

        return new ResumenInventarioResponse(
                totalProductos,
                productosActivos,
                totalAlmacenes,
                totalMovimientos,
                movimientosConfirmados,
                movimientosBorrador,
                movimientosAnulados,
                totalStockActual,
                productosBajoMinimo,
                valorInventarioEstimado
        );
    }

    @Transactional(readOnly = true)
    public Page<MovimientoInventarioReporteResponse> listarMovimientos(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            TipoMovimientoInventario tipoMovimiento,
            EstadoMovimientoInventario estado,
            Long almacenId,
            Long productoId,
            Pageable pageable
    ) {
        QueryParts parts = filtrosMovimientoDetalle(fechaDesde, fechaHasta, tipoMovimiento, estado, almacenId, productoId);
        String select = """
                select d
                from MovimientoDetalleEntity d
                join d.movimiento m
                join d.producto p
                left join m.almacenOrigen ao
                left join m.almacenDestino ad
                """;
        String order = " order by m.fechaMovimiento desc, m.id desc, d.id asc";
        List<MovimientoDetalleEntity> detalles = pagedQuery(select + parts.where() + order, MovimientoDetalleEntity.class, parts.params(), pageable);
        Long total = countQuery("select count(d.id) from MovimientoDetalleEntity d join d.movimiento m join d.producto p " + parts.where(), parts.params());
        return new PageImpl<>(detalles.stream().map(this::toMovimientoReporte).toList(), pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<StockValorizadoResponse> listarStockValorizado(
            Long almacenId,
            Long productoId,
            Long categoriaId,
            Boolean soloConStock,
            Pageable pageable
    ) {
        QueryParts parts = filtrosStock(almacenId, productoId, categoriaId, soloConStock, false);
        String select = "select s from StockActualEntity s join s.producto p join s.almacen a left join s.ubicacion u ";
        String order = " order by p.codigo asc, a.codigo asc, s.id asc";
        List<StockActualEntity> stock = pagedQuery(select + parts.where() + order, StockActualEntity.class, parts.params(), pageable);
        Long total = countQuery("select count(s.id) from StockActualEntity s join s.producto p join s.almacen a left join s.ubicacion u " + parts.where(), parts.params());
        return new PageImpl<>(stock.stream().map(this::toStockValorizado).toList(), pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<EntradaProveedorResponse> listarEntradasProveedor(
            Long proveedorId,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Pageable pageable
    ) {
        QueryParts parts = filtrosEntradasProveedor(proveedorId, fechaDesde, fechaHasta);
        String select = """
                select d
                from MovimientoDetalleEntity d
                join d.movimiento m
                join m.proveedor pr
                join d.producto p
                """;
        String order = " order by m.fechaMovimiento desc, m.id desc, d.id asc";
        List<MovimientoDetalleEntity> detalles = pagedQuery(select + parts.where() + order, MovimientoDetalleEntity.class, parts.params(), pageable);
        Long total = countQuery("select count(d.id) from MovimientoDetalleEntity d join d.movimiento m join m.proveedor pr join d.producto p " + parts.where(), parts.params());
        return new PageImpl<>(detalles.stream().map(this::toEntradaProveedor).toList(), pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<SalidaInventarioResponse> listarSalidas(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            TipoMovimientoInventario tipoMovimiento,
            Long almacenId,
            Long productoId,
            Pageable pageable
    ) {
        QueryParts parts = filtrosSalidas(fechaDesde, fechaHasta, tipoMovimiento, almacenId, productoId);
        String select = """
                select d
                from MovimientoDetalleEntity d
                join d.movimiento m
                join d.producto p
                join m.almacenOrigen ao
                """;
        String order = " order by m.fechaMovimiento desc, m.id desc, d.id asc";
        List<MovimientoDetalleEntity> detalles = pagedQuery(select + parts.where() + order, MovimientoDetalleEntity.class, parts.params(), pageable);
        Long total = countQuery("select count(d.id) from MovimientoDetalleEntity d join d.movimiento m join d.producto p join m.almacenOrigen ao " + parts.where(), parts.params());
        return new PageImpl<>(detalles.stream().map(this::toSalidaInventario).toList(), pageable, total);
    }

    @Transactional(readOnly = true)
    public Page<ProductoBajoMinimoReporteResponse> listarProductosBajoMinimo(
            Long almacenId,
            Long categoriaId,
            Pageable pageable
    ) {
        QueryParts parts = filtrosStock(almacenId, null, categoriaId, null, true);
        String select = "select s from StockActualEntity s join s.producto p join s.almacen a left join s.ubicacion u ";
        String order = " order by p.codigo asc, a.codigo asc, s.id asc";
        List<StockActualEntity> stock = pagedQuery(select + parts.where() + order, StockActualEntity.class, parts.params(), pageable);
        Long total = countQuery("select count(s.id) from StockActualEntity s join s.producto p join s.almacen a left join s.ubicacion u " + parts.where(), parts.params());
        return new PageImpl<>(stock.stream().map(this::toProductoBajoMinimo).toList(), pageable, total);
    }

    private QueryParts filtrosMovimientoDetalle(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            TipoMovimientoInventario tipoMovimiento,
            EstadoMovimientoInventario estado,
            Long almacenId,
            Long productoId
    ) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        addDateFilters(predicates, params, fechaDesde, fechaHasta);
        if (tipoMovimiento != null) {
            predicates.add("m.tipoMovimiento = :tipoMovimiento");
            params.put("tipoMovimiento", tipoMovimiento);
        }
        if (estado != null) {
            predicates.add("m.estado = :estado");
            params.put("estado", estado);
        }
        if (almacenId != null) {
            predicates.add("(m.almacenOrigen.id = :almacenId or m.almacenDestino.id = :almacenId)");
            params.put("almacenId", almacenId);
        }
        if (productoId != null) {
            predicates.add("p.id = :productoId");
            params.put("productoId", productoId);
        }
        return new QueryParts(toWhere(predicates), params);
    }

    private QueryParts filtrosStock(
            Long almacenId,
            Long productoId,
            Long categoriaId,
            Boolean soloConStock,
            boolean bajoMinimo
    ) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (almacenId != null) {
            predicates.add("a.id = :almacenId");
            params.put("almacenId", almacenId);
        }
        if (productoId != null) {
            predicates.add("p.id = :productoId");
            params.put("productoId", productoId);
        }
        if (categoriaId != null) {
            predicates.add("p.categoria.id = :categoriaId");
            params.put("categoriaId", categoriaId);
        }
        if (Boolean.TRUE.equals(soloConStock)) {
            predicates.add("s.cantidadActual > 0");
        }
        if (bajoMinimo) {
            predicates.add("p.stockMinimo > 0");
            predicates.add("s.cantidadActual < p.stockMinimo");
        }
        return new QueryParts(toWhere(predicates), params);
    }

    private QueryParts filtrosEntradasProveedor(Long proveedorId, LocalDateTime fechaDesde, LocalDateTime fechaHasta) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        predicates.add("m.tipoMovimiento = :tipoMovimiento");
        predicates.add("m.estado = :estado");
        params.put("tipoMovimiento", TipoMovimientoInventario.ENTRADA_COMPRA);
        params.put("estado", EstadoMovimientoInventario.CONFIRMADO);
        addDateFilters(predicates, params, fechaDesde, fechaHasta);
        if (proveedorId != null) {
            predicates.add("pr.id = :proveedorId");
            params.put("proveedorId", proveedorId);
        }
        return new QueryParts(toWhere(predicates), params);
    }

    private QueryParts filtrosSalidas(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            TipoMovimientoInventario tipoMovimiento,
            Long almacenId,
            Long productoId
    ) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        predicates.add("m.estado = :estado");
        params.put("estado", EstadoMovimientoInventario.CONFIRMADO);
        if (tipoMovimiento != null) {
            predicates.add("m.tipoMovimiento = :tipoMovimiento");
            params.put("tipoMovimiento", tipoMovimiento);
        } else {
            predicates.add("m.tipoMovimiento in :tiposSalida");
            params.put("tiposSalida", TIPOS_SALIDA);
        }
        addDateFilters(predicates, params, fechaDesde, fechaHasta);
        if (almacenId != null) {
            predicates.add("ao.id = :almacenId");
            params.put("almacenId", almacenId);
        }
        if (productoId != null) {
            predicates.add("p.id = :productoId");
            params.put("productoId", productoId);
        }
        return new QueryParts(toWhere(predicates), params);
    }

    private void addDateFilters(
            List<String> predicates,
            Map<String, Object> params,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    ) {
        if (fechaDesde != null) {
            predicates.add("m.fechaMovimiento >= :fechaDesde");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            predicates.add("m.fechaMovimiento <= :fechaHasta");
            params.put("fechaHasta", fechaHasta);
        }
    }

    private MovimientoInventarioReporteResponse toMovimientoReporte(MovimientoDetalleEntity detalle) {
        MovimientoInventarioEntity movimiento = detalle.getMovimiento();
        ProductoEntity producto = detalle.getProducto();
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        AlmacenEntity almacenDestino = movimiento.getAlmacenDestino();
        return new MovimientoInventarioReporteResponse(
                movimiento.getId(),
                movimiento.getNumero(),
                movimiento.getTipoMovimiento(),
                movimiento.getEstado(),
                movimiento.getFechaMovimiento(),
                movimiento.getDocumentoReferencia(),
                producto.getCodigo(),
                producto.getNombre(),
                almacenOrigen == null ? null : almacenOrigen.getNombre(),
                almacenDestino == null ? null : almacenDestino.getNombre(),
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                detalle.getTotalLinea()
        );
    }

    private StockValorizadoResponse toStockValorizado(StockActualEntity stock) {
        ProductoEntity producto = stock.getProducto();
        AlmacenEntity almacen = stock.getAlmacen();
        UbicacionAlmacenEntity ubicacion = stock.getUbicacion();
        BigDecimal costoPromedio = stock.getCostoPromedio();
        BigDecimal valorTotal = costoPromedio == null ? BigDecimal.ZERO : stock.getCantidadActual().multiply(costoPromedio);
        return new StockValorizadoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                almacen.getId(),
                almacen.getNombre(),
                ubicacion == null ? null : ubicacion.getId(),
                ubicacion == null ? null : ubicacion.getNombre(),
                stock.getCantidadActual(),
                costoPromedio,
                valorTotal
        );
    }

    private EntradaProveedorResponse toEntradaProveedor(MovimientoDetalleEntity detalle) {
        MovimientoInventarioEntity movimiento = detalle.getMovimiento();
        ProveedorEntity proveedor = movimiento.getProveedor();
        ProductoEntity producto = detalle.getProducto();
        return new EntradaProveedorResponse(
                proveedor.getId(),
                proveedor.getNumeroDocumento(),
                proveedor.getRazonSocial(),
                movimiento.getId(),
                movimiento.getNumero(),
                movimiento.getFechaMovimiento(),
                movimiento.getDocumentoReferencia(),
                producto.getCodigo(),
                producto.getNombre(),
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                detalle.getTotalLinea()
        );
    }

    private SalidaInventarioResponse toSalidaInventario(MovimientoDetalleEntity detalle) {
        MovimientoInventarioEntity movimiento = detalle.getMovimiento();
        ProductoEntity producto = detalle.getProducto();
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        BigDecimal costoReferencial = detalle.getCostoUnitario() == null
                ? producto.getCostoReferencial()
                : detalle.getCostoUnitario();
        return new SalidaInventarioResponse(
                movimiento.getId(),
                movimiento.getNumero(),
                movimiento.getTipoMovimiento(),
                movimiento.getFechaMovimiento(),
                almacenOrigen == null ? null : almacenOrigen.getNombre(),
                producto.getCodigo(),
                producto.getNombre(),
                detalle.getCantidad(),
                costoReferencial,
                movimiento.getObservacion()
        );
    }

    private ProductoBajoMinimoReporteResponse toProductoBajoMinimo(StockActualEntity stock) {
        ProductoEntity producto = stock.getProducto();
        CategoriaEntity categoria = producto.getCategoria();
        AlmacenEntity almacen = stock.getAlmacen();
        return new ProductoBajoMinimoReporteResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                categoria == null ? null : categoria.getNombre(),
                almacen.getId(),
                almacen.getNombre(),
                stock.getCantidadActual(),
                producto.getStockMinimo(),
                producto.getStockMinimo().subtract(stock.getCantidadActual())
        );
    }

    private Long contarMovimientosPorEstado(EstadoMovimientoInventario estado) {
        return entityManager.createQuery(
                        "select count(m.id) from MovimientoInventarioEntity m where m.estado = :estado",
                        Long.class
                )
                .setParameter("estado", estado)
                .getSingleResult();
    }

    private BigDecimal sumBigDecimal(String query) {
        BigDecimal result = entityManager.createQuery(query, BigDecimal.class).getSingleResult();
        return result == null ? BigDecimal.ZERO : result;
    }

    private <T> T singleResult(String query, Class<T> type) {
        return entityManager.createQuery(query, type).getSingleResult();
    }

    private <T> List<T> pagedQuery(String query, Class<T> type, Map<String, Object> params, Pageable pageable) {
        TypedQuery<T> typedQuery = entityManager.createQuery(query, type);
        params.forEach(typedQuery::setParameter);
        typedQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
        typedQuery.setMaxResults(pageable.getPageSize());
        return typedQuery.getResultList();
    }

    private Long countQuery(String query, Map<String, Object> params) {
        TypedQuery<Long> typedQuery = entityManager.createQuery(query, Long.class);
        params.forEach(typedQuery::setParameter);
        return typedQuery.getSingleResult();
    }

    private String toWhere(List<String> predicates) {
        return predicates.isEmpty() ? "" : " where " + String.join(" and ", predicates);
    }

    private record QueryParts(String where, Map<String, Object> params) {
    }
}
