package pe.com.ballena.erpalmacen.inventario.movimientos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventarioEntity, Long>,
        JpaSpecificationExecutor<MovimientoInventarioEntity> {

    Optional<MovimientoInventarioEntity> findByNumero(String numero);

    boolean existsByNumero(String numero);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from MovimientoInventarioEntity m
            where m.id = :id
            """)
    Optional<MovimientoInventarioEntity> findByIdForUpdate(@Param("id") Long id);

    Page<MovimientoInventarioEntity> findByEstado(EstadoMovimientoInventario estado, Pageable pageable);

    Page<MovimientoInventarioEntity> findByTipoMovimiento(TipoMovimientoInventario tipoMovimiento, Pageable pageable);
}
