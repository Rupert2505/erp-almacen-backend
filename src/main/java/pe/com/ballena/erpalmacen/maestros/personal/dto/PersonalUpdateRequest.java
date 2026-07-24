package pe.com.ballena.erpalmacen.maestros.personal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonalUpdateRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 20, message = "El tipo de documento no debe exceder 20 caracteres")
        String tipoDocumento,

        @NotBlank(message = "El numero de documento es obligatorio")
        @Size(max = 20, message = "El numero de documento no debe exceder 20 caracteres")
        String numeroDocumento,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 150, message = "Los nombres no deben exceder 150 caracteres")
        String nombres,

        @Size(max = 150, message = "Los apellidos no deben exceder 150 caracteres")
        String apellidos,

        @Size(max = 120, message = "El cargo no debe exceder 120 caracteres")
        String cargo,

        @Size(max = 120, message = "El area no debe exceder 120 caracteres")
        String area,

        @Size(max = 50, message = "El telefono no debe exceder 50 caracteres")
        String telefono,

        @Email(message = "El email debe tener un formato valido")
        @Size(max = 150, message = "El email no debe exceder 150 caracteres")
        String email
) {
}
