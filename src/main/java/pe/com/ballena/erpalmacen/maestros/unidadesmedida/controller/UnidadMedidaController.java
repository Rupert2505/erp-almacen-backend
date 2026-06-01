package pe.com.ballena.erpalmacen.maestros.unidadesmedida.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto.UnidadMedidaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto.UnidadMedidaResponse;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto.UnidadMedidaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.service.UnidadMedidaService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/unidades-medida")
public class UnidadMedidaController {

    private final UnidadMedidaService unidadMedidaService;

    public UnidadMedidaController(UnidadMedidaService unidadMedidaService) {
        this.unidadMedidaService = unidadMedidaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UnidadMedidaResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(unidadMedidaService.listar(texto, activo, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnidadMedidaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(unidadMedidaService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnidadMedidaResponse>> crear(@Valid @RequestBody UnidadMedidaCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Unidad de medida creada correctamente", unidadMedidaService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnidadMedidaResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UnidadMedidaUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Unidad de medida actualizada correctamente", unidadMedidaService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnidadMedidaResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Unidad de medida desactivada correctamente", unidadMedidaService.desactivar(id)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UnidadMedidaResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Unidad de medida activada correctamente", unidadMedidaService.activar(id)));
    }
}
