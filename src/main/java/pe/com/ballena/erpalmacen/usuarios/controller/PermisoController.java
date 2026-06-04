package pe.com.ballena.erpalmacen.usuarios.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;
import pe.com.ballena.erpalmacen.usuarios.dto.PermisoResponse;
import pe.com.ballena.erpalmacen.usuarios.service.UsuarioAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/permisos")
@PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
public class PermisoController {

    private final UsuarioAdminService usuarioAdminService;

    public PermisoController(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PermisoResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(usuarioAdminService.listarPermisos()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PermisoResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioAdminService.obtenerPermiso(id)));
    }
}
