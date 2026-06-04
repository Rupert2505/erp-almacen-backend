package pe.com.ballena.erpalmacen.usuarios.controller;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.com.ballena.erpalmacen.shared.response.ApiResponse;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioCambiarRolesRequest;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioCreateRequest;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioResetPasswordRequest;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioResponse;
import pe.com.ballena.erpalmacen.usuarios.dto.UsuarioUpdateRequest;
import pe.com.ballena.erpalmacen.usuarios.service.UsuarioAdminService;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasAuthority('USUARIO_ADMIN') or hasRole('ADMIN')")
public class UsuarioAdminController {

    private final UsuarioAdminService usuarioAdminService;

    public UsuarioAdminController(UsuarioAdminService usuarioAdminService) {
        this.usuarioAdminService = usuarioAdminService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<UsuarioResponse>>> listar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(required = false) Long rolId,
            @PageableDefault(sort = "id") Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioAdminService.listar(texto, activo, rolId, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(usuarioAdminService.obtener(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UsuarioResponse>> crear(@Valid @RequestBody UsuarioCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Usuario creado correctamente",
                usuarioAdminService.crear(request)
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponse>> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioUpdateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Usuario actualizado correctamente",
                usuarioAdminService.actualizar(id, request, authentication)
        ));
    }

    @PatchMapping("/{id}/activar")
    public ResponseEntity<ApiResponse<UsuarioResponse>> activar(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Usuario activado correctamente",
                usuarioAdminService.activar(id)
        ));
    }

    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<ApiResponse<UsuarioResponse>> desactivar(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Usuario desactivado correctamente",
                usuarioAdminService.desactivar(id, authentication)
        ));
    }

    @PutMapping("/{id}/roles")
    public ResponseEntity<ApiResponse<UsuarioResponse>> cambiarRoles(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioCambiarRolesRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Roles actualizados correctamente",
                usuarioAdminService.cambiarRoles(id, request, authentication)
        ));
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<ApiResponse<UsuarioResponse>> resetPassword(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioResetPasswordRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Password actualizada correctamente",
                usuarioAdminService.resetPassword(id, request)
        ));
    }
}
