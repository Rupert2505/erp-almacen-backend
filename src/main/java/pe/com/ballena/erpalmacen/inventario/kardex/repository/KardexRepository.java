package pe.com.ballena.erpalmacen.inventario.kardex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.inventario.kardex.entity.KardexEntity;

import java.time.LocalDateTime;
import java.util.List;

public interface KardexRepository extends JpaRepository<KardexEntity, Long>, JpaSpecificationExecutor<KardexEntity> {

    Page<KardexEntity> findByProductoIdOrderByFechaMovimientoAsc(Long productoId, Pageable pageable);

    Page<KardexEntity> findByProductoIdAndAlmacenIdOrderByFechaMovimientoAsc(
            Long productoId,
            Long almacenId,
            Pageable pageable
    );

    List<KardexEntity> findByMovimientoId(Long movimientoId);

    @Query("""
            select k
            from KardexEntity k
            where k.movimiento.id = :movimientoId
              and k.movimientoDetalle.id = :detalleId
            order by k.id asc
            """)
    List<KardexEntity> findByMovimientoDetalle(
            @Param("movimientoId") Long movimientoId,
            @Param("detalleId") Long detalleId
    );

    @Query("""
            select case when count(k) > 0 then true else false end
            from KardexEntity k
            where k.producto.id = :productoId
              and k.almacen.id = :almacenId
              and k.ubicacion is null
              and k.movimiento.id <> :movimientoId
              and (
                    k.fechaMovimiento > :fechaMovimiento
                    or (k.fechaMovimiento = :fechaMovimiento and k.id > :kardexId)
              )
            """)
    boolean existsPosteriorSinUbicacion(
            @Param("productoId") Long productoId,
            @Param("almacenId") Long almacenId,
            @Param("movimientoId") Long movimientoId,
            @Param("fechaMovimiento") LocalDateTime fechaMovimiento,
            @Param("kardexId") Long kardexId
    );

    @Query("""
            select case when count(k) > 0 then true else false end
            from KardexEntity k
            where k.producto.id = :productoId
              and k.almacen.id = :almacenId
              and k.ubicacion.id = :ubicacionId
              and k.movimiento.id <> :movimientoId
              and (
                    k.fechaMovimiento > :fechaMovimiento
                    or (k.fechaMovimiento = :fechaMovimiento and k.id > :kardexId)
              )
            """)
    boolean existsPosteriorConUbicacion(
            @Param("productoId") Long productoId,
            @Param("almacenId") Long almacenId,
            @Param("ubicacionId") Long ubicacionId,
            @Param("movimientoId") Long movimientoId,
            @Param("fechaMovimiento") LocalDateTime fechaMovimiento,
            @Param("kardexId") Long kardexId
    );
}
