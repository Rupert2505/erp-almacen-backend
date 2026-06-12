package pe.com.ballena.erpalmacen.maestros.tiposarticulo.controller;

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
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto.TipoArticuloCreateRequest;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto.TipoArticuloResponse;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto.TipoArticuloUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.service.TipoArticuloService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/tipos-articulo")
public class TipoArticuloController {

    private final TipoArticuloService tipoArticuloService;

    public TipoArticuloController(TipoArticuloService tipoArticuloService) {
        this.tipoArticuloService = tipoArticuloService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TipoArticuloResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(tipoArticuloService.listar(texto, activo, pageable)));
    }

    @GetMapping("/activos")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<TipoArticuloResponse>>> listarActivos(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(tipoArticuloService.listarActivos(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TipoArticuloResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(tipoArticuloService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TipoArticuloResponse>> crear(@Valid @RequestBody TipoArticuloCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Tipo de articulo creado correctamente", tipoArticuloService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TipoArticuloResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody TipoArticuloUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Tipo de articulo actualizado correctamente", tipoArticuloService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TipoArticuloResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Tipo de articulo desactivado correctamente", tipoArticuloService.desactivar(id)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TipoArticuloResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Tipo de articulo activado correctamente", tipoArticuloService.activar(id)));
    }
}
