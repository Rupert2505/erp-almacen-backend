package pe.com.ballena.erpalmacen.maestros.marcas.controller;

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
import pe.com.ballena.erpalmacen.maestros.marcas.dto.MarcaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.marcas.dto.MarcaResponse;
import pe.com.ballena.erpalmacen.maestros.marcas.dto.MarcaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.marcas.service.MarcaService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/marcas")
public class MarcaController {

    private final MarcaService marcaService;

    public MarcaController(MarcaService marcaService) {
        this.marcaService = marcaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<MarcaResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(marcaService.listar(texto, activo, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarcaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(marcaService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarcaResponse>> crear(@Valid @RequestBody MarcaCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Marca creada correctamente", marcaService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarcaResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody MarcaUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Marca actualizada correctamente", marcaService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarcaResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Marca desactivada correctamente", marcaService.desactivar(id)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MarcaResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Marca activada correctamente", marcaService.activar(id)));
    }
}
