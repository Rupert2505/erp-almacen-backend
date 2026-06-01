package pe.com.ballena.erpalmacen.almacen.ubicaciones.controller;

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
import pe.com.ballena.erpalmacen.almacen.ubicaciones.dto.UbicacionAlmacenCreateRequest;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.dto.UbicacionAlmacenResponse;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.dto.UbicacionAlmacenUpdateRequest;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.service.UbicacionAlmacenService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
public class UbicacionAlmacenController {

    private final UbicacionAlmacenService ubicacionService;

    public UbicacionAlmacenController(UbicacionAlmacenService ubicacionService) {
        this.ubicacionService = ubicacionService;
    }

    @GetMapping("/api/ubicaciones")
    @PreAuthorize("hasAuthority('ALMACEN_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UbicacionAlmacenResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Long almacenId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(ubicacionService.listar(texto, activo, almacenId, pageable)));
    }

    @GetMapping("/api/almacenes/{almacenId}/ubicaciones")
    @PreAuthorize("hasAuthority('ALMACEN_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UbicacionAlmacenResponse>>> listarPorAlmacen(
            @PathVariable Long almacenId,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(ubicacionService.listarPorAlmacen(almacenId, texto, activo, pageable)));
    }

    @GetMapping("/api/ubicaciones/{id}")
    @PreAuthorize("hasAuthority('ALMACEN_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UbicacionAlmacenResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ubicacionService.obtener(id)));
    }

    @PostMapping("/api/ubicaciones")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UbicacionAlmacenResponse>> crear(@Valid @RequestBody UbicacionAlmacenCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Ubicacion creada correctamente", ubicacionService.crear(request)));
    }

    @PutMapping("/api/ubicaciones/{id}")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UbicacionAlmacenResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UbicacionAlmacenUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Ubicacion actualizada correctamente", ubicacionService.actualizar(id, request)));
    }

    @PatchMapping("/api/ubicaciones/{id}/desactivar")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UbicacionAlmacenResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Ubicacion desactivada correctamente", ubicacionService.desactivar(id)));
    }

    @PatchMapping("/api/ubicaciones/{id}/activar")
    @PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UbicacionAlmacenResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Ubicacion activada correctamente", ubicacionService.activar(id)));
    }
}
