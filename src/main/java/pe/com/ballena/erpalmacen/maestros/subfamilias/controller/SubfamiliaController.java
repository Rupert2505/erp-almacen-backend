package pe.com.ballena.erpalmacen.maestros.subfamilias.controller;

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
import pe.com.ballena.erpalmacen.maestros.subfamilias.dto.SubfamiliaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.subfamilias.dto.SubfamiliaResponse;
import pe.com.ballena.erpalmacen.maestros.subfamilias.dto.SubfamiliaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.subfamilias.service.SubfamiliaService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/subfamilias")
public class SubfamiliaController {

    private final SubfamiliaService subfamiliaService;

    public SubfamiliaController(SubfamiliaService subfamiliaService) {
        this.subfamiliaService = subfamiliaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<SubfamiliaResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(subfamiliaService.listar(texto, activo, pageable)));
    }

    @GetMapping("/activas")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<SubfamiliaResponse>>> listarActivas(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(subfamiliaService.listarActivas(pageable)));
    }

    @GetMapping("/por-familia/{familiaId}")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<SubfamiliaResponse>>> listarPorFamilia(
            @PathVariable Long familiaId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(subfamiliaService.listarPorFamilia(familiaId, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubfamiliaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(subfamiliaService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubfamiliaResponse>> crear(@Valid @RequestBody SubfamiliaCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Subfamilia creada correctamente", subfamiliaService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubfamiliaResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody SubfamiliaUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Subfamilia actualizada correctamente", subfamiliaService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubfamiliaResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Subfamilia desactivada correctamente", subfamiliaService.desactivar(id)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubfamiliaResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Subfamilia activada correctamente", subfamiliaService.activar(id)));
    }
}
