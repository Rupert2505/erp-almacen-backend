package pe.com.ballena.erpalmacen.inventario.service;

import org.springframework.stereotype.Service;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.inventario.stock.entity.StockActualEntity;
import pe.com.ballena.erpalmacen.inventario.stock.repository.StockActualRepository;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class StockService {

    private static final int COST_SCALE = 4;

    private final StockActualRepository stockActualRepository;

    public StockService(StockActualRepository stockActualRepository) {
        this.stockActualRepository = stockActualRepository;
    }

    public StockResultado registrarEntrada(
            ProductoEntity producto,
            AlmacenEntity almacen,
            UbicacionAlmacenEntity ubicacion,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            LocalDateTime fechaMovimiento
    ) {
        StockActualEntity stock = buscarConBloqueo(producto.getId(), almacen.getId(), ubicacion)
                .orElseGet(() -> crearStock(producto, almacen, ubicacion));

        BigDecimal saldoAnterior = stock.getCantidadActual();
        BigDecimal costoPromedioAnterior = stock.getCostoPromedio();
        BigDecimal saldoFinal = saldoAnterior.add(cantidad);

        stock.setCantidadActual(saldoFinal);
        if (costoUnitario != null) {
            stock.setCostoPromedio(calcularCostoPromedioPonderado(
                    saldoAnterior,
                    costoPromedioAnterior,
                    cantidad,
                    costoUnitario,
                    saldoFinal
            ));
        }
        stock.setFechaUltimoMovimiento(fechaMovimiento);

        StockActualEntity guardado = stockActualRepository.save(stock);
        return new StockResultado(
                guardado,
                saldoAnterior,
                saldoFinal,
                costoUnitario,
                guardado.getCostoPromedio()
        );
    }

    public StockResultado registrarSalida(
            ProductoEntity producto,
            AlmacenEntity almacen,
            UbicacionAlmacenEntity ubicacion,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            LocalDateTime fechaMovimiento
    ) {
        StockActualEntity stock = buscarConBloqueo(producto.getId(), almacen.getId(), ubicacion)
                .orElseThrow(() -> new BusinessException("No existe stock para el producto en el almacen indicado"));

        BigDecimal saldoAnterior = stock.getCantidadActual();
        if (saldoAnterior.compareTo(cantidad) < 0) {
            throw new BusinessException("Stock insuficiente para confirmar el movimiento");
        }

        BigDecimal saldoFinal = saldoAnterior.subtract(cantidad);
        stock.setCantidadActual(saldoFinal);
        stock.setFechaUltimoMovimiento(fechaMovimiento);

        StockActualEntity guardado = stockActualRepository.save(stock);
        BigDecimal costoReferencia = costoUnitario != null ? costoUnitario : guardado.getCostoPromedio();
        return new StockResultado(
                guardado,
                saldoAnterior,
                saldoFinal,
                costoReferencia,
                guardado.getCostoPromedio()
        );
    }

    private java.util.Optional<StockActualEntity> buscarConBloqueo(
            Long productoId,
            Long almacenId,
            UbicacionAlmacenEntity ubicacion
    ) {
        if (ubicacion == null) {
            return stockActualRepository.findByProductoAlmacenSinUbicacionForUpdate(productoId, almacenId);
        }
        return stockActualRepository.findByProductoAlmacenUbicacionForUpdate(productoId, almacenId, ubicacion.getId());
    }

    private StockActualEntity crearStock(
            ProductoEntity producto,
            AlmacenEntity almacen,
            UbicacionAlmacenEntity ubicacion
    ) {
        StockActualEntity stock = new StockActualEntity();
        stock.setProducto(producto);
        stock.setAlmacen(almacen);
        stock.setUbicacion(ubicacion);
        stock.setCantidadActual(BigDecimal.ZERO);
        return stock;
    }

    private BigDecimal calcularCostoPromedioPonderado(
            BigDecimal cantidadAnterior,
            BigDecimal costoPromedioAnterior,
            BigDecimal cantidadEntrada,
            BigDecimal costoUnitario,
            BigDecimal cantidadFinal
    ) {
        if (cantidadFinal.compareTo(BigDecimal.ZERO) == 0) {
            return costoPromedioAnterior;
        }

        BigDecimal costoAnterior = costoPromedioAnterior == null ? BigDecimal.ZERO : costoPromedioAnterior;
        BigDecimal valorAnterior = cantidadAnterior.multiply(costoAnterior);
        BigDecimal valorEntrada = cantidadEntrada.multiply(costoUnitario);
        return valorAnterior.add(valorEntrada).divide(cantidadFinal, COST_SCALE, RoundingMode.HALF_UP);
    }

    public record StockResultado(
            StockActualEntity stock,
            BigDecimal saldoAnterior,
            BigDecimal saldoFinal,
            BigDecimal costoUnitario,
            BigDecimal costoPromedio
    ) {
    }
}
