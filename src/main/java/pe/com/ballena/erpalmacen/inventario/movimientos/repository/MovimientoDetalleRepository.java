package pe.com.ballena.erpalmacen.inventario.movimientos.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoDetalleEntity;

import java.util.List;

public interface MovimientoDetalleRepository extends JpaRepository<MovimientoDetalleEntity, Long> {

    List<MovimientoDetalleEntity> findByMovimientoId(Long movimientoId);

    List<MovimientoDetalleEntity> findByMovimientoIdOrderByIdAsc(Long movimientoId);
}
