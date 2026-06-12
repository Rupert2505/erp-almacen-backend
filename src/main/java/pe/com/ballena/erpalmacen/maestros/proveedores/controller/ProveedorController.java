package pe.com.ballena.erpalmacen.maestros.proveedores.controller;

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
import pe.com.ballena.erpalmacen.maestros.proveedores.dto.ProveedorCreateRequest;
import pe.com.ballena.erpalmacen.maestros.proveedores.dto.ProveedorResponse;
import pe.com.ballena.erpalmacen.maestros.proveedores.dto.ProveedorUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.proveedores.service.ProveedorService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/proveedores")
public class ProveedorController {

    private final ProveedorService proveedorService;

    public ProveedorController(ProveedorService proveedorService) {
        this.proveedorService = proveedorService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROVEEDOR_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProveedorResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String term,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(proveedorService.listar(
                firstNonBlank(texto, search, q, nombre, term),
                activo,
                pageable
        )));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROVEEDOR_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProveedorResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(proveedorService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROVEEDOR_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProveedorResponse>> crear(@Valid @RequestBody ProveedorCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Proveedor creado correctamente", proveedorService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROVEEDOR_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProveedorResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProveedorUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Proveedor actualizado correctamente", proveedorService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('PROVEEDOR_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProveedorResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Proveedor desactivado correctamente", proveedorService.desactivar(id)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('PROVEEDOR_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProveedorResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Proveedor activado correctamente", proveedorService.activar(id)));
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
