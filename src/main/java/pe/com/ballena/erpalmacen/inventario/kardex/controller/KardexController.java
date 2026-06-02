package pe.com.ballena.erpalmacen.inventario.kardex.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.inventario.kardex.dto.KardexResponse;
import pe.com.ballena.erpalmacen.inventario.kardex.service.KardexConsultaService;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventario/kardex")
public class KardexController {

    private final KardexConsultaService kardexConsultaService;

    public KardexController(KardexConsultaService kardexConsultaService) {
        this.kardexConsultaService = kardexConsultaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('KARDEX_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<KardexResponse>>> listar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long almacenId,
            @RequestParam(required = false) Long ubicacionId,
            @RequestParam(required = false) TipoMovimientoInventario tipoMovimiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) String texto,
            @PageableDefault(sort = "fechaMovimiento") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                kardexConsultaService.listar(
                        productoId,
                        almacenId,
                        ubicacionId,
                        tipoMovimiento,
                        fechaDesde,
                        fechaHasta,
                        texto,
                        pageable
                )
        ));
    }

    @GetMapping("/producto/{productoId}")
    @PreAuthorize("hasAuthority('KARDEX_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<KardexResponse>>> listarPorProducto(
            @PathVariable Long productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @PageableDefault(sort = "fechaMovimiento") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                kardexConsultaService.listarPorProducto(productoId, fechaDesde, fechaHasta, pageable)
        ));
    }

    @GetMapping("/producto/{productoId}/almacen/{almacenId}")
    @PreAuthorize("hasAuthority('KARDEX_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<KardexResponse>>> listarPorProductoYAlmacen(
            @PathVariable Long productoId,
            @PathVariable Long almacenId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @PageableDefault(sort = "fechaMovimiento") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                kardexConsultaService.listarPorProductoYAlmacen(productoId, almacenId, fechaDesde, fechaHasta, pageable)
        ));
    }

    @GetMapping("/movimiento/{movimientoId}")
    @PreAuthorize("hasAuthority('KARDEX_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<KardexResponse>>> listarPorMovimiento(@PathVariable Long movimientoId) {
        return ResponseEntity.ok(ApiResponse.ok(kardexConsultaService.listarPorMovimiento(movimientoId)));
    }
}
