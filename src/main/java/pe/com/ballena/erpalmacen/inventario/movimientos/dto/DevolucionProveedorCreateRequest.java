package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record DevolucionProveedorCreateRequest(
        @NotNull(message = "El movimiento origen es obligatorio")
        Long movimientoOrigenId,

        Long proveedorId,

        LocalDateTime fechaMovimiento,

        LocalDate fechaDocumento,

        @Size(max = 50, message = "El numero de nota de credito no debe superar 50 caracteres")
        String numeroNotaCredito,

        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 150, message = "El motivo no debe superar 150 caracteres")
        String motivo,

        @Size(max = 500, message = "La observacion no debe superar 500 caracteres")
        String observacion,

        Boolean confirmar,

        @Valid
        @NotEmpty(message = "La devolucion debe tener al menos un detalle")
        List<DevolucionProveedorDetalleCreateRequest> detalles
) {
}
