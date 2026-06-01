package pe.com.ballena.erpalmacen.almacen.ubicaciones.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UbicacionAlmacenUpdateRequest(
        @NotNull(message = "El almacen es obligatorio")
        Long almacenId,

        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 30, message = "El codigo no debe superar 30 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no debe superar 150 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripcion no debe superar 255 caracteres")
        String descripcion
) {
}
