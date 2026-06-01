package pe.com.ballena.erpalmacen.maestros.categorias.controller;

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
import pe.com.ballena.erpalmacen.maestros.categorias.dto.CategoriaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.categorias.dto.CategoriaResponse;
import pe.com.ballena.erpalmacen.maestros.categorias.dto.CategoriaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.categorias.service.CategoriaService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<CategoriaResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.listar(texto, activo, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoriaService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> crear(@Valid @RequestBody CategoriaCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Categoria creada correctamente", categoriaService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Categoria actualizada correctamente", categoriaService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Categoria desactivada correctamente", categoriaService.desactivar(id)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoriaResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Categoria activada correctamente", categoriaService.activar(id)));
    }
}
