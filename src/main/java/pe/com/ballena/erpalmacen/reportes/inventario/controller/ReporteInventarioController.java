package pe.com.ballena.erpalmacen.reportes.inventario.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.EntradaProveedorResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.MovimientoInventarioReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ProductoBajoMinimoReporteResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ResumenInventarioResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.SalidaInventarioResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.StockValorizadoResponse;
import pe.com.ballena.erpalmacen.reportes.inventario.service.ReporteInventarioService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/reportes/inventario")
@PreAuthorize("hasAuthority('REPORTE_VER') or hasRole('ADMIN')")
public class ReporteInventarioController {

    private final ReporteInventarioService reporteInventarioService;

    public ReporteInventarioController(ReporteInventarioService reporteInventarioService) {
        this.reporteInventarioService = reporteInventarioService;
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
    public ResponseEntity<ApiResponse<Page<StockValorizadoResponse>>> listarStockValorizado(
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) Boolean soloConStock,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.listarStockValorizado(
                almacenId,
                productoId,
                categoriaId,
                soloConStock,
                pageable
        )));
    }

    @GetMapping("/entradas-proveedor")
    public ResponseEntity<ApiResponse<Page<EntradaProveedorResponse>>> listarEntradasProveedor(
            @RequestParam(required = false) Long proveedorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reporteInventarioService.listarEntradasProveedor(
                proveedorId,
                fechaDesde,
                fechaHasta,
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
}
