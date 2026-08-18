package pe.com.ballena.erpalmacen.inventario.movimientos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.math.BigDecimal;
import java.util.List;

public interface MovimientoDetalleRepository extends JpaRepository<MovimientoDetalleEntity, Long> {

    List<MovimientoDetalleEntity> findByMovimientoId(Long movimientoId);

    List<MovimientoDetalleEntity> findByMovimientoIdOrderByIdAsc(Long movimientoId);

    @Query("""
            select coalesce(sum(d.cantidad), 0)
            from MovimientoDetalleEntity d
            join d.movimiento m
            where d.movimientoDetalleOrigen.id = :detalleOrigenId
              and m.tipoMovimiento = :tipoMovimiento
              and m.estado = :estado
            """)
    BigDecimal sumCantidadByDetalleOrigenAndMovimientoTipoAndEstado(
            @Param("detalleOrigenId") Long detalleOrigenId,
            @Param("tipoMovimiento") TipoMovimientoInventario tipoMovimiento,
            @Param("estado") EstadoMovimientoInventario estado
    );
}
