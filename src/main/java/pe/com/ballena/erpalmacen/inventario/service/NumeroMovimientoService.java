package pe.com.ballena.erpalmacen.inventario.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoCorrelativoEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoCorrelativoRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.util.Locale;

@Service
public class NumeroMovimientoService {

    private static final String PREFIJO_MOVIMIENTO = "MOV";
    private static final String PREFIJO_DEVOLUCION = "DEV";
    private static final int CORRELATIVO_LENGTH = 6;

    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MovimientoCorrelativoRepository movimientoCorrelativoRepository;

    public NumeroMovimientoService(
            MovimientoInventarioRepository movimientoInventarioRepository,
            MovimientoCorrelativoRepository movimientoCorrelativoRepository
    ) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.movimientoCorrelativoRepository = movimientoCorrelativoRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String generar() {
        return generar(PREFIJO_MOVIMIENTO);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String generar(TipoMovimientoInventario tipoMovimiento) {
        if (tipoMovimiento == TipoMovimientoInventario.DEVOLUCION_PROVEEDOR) {
            return generar(PREFIJO_DEVOLUCION);
        }
        return generar();
    }

    private String generar(String prefijo) {
        MovimientoCorrelativoEntity correlativo = movimientoCorrelativoRepository.findByPrefijoForUpdate(prefijo)
                .orElseThrow(() -> new IllegalStateException("No existe correlativo configurado para el prefijo " + prefijo));

        long siguiente = correlativo.getUltimoCorrelativo() + 1;
        String numero = construirNumero(prefijo, siguiente);
        while (movimientoInventarioRepository.existsByNumero(numero)) {
            siguiente++;
            numero = construirNumero(prefijo, siguiente);
        }

        correlativo.setUltimoCorrelativo(siguiente);
        return numero;
    }

    private String construirNumero(String prefijo, long correlativo) {
        return prefijo + "-" + String.format(Locale.ROOT, "%0" + CORRELATIVO_LENGTH + "d", correlativo);
    }
}
