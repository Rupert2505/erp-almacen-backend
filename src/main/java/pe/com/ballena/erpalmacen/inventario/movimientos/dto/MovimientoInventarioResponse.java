package pe.com.ballena.erpalmacen.inventario.movimientos.dto;

import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.time.LocalDateTime;
import java.util.List;

public record MovimientoInventarioResponse(
        Long id,
        String numero,
        TipoMovimientoInventario tipoMovimiento,
        EstadoMovimientoInventario estado,
        LocalDateTime fechaMovimiento,
        Long proveedorId,
        String proveedorNumeroDocumento,
        String proveedorRazonSocial,
        Long almacenOrigenId,
        String almacenOrigenCodigo,
        String almacenOrigenNombre,
        Long almacenDestinoId,
        String almacenDestinoCodigo,
        String almacenDestinoNombre,
        String documentoReferencia,
        String observacion,
        Long usuarioId,
        String username,
        LocalDateTime confirmadoEn,
        LocalDateTime anuladoEn,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn,
        List<MovimientoDetalleResponse> detalles
) {
}
