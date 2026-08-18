package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DevolucionProveedorDetalleCreateRequest(
        @NotNull(message = "El detalle origen es obligatorio")
        Long movimientoDetalleOrigenId,

        @NotNull(message = "El producto es obligatorio")
        Long productoId,

        @NotNull(message = "La cantidad es obligatoria")
        @DecimalMin(value = "0.0001", message = "La cantidad debe ser mayor que cero")
        BigDecimal cantidad,

        @Size(max = 255, message = "La observacion del detalle no debe superar 255 caracteres")
        String observacion
) {
}
