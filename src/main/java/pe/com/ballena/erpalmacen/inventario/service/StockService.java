package pe.com.ballena.erpalmacen.inventario.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
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

    private static final int COST_SCALE = 6;

    private final StockActualRepository stockActualRepository;
    private final JdbcTemplate jdbcTemplate;
    private final String stockActualTable;

    public StockService(
            StockActualRepository stockActualRepository,
            JdbcTemplate jdbcTemplate,
            @Value("${spring.jpa.properties.hibernate.default_schema:}") String defaultSchema
    ) {
        this.stockActualRepository = stockActualRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.stockActualTable = resolverTabla(defaultSchema, "stock_actual");
    }

    public StockResultado registrarEntrada(
            ProductoEntity producto,
            AlmacenEntity almacen,
            UbicacionAlmacenEntity ubicacion,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            LocalDateTime fechaMovimiento
    ) {
        asegurarFilaStock(producto, almacen, ubicacion);
        StockActualEntity stock = bloquearStock(producto, almacen, ubicacion);

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

    public StockActualEntity bloquearStock(
            ProductoEntity producto,
            AlmacenEntity almacen,
            UbicacionAlmacenEntity ubicacion
    ) {
        return buscarConBloqueo(producto.getId(), almacen.getId(), ubicacion)
                .orElseThrow(() -> new BusinessException("No existe stock para el producto en el almacen indicado"));
    }

    public StockResultado revertirEntrada(
            StockActualEntity stock,
            BigDecimal cantidad,
            BigDecimal costoUnitario,
            LocalDateTime fechaMovimiento
    ) {
        BigDecimal saldoAnterior = stock.getCantidadActual();
        if (saldoAnterior.compareTo(cantidad) < 0) {
            throw new BusinessException("Stock insuficiente para confirmar el movimiento");
        }

        BigDecimal saldoFinal = saldoAnterior.subtract(cantidad);
        stock.setCantidadActual(saldoFinal);
        stock.setCostoPromedio(calcularCostoPromedioRevertido(
                saldoAnterior,
                stock.getCostoPromedio(),
                cantidad,
                costoUnitario,
                saldoFinal
        ));
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

    private void asegurarFilaStock(
            ProductoEntity producto,
            AlmacenEntity almacen,
            UbicacionAlmacenEntity ubicacion
    ) {
        if (ubicacion == null) {
            jdbcTemplate.update("""
                    insert into %s (producto_id, almacen_id, ubicacion_id, cantidad_actual, creado_en)
                    values (?, ?, null, 0, current_timestamp)
                    on conflict (producto_id, almacen_id) where ubicacion_id is null do nothing
                    """.formatted(stockActualTable), producto.getId(), almacen.getId());
            return;
        }

        jdbcTemplate.update("""
                insert into %s (producto_id, almacen_id, ubicacion_id, cantidad_actual, creado_en)
                values (?, ?, ?, 0, current_timestamp)
                on conflict (producto_id, almacen_id, ubicacion_id) where ubicacion_id is not null do nothing
                """.formatted(stockActualTable), producto.getId(), almacen.getId(), ubicacion.getId());
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

    private BigDecimal calcularCostoPromedioRevertido(
            BigDecimal cantidadAnterior,
            BigDecimal costoPromedioAnterior,
            BigDecimal cantidadEntrada,
            BigDecimal costoUnitario,
            BigDecimal cantidadFinal
    ) {
        if (cantidadFinal.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        if (costoUnitario == null) {
            return costoPromedioAnterior;
        }

        BigDecimal costoAnterior = costoPromedioAnterior == null ? BigDecimal.ZERO : costoPromedioAnterior;
        BigDecimal valorAnterior = cantidadAnterior.multiply(costoAnterior);
        BigDecimal valorEntrada = cantidadEntrada.multiply(costoUnitario);
        BigDecimal valorFinal = valorAnterior.subtract(valorEntrada);
        if (valorFinal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El valor de stock no puede quedar negativo al anular la entrada");
        }
        return valorFinal.divide(cantidadFinal, COST_SCALE, RoundingMode.HALF_UP);
    }

    private String resolverTabla(String schema, String table) {
        String cleanSchema = schema == null ? "" : schema.trim();
        if (cleanSchema.isBlank()) {
            return table;
        }
        if (!cleanSchema.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Nombre de esquema invalido para consultas de stock");
        }
        return cleanSchema + "." + table;
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
