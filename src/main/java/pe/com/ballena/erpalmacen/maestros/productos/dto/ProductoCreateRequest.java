package pe.com.ballena.erpalmacen.maestros.productos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductoCreateRequest(
        @NotBlank(message = "El codigo es obligatorio")
        @Size(max = 50, message = "El codigo no debe superar 50 caracteres")
        String codigo,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre no debe superar 200 caracteres")
        String nombre,

        @Size(max = 255, message = "La descripcion no debe superar 255 caracteres")
        String descripcion,

        @NotNull(message = "El tipo de articulo es obligatorio")
        Long tipoArticuloId,

        @NotNull(message = "La familia es obligatoria")
        Long familiaId,

        Long subfamiliaId,

        Long marcaId,

        @NotNull(message = "La unidad de medida es obligatoria")
        Long unidadMedidaId,

        @DecimalMin(value = "0.0000", message = "El stock minimo no puede ser negativo")
        BigDecimal stockMinimo,

        @DecimalMin(value = "0.0000", message = "El stock maximo no puede ser negativo")
        BigDecimal stockMaximo,

        @DecimalMin(value = "0.0000", message = "El costo referencial no puede ser negativo")
        BigDecimal costoReferencial,

        Boolean controlaLote,

        Boolean controlaSerie
) {
}
