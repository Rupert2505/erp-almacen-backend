package pe.com.ballena.erpalmacen.almacen.almacenes.controller;

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
import pe.com.ballena.erpalmacen.almacen.almacenes.dto.AlmacenCreateRequest;
import pe.com.ballena.erpalmacen.almacen.almacenes.dto.AlmacenResponse;
import pe.com.ballena.erpalmacen.almacen.almacenes.dto.AlmacenUpdateRequest;
import pe.com.ballena.erpalmacen.almacen.almacenes.service.AlmacenService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/almacenes")
public class AlmacenController {

    private final AlmacenService almacenService;

    public AlmacenController(AlmacenService almacenService) {
        this.almacenService = almacenService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ALMACEN_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<AlmacenResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(almacenService.listar(texto, activo, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ALMACEN_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlmacenResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(almacenService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlmacenResponse>> crear(@Valid @RequestBody AlmacenCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Almacen creado correctamente", almacenService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlmacenResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody AlmacenUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Almacen actualizado correctamente", almacenService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlmacenResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Almacen desactivado correctamente", almacenService.desactivar(id)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AlmacenResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Almacen activado correctamente", almacenService.activar(id)));
    }
}
