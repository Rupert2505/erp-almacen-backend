package pe.com.ballena.erpalmacen.inventario.service;

import org.springframework.stereotype.Service;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.inventario.kardex.entity.KardexEntity;
import pe.com.ballena.erpalmacen.inventario.kardex.repository.KardexRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class KardexService {

    private final KardexRepository kardexRepository;

    public KardexService(KardexRepository kardexRepository) {
        this.kardexRepository = kardexRepository;
    }

    public KardexEntity registrar(
            MovimientoInventarioEntity movimiento,
            MovimientoDetalleEntity detalle,
            AlmacenEntity almacen,
            UbicacionAlmacenEntity ubicacion,
            BigDecimal entrada,
            BigDecimal salida,
            StockService.StockResultado stockResultado,
            String observacion
    ) {
        return registrarEnFecha(
                movimiento,
                detalle,
                almacen,
                ubicacion,
                entrada,
                salida,
                stockResultado,
                movimiento.getFechaMovimiento(),
                observacion
        );
    }

    public KardexEntity registrarEnFecha(
            MovimientoInventarioEntity movimiento,
            MovimientoDetalleEntity detalle,
            AlmacenEntity almacen,
            UbicacionAlmacenEntity ubicacion,
            BigDecimal entrada,
            BigDecimal salida,
            StockService.StockResultado stockResultado,
            LocalDateTime fechaMovimiento,
            String observacion
    ) {
        KardexEntity kardex = new KardexEntity();
        kardex.setMovimiento(movimiento);
        kardex.setMovimientoDetalle(detalle);
        kardex.setProducto(detalle.getProducto());
        kardex.setAlmacen(almacen);
        kardex.setUbicacion(ubicacion);
        kardex.setFechaMovimiento(fechaMovimiento);
        kardex.setTipoMovimiento(movimiento.getTipoMovimiento());
        kardex.setNumeroMovimiento(movimiento.getNumero());
        kardex.setEntrada(entrada);
        kardex.setSalida(salida);
        kardex.setSaldoAnterior(stockResultado.saldoAnterior());
        kardex.setSaldoFinal(stockResultado.saldoFinal());
        kardex.setCostoUnitario(stockResultado.costoUnitario());
        kardex.setCostoPromedio(stockResultado.costoPromedio());
        kardex.setUsuario(movimiento.getUsuario());
        kardex.setObservacion(observacion);
        return kardexRepository.save(kardex);
    }

    public List<KardexEntity> findByMovimientoDetalle(Long movimientoId, Long detalleId) {
        return kardexRepository.findByMovimientoDetalle(movimientoId, detalleId);
    }

    public boolean existsPosteriorSinUbicacion(KardexEntity kardexOriginal, Long movimientoId) {
        return kardexRepository.existsPosteriorSinUbicacion(
                kardexOriginal.getProducto().getId(),
                kardexOriginal.getAlmacen().getId(),
                movimientoId,
                kardexOriginal.getFechaMovimiento(),
                kardexOriginal.getId()
        );
    }

    public boolean existsPosteriorConUbicacion(KardexEntity kardexOriginal, Long movimientoId) {
        return kardexRepository.existsPosteriorConUbicacion(
                kardexOriginal.getProducto().getId(),
                kardexOriginal.getAlmacen().getId(),
                kardexOriginal.getUbicacion().getId(),
                movimientoId,
                kardexOriginal.getFechaMovimiento(),
                kardexOriginal.getId()
        );
    }
}
