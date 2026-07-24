package pe.com.ballena.erpalmacen.maestros.familias.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FamiliaCreateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no debe superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El prefijo es obligatorio")
        @Size(max = 20, message = "El prefijo no debe superar 20 caracteres")
        String prefijo,

        @Size(max = 255, message = "La descripcion no debe superar 255 caracteres")
        String descripcion
) {
}
