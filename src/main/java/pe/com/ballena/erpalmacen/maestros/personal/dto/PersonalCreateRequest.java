package pe.com.ballena.erpalmacen.maestros.personal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PersonalCreateRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 20, message = "El tipo de documento no debe superar 20 caracteres")
        String tipoDocumento,

        @NotBlank(message = "El numero de documento es obligatorio")
        @Size(max = 20, message = "El numero de documento no debe superar 20 caracteres")
        String numeroDocumento,

        @NotBlank(message = "Los nombres son obligatorios")
        @Size(max = 150, message = "Los nombres no deben superar 150 caracteres")
        String nombres,

        @Size(max = 150, message = "Los apellidos no deben superar 150 caracteres")
        String apellidos,

        @Size(max = 120, message = "El cargo no debe superar 120 caracteres")
        String cargo,

        @Size(max = 120, message = "El area no debe superar 120 caracteres")
        String area,

        @Size(max = 50, message = "El telefono no debe superar 50 caracteres")
        String telefono,

        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150, message = "El email no debe superar 150 caracteres")
        String email
) {
}
