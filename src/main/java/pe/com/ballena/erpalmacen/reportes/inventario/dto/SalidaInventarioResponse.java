package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalidaInventarioResponse(
        Long movimientoId,
        String numeroMovimiento,
        TipoMovimientoInventario tipoMovimiento,
        LocalDateTime fechaMovimiento,
        String almacenNombre,
        String productoCodigo,
        String productoNombre,
        BigDecimal cantidad,
        BigDecimal costoReferencial,
        String observacion
) {
}
