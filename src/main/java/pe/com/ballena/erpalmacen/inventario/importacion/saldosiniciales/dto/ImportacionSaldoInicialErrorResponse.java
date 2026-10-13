package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.dto;

import java.time.LocalDateTime;

public record ImportacionSaldoInicialErrorResponse(
        Long id,
        Long stagingId,
        String loteMigracion,
        String codigoMovimientoExterno,
        String lineaOrigen,
        String codigoError,
        String mensaje,
        LocalDateTime creadoEn
) {
}
