package pe.com.ballena.erpalmacen.inventario.movimientos.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioCreateRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoInventarioResponse;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoAnulacionRequest;
import pe.com.ballena.erpalmacen.inventario.service.MovimientoInventarioService;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/inventario/movimientos")
public class MovimientoInventarioController {

    private final MovimientoInventarioService movimientoInventarioService;

    public MovimientoInventarioController(MovimientoInventarioService movimientoInventarioService) {
        this.movimientoInventarioService = movimientoInventarioService;
    }

    @PostMapping
    @PreAuthorize("""
            hasAuthority('INVENTARIO_ENTRADA')
            or hasAuthority('INVENTARIO_SALIDA')
            or hasAuthority('INVENTARIO_TRANSFERENCIA')
            or hasAuthority('INVENTARIO_AJUSTE')
            or hasRole('ADMIN')
            """)
    public ResponseEntity<ApiResponse<MovimientoInventarioResponse>> crear(
            @Valid @RequestBody MovimientoInventarioCreateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Movimiento creado correctamente",
                movimientoInventarioService.crearMovimiento(request, authentication)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STOCK_VER') or hasAuthority('KARDEX_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MovimientoInventarioResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(movimientoInventarioService.obtener(id)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STOCK_VER') or hasAuthority('KARDEX_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<MovimientoInventarioResponse>>> listar(
            @RequestParam(required = false) TipoMovimientoInventario tipoMovimiento,
            @RequestParam(required = false) EstadoMovimientoInventario estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) String texto,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                movimientoInventarioService.listar(tipoMovimiento, estado, fechaDesde, fechaHasta, texto, pageable)
        ));
    }

    @PostMapping("/{id}/confirmar")
    @PreAuthorize("""
            hasAuthority('INVENTARIO_ENTRADA')
            or hasAuthority('INVENTARIO_SALIDA')
            or hasAuthority('INVENTARIO_TRANSFERENCIA')
            or hasAuthority('INVENTARIO_AJUSTE')
            or hasRole('ADMIN')
            """)
    public ResponseEntity<ApiResponse<MovimientoInventarioResponse>> confirmar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Movimiento confirmado correctamente",
                movimientoInventarioService.confirmarMovimiento(id, authentication)
        ));
    }

    @PostMapping("/{id}/anular")
    @PreAuthorize("hasAuthority('INVENTARIO_AJUSTE') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MovimientoInventarioResponse>> anular(
            @PathVariable Long id,
            @Valid @RequestBody MovimientoAnulacionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Movimiento anulado correctamente",
                movimientoInventarioService.anularMovimiento(id, request, authentication)
        ));
    }
}
