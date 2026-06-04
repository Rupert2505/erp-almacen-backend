package pe.com.ballena.erpalmacen.usuarios.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record UsuarioCreateRequest(
        @NotBlank(message = "El username es obligatorio")
        @Size(max = 50, message = "El username no puede superar 50 caracteres")
        String username,

        @NotBlank(message = "La password es obligatoria")
        @Size(min = 8, max = 100, message = "La password debe tener entre 8 y 100 caracteres")
        String password,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 100, message = "Los nombres no pueden superar 100 caracteres")
        String nombres,

        @NotBlank(message = "Los apellidos son obligatorios")
        @Size(max = 100, message = "Los apellidos no pueden superar 100 caracteres")
        String apellidos,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150, message = "El email no puede superar 150 caracteres")
        String email,

        Boolean activo,

        @NotEmpty(message = "Debe asignar al menos un rol")
        Set<Long> rolesIds
) {
}
