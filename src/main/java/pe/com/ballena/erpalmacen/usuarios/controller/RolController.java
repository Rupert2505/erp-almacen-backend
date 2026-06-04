package pe.com.ballena.erpalmacen.usuarios.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;
import pe.com.ballena.erpalmacen.usuarios.dto.RolResponse;
import pe.com.ballena.erpalmacen.usuarios.service.UsuarioAdminService;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
@PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
public class RolController {

    private final UsuarioAdminService usuarioAdminService;

    public RolController(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RolResponse>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(usuarioAdminService.listarRoles()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RolResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioAdminService.obtenerRol(id)));
    }
}
