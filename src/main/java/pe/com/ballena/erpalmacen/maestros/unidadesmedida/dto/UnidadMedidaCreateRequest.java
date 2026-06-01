package pe.com.ballena.erpalmacen.maestros.unidadesmedida.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UnidadMedidaCreateRequest(
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 20, message = "El codigo no debe superar 20 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre,

        @NotBlank(message = "La abreviatura es obligatoria")
        @Size(max = 20, message = "La abreviatura no debe superar 20 caracteres")
        String abreviatura
) {
}
