package pe.com.ballena.erpalmacen.usuarios.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.Set;

public record UsuarioCambiarRolesRequest(
        @NotEmpty(message = "Debe asignar al menos un rol")
        Set<Long> rolesIds
) {
}
