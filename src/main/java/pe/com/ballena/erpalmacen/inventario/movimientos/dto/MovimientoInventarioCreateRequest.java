package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.util.List;

public record MovimientoInventarioCreateRequest(
        @NotNull(message = "El tipo de movimiento es obligatorio")
        TipoMovimientoInventario tipoMovimiento,

        Long proveedorId,

        Long almacenOrigenId,

        Long almacenDestinoId,

        @Size(max = 100, message = "El documento de referencia no puede superar 100 caracteres")
        String documentoReferencia,

        @Size(max = 500, message = "La observacion no puede superar 500 caracteres")
        String observacion,

        @Valid
        @NotEmpty(message = "El movimiento debe tener al menos un detalle")
        List<MovimientoDetalleCreateRequest> detalles
) {
}
