package pe.com.ballena.erpalmacen.inventario.stock.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.inventario.stock.entity.StockActualEntity;

import java.util.Optional;

public interface StockActualRepository extends JpaRepository<StockActualEntity, Long> {

    Optional<StockActualEntity> findByProductoIdAndAlmacenIdAndUbicacionId(
            Long productoId,
            Long almacenId,
            Long ubicacionId
    );

    Optional<StockActualEntity> findByProductoIdAndAlmacenIdAndUbicacionIsNull(Long productoId, Long almacenId);

    Page<StockActualEntity> findByProductoId(Long productoId, Pageable pageable);

    Page<StockActualEntity> findByAlmacenId(Long almacenId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from StockActualEntity s
            where s.producto.id = :productoId
              and s.almacen.id = :almacenId
              and s.ubicacion.id = :ubicacionId
            """)
    Optional<StockActualEntity> findByProductoAlmacenUbicacionForUpdate(
            @Param("productoId") Long productoId,
            @Param("almacenId") Long almacenId,
            @Param("ubicacionId") Long ubicacionId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from StockActualEntity s
            where s.producto.id = :productoId
              and s.almacen.id = :almacenId
              and s.ubicacion is null
            """)
    Optional<StockActualEntity> findByProductoAlmacenSinUbicacionForUpdate(
            @Param("productoId") Long productoId,
            @Param("almacenId") Long almacenId
    );
}
