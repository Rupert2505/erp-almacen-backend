package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MovimientoDetalleCreateRequest(
        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        Long ubicacionOrigenId,

        Long ubicacionDestinoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad,

        @DecimalMin(value = "0.0000", message = "El costo unitario no puede ser negativo")
        BigDecimal costoUnitario,

        String observacion
) {
}
