package pe.com.ballena.erpalmacen.maestros.tiposarticulo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TipoArticuloCreateRequest(
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 50, message = "El codigo no debe superar 50 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no debe superar 150 caracteres")
        String nombre,

        String descripcion
) {
}
