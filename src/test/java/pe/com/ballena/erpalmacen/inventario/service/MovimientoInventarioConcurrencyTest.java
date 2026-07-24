package pe.com.ballena.erpalmacen.inventario.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.almacenes.repository.AlmacenRepository;
import pe.com.ballena.erpalmacen.inventario.kardex.repository.KardexRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.dto.MovimientoCancelacionRequest;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoDetalleRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.stock.repository.StockActualRepository;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaRepository;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.entity.TipoArticuloEntity;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;
import pe.com.ballena.erpalmacen.usuarios.repository.UsuarioRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MovimientoInventarioConcurrencyTest {

    @Autowired
    private MovimientoInventarioService movimientoInventarioService;

    @Autowired
    private MovimientoInventarioRepository movimientoInventarioRepository;

    @Autowired
    private MovimientoDetalleRepository movimientoDetalleRepository;

    @Autowired
    private StockActualRepository stockActualRepository;

    @Autowired
    private KardexRepository kardexRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UnidadMedidaRepository unidadMedidaRepository;

    @Autowired
    private FamiliaRepository familiaRepository;

    @Autowired
    private TipoArticuloRepository tipoArticuloRepository;

    @Autowired
    private AlmacenRepository almacenRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void confirmarMismoBorradorConcurrentementeSoloConfirmaUnaVez() throws Exception {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity movimiento = crearMovimientoEntrada(data, new BigDecimal("5.0000"));

        List<ResultadoConcurrente> resultados = ejecutarConcurrentemente(
                () -> {
                    movimientoInventarioService.confirmarMovimiento(movimiento.getId());
                    return ResultadoConcurrente.exito();
                },
                () -> {
                    movimientoInventarioService.confirmarMovimiento(movimiento.getId());
                    return ResultadoConcurrente.exito();
                }
        );

        assertThat(resultados).filteredOn(ResultadoConcurrente::exitoso).hasSize(1);
        assertThat(resultados).filteredOn(resultado -> !resultado.exitoso()).hasSize(1);
        assertThat(resultados)
                .filteredOn(resultado -> !resultado.exitoso())
                .allSatisfy(resultado -> assertThat(resultado.error())
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("confirmado"));

        MovimientoInventarioEntity actualizado = movimientoInventarioRepository.findById(movimiento.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoMovimientoInventario.CONFIRMADO);
        assertThat(kardexRepository.findByMovimientoId(movimiento.getId())).hasSize(1);
        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow().getCantidadActual()).isEqualByComparingTo("5.0000");
    }

    @Test
    void confirmarYCancelarMismoBorradorConcurrentementeDejaUnSoloEstadoFinal() throws Exception {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity movimiento = crearMovimientoEntrada(data, new BigDecimal("6.0000"));

        List<ResultadoConcurrente> resultados = ejecutarConcurrentemente(
                () -> {
                    movimientoInventarioService.confirmarMovimiento(movimiento.getId(), authentication());
                    return ResultadoConcurrente.exito();
                },
                () -> {
                    movimientoInventarioService.cancelarMovimiento(
                            movimiento.getId(),
                            new MovimientoCancelacionRequest("Cancelacion concurrente"),
                            authentication()
                    );
                    return ResultadoConcurrente.exito();
                }
        );

        assertThat(resultados).filteredOn(ResultadoConcurrente::exitoso).hasSize(1);
        assertThat(resultados).filteredOn(resultado -> !resultado.exitoso()).hasSize(1);

        MovimientoInventarioEntity actualizado = movimientoInventarioRepository.findById(movimiento.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isIn(EstadoMovimientoInventario.CONFIRMADO, EstadoMovimientoInventario.CANCELADO);

        var stock = stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        );
        if (actualizado.getEstado() == EstadoMovimientoInventario.CONFIRMADO) {
            assertThat(stock).isPresent();
            assertThat(stock.orElseThrow().getCantidadActual()).isEqualByComparingTo("6.0000");
            assertThat(kardexRepository.findByMovimientoId(movimiento.getId())).hasSize(1);
        } else {
            assertThat(stock).isEmpty();
            assertThat(kardexRepository.findByMovimientoId(movimiento.getId())).isEmpty();
            assertThat(actualizado.getMotivoCancelacion()).isEqualTo("Cancelacion concurrente");
        }
    }

    @Test
    void salidasConcurrentesSobreMismoStockNoPermitenStockNegativo() throws Exception {
        TestData data = crearDatosBase();
        MovimientoInventarioEntity entrada = crearMovimientoEntrada(data, new BigDecimal("5.0000"));
        movimientoInventarioService.confirmarMovimiento(entrada.getId());

        MovimientoInventarioEntity salida1 = crearMovimientoSalida(data, new BigDecimal("4.0000"));
        MovimientoInventarioEntity salida2 = crearMovimientoSalida(data, new BigDecimal("4.0000"));

        List<ResultadoConcurrente> resultados = ejecutarConcurrentemente(
                () -> {
                    movimientoInventarioService.confirmarMovimiento(salida1.getId());
                    return ResultadoConcurrente.exito();
                },
                () -> {
                    movimientoInventarioService.confirmarMovimiento(salida2.getId());
                    return ResultadoConcurrente.exito();
                }
        );

        assertThat(resultados).filteredOn(ResultadoConcurrente::exitoso).hasSize(1);
        assertThat(resultados)
                .filteredOn(resultado -> !resultado.exitoso())
                .allSatisfy(resultado -> assertThat(resultado.error())
                        .isInstanceOf(BusinessException.class)
                        .hasMessageContaining("Stock insuficiente"));

        assertThat(stockActualRepository.findByProductoIdAndAlmacenIdAndUbicacionIsNull(
                data.producto().getId(),
                data.almacenOrigen().getId()
        ).orElseThrow().getCantidadActual()).isEqualByComparingTo("1.0000");

        List<MovimientoInventarioEntity> salidas = movimientoInventarioRepository.findAllById(
                List.of(salida1.getId(), salida2.getId())
        );
        assertThat(salidas).filteredOn(movimiento -> movimiento.getEstado() == EstadoMovimientoInventario.CONFIRMADO)
                .hasSize(1);
        assertThat(salidas).filteredOn(movimiento -> movimiento.getEstado() == EstadoMovimientoInventario.BORRADOR)
                .hasSize(1);
        assertThat(kardexRepository.findByMovimientoId(entrada.getId())).hasSize(1);
        assertThat(kardexRepository.findByMovimientoId(salida1.getId()).size()
                + kardexRepository.findByMovimientoId(salida2.getId()).size()).isEqualTo(1);
    }

    @SafeVarargs
    private List<ResultadoConcurrente> ejecutarConcurrentemente(Callable<ResultadoConcurrente>... tareas)
            throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tareas.length);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            List<Future<ResultadoConcurrente>> futures = new ArrayList<>();
            for (Callable<ResultadoConcurrente> tarea : tareas) {
                futures.add(executor.submit(() -> {
                    inicio.await(5, TimeUnit.SECONDS);
                    try {
                        return tarea.call();
                    } catch (Exception exception) {
                        return ResultadoConcurrente.error(exception);
                    }
                }));
            }
            inicio.countDown();

            List<ResultadoConcurrente> resultados = new ArrayList<>();
            for (Future<ResultadoConcurrente> future : futures) {
                resultados.add(future.get(20, TimeUnit.SECONDS));
            }
            return resultados;
        } finally {
            executor.shutdownNow();
        }
    }

    private MovimientoInventarioEntity crearMovimientoEntrada(TestData data, BigDecimal cantidad) {
        MovimientoInventarioEntity movimiento = crearMovimiento(
                TipoMovimientoInventario.ENTRADA_AJUSTE,
                null,
                data.almacenOrigen(),
                data.usuario()
        );
        crearDetalle(movimiento, data.producto(), cantidad, new BigDecimal("7.500000"));
        return movimiento;
    }

    private MovimientoInventarioEntity crearMovimientoSalida(TestData data, BigDecimal cantidad) {
        MovimientoInventarioEntity movimiento = crearMovimiento(
                TipoMovimientoInventario.SALIDA_CONSUMO,
                data.almacenOrigen(),
                null,
                data.usuario()
        );
        crearDetalle(movimiento, data.producto(), cantidad, null);
        return movimiento;
    }

    private MovimientoInventarioEntity crearMovimiento(
            TipoMovimientoInventario tipoMovimiento,
            AlmacenEntity almacenOrigen,
            AlmacenEntity almacenDestino,
            UsuarioEntity usuario
    ) {
        MovimientoInventarioEntity movimiento = new MovimientoInventarioEntity();
        movimiento.setNumero("MOV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        movimiento.setTipoMovimiento(tipoMovimiento);
        movimiento.setEstado(EstadoMovimientoInventario.BORRADOR);
        movimiento.setFechaMovimiento(LocalDateTime.now());
        movimiento.setAlmacenOrigen(almacenOrigen);
        movimiento.setAlmacenDestino(almacenDestino);
        movimiento.setUsuario(usuario);
        movimiento.setObservacion("Movimiento concurrente de prueba");
        movimiento.setMotivoMovimiento("Prueba concurrencia");
        return movimientoInventarioRepository.save(movimiento);
    }

    private MovimientoDetalleEntity crearDetalle(
            MovimientoInventarioEntity movimiento,
            ProductoEntity producto,
            BigDecimal cantidad,
            BigDecimal costoUnitario
    ) {
        MovimientoDetalleEntity detalle = new MovimientoDetalleEntity();
        detalle.setMovimiento(movimiento);
        detalle.setProducto(producto);
        detalle.setCantidad(cantidad);
        detalle.setCostoUnitario(costoUnitario);
        return movimientoDetalleRepository.save(detalle);
    }

    private TestData crearDatosBase() {
        UsuarioEntity usuario = usuarioRepository.findByUsername("admin").orElseThrow();
        UnidadMedidaEntity unidad = crearUnidadMedida();
        FamiliaEntity familia = crearFamilia();
        TipoArticuloEntity tipoArticulo = tipoArticuloRepository.findByCodigoIgnoreCase("ALMACEN").orElseThrow();
        ProductoEntity producto = crearProducto(unidad, familia, tipoArticulo);
        AlmacenEntity almacenOrigen = crearAlmacen();
        return new TestData(usuario, producto, almacenOrigen);
    }

    private UnidadMedidaEntity crearUnidadMedida() {
        String suffix = suffix();
        UnidadMedidaEntity unidad = new UnidadMedidaEntity();
        unidad.setCodigo("UC" + suffix);
        unidad.setNombre("Unidad Concurrencia " + suffix);
        unidad.setAbreviatura("UC" + suffix.substring(0, 4));
        unidad.setActivo(true);
        return unidadMedidaRepository.save(unidad);
    }

    private FamiliaEntity crearFamilia() {
        String suffix = suffix();
        FamiliaEntity familia = new FamiliaEntity();
        familia.setNombre("Familia Concurrencia " + suffix);
        familia.setPrefijo("FC" + suffix.substring(0, 8));
        familia.setActivo(true);
        return familiaRepository.save(familia);
    }

    private ProductoEntity crearProducto(
            UnidadMedidaEntity unidad,
            FamiliaEntity familia,
            TipoArticuloEntity tipoArticulo
    ) {
        String suffix = suffix();
        ProductoEntity producto = new ProductoEntity();
        producto.setCodigo("PC" + suffix);
        producto.setNombre("Producto Concurrencia " + suffix);
        producto.setTipoArticulo(tipoArticulo);
        producto.setFamilia(familia);
        producto.setUnidadMedida(unidad);
        producto.setStockMinimo(BigDecimal.ZERO);
        producto.setActivo(true);
        return productoRepository.save(producto);
    }

    private AlmacenEntity crearAlmacen() {
        String suffix = suffix();
        AlmacenEntity almacen = new AlmacenEntity();
        almacen.setCodigo("AC" + suffix);
        almacen.setNombre("Almacen Concurrencia " + suffix);
        almacen.setActivo(true);
        return almacenRepository.save(almacen);
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private record TestData(
            UsuarioEntity usuario,
            ProductoEntity producto,
            AlmacenEntity almacenOrigen
    ) {
    }

    private record ResultadoConcurrente(boolean exitoso, Exception error) {

        static ResultadoConcurrente exito() {
            return new ResultadoConcurrente(true, null);
        }

        static ResultadoConcurrente error(Exception exception) {
            return new ResultadoConcurrente(false, exception);
        }
    }
}
