package pe.com.ballena.erpalmacen.usuarios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UsuarioResetPasswordRequest(
        @NotBlank(message = "La nueva password es obligatoria")
        @Size(min = 8, max = 100, message = "La nueva password debe tener entre 8 y 100 caracteres")
        String nuevaPassword
) {
}
