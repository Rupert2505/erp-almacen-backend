package pe.com.ballena.erpalmacen.maestros.proveedores.dto;

import java.time.LocalDateTime;

public record ProveedorResponse(
        Long id,
        String tipoDocumento,
        String numeroDocumento,
        String razonSocial,
        String nombreComercial,
        String direccion,
        String telefono,
        String email,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
