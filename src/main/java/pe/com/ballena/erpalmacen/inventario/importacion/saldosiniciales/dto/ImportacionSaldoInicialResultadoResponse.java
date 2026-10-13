package pe.com.ballena.erpalmacen.inventario.importacion.saldosiniciales.dto;

public record ImportacionSaldoInicialResultadoResponse(
        String loteMigracion,
        long totalFilas,
        long movimientosExternos,
        long lineasPendientes,
        long lineasValidadas,
        long lineasConError,
        long lineasProcesadas,
        long errores,
        long trazas,
        int movimientosProcesadosEnEjecucion,
        int movimientosOmitidosEnEjecucion,
        String estado
) {
}
