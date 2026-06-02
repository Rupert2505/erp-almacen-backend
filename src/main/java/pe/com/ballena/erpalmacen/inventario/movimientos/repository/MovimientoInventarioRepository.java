package pe.com.ballena.erpalmacen.inventario.movimientos.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoInventarioEntity;
import pe.com.ballena.erpalmacen.inventario.shared.EstadoMovimientoInventario;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;

import java.util.Optional;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventarioEntity, Long> {

    Optional<MovimientoInventarioEntity> findByNumero(String numero);

    boolean existsByNumero(String numero);

    Page<MovimientoInventarioEntity> findByEstado(EstadoMovimientoInventario estado, Pageable pageable);

    Page<MovimientoInventarioEntity> findByTipoMovimiento(TipoMovimientoInventario tipoMovimiento, Pageable pageable);
}
