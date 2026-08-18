package pe.com.ballena.erpalmacen.reportes.inventario.pdf;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.reportes.inventario.dto.ArchivoReporteResponse;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MovimientoPdfService {

    private final PdfReportService pdfReportService;

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${erp.reportes.empresa.nombre:ERP Almacen}")
    private String nombreEmpresa;

    public MovimientoPdfService(PdfReportService pdfReportService) {
        this.pdfReportService = pdfReportService;
    }

    @Transactional(readOnly = true)
    public ArchivoReporteResponse generarMovimientoPdf(Long movimientoId, boolean verCostos, String generadoPor) {
        MovimientoInventarioEntity movimiento = obtenerMovimiento(movimientoId);
        List<MovimientoDetalleEntity> detalles = obtenerDetalles(movimientoId);
        Map<String, Object> model = new HashMap<>();
        model.put("empresa", nombreEmpresa);
        model.put("documentoNombre", nombreDocumento(movimiento.getTipoMovimiento()));
        model.put("movimiento", movimiento);
        model.put("proveedor", movimiento.getProveedor());
        model.put("almacenOrigen", movimiento.getAlmacenOrigen());
        model.put("almacenDestino", movimiento.getAlmacenDestino());
        model.put("detalles", detalles);
        model.put("mostrarCostos", verCostos);
        model.put("generadoPor", generadoPor == null ? "sistema" : generadoPor);
        model.put("generadoEn", LocalDateTime.now());
        byte[] pdf = pdfReportService.render("reportes/movimiento", model);
        return new ArchivoReporteResponse(filename(movimiento), pdf);
    }

    private MovimientoInventarioEntity obtenerMovimiento(Long movimientoId) {
        List<MovimientoInventarioEntity> movimientos = entityManager.createQuery("""
                        select m
                        from MovimientoInventarioEntity m
                        join fetch m.usuario u
                        left join fetch m.proveedor pr
                        left join fetch m.almacenOrigen ao
                        left join fetch m.almacenDestino ad
                        where m.id = :movimientoId
                        """, MovimientoInventarioEntity.class)
                .setParameter("movimientoId", movimientoId)
                .getResultList();
        if (movimientos.isEmpty()) {
            throw new ResourceNotFoundException("Movimiento no encontrado");
        }
        return movimientos.get(0);
    }

    private List<MovimientoDetalleEntity> obtenerDetalles(Long movimientoId) {
        return entityManager.createQuery("""
                        select d
                        from MovimientoDetalleEntity d
                        join fetch d.producto p
                        join fetch p.unidadMedida um
                        left join fetch d.ubicacionOrigen uo
                        left join fetch d.ubicacionDestino ud
                        where d.movimiento.id = :movimientoId
                        order by d.id asc
                        """, MovimientoDetalleEntity.class)
                .setParameter("movimientoId", movimientoId)
                .getResultList();
    }

    private String nombreDocumento(TipoMovimientoInventario tipoMovimiento) {
        return switch (tipoMovimiento) {
            case ENTRADA_COMPRA -> "NOTA DE INGRESO";
            case SALIDA_CONSUMO, SALIDA_VENTA -> "VALE DE SALIDA";
            case TRANSFERENCIA -> "NOTA DE TRANSFERENCIA";
            case ENTRADA_AJUSTE, SALIDA_AJUSTE, AJUSTE_POSITIVO, AJUSTE_NEGATIVO -> "COMPROBANTE DE AJUSTE";
            case DEVOLUCION_PROVEEDOR -> "DEVOLUCION A PROVEEDOR";
            default -> "COMPROBANTE DE MOVIMIENTO";
        };
    }

    private String filename(MovimientoInventarioEntity movimiento) {
        String base = nombreDocumento(movimiento.getTipoMovimiento())
                .toLowerCase()
                .replace(" ", "-")
                .replaceAll("[^a-z0-9\\-]", "");
        return base + "-" + sanitize(movimiento.getNumero()) + ".pdf";
    }

    private String sanitize(String value) {
        return value == null ? "sin-numero" : value.replaceAll("[^A-Za-z0-9\\-]", "-");
    }
}
