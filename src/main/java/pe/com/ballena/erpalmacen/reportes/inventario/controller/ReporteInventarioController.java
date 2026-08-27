package pe.com.ballena.erpalmacen.reportes.inventario.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ArchivoReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.EntradaProveedorReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.EntradaProveedorResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.KardexValorizadoResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.MovimientoInventarioReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ProductoBajoMinimoReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ResumenInventarioResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaInventarioReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaInventarioResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaValorizadaOsReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.StockValorizadoReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.StockValorizadoResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.excel.ExcelReportService;
import pe.com.ballena.erpalmacen.reportes.inventario.pdf.MovimientoPdfService;
import pe.com.ballena.erpalmacen.reportes.inventario.service.ReporteInventarioService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reportes/inventario")
@PreAuthorize("hasAuthority('REPORTE_VER') or hasRole('ADMIN')")
public class ReporteInventarioController {

    private static final Pageable EXPORT_PAGEABLE = Pageable.unpaged();
    private static final MediaType EXCEL_MEDIA_TYPE = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final ReporteInventarioService reporteInventarioService;
    private final ExcelReportService excelReportService;
    private final MovimientoPdfService movimientoPdfService;

    public ReporteInventarioController(
            ReporteInventarioService reporteInventarioService,
            ExcelReportService excelReportService,
            MovimientoPdfService movimientoPdfService
    ) {
        this.reporteInventarioService = reporteInventarioService;
        this.excelReportService = excelReportService;
        this.movimientoPdfService = movimientoPdfService;
    }

