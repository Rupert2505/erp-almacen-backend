package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ImportacionSaldoInicialTrazaResponse(
        Long id,
        Long stagingId,
        String loteMigracion,
        String sistemaOrigen,
        String idOrigenCabecera,
        String idOrigenDetalle,
        String codigoMovimientoExterno,
        String lineaOrigen,
        String codigoProducto,
        String codigoAlmacenDestino,
        String codigoUbicacionDestino,
        BigDecimal cantidad,
        BigDecimal costoUnitarioInventariable,
        Long movimientoId,
        Long movimientoDetalleId,
        String numeroMovimiento,
        LocalDateTime creadoEn
) {
}
