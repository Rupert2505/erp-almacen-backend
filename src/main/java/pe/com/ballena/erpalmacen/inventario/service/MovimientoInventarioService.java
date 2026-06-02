package pe.com.ballena.erpalmacen.inventario.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoDetalleRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MovimientoInventarioService {

    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final MovimientoDetalleRepository movimientoDetalleRepository;
    private final StockService stockService;
    private final KardexService kardexService;

    public MovimientoInventarioService(
            MovimientoInventarioRepository movimientoInventarioRepository,
            MovimientoDetalleRepository movimientoDetalleRepository,
            StockService stockService,
            KardexService kardexService
    ) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.movimientoDetalleRepository = movimientoDetalleRepository;
        this.stockService = stockService;
        this.kardexService = kardexService;
    }

    @Transactional
    public MovimientoInventarioEntity confirmarMovimiento(Long movimientoId) {
        MovimientoInventarioEntity movimiento = movimientoInventarioRepository.findById(movimientoId)
                .orElseThrow(() -> new ResourceNotFoundException("Movimiento de inventario no encontrado"));

        validarEstadoConfirmable(movimiento);
        List<MovimientoDetalleEntity> detalles = movimientoDetalleRepository.findByMovimientoId(movimientoId);
        if (detalles.isEmpty()) {
            throw new BusinessException("El movimiento debe tener al menos un detalle");
        }

        validarCabecera(movimiento);
        for (MovimientoDetalleEntity detalle : detalles) {
            confirmarDetalle(movimiento, detalle);
        }

        movimiento.setEstado(EstadoMovimientoInventario.CONFIRMADO);
        movimiento.setConfirmadoEn(LocalDateTime.now());
        return movimiento;
    }

    private void confirmarDetalle(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        validarDetalle(detalle);

        TipoMovimientoInventario tipoMovimiento = movimiento.getTipoMovimiento();
        if (esEntrada(tipoMovimiento)) {
            confirmarEntrada(movimiento, detalle);
            return;
        }
        if (esSalida(tipoMovimiento)) {
            confirmarSalida(movimiento, detalle);
            return;
        }
        if (tipoMovimiento == TipoMovimientoInventario.TRANSFERENCIA) {
            confirmarTransferencia(movimiento, detalle);
            return;
        }

        throw new BusinessException("Tipo de movimiento no soportado");
    }

    private void confirmarEntrada(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        AlmacenEntity almacenDestino = movimiento.getAlmacenDestino();
        UbicacionAlmacenEntity ubicacionDestino = detalle.getUbicacionDestino();
        validarAlmacenActivo(almacenDestino, "almacen destino");
        validarUbicacionActivaYCoherente(ubicacionDestino, almacenDestino, "ubicacion destino");

        StockService.StockResultado stockResultado = stockService.registrarEntrada(
                detalle.getProducto(),
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                movimiento.getFechaMovimiento()
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                BigDecimal.ZERO,
                stockResultado,
                resolverObservacion(movimiento, detalle)
        );
    }

    private void confirmarSalida(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        UbicacionAlmacenEntity ubicacionOrigen = detalle.getUbicacionOrigen();
        validarAlmacenActivo(almacenOrigen, "almacen origen");
        validarUbicacionActivaYCoherente(ubicacionOrigen, almacenOrigen, "ubicacion origen");

        StockService.StockResultado stockResultado = stockService.registrarSalida(
                detalle.getProducto(),
                almacenOrigen,
                ubicacionOrigen,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                movimiento.getFechaMovimiento()
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenOrigen,
                ubicacionOrigen,
                BigDecimal.ZERO,
                detalle.getCantidad(),
                stockResultado,
                resolverObservacion(movimiento, detalle)
        );
    }

    private void confirmarTransferencia(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        AlmacenEntity almacenOrigen = movimiento.getAlmacenOrigen();
        AlmacenEntity almacenDestino = movimiento.getAlmacenDestino();
        UbicacionAlmacenEntity ubicacionOrigen = detalle.getUbicacionOrigen();
        UbicacionAlmacenEntity ubicacionDestino = detalle.getUbicacionDestino();

        validarAlmacenActivo(almacenOrigen, "almacen origen");
        validarAlmacenActivo(almacenDestino, "almacen destino");
        if (almacenOrigen.getId().equals(almacenDestino.getId())) {
            throw new BusinessException("El almacen origen no puede ser igual al almacen destino");
        }
        validarUbicacionActivaYCoherente(ubicacionOrigen, almacenOrigen, "ubicacion origen");
        validarUbicacionActivaYCoherente(ubicacionDestino, almacenDestino, "ubicacion destino");

        StockService.StockResultado salida = stockService.registrarSalida(
                detalle.getProducto(),
                almacenOrigen,
                ubicacionOrigen,
                detalle.getCantidad(),
                detalle.getCostoUnitario(),
                movimiento.getFechaMovimiento()
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenOrigen,
                ubicacionOrigen,
                BigDecimal.ZERO,
                detalle.getCantidad(),
                salida,
                resolverObservacion(movimiento, detalle)
        );

        BigDecimal costoTransferencia = detalle.getCostoUnitario() != null
                ? detalle.getCostoUnitario()
                : salida.costoPromedio();
        StockService.StockResultado entrada = stockService.registrarEntrada(
                detalle.getProducto(),
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                costoTransferencia,
                movimiento.getFechaMovimiento()
        );

        kardexService.registrar(
                movimiento,
                detalle,
                almacenDestino,
                ubicacionDestino,
                detalle.getCantidad(),
                BigDecimal.ZERO,
                entrada,
                resolverObservacion(movimiento, detalle)
        );
    }

    private void validarEstadoConfirmable(MovimientoInventarioEntity movimiento) {
        if (movimiento.getEstado() == EstadoMovimientoInventario.CONFIRMADO) {
            throw new BusinessException("El movimiento ya fue confirmado");
        }
        if (movimiento.getEstado() == EstadoMovimientoInventario.ANULADO) {
            throw new BusinessException("No se puede confirmar un movimiento anulado");
        }
        if (movimiento.getEstado() != EstadoMovimientoInventario.BORRADOR) {
            throw new BusinessException("Solo se pueden confirmar movimientos en estado BORRADOR");
        }
    }

    private void validarCabecera(MovimientoInventarioEntity movimiento) {
        if (movimiento.getTipoMovimiento() == null) {
            throw new BusinessException("El tipo de movimiento es obligatorio");
        }
        if (movimiento.getUsuario() == null || !movimiento.getUsuario().isActivo()) {
            throw new BusinessException("El usuario del movimiento es obligatorio y debe estar activo");
        }
        if (movimiento.getTipoMovimiento() == TipoMovimientoInventario.ENTRADA_COMPRA && movimiento.getProveedor() == null) {
            throw new BusinessException("La entrada por compra requiere proveedor");
        }
        if (esEntrada(movimiento.getTipoMovimiento()) && movimiento.getAlmacenDestino() == null) {
            throw new BusinessException("El movimiento requiere almacen destino");
        }
        if (esSalida(movimiento.getTipoMovimiento()) && movimiento.getAlmacenOrigen() == null) {
            throw new BusinessException("El movimiento requiere almacen origen");
        }
        if (movimiento.getTipoMovimiento() == TipoMovimientoInventario.TRANSFERENCIA
                && (movimiento.getAlmacenOrigen() == null || movimiento.getAlmacenDestino() == null)) {
            throw new BusinessException("La transferencia requiere almacen origen y destino");
        }
    }

    private void validarDetalle(MovimientoDetalleEntity detalle) {
        ProductoEntity producto = detalle.getProducto();
        if (producto == null || !producto.isActivo()) {
            throw new BusinessException("El producto del detalle es obligatorio y debe estar activo");
        }
        if (detalle.getCantidad() == null || detalle.getCantidad().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La cantidad del detalle debe ser mayor que cero");
        }
    }

    private void validarAlmacenActivo(AlmacenEntity almacen, String etiqueta) {
        if (almacen == null || !almacen.isActivo()) {
            throw new BusinessException("El " + etiqueta + " es obligatorio y debe estar activo");
        }
    }

    private void validarUbicacionActivaYCoherente(
            UbicacionAlmacenEntity ubicacion,
            AlmacenEntity almacen,
            String etiqueta
    ) {
        if (ubicacion == null) {
            return;
        }
        if (!ubicacion.isActivo()) {
            throw new BusinessException("La " + etiqueta + " debe estar activa");
        }
        if (!ubicacion.getAlmacen().getId().equals(almacen.getId())) {
            throw new BusinessException("La " + etiqueta + " no pertenece al almacen indicado");
        }
    }

    private boolean esEntrada(TipoMovimientoInventario tipoMovimiento) {
        return tipoMovimiento == TipoMovimientoInventario.ENTRADA_COMPRA
                || tipoMovimiento == TipoMovimientoInventario.ENTRADA_AJUSTE
                || tipoMovimiento == TipoMovimientoInventario.AJUSTE_POSITIVO;
    }

    private boolean esSalida(TipoMovimientoInventario tipoMovimiento) {
        return tipoMovimiento == TipoMovimientoInventario.SALIDA_CONSUMO
                || tipoMovimiento == TipoMovimientoInventario.SALIDA_VENTA
                || tipoMovimiento == TipoMovimientoInventario.SALIDA_AJUSTE
                || tipoMovimiento == TipoMovimientoInventario.AJUSTE_NEGATIVO;
    }

    private String resolverObservacion(MovimientoInventarioEntity movimiento, MovimientoDetalleEntity detalle) {
        return detalle.getObservacion() != null ? detalle.getObservacion() : movimiento.getObservacion();
    }
}