    @GetMapping("/resumen")
    public ResponseEntity<ApiResponse<ResumenInventarioResponse>> obtenerResumen() {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.obtenerResumen()));
    }

    @GetMapping("/movimientos")
    public ResponseEntity<ApiResponse<Page<MovimientoInventarioReporteResponse>>> listarMovimientos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) TipoMovimientoInventario tipoMovimiento,
            @RequestParam(required = false) EstadoMovimientoInventario estado,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.listarMovimientos(
                fechaDesde,
                fechaHasta,
                tipoMovimiento,
                estado,
                almacenId,
                productoId,
                pageable
        )));
    }

    @GetMapping("/stock-valorizado")
    public ResponseEntity<ApiResponse<StockValorizadoReporteResponse>> listarStockValorizado(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long ubicacionId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Long subfamiliaId,
            @RequestParam(required = false) Long tipoArticuloId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean soloConStock,
            @RequestParam(required = false) Boolean bajoMinimo,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.obtenerStockValorizado(
                almacenId,
                ubicacionId,
                productoId,
                familiaId,
                subfamiliaId,
                tipoArticuloId,
                marcaId,
                categoriaId,
                soloConStock,
                bajoMinimo,
                texto,
                puedeVerCostos(authentication),
                pageable
        )));
    }

    @GetMapping("/kardex-valorizado")
    public ResponseEntity<ApiResponse<KardexValorizadoResponse>> obtenerKardexValorizado(
            @RequestParam Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long ubicacionId,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.obtenerKardexValorizado(
                productoId,
                almacenId,
                ubicacionId,
                parseFechaDesde(fechaDesde),
                parseFechaHastaExclusiva(fechaHasta),
                puedeVerCostos(authentication)
        )));
    }

    @GetMapping("/entradas-proveedor")
    public ResponseEntity<ApiResponse<EntradaProveedorReporteResponse>> listarEntradasProveedor(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) String tipoComprobante,
            @RequestParam(required = false) String texto,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.obtenerEntradasProveedor(
                parseFechaDesde(fechaDesde),
                parseFechaHastaExclusiva(fechaHasta),
                proveedorId,
                productoId,
                familiaId,
                almacenId,
                tipoComprobante,
                texto,
                puedeVerCostos(authentication),
                pageable
        )));
    }

    @GetMapping("/salidas")
    public ResponseEntity<ApiResponse<Page<SalidaInventarioResponse>>> listarSalidas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) TipoMovimientoInventario tipoMovimiento,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.listarSalidas(
                fechaDesde,
                fechaHasta,
                tipoMovimiento,
                almacenId,
                productoId,
                pageable
        )));
    }

    @GetMapping("/salidas-detalle")
    public ResponseEntity<ApiResponse<SalidaInventarioReporteResponse>> listarSalidasDetalle(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Long subfamiliaId,
            @RequestParam(required = false) Long tipoArticuloId,
            @RequestParam(required = false) TipoMovimientoInventario tipoMovimiento,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) EstadoMovimientoInventario estado,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String agruparPor,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.obtenerSalidasDetalle(
                parseFechaDesde(fechaDesde),
                parseFechaHastaExclusiva(fechaHasta),
                almacenId,
                productoId,
                familiaId,
                subfamiliaId,
                tipoArticuloId,
                tipoMovimiento,
                usuarioId,
                estado,
                texto,
                agruparPor,
                puedeVerCostos(authentication),
                pageable
        )));
    }

    @GetMapping("/salidas-por-os")
    public ResponseEntity<ApiResponse<SalidaValorizadaOsReporteResponse>> listarSalidasValorizadasPorOs(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String ordenTrabajo,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) EstadoMovimientoInventario estado,
            @PageableDefault(size = 20) Pageable pageable,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.obtenerSalidasValorizadasPorOs(
                parseFechaDesde(fechaDesde),
                parseFechaHastaExclusiva(fechaHasta),
                firstNonBlank(orden, ordenTrabajo),
                productoId,
                estado,
                puedeVerCostos(authentication),
                pageable
        )));
    }

    @GetMapping("/productos-bajo-minimo")
    public ResponseEntity<ApiResponse<Page<ProductoBajoMinimoReporteResponse>>> listarProductosBajoMinimo(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long categoriaId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.listarProductosBajoMinimo(
                almacenId,
                categoriaId,
                pageable
        )));
    }

    @GetMapping("/movimientos/{movimientoId}/pdf")
    public ResponseEntity<byte[]> obtenerMovimientoPdf(
            @PathVariable Long movimientoId,
            Authentication authentication
    ) {
        ArchivoReporteResponse archivo = movimientoPdfService.generarMovimientoPdf(
                movimientoId,
                puedeVerCostos(authentication),
                authentication == null ? null : authentication.getName()
        );
        return archivoResponse(archivo, MediaType.APPLICATION_PDF, true);
    }

    @GetMapping("/movimientos/excel")
    @PreAuthorize("(hasAuthority('REPORTE_VER') and hasAuthority('REPORTE_EXPORTAR')) or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarMovimientos(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) TipoMovimientoInventario tipoMovimiento,
            @RequestParam(required = false) EstadoMovimientoInventario estado,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId
    ) {
        Page<MovimientoInventarioReporteResponse> reporte = reporteInventarioService.listarMovimientos(
                parseFechaDesde(fechaDesde),
                parseFechaHastaExclusiva(fechaHasta),
                tipoMovimiento,
                estado,
                almacenId,
                productoId,
                EXPORT_PAGEABLE
        );
        byte[] content = excelReportService.generar(
                "Movimientos",
                List.of("Movimiento", "Tipo", "Estado", "Fecha", "Documento", "Codigo producto", "Producto",
                        "Almacen origen", "Almacen destino", "Cantidad", "Costo unitario", "Total linea"),
                reporte.getContent().stream()
                        .map(r -> List.<Object>of(
                                r.numero(), r.tipoMovimiento(), r.estado(), r.fechaMovimiento(),
                                nullable(r.documentoReferencia()), nullable(r.productoCodigo()),
                                nullable(r.productoNombre()), nullable(r.almacenOrigenNombre()),
                                nullable(r.almacenDestinoNombre()), nullable(r.cantidad()),
                                nullable(r.costoUnitario()), nullable(r.totalLinea())
                        ))
                        .toList()
        );
        return archivoResponse(new ArchivoReporteResponse("movimientos-inventario.xlsx", content), EXCEL_MEDIA_TYPE, false);
    }

    @GetMapping("/productos-bajo-minimo/excel")
    @PreAuthorize("(hasAuthority('REPORTE_VER') and hasAuthority('REPORTE_EXPORTAR')) or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarProductosBajoMinimo(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long categoriaId
    ) {
        Page<ProductoBajoMinimoReporteResponse> reporte = reporteInventarioService.listarProductosBajoMinimo(
                almacenId,
                categoriaId,
                EXPORT_PAGEABLE
        );
        byte[] content = excelReportService.generar(
                "Productos bajo minimo",
                List.of("Codigo producto", "Producto", "Categoria", "Almacen", "Cantidad actual", "Stock minimo", "Diferencia"),
                reporte.getContent().stream()
                        .map(r -> List.<Object>of(
                                r.productoCodigo(), r.productoNombre(), nullable(r.categoriaNombre()),
                                r.almacenNombre(), r.cantidadActual(), r.stockMinimo(), r.diferencia()
                        ))
                        .toList()
        );
        return archivoResponse(new ArchivoReporteResponse("productos-bajo-minimo.xlsx", content), EXCEL_MEDIA_TYPE, false);
    }

    @GetMapping("/stock-valorizado/excel")
    @PreAuthorize("(hasAuthority('REPORTE_VER') and hasAuthority('REPORTE_EXPORTAR')) or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarStockValorizado(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long ubicacionId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Long subfamiliaId,
            @RequestParam(required = false) Long tipoArticuloId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) Boolean soloConStock,
            @RequestParam(required = false) Boolean bajoMinimo,
            @RequestParam(required = false) String texto,
            Authentication authentication
    ) {
        StockValorizadoReporteResponse reporte = reporteInventarioService.obtenerStockValorizado(
                almacenId, ubicacionId, productoId, familiaId, subfamiliaId, tipoArticuloId, marcaId, null,
                soloConStock, bajoMinimo, texto, puedeVerCostos(authentication), EXPORT_PAGEABLE
        );
        byte[] content = excelReportService.generar(
                "Stock valorizado",
                List.of("Codigo", "Producto", "Tipo articulo", "Marca", "Unidad", "Almacen", "Ubicacion", "Cantidad", "Stock minimo", "Stock maximo", "Costo promedio", "Valor total", "Estado"),
                reporte.registros().getContent().stream()
                        .map(r -> List.<Object>of(
                                r.codigoProducto(), r.nombreProducto(), r.tipoArticulo(), nullable(r.marca()), r.unidadMedida(),
                                r.almacen(), nullable(r.ubicacion()), r.cantidadActual(), r.stockMinimo(), nullable(r.stockMaximo()), nullable(r.costoPromedio()), nullable(r.valorTotal()), r.estadoStock()
                        ))
                        .toList()
        );
        return archivoResponse(new ArchivoReporteResponse("stock-valorizado.xlsx", content), EXCEL_MEDIA_TYPE, false);
    }

    @GetMapping("/kardex-valorizado/excel")
    @PreAuthorize("(hasAuthority('REPORTE_VER') and hasAuthority('REPORTE_EXPORTAR')) or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarKardexValorizado(
            @RequestParam Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long ubicacionId,
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            Authentication authentication
    ) {
        KardexValorizadoResponse reporte = reporteInventarioService.obtenerKardexValorizado(
                productoId, almacenId, ubicacionId, parseFechaDesde(fechaDesde), parseFechaHastaExclusiva(fechaHasta), puedeVerCostos(authentication)
        );
        byte[] content = excelReportService.generar(
                "Kardex valorizado",
                List.of("Fecha", "Movimiento", "Tipo", "Estado", "Documento", "Entrada", "Salida", "Saldo anterior", "Saldo final", "Costo movimiento", "Costo promedio", "Valor entrada", "Valor salida", "Valor saldo", "Usuario"),
                reporte.detalles().stream()
                        .map(r -> List.<Object>of(
                                r.fecha(), r.numeroMovimiento(), r.tipoMovimiento(), r.estadoMovimiento(), nullable(r.documentoReferencia()),
                                r.cantidadEntrada(), r.cantidadSalida(), r.saldoAnterior(), r.saldoFinal(), nullable(r.costoUnitarioMovimiento()),
                                nullable(r.costoPromedio()), nullable(r.valorEntrada()), nullable(r.valorSalida()), nullable(r.valorSaldo()), r.usuario()
                        ))
                        .toList()
        );
        return archivoResponse(new ArchivoReporteResponse("kardex-valorizado.xlsx", content), EXCEL_MEDIA_TYPE, false);
    }

    @GetMapping("/salidas-detalle/excel")
    @PreAuthorize("(hasAuthority('REPORTE_VER') and hasAuthority('REPORTE_EXPORTAR')) or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarSalidasDetalle(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Long subfamiliaId,
            @RequestParam(required = false) Long tipoArticuloId,
            @RequestParam(required = false) TipoMovimientoInventario tipoMovimiento,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) EstadoMovimientoInventario estado,
            @RequestParam(required = false) String texto,
            Authentication authentication
    ) {
        SalidaInventarioReporteResponse reporte = reporteInventarioService.obtenerSalidasDetalle(
                parseFechaDesde(fechaDesde), parseFechaHastaExclusiva(fechaHasta), almacenId, productoId, familiaId,
                subfamiliaId, tipoArticuloId, tipoMovimiento, usuarioId, estado, texto, null, puedeVerCostos(authentication), EXPORT_PAGEABLE
        );
        byte[] content = excelReportService.generar(
                "Salidas detalle",
                List.of("Fecha", "Movimiento", "Tipo", "Estado", "Almacen", "Ubicacion", "Codigo", "Producto", "Familia", "Unidad", "Cantidad", "Costo aplicado", "Valor salida", "Usuario", "Motivo", "Observacion"),
                reporte.registros().getContent().stream()
                        .map(r -> List.<Object>of(
                                r.fecha(), r.numeroMovimiento(), r.tipoMovimiento(), r.estado(), r.almacenOrigen(), nullable(r.ubicacionOrigen()),
                                r.codigoProducto(), r.nombreProducto(), r.familia(), r.unidadMedida(), r.cantidad(), nullable(r.costoPromedioAplicado()),
                                nullable(r.valorSalida()), r.usuario(), nullable(r.motivo()), nullable(r.observacion())
                        ))
                        .toList()
        );
        return archivoResponse(new ArchivoReporteResponse("salidas-detalle.xlsx", content), EXCEL_MEDIA_TYPE, false);
    }

    @GetMapping("/salidas-por-os/excel")
    @PreAuthorize("(hasAuthority('REPORTE_VER') and hasAuthority('REPORTE_EXPORTAR')) or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarSalidasValorizadasPorOs(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) String orden,
            @RequestParam(required = false) String ordenTrabajo,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) EstadoMovimientoInventario estado,
            Authentication authentication
    ) {
        SalidaValorizadaOsReporteResponse reporte = reporteInventarioService.obtenerSalidasValorizadasPorOs(
                parseFechaDesde(fechaDesde),
                parseFechaHastaExclusiva(fechaHasta),
                firstNonBlank(orden, ordenTrabajo),
                productoId,
                estado,
                puedeVerCostos(authentication),
                EXPORT_PAGEABLE
        );
        byte[] content = excelReportService.generar(
                "Salidas valorizadas por OS",
                List.of("Fecha", "Movimiento", "Estado", "OS", "Codigo", "Producto", "Unidad", "Cantidad",
                        "Costo inventario unitario", "Costo inventario total", "Costo base comercial", "Recargo %",
                        "Valor venta unitario sin IGV", "Valor venta total sin IGV", "IGV unitario", "IGV total",
                        "Precio unitario con IGV", "Precio total con IGV", "Diferencia comercial sin IGV"),
                reporte.registros().getContent().stream()
                        .map(r -> List.<Object>of(
                                r.fecha(), r.numeroMovimiento(), r.estado(), nullable(r.ordenTrabajo()), r.codigoProducto(),
                                r.descripcionProducto(), r.unidadMedida(), r.cantidad(), nullable(r.costoInventarioUnitario()),
                                nullable(r.costoInventarioTotal()), nullable(r.costoBaseComercialUnitarioSinIgv()),
                                nullable(r.recargoAplicadoPct()), nullable(r.valorVentaUnitarioSinIgv()),
                                nullable(r.valorVentaTotalSinIgv()), nullable(r.igvUnitario()), nullable(r.igvTotal()),
                                nullable(r.precioVentaUnitarioConIgv()), nullable(r.precioVentaTotalConIgv()),
                                nullable(r.diferenciaComercialSinIgv())
                        ))
                        .toList()
        );
        return archivoResponse(new ArchivoReporteResponse("salidas-valorizadas-por-os.xlsx", content), EXCEL_MEDIA_TYPE, false);
    }

    @GetMapping("/entradas-proveedor/excel")
    @PreAuthorize("(hasAuthority('REPORTE_VER') and hasAuthority('REPORTE_EXPORTAR')) or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportarEntradasProveedor(
            @RequestParam(required = false) String fechaDesde,
            @RequestParam(required = false) String fechaHasta,
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) String tipoComprobante,
            @RequestParam(required = false) String texto,
            Authentication authentication
    ) {
        EntradaProveedorReporteResponse reporte = reporteInventarioService.obtenerEntradasProveedor(
                parseFechaDesde(fechaDesde), parseFechaHastaExclusiva(fechaHasta), proveedorId, productoId, familiaId,
                almacenId, tipoComprobante, texto, puedeVerCostos(authentication), EXPORT_PAGEABLE
        );
        byte[] content = excelReportService.generar(
                "Entradas proveedor",
                List.of("Fecha movimiento", "Fecha pedido", "Fecha recepcion", "Movimiento", "Proveedor documento", "Proveedor", "Orden compra", "Guia", "Comprobante", "Almacen destino", "Codigo", "Producto", "Unidad", "Cantidad", "Costo unitario", "Valor linea", "Flete", "Movilidad", "Otros gastos"),
                reporte.registros().getContent().stream()
                        .map(r -> List.<Object>of(
                                r.fechaMovimiento(), nullable(r.fechaPedido()), nullable(r.fechaRecepcion()), r.numeroMovimiento(),
                                r.proveedorNumeroDocumento(), r.proveedorRazonSocial(), nullable(r.ordenCompra()),
                                joinDocumento(r.guiaSerie(), r.guiaNumero()), joinDocumento(r.comprobanteSerie(), r.comprobanteNumero()),
                                nullable(r.almacenDestino()), r.codigoProducto(), r.nombreProducto(), r.unidadMedida(), r.cantidad(),
                                nullable(r.costoUnitario()), nullable(r.valorLinea()), nullable(r.flete()), nullable(r.movilidad()), nullable(r.otrosGastos())
                        ))
                        .toList()
        );
        return archivoResponse(new ArchivoReporteResponse("entradas-proveedor.xlsx", content), EXCEL_MEDIA_TYPE, false);
    }

    private ResponseEntity<byte[]> archivoResponse(ArchivoReporteResponse archivo, MediaType mediaType, boolean inline) {
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(archivo.filename())
                .build();
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(archivo.content());
    }

    private boolean puedeVerCostos(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        return authorities.contains("ROLE_ADMIN") || authorities.contains("REPORTE_COSTOS_VER");
    }

    private LocalDateTime parseFechaDesde(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() == 10) {
            return LocalDate.parse(normalized).atStartOfDay();
        }
        return LocalDateTime.parse(normalized);
    }

    private LocalDateTime parseFechaHastaExclusiva(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() == 10) {
            return LocalDate.parse(normalized).plusDays(1).atStartOfDay();
        }
        return LocalDateTime.parse(normalized);
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private Object nullable(Object value) {
        return value == null ? "" : value;
    }

    private String joinDocumento(String serie, String numero) {
        if ((serie == null || serie.isBlank()) && (numero == null || numero.isBlank())) {
            return "";
        }
        return (serie == null ? "" : serie) + "-" + (numero == null ? "" : numero);
    }
}
