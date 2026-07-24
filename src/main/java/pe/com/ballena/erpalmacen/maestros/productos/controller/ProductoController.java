package pe.com.ballena.erpalmacen.maestros.productos.controller;

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
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoCreateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoResponse;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.service.ProductoService;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<ProductoResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Long tipoArticuloId,
            @RequestParam(required = false) Long familiaId,
            @RequestParam(required = false) Long subfamiliaId,
            @RequestParam(required = false) Long marcaId,
            @RequestParam(required = false) Long unidadMedidaId,
            Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                productoService.listar(texto, activo, tipoArticuloId, familiaId, subfamiliaId, marcaId, unidadMedidaId, pageable)
        ));
    }

    @GetMapping("/siguiente-codigo")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> obtenerSiguienteCodigo(@RequestParam Long familiaId) {
        return ResponseEntity.ok(ApiResponse.ok(productoService.obtenerSiguienteCodigo(familiaId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productoService.obtener(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCTO_CREAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductoResponse>> crear(@Valid @RequestBody ProductoCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Producto creado correctamente", productoService.crear(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCTO_EDITAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductoResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProductoUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Producto actualizado correctamente", productoService.actualizar(id, request)));
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductoResponse>> desactivar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Producto desactivado correctamente", productoService.desactivar(id)));
    }

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasAuthority('PRODUCTO_DESACTIVAR') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProductoResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok("Producto activado correctamente", productoService.activar(id)));
    }
}
