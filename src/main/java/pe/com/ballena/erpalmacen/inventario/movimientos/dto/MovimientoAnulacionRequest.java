package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MovimientoAnulacionRequest(
        @NotBlank(message = "El motivo de anulacion es obligatorio")
        @Size(max = 500, message = "El motivo de anulacion no puede superar 500 caracteres")
        String motivo
) {
}
