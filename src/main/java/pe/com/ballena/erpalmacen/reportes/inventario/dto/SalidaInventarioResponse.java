package pe.com.ballena.erpalmacen.reportes.inventario.dto;

import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SalidaInventarioResponse(
        LocalDateTime fecha,
        Long movimientoId,
        String numeroMovimiento,
        TipoMovimientoInventario tipoMovimiento,
        pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario estado,
        String almacenOrigen,
        String almacenOrigenCodigo,
        String ubicacionOrigen,
        Long productoId,
        String codigoProducto,
        String nombreProducto,
        String familia,
        String unidadMedida,
        String unidadMedidaAbreviatura,
        BigDecimal cantidad,
        BigDecimal costoPromedioAplicado,
        BigDecimal valorSalida,
        String usuario,
        String motivo,
        String observacion
) {
}
