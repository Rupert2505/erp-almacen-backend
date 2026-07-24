package pe.com.ballena.erpalmacen.maestros.personal.dto;

import java.time.LocalDateTime;

public record PersonalResponse(
        Long id,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String cargo,
        String area,
        String telefono,
        String email,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
