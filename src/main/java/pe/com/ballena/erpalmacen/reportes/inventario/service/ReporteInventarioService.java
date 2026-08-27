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
import pe.com.ballena.erpalmacen.inventario.kardex.entity.KardexEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.service.ValorizacionComercialService;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.stock.entity.StockActualEntity;
import pe.com.ballena.erpalmacen.maestros.categorias.entity.CategoriaEntity;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.proveedores.entity.ProveedorEntity;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.EntradaProveedorReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.EntradaProveedorResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.EntradaProveedorResumenResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.EstadoStockReporte;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.KardexValorizadoCabeceraResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.KardexValorizadoDetalleResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.KardexValorizadoResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.KardexValorizadoResumenResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.MovimientoInventarioReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ProductoBajoMinimoReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ResumenInventarioResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaInventarioAgrupacionResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaInventarioReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaInventarioResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaInventarioResumenResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaValorizadaOsReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaValorizadaOsResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaValorizadaOsResumenResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.StockValorizadoReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.StockValorizadoResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.StockValorizadoResumenResponse;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

@Service
public class ReporteInventarioService {

    private static final int COST_SCALE = 6;

    private static final EnumSet<TipoMovimientoInventario> TIPOS_SALIDA = EnumSet.of(
            TipoMovimientoInventario.SALIDA_CONSUMO,
            TipoMovimientoInventario.SALIDA_VENTA,
            TipoMovimientoInventario.SALIDA_AJUSTE,
            TipoMovimientoInventario.AJUSTE_NEGATIVO,
            TipoMovimientoInventario.TRANSFERENCIA
    );

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public ResumenInventarioResponse obtenerResumen() {
        Long totalProductos = singleResult("select count(p.id) from ProductoEntity p", Long.class);
        Long productosActivos = singleResult("select count(p.id) from ProductoEntity p where p.activo = true", Long.class);
        Long totalAlmacenes = singleResult("select count(a.id) from AlmacenEntity a", Long.class);
        Long totalMovimientos = singleResult("select count(m.id) from MovimientoInventarioEntity m", Long.class);
        Long movimientosConfirmados = contarMovimientosPorEstado(EstadoMovimientoInventario.CONFIRMADO);
        Long movimientosBorrador = contarMovimientosPorEstado(EstadoMovimientoInventario.BORRADOR);
        Long movimientosAnulados = contarMovimientosPorEstado(EstadoMovimientoInventario.ANULADO);
        BigDecimal totalStockActual = sumBigDecimal("select coalesce(sum(s.cantidadActual), 0) from StockActualEntity s");
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
                join fetch d.movimiento m
                join fetch d.producto p
                left join fetch m.almacenOrigen ao
                left join fetch m.almacenDestino ad
                """;
        String count = """
                select count(d.id)
                from MovimientoDetalleEntity d
                join d.movimiento m
                join d.producto p
                """;
        String order = " order by m.fechaMovimiento desc, m.id desc, d.id asc";
        List<MovimientoDetalleEntity> detalles = pagedQuery(select + parts.where() + order, MovimientoDetalleEntity.class, parts.params(), pageable);
        Long total = countQuery(count + parts.where(), parts.params());
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
        return obtenerStockValorizado(
                almacenId,
                null,
                productoId,
                null,
                null,
                null,
                null,
                categoriaId,
                soloConStock,
                null,
                null,
                true,
                pageable
        ).registros();
    }

    @Transactional(readOnly = true)
    public StockValorizadoReporteResponse obtenerStockValorizado(
            Long almacenId,
            Long ubicacionId,
            Long productoId,
            Long familiaId,
            Long subfamiliaId,
            Long tipoArticuloId,
            Long marcaId,
            Long categoriaId,
            Boolean soloConStock,
            Boolean bajoMinimo,
            String texto,
            boolean verCostos,
            Pageable pageable
    ) {
        QueryParts parts = filtrosStock(
                almacenId,
                ubicacionId,
                productoId,
                familiaId,
                subfamiliaId,
                tipoArticuloId,
                marcaId,
                categoriaId,
                soloConStock,
                Boolean.TRUE.equals(bajoMinimo),
                texto
        );
        String select = stockSelect(true);
        String count = stockSelect(false).replace("select s", "select count(s.id)");
        String order = " order by p.codigo asc, a.codigo asc, s.id asc";
        List<StockActualEntity> stock = pagedQuery(select + parts.where() + order, StockActualEntity.class, parts.params(), pageable);
        Long total = countQuery(count + parts.where(), parts.params());
        Page<StockValorizadoResponse> registros = new PageImpl<>(
                stock.stream().map(s -> toStockValorizado(s, verCostos)).toList(),
                pageable,
                total
        );
        return new StockValorizadoReporteResponse(registros, calcularResumenStock(parts, verCostos));
    }

    @Transactional(readOnly = true)
    public KardexValorizadoResponse obtenerKardexValorizado(
            Long productoId,
            Long almacenId,
            Long ubicacionId,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            boolean verCostos
    ) {
        ProductoEntity producto = entityManager.find(ProductoEntity.class, productoId);
        if (producto == null) {
            throw new ResourceNotFoundException("Producto no encontrado");
        }
        AlmacenEntity almacen = almacenId == null ? null : entityManager.find(AlmacenEntity.class, almacenId);
        if (almacenId != null && almacen == null) {
            throw new ResourceNotFoundException("Almacen no encontrado");
        }
        UbicacionAlmacenEntity ubicacion = ubicacionId == null ? null : entityManager.find(UbicacionAlmacenEntity.class, ubicacionId);
        if (ubicacionId != null && ubicacion == null) {
            throw new ResourceNotFoundException("Ubicacion no encontrada");
        }

        KardexEstadoConsolidado saldoInicial = fechaDesde == null
                ? KardexEstadoConsolidado.vacio()
                : consolidarEstadoKardex(productoId, almacenId, ubicacionId, fechaDesde);

        QueryParts parts = filtrosKardex(productoId, almacenId, ubicacionId, fechaDesde, fechaHasta);
        List<KardexEntity> kardex = listQuery(
                kardexSelect() + parts.where() + " order by k.fechaMovimiento asc, k.id asc",
                KardexEntity.class,
                parts.params()
        );
        List<KardexValorizadoDetalleResponse> detalles = kardex.stream()
                .map(k -> toKardexValorizadoDetalle(k, verCostos))
                .toList();

        BigDecimal totalEntradas = kardex.stream().map(KardexEntity::getEntrada).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSalidas = kardex.stream().map(KardexEntity::getSalida).reduce(BigDecimal.ZERO, BigDecimal::add);
        KardexEstadoConsolidado saldoFinal = consolidarEstadoKardex(productoId, almacenId, ubicacionId, fechaHasta);

        KardexValorizadoCabeceraResponse cabecera = new KardexValorizadoCabeceraResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getUnidadMedida().getNombre(),
                almacen == null ? null : almacen.getId(),
                almacen == null ? null : almacen.getNombre(),
                ubicacion == null ? null : ubicacion.getId(),
                ubicacion == null ? null : ubicacion.getNombre(),
                fechaDesde,
                fechaHasta,
                saldoInicial.saldoCantidad(),
                verCostos ? saldoInicial.costoPromedio() : null,
                verCostos ? saldoInicial.valorSaldo() : null
        );
        KardexValorizadoResumenResponse resumen = new KardexValorizadoResumenResponse(
                totalEntradas,
                totalSalidas,
                saldoFinal.saldoCantidad(),
                verCostos ? saldoFinal.costoPromedio() : null,
                verCostos ? saldoFinal.valorSaldo() : null
        );
        return new KardexValorizadoResponse(cabecera, detalles, resumen);
    }

    @Transactional(readOnly = true)
    public Page<EntradaProveedorResponse> listarEntradasProveedor(
            Long proveedorId,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Pageable pageable
    ) {
        return obtenerEntradasProveedor(
                fechaDesde,
                fechaHasta,
                proveedorId,
                null,
                null,
                null,
                null,
                null,
                true,
                pageable
        ).registros();
    }

    @Transactional(readOnly = true)
    public EntradaProveedorReporteResponse obtenerEntradasProveedor(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Long proveedorId,
            Long productoId,
            Long familiaId,
            Long almacenId,
            String tipoComprobante,
            String texto,
            boolean verCostos,
            Pageable pageable
    ) {
        QueryParts parts = filtrosEntradasProveedor(proveedorId, productoId, familiaId, almacenId, tipoComprobante, fechaDesde, fechaHasta, texto);
        String select = detalleEntradaSelect(true);
        String count = detalleEntradaSelect(false).replace("select d", "select count(d.id)");
        String order = " order by m.fechaMovimiento desc, m.id desc, d.id asc";
        List<MovimientoDetalleEntity> detalles = pagedQuery(select + parts.where() + order, MovimientoDetalleEntity.class, parts.params(), pageable);
        Long total = countQuery(count + parts.where(), parts.params());
        Page<EntradaProveedorResponse> registros = new PageImpl<>(
                detalles.stream().map(d -> toEntradaProveedor(d, verCostos)).toList(),
                pageable,
                total
        );
        return new EntradaProveedorReporteResponse(registros, calcularResumenEntradas(parts, verCostos));
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
        return obtenerSalidasDetalle(
                fechaDesde,
                fechaHasta,
                almacenId,
                productoId,
                null,
                null,
                null,
                tipoMovimiento,
                null,
                EstadoMovimientoInventario.CONFIRMADO,
                null,
                null,
                true,
                pageable
        ).registros();
    }

    @Transactional(readOnly = true)
    public SalidaInventarioReporteResponse obtenerSalidasDetalle(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Long almacenId,
            Long productoId,
            Long familiaId,
            Long subfamiliaId,
            Long tipoArticuloId,
            TipoMovimientoInventario tipoMovimiento,
            Long usuarioId,
            EstadoMovimientoInventario estado,
            String texto,
            String agruparPor,
            boolean verCostos,
            Pageable pageable
    ) {
        QueryParts parts = filtrosSalidas(
                fechaDesde,
                fechaHasta,
                tipoMovimiento,
                almacenId,
                productoId,
                familiaId,
                subfamiliaId,
                tipoArticuloId,
                usuarioId,
                estado == null ? EstadoMovimientoInventario.CONFIRMADO : estado,
                texto
        );
        String select = detalleSalidaSelect(true);
        String count = detalleSalidaSelect(false).replace("select d", "select count(d.id)");
        String order = " order by m.fechaMovimiento desc, m.id desc, d.id asc";
        List<MovimientoDetalleEntity> detalles = pagedQuery(select + parts.where() + order, MovimientoDetalleEntity.class, parts.params(), pageable);
        Long total = countQuery(count + parts.where(), parts.params());
        Page<SalidaInventarioResponse> registros = new PageImpl<>(
                detalles.stream().map(d -> toSalidaInventario(d, verCostos)).toList(),
                pageable,
                total
        );
        List<MovimientoDetalleEntity> todos = listQuery(select + parts.where(), MovimientoDetalleEntity.class, parts.params());
        return new SalidaInventarioReporteResponse(
                registros,
                calcularResumenSalidas(todos, verCostos),
                agruparSalidas(todos, agruparPor, verCostos)
        );
    }

    @Transactional(readOnly = true)
    public SalidaValorizadaOsReporteResponse obtenerSalidasValorizadasPorOs(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            String ordenTrabajo,
            Long productoId,
            EstadoMovimientoInventario estado,
            boolean verCostos,
            Pageable pageable
    ) {
        QueryParts parts = filtrosSalidasValorizadasPorOs(
                fechaDesde,
                fechaHasta,
                ordenTrabajo,
                productoId,
                estado == null ? EstadoMovimientoInventario.CONFIRMADO : estado
        );
        String select = detalleSalidaVentaOsSelect(true);
        String count = detalleSalidaVentaOsSelect(false).replace("select d", "select count(d.id)");
        String order = " order by m.ordenTrabajo asc, m.fechaMovimiento desc, m.id desc, d.id asc";
        List<MovimientoDetalleEntity> detalles = pagedQuery(select + parts.where() + order, MovimientoDetalleEntity.class, parts.params(), pageable);
        Long total = countQuery(count + parts.where(), parts.params());
        Page<SalidaValorizadaOsResponse> registros = new PageImpl<>(
                detalles.stream().map(d -> toSalidaValorizadaOs(d, verCostos)).toList(),
                pageable,
                total
        );
        List<MovimientoDetalleEntity> todos = listQuery(select + parts.where(), MovimientoDetalleEntity.class, parts.params());
        return new SalidaValorizadaOsReporteResponse(
                registros,
                resumirSalidasValorizadasPorOs(todos, verCostos)
        );
    }

    @Transactional(readOnly = true)
    public Page<ProductoBajoMinimoReporteResponse> listarProductosBajoMinimo(
            Long almacenId,
            Long categoriaId,
            Pageable pageable
    ) {
        QueryParts parts = filtrosStock(almacenId, null, null, null, null, null, null, categoriaId, null, true, null);
        String select = stockSelect(true);
        String count = stockSelect(false).replace("select s", "select count(s.id)");
        String order = " order by p.codigo asc, a.codigo asc, s.id asc";
        List<StockActualEntity> stock = pagedQuery(select + parts.where() + order, StockActualEntity.class, parts.params(), pageable);
        Long total = countQuery(count + parts.where(), parts.params());
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
            Long ubicacionId,
            Long productoId,
            Long familiaId,
            Long subfamiliaId,
            Long tipoArticuloId,
            Long marcaId,
            Long categoriaId,
            Boolean soloConStock,
            boolean bajoMinimo,
            String texto
    ) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        if (almacenId != null) {
            predicates.add("a.id = :almacenId");
            params.put("almacenId", almacenId);
        }
        if (ubicacionId != null) {
            predicates.add("u.id = :ubicacionId");
            params.put("ubicacionId", ubicacionId);
        }
        if (productoId != null) {
            predicates.add("p.id = :productoId");
            params.put("productoId", productoId);
        }
        if (familiaId != null) {
            predicates.add("f.id = :familiaId");
            params.put("familiaId", familiaId);
        }
        if (subfamiliaId != null) {
            predicates.add("sf.id = :subfamiliaId");
            params.put("subfamiliaId", subfamiliaId);
        }
        if (tipoArticuloId != null) {
            predicates.add("ta.id = :tipoArticuloId");
            params.put("tipoArticuloId", tipoArticuloId);
        }
        if (marcaId != null) {
            predicates.add("ma.id = :marcaId");
            params.put("marcaId", marcaId);
        }
        if (categoriaId != null) {
            predicates.add("cat.id = :categoriaId");
            params.put("categoriaId", categoriaId);
        }
        if (Boolean.TRUE.equals(soloConStock)) {
            predicates.add("s.cantidadActual > 0");
        }
        if (bajoMinimo) {
            predicates.add("p.stockMinimo > 0");
            predicates.add("s.cantidadActual < p.stockMinimo");
        }
        addTextFilter(predicates, params, texto, "p.codigo", "p.nombre", "p.descripcion", "f.nombre", "sf.nombre", "ta.nombre", "ma.nombre", "a.nombre", "u.nombre");
        return new QueryParts(toWhere(predicates), params);
    }

    private QueryParts filtrosKardex(
            Long productoId,
            Long almacenId,
            Long ubicacionId,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta
    ) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        predicates.add("p.id = :productoId");
        params.put("productoId", productoId);
        if (almacenId != null) {
            predicates.add("a.id = :almacenId");
            params.put("almacenId", almacenId);
        }
        if (ubicacionId != null) {
            predicates.add("ub.id = :ubicacionId");
            params.put("ubicacionId", ubicacionId);
        }
        if (fechaDesde != null) {
            predicates.add("k.fechaMovimiento >= :fechaDesde");
            params.put("fechaDesde", fechaDesde);
        }
        if (fechaHasta != null) {
            predicates.add("k.fechaMovimiento < :fechaHasta");
            params.put("fechaHasta", fechaHasta);
        }
        return new QueryParts(toWhere(predicates), params);
    }

    private QueryParts filtrosEntradasProveedor(
            Long proveedorId,
            Long productoId,
            Long familiaId,
            Long almacenId,
            String tipoComprobante,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            String texto
    ) {
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
        if (productoId != null) {
            predicates.add("p.id = :productoId");
            params.put("productoId", productoId);
        }
        if (familiaId != null) {
            predicates.add("f.id = :familiaId");
            params.put("familiaId", familiaId);
        }
        if (almacenId != null) {
            predicates.add("ad.id = :almacenId");
            params.put("almacenId", almacenId);
        }
        if (tipoComprobante != null && !tipoComprobante.isBlank()) {
            predicates.add("upper(m.comprobanteTipo) = :tipoComprobante");
            params.put("tipoComprobante", tipoComprobante.trim().toUpperCase());
        }
        addTextFilter(predicates, params, texto, "m.numero", "m.documentoReferencia", "m.ordenCompra", "m.ordenCompraNumero", "m.guiaSerie", "m.guiaNumero", "m.comprobanteSerie", "m.comprobanteNumero", "pr.numeroDocumento", "pr.razonSocial", "p.codigo", "p.nombre");
        return new QueryParts(toWhere(predicates), params);
    }

    private QueryParts filtrosSalidas(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            TipoMovimientoInventario tipoMovimiento,
            Long almacenId,
            Long productoId,
            Long familiaId,
            Long subfamiliaId,
            Long tipoArticuloId,
            Long usuarioId,
            EstadoMovimientoInventario estado,
            String texto
    ) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        predicates.add("m.estado = :estado");
        params.put("estado", estado);
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
        if (familiaId != null) {
            predicates.add("f.id = :familiaId");
            params.put("familiaId", familiaId);
        }
        if (subfamiliaId != null) {
            predicates.add("sf.id = :subfamiliaId");
            params.put("subfamiliaId", subfamiliaId);
        }
        if (tipoArticuloId != null) {
            predicates.add("ta.id = :tipoArticuloId");
            params.put("tipoArticuloId", tipoArticuloId);
        }
        if (usuarioId != null) {
            predicates.add("u.id = :usuarioId");
            params.put("usuarioId", usuarioId);
        }
        addTextFilter(predicates, params, texto, "m.numero", "m.documentoReferencia", "m.motivoMovimiento", "m.observacion", "p.codigo", "p.nombre", "f.nombre", "ao.nombre");
        return new QueryParts(toWhere(predicates), params);
    }

    private QueryParts filtrosSalidasValorizadasPorOs(
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            String ordenTrabajo,
            Long productoId,
            EstadoMovimientoInventario estado
    ) {
        List<String> predicates = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        predicates.add("m.tipoMovimiento = :tipoMovimiento");
        params.put("tipoMovimiento", TipoMovimientoInventario.SALIDA_VENTA);
        predicates.add("m.estado = :estado");
        params.put("estado", estado);
        addDateFilters(predicates, params, fechaDesde, fechaHasta);
        if (ordenTrabajo != null && !ordenTrabajo.isBlank()) {
            predicates.add("lower(m.ordenTrabajo) like :ordenTrabajo");
            params.put("ordenTrabajo", "%" + ordenTrabajo.trim().toLowerCase() + "%");
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
            predicates.add("m.fechaMovimiento < :fechaHasta");
            params.put("fechaHasta", fechaHasta);
        }
    }

    private void addTextFilter(
            List<String> predicates,
            Map<String, Object> params,
            String texto,
            String... fields
    ) {
        if (texto == null || texto.isBlank()) {
            return;
        }
        StringBuilder predicate = new StringBuilder("(");
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                predicate.append(" or ");
            }
            predicate.append("lower(coalesce(").append(fields[i]).append(", '')) like :texto");
        }
        predicate.append(")");
        predicates.add(predicate.toString());
        params.put("texto", "%" + texto.trim().toLowerCase() + "%");
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

    private StockValorizadoResponse toStockValorizado(StockActualEntity stock, boolean verCostos) {
        ProductoEntity producto = stock.getProducto();
        AlmacenEntity almacen = stock.getAlmacen();
        UbicacionAlmacenEntity ubicacion = stock.getUbicacion();
        BigDecimal costoPromedio = stock.getCostoPromedio();
        BigDecimal valorTotal = valor(stock.getCantidadActual(), costoPromedio);
        return new StockValorizadoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getFamilia().getNombre(),
                producto.getSubfamilia() == null ? null : producto.getSubfamilia().getNombre(),
                producto.getTipoArticulo().getNombre(),
                producto.getMarca() == null ? null : producto.getMarca().getNombre(),
                producto.getUnidadMedida().getAbreviatura(),
                almacen.getId(),
                almacen.getNombre(),
                ubicacion == null ? null : ubicacion.getId(),
                ubicacion == null ? null : ubicacion.getNombre(),
                stock.getCantidadActual(),
                producto.getStockMinimo(),
                producto.getStockMaximo(),
                verCostos ? costoPromedio : null,
                verCostos ? valorTotal : null,
                estadoStock(stock)
        );
    }

    private EntradaProveedorResponse toEntradaProveedor(MovimientoDetalleEntity detalle, boolean verCostos) {
        MovimientoInventarioEntity movimiento = detalle.getMovimiento();
        ProveedorEntity proveedor = movimiento.getProveedor();
        ProductoEntity producto = detalle.getProducto();
        AlmacenEntity almacenDestino = movimiento.getAlmacenDestino();
        return new EntradaProveedorResponse(
                movimiento.getFechaMovimiento(),
                movimiento.getFechaPedido(),
                movimiento.getFechaRecepcion(),
                movimiento.getId(),
                movimiento.getNumero(),
                proveedor.getId(),
                proveedor.getNumeroDocumento(),
                proveedor.getRazonSocial(),
                firstNonBlank(movimiento.getOrdenCompraNumero(), movimiento.getOrdenCompra()),
                movimiento.getGuiaSerie(),
                movimiento.getGuiaNumero(),
                movimiento.getGuiaFecha(),
                movimiento.getComprobanteTipo(),
                movimiento.getComprobanteSerie(),
                movimiento.getComprobanteNumero(),
                movimiento.getComprobanteFechaEmision(),
                almacenDestino == null ? null : almacenDestino.getNombre(),
                almacenDestino == null ? null : almacenDestino.getCodigo(),
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getUnidadMedida().getNombre(),
                producto.getUnidadMedida().getAbreviatura(),
                detalle.getCantidad(),
                verCostos ? detalle.getCostoUnitario() : null,
                verCostos ? detalle.getTotalLinea() : null,
                verCostos ? coalesce(movimiento.getFlete()) : null,
                verCostos ? coalesce(movimiento.getMovilidad()) : null,
                verCostos ? coalesce(movimiento.getOtrosGastos()) : null
        );
    }

    private SalidaInventarioResponse toSalidaInventario(MovimientoDetalleEntity detalle, boolean verCostos) {
        MovimientoInventarioEntity movimiento = detalle.getMovimiento();
        ProductoEntity producto = detalle.getProducto();
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        UbicacionAlmacenEntity ubicacionOrigen = detalle.getUbicacionOrigen();
        BigDecimal costo = costoSalida(detalle);
        return new SalidaInventarioResponse(
                movimiento.getFechaMovimiento(),
                movimiento.getId(),
                movimiento.getNumero(),
                movimiento.getTipoMovimiento(),
                movimiento.getEstado(),
                almacenOrigen == null ? null : almacenOrigen.getNombre(),
                almacenOrigen == null ? null : almacenOrigen.getCodigo(),
                ubicacionOrigen == null ? null : ubicacionOrigen.getNombre(),
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getFamilia().getNombre(),
                producto.getUnidadMedida().getNombre(),
                producto.getUnidadMedida().getAbreviatura(),
                detalle.getCantidad(),
                verCostos ? costo : null,
                verCostos ? valor(detalle.getCantidad(), costo) : null,
                movimiento.getUsuario().getUsername(),
                movimiento.getMotivoMovimiento(),
                movimiento.getObservacion()
        );
    }

    private SalidaValorizadaOsResponse toSalidaValorizadaOs(MovimientoDetalleEntity detalle, boolean verCostos) {
        MovimientoInventarioEntity movimiento = detalle.getMovimiento();
        ProductoEntity producto = detalle.getProducto();
        return new SalidaValorizadaOsResponse(
                movimiento.getFechaMovimiento(),
                movimiento.getId(),
                movimiento.getNumero(),
                movimiento.getEstado(),
                movimiento.getOrdenTrabajo(),
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                detalle.getCantidad(),
                producto.getUnidadMedida().getNombre(),
                verCostos ? costoSalida(detalle) : null,
                verCostos ? costoInventarioTotal(detalle) : null,
                verCostos ? detalle.getCostoBaseComercialSinIgv() : null,
                verCostos ? detalle.getRecargoComercialPct() : null,
                verCostos ? detalle.getValorVentaUnitarioSinIgv() : null,
                verCostos ? detalle.getValorVentaTotalSinIgv() : null,
                verCostos ? detalle.getIgvUnitario() : null,
                verCostos ? detalle.getIgvTotal() : null,
                verCostos ? detalle.getPrecioVentaUnitarioConIgv() : null,
                verCostos ? detalle.getPrecioVentaTotalConIgv() : null,
                verCostos ? diferenciaComercialSinIgv(detalle) : null
        );
    }

    private KardexValorizadoDetalleResponse toKardexValorizadoDetalle(KardexEntity kardex, boolean verCostos) {
        MovimientoInventarioEntity movimiento = kardex.getMovimiento();
        BigDecimal valorEntrada = valor(kardex.getEntrada(), kardex.getCostoUnitario());
        BigDecimal valorSalida = valor(kardex.getSalida(), kardex.getCostoUnitario());
        BigDecimal valorSaldo = valor(kardex.getSaldoFinal(), kardex.getCostoPromedio());
        return new KardexValorizadoDetalleResponse(
                kardex.getFechaMovimiento(),
                movimiento.getId(),
                kardex.getNumeroMovimiento(),
                kardex.getTipoMovimiento(),
                movimiento.getEstado(),
                movimiento.getDocumentoReferencia(),
                kardex.getEntrada(),
                kardex.getSalida(),
                kardex.getSaldoAnterior(),
                kardex.getSaldoFinal(),
                verCostos ? kardex.getCostoUnitario() : null,
                verCostos ? kardex.getCostoPromedio() : null,
                verCostos ? valorEntrada : null,
                verCostos ? valorSalida : null,
                verCostos ? valorSaldo : null,
                kardex.getUsuario().getUsername()
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
                producto.getFamilia().getNombre(),
                categoria == null ? null : categoria.getNombre(),
                almacen.getId(),
                almacen.getNombre(),
                almacen.getCodigo(),
                stock.getCantidadActual(),
                producto.getStockMinimo(),
                producto.getStockMinimo().subtract(stock.getCantidadActual())
        );
    }

    private StockValorizadoResumenResponse calcularResumenStock(QueryParts parts, boolean verCostos) {
        List<StockActualEntity> stock = listQuery(stockSelect(true) + parts.where(), StockActualEntity.class, parts.params());
        long productos = stock.stream().map(s -> s.getProducto().getId()).distinct().count();
        BigDecimal unidades = stock.stream().map(StockActualEntity::getCantidadActual).reduce(BigDecimal.ZERO, BigDecimal::add);
        long bajoMinimo = stock.stream().filter(s -> estadoStock(s) == EstadoStockReporte.BAJO_MINIMO).count();
        Map<Long, BigDecimal> saldoPorProductoActivo = new HashMap<>();
        stock.stream()
                .filter(s -> s.getProducto().isActivo())
                .forEach(s -> saldoPorProductoActivo.merge(
                        s.getProducto().getId(),
                        s.getCantidadActual(),
                        BigDecimal::add
                ));
        long productosSinStockActivos = saldoPorProductoActivo.values().stream()
                .filter(cantidad -> cantidad.compareTo(BigDecimal.ZERO) <= 0)
                .count();
        BigDecimal valorTotal = stock.stream()
                .map(s -> valor(s.getCantidadActual(), s.getCostoPromedio()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new StockValorizadoResumenResponse(
                productos,
                unidades,
                verCostos ? valorTotal : null,
                bajoMinimo,
                productosSinStockActivos
        );
    }

    private EntradaProveedorResumenResponse calcularResumenEntradas(QueryParts parts, boolean verCostos) {
        List<MovimientoDetalleEntity> detalles = listQuery(detalleEntradaSelect(true) + parts.where(), MovimientoDetalleEntity.class, parts.params());
        long movimientos = detalles.stream().map(d -> d.getMovimiento().getId()).distinct().count();
        long proveedores = detalles.stream().map(d -> d.getMovimiento().getProveedor().getId()).distinct().count();
        long productos = detalles.stream().map(d -> d.getProducto().getId()).distinct().count();
        BigDecimal valorProductos = detalles.stream()
                .map(MovimientoDetalleEntity::getTotalLinea)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<MovimientoInventarioEntity> movimientosUnicos = detalles.stream()
                .map(MovimientoDetalleEntity::getMovimiento)
                .collect(LinkedHashMap<Long, MovimientoInventarioEntity>::new, (m, v) -> m.putIfAbsent(v.getId(), v), Map::putAll)
                .values()
                .stream()
                .toList();
        BigDecimal flete = movimientosUnicos.stream().map(m -> coalesce(m.getFlete())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal movilidad = movimientosUnicos.stream().map(m -> coalesce(m.getMovilidad())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal otrosGastos = movimientosUnicos.stream().map(m -> coalesce(m.getOtrosGastos())).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new EntradaProveedorResumenResponse(
                movimientos,
                proveedores,
                productos,
                verCostos ? valorProductos : null,
                verCostos ? flete : null,
                verCostos ? movilidad : null,
                verCostos ? otrosGastos : null,
                verCostos ? valorProductos.add(flete).add(movilidad).add(otrosGastos) : null
        );
    }

    private SalidaInventarioResumenResponse calcularResumenSalidas(List<MovimientoDetalleEntity> detalles, boolean verCostos) {
        long movimientos = detalles.stream().map(d -> d.getMovimiento().getId()).distinct().count();
        BigDecimal cantidad = detalles.stream().map(MovimientoDetalleEntity::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorTotal = detalles.stream()
                .map(d -> valor(d.getCantidad(), costoSalida(d)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalidaInventarioResumenResponse(
                movimientos,
                (long) detalles.size(),
                cantidad,
                verCostos ? valorTotal : null
        );
    }

    private List<SalidaValorizadaOsResumenResponse> resumirSalidasValorizadasPorOs(
            List<MovimientoDetalleEntity> detalles,
            boolean verCostos
    ) {
        Map<String, List<MovimientoDetalleEntity>> porOrden = new LinkedHashMap<>();
        detalles.stream()
                .sorted(Comparator.comparing(d -> ordenTrabajoReporte(d.getMovimiento())))
                .forEach(d -> porOrden.computeIfAbsent(ordenTrabajoReporte(d.getMovimiento()), ignored -> new ArrayList<>()).add(d));

        return porOrden.entrySet().stream()
                .map(entry -> {
                    List<MovimientoDetalleEntity> detallesOs = entry.getValue();
                    long movimientos = detallesOs.stream().map(d -> d.getMovimiento().getId()).distinct().count();
                    BigDecimal costoInventario = sumar(detallesOs, this::costoInventarioTotal);
                    BigDecimal valorComercial = sumar(detallesOs, MovimientoDetalleEntity::getValorVentaTotalSinIgv);
                    BigDecimal igv = sumar(detallesOs, MovimientoDetalleEntity::getIgvTotal);
                    BigDecimal precio = sumar(detallesOs, MovimientoDetalleEntity::getPrecioVentaTotalConIgv);
                    BigDecimal diferencia = sumar(detallesOs, this::diferenciaComercialSinIgv);
                    return new SalidaValorizadaOsResumenResponse(
                            entry.getKey(),
                            movimientos,
                            (long) detallesOs.size(),
                            verCostos ? costoInventario : null,
                            verCostos ? valorComercial : null,
                            verCostos ? igv : null,
                            verCostos ? precio : null,
                            verCostos ? diferencia : null
                    );
                })
                .toList();
    }

    private List<SalidaInventarioAgrupacionResponse> agruparSalidas(
            List<MovimientoDetalleEntity> detalles,
            String agruparPor,
            boolean verCostos
    ) {
        if (agruparPor == null || agruparPor.isBlank()) {
            return List.of();
        }
        Map<String, List<MovimientoDetalleEntity>> grupos = new LinkedHashMap<>();
        detalles.stream()
                .sorted(Comparator.comparing(d -> grupoSalida(d, agruparPor)))
                .forEach(d -> grupos.computeIfAbsent(grupoSalida(d, agruparPor), ignored -> new ArrayList<>()).add(d));
        return grupos.entrySet().stream()
                .map(entry -> {
                    BigDecimal cantidad = entry.getValue().stream().map(MovimientoDetalleEntity::getCantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal valorTotal = entry.getValue().stream()
                            .map(d -> valor(d.getCantidad(), costoSalida(d)))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new SalidaInventarioAgrupacionResponse(
                            entry.getKey(),
                            cantidad,
                            verCostos ? valorTotal : null,
                            (long) entry.getValue().size()
                    );
                })
                .toList();
    }

    private String grupoSalida(MovimientoDetalleEntity detalle, String agruparPor) {
        return switch (agruparPor.trim().toLowerCase()) {
            case "tipomovimiento", "tipo_movimiento" -> detalle.getMovimiento().getTipoMovimiento().name();
            case "familia" -> detalle.getProducto().getFamilia().getNombre();
            case "producto" -> detalle.getProducto().getCodigo() + " - " + detalle.getProducto().getNombre();
            case "almacen" -> detalle.getMovimiento().getAlmacenOrigen() == null ? "SIN ALMACEN" : detalle.getMovimiento().getAlmacenOrigen().getNombre();
            default -> "SIN AGRUPACION";
        };
    }

    private KardexEstadoConsolidado consolidarEstadoKardex(
            Long productoId,
            Long almacenId,
            Long ubicacionId,
            LocalDateTime fechaHasta
    ) {
        QueryParts parts = filtrosKardex(productoId, almacenId, ubicacionId, null, fechaHasta);
        List<KardexEntity> kardex = listQuery(
                kardexSelect() + parts.where() + " order by k.fechaMovimiento asc, k.id asc",
                KardexEntity.class,
                parts.params()
        );
        Map<KardexLineaKey, KardexEntity> ultimoPorLinea = new LinkedHashMap<>();
        kardex.forEach(k -> ultimoPorLinea.put(KardexLineaKey.from(k), k));

        BigDecimal saldo = ultimoPorLinea.values().stream()
                .map(KardexEntity::getSaldoFinal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorSaldo = ultimoPorLinea.values().stream()
                .map(k -> valor(k.getSaldoFinal(), k.getCostoPromedio()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new KardexEstadoConsolidado(
                saldo,
                costoPromedioConsolidado(ultimoPorLinea.values(), saldo, valorSaldo),
                valorSaldo
        );
    }

    private BigDecimal costoPromedioConsolidado(
            Collection<KardexEntity> lineas,
            BigDecimal saldo,
            BigDecimal valorSaldo
    ) {
        if (lineas.isEmpty()) {
            return null;
        }
        if (lineas.size() == 1) {
            return lineas.iterator().next().getCostoPromedio();
        }
        if (saldo.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return valorSaldo.divide(saldo, COST_SCALE, RoundingMode.HALF_UP);
    }

    private EstadoStockReporte estadoStock(StockActualEntity stock) {
        if (stock.getCantidadActual().compareTo(BigDecimal.ZERO) <= 0) {
            return EstadoStockReporte.SIN_STOCK;
        }
        BigDecimal stockMinimo = stock.getProducto().getStockMinimo();
        if (stockMinimo != null && stockMinimo.compareTo(BigDecimal.ZERO) > 0 && stock.getCantidadActual().compareTo(stockMinimo) < 0) {
            return EstadoStockReporte.BAJO_MINIMO;
        }
        return EstadoStockReporte.NORMAL;
    }

    private String stockSelect(boolean fetch) {
        if (fetch) {
            return """
                    select s
                    from StockActualEntity s
                    join fetch s.producto p
                    join fetch p.familia f
                    join fetch p.tipoArticulo ta
                    join fetch p.unidadMedida um
                    join fetch s.almacen a
                    left join fetch s.ubicacion u
                    left join fetch p.subfamilia sf
                    left join fetch p.marca ma
                    left join fetch p.categoria cat
                    """;
        }
        return """
                select s
                from StockActualEntity s
                join s.producto p
                join p.familia f
                join p.tipoArticulo ta
                join p.unidadMedida um
                join s.almacen a
                left join s.ubicacion u
                left join p.subfamilia sf
                left join p.marca ma
                left join p.categoria cat
                """;
    }

    private String detalleEntradaSelect(boolean fetch) {
        if (fetch) {
            return """
                    select d
                    from MovimientoDetalleEntity d
                    join fetch d.movimiento m
                    join fetch m.proveedor pr
                    join fetch d.producto p
                    join fetch p.familia f
                    join fetch p.unidadMedida um
                    left join fetch m.almacenDestino ad
                    """;
        }
        return """
                select d
                from MovimientoDetalleEntity d
                join d.movimiento m
                join m.proveedor pr
                join d.producto p
                join p.familia f
                join p.unidadMedida um
                left join m.almacenDestino ad
                """;
    }

    private String detalleSalidaSelect(boolean fetch) {
        if (fetch) {
            return """
                    select d
                    from MovimientoDetalleEntity d
                    join fetch d.movimiento m
                    join fetch m.usuario u
                    join fetch d.producto p
                    join fetch p.familia f
                    join fetch p.tipoArticulo ta
                    join fetch p.unidadMedida um
                    join fetch m.almacenOrigen ao
                    left join fetch d.ubicacionOrigen uo
                    left join fetch p.subfamilia sf
                    """;
        }
        return """
                select d
                from MovimientoDetalleEntity d
                join d.movimiento m
                join m.usuario u
                join d.producto p
                join p.familia f
                join p.tipoArticulo ta
                join p.unidadMedida um
                join m.almacenOrigen ao
                left join d.ubicacionOrigen uo
                left join p.subfamilia sf
                """;
    }

    private String detalleSalidaVentaOsSelect(boolean fetch) {
        if (fetch) {
            return """
                    select d
                    from MovimientoDetalleEntity d
                    join fetch d.movimiento m
                    join fetch d.producto p
                    join fetch p.unidadMedida um
                    """;
        }
        return """
                select d
                from MovimientoDetalleEntity d
                join d.movimiento m
                join d.producto p
                join p.unidadMedida um
                """;
    }

    private String kardexSelect() {
        return """
                select k
                from KardexEntity k
                join fetch k.movimiento m
                join fetch k.movimientoDetalle d
                join fetch k.producto p
                join fetch p.unidadMedida um
                join fetch k.almacen a
                join fetch k.usuario u
                left join fetch k.ubicacion ub
                """;
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
        if (pageable.isPaged()) {
            typedQuery.setFirstResult(Math.toIntExact(pageable.getOffset()));
            typedQuery.setMaxResults(pageable.getPageSize());
        }
        return typedQuery.getResultList();
    }

    private <T> List<T> listQuery(String query, Class<T> type, Map<String, Object> params) {
        return listQuery(query, type, params, null);
    }

    private <T> List<T> listQuery(String query, Class<T> type, Map<String, Object> params, Integer maxResults) {
        TypedQuery<T> typedQuery = entityManager.createQuery(query, type);
        params.forEach(typedQuery::setParameter);
        if (maxResults != null) {
            typedQuery.setMaxResults(maxResults);
        }
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

    private BigDecimal valor(BigDecimal cantidad, BigDecimal costo) {
        if (cantidad == null || costo == null) {
            return BigDecimal.ZERO;
        }
        return cantidad.multiply(costo);
    }

    private BigDecimal costoInventarioTotal(MovimientoDetalleEntity detalle) {
        BigDecimal costo = costoSalida(detalle);
        if (detalle.getCantidad() == null || costo == null) {
            return null;
        }
        return dinero(detalle.getCantidad().multiply(costo));
    }

    private BigDecimal diferenciaComercialSinIgv(MovimientoDetalleEntity detalle) {
        BigDecimal valorComercial = detalle.getValorVentaTotalSinIgv();
        BigDecimal costoInventario = costoInventarioTotal(detalle);
        if (valorComercial == null || costoInventario == null) {
            return null;
        }
        return dinero(valorComercial.subtract(costoInventario));
    }

    private BigDecimal sumar(
            List<MovimientoDetalleEntity> detalles,
            Function<MovimientoDetalleEntity, BigDecimal> mapper
    ) {
        return dinero(detalles.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal dinero(BigDecimal value) {
        return value == null ? null : value.setScale(ValorizacionComercialService.MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private String ordenTrabajoReporte(MovimientoInventarioEntity movimiento) {
        String orden = movimiento.getOrdenTrabajo();
        return orden == null || orden.isBlank() ? "SIN OS" : orden;
    }

    private BigDecimal costoSalida(MovimientoDetalleEntity detalle) {
        List<KardexEntity> kardexSalida = entityManager.createQuery("""
                        select k
                        from KardexEntity k
                        where k.movimientoDetalle.id = :detalleId
                          and k.salida > 0
                        order by k.id asc
                        """, KardexEntity.class)
                .setParameter("detalleId", detalle.getId())
                .setMaxResults(1)
                .getResultList();
        if (!kardexSalida.isEmpty()) {
            return kardexSalida.get(0).getCostoUnitario();
        }
        return detalle.getCostoUnitario();
    }

    private BigDecimal coalesce(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private record QueryParts(String where, Map<String, Object> params) {
    }

    private record KardexLineaKey(Long productoId, Long almacenId, Long ubicacionId) {
        static KardexLineaKey from(KardexEntity kardex) {
            return new KardexLineaKey(
                    kardex.getProducto().getId(),
                    kardex.getAlmacen().getId(),
                    kardex.getUbicacion() == null ? null : kardex.getUbicacion().getId()
            );
        }
    }

    private record KardexEstadoConsolidado(
            BigDecimal saldoCantidad,
            BigDecimal costoPromedio,
            BigDecimal valorSaldo
    ) {
        static KardexEstadoConsolidado vacio() {
            return new KardexEstadoConsolidado(BigDecimal.ZERO, null, BigDecimal.ZERO);
        }
    }
}
