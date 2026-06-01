package pe.com.ballena.erpalmacen.maestros.proveedores.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProveedorUpdateRequest(
        @NotBlank(message = "El tipo de documento es obligatorio")
        @Size(max = 20, message = "El tipo de documento no debe superar 20 caracteres")
        String tipoDocumento,

        @NotBlank(message = "El numero de documento es obligatorio")
        @Size(max = 20, message = "El numero de documento no debe superar 20 caracteres")
        String numeroDocumento,

        @NotBlank(message = "La razon social es obligatoria")
        @Size(max = 200, message = "La razon social no debe superar 200 caracteres")
        String razonSocial,

        @Size(max = 200, message = "El nombre comercial no debe superar 200 caracteres")
        String nombreComercial,

        @Size(max = 255, message = "La direccion no debe superar 255 caracteres")
        String direccion,

        @Size(max = 50, message = "El telefono no debe superar 50 caracteres")
        String telefono,

        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150, message = "El email no debe superar 150 caracteres")
        String email
) {
}
