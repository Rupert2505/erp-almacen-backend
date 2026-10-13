package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.dto.ImportacionSaldoInicialErrorResponse;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.dto.ImportacionSaldoInicialResultadoResponse;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.dto.ImportacionSaldoInicialTrazaResponse;
import pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.service.ImportacionSaldoInicialService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

import java.util.List;

@RestController
@RequestMapping("/api/inventario/importaciones/saldos-iniciales")
public class ImportacionSaldoInicialController {

    private static final String PERMISO_IMPORTACION = "INVENTARIO_IMPORTACION_SALDOS_INICIALES";

    private final ImportacionSaldoInicialService importacionSaldoInicialService;

    public ImportacionSaldoInicialController(ImportacionSaldoInicialService importacionSaldoInicialService) {
        this.importacionSaldoInicialService = importacionSaldoInicialService;
    }

    @PostMapping("/{loteMigracion}/cargar-staging-entrada-ajuste")
    @PreAuthorize("hasAuthority('" + PERMISO_IMPORTACION + "') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImportacionSaldoInicialResultadoResponse>> cargarDesdeStagingEntradaAjuste(
            @PathVariable String loteMigracion
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Staging de entrada por ajuste cargado",
                importacionSaldoInicialService.cargarDesdeStagingEntradaAjuste(loteMigracion)
        ));
    }

    @PostMapping("/{loteMigracion}/validar")
    @PreAuthorize("hasAuthority('" + PERMISO_IMPORTACION + "') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImportacionSaldoInicialResultadoResponse>> validar(
            @PathVariable String loteMigracion,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lote validado",
                importacionSaldoInicialService.validar(loteMigracion, authentication)
        ));
    }

    @PostMapping("/{loteMigracion}/procesar")
    @PreAuthorize("""
            (hasAuthority('INVENTARIO_IMPORTACION_SALDOS_INICIALES') and hasAuthority('INVENTARIO_ENTRADA'))
            or hasRole('ADMIN')
            """)
    public ResponseEntity<ApiResponse<ImportacionSaldoInicialResultadoResponse>> procesar(
            @PathVariable String loteMigracion,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Lote procesado",
                importacionSaldoInicialService.procesar(loteMigracion, authentication)
        ));
    }

    @GetMapping("/{loteMigracion}/resumen")
    @PreAuthorize("hasAuthority('" + PERMISO_IMPORTACION + "') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ImportacionSaldoInicialResultadoResponse>> resumen(
            @PathVariable String loteMigracion
    ) {
        return ResponseEntity.ok(ApiResponse.ok(importacionSaldoInicialService.resumen(loteMigracion)));
    }

    @GetMapping("/{loteMigracion}/errores")
    @PreAuthorize("hasAuthority('" + PERMISO_IMPORTACION + "') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ImportacionSaldoInicialErrorResponse>>> errores(
            @PathVariable String loteMigracion
    ) {
        return ResponseEntity.ok(ApiResponse.ok(importacionSaldoInicialService.listarErrores(loteMigracion)));
    }

    @GetMapping("/{loteMigracion}/trazabilidad")
    @PreAuthorize("hasAuthority('" + PERMISO_IMPORTACION + "') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ImportacionSaldoInicialTrazaResponse>>> trazabilidad(
            @PathVariable String loteMigracion
    ) {
        return ResponseEntity.ok(ApiResponse.ok(importacionSaldoInicialService.listarTrazabilidad(loteMigracion)));
    }
}
