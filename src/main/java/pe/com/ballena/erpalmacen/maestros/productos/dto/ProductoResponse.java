package pe.com.ballena.erpalmacen.maestros.productos.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String codigo,
        String nombre,
        String descripcion,
        Long tipoArticuloId,
        String tipoArticuloCodigo,
        String tipoArticuloNombre,
        Long familiaId,
        String familiaNombre,
        Long subfamiliaId,
        String subfamiliaNombre,
        Long marcaId,
        String marcaNombre,
        Long unidadMedidaId,
        String unidadMedidaNombre,
        BigDecimal stockMinimo,
        BigDecimal stockMaximo,
        BigDecimal costoReferencial,
        boolean controlaLote,
        boolean controlaSerie,
        boolean activo,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn
) {
}
