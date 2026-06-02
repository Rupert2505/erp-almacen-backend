package pe.com.ballena.erpalmacen.inventario.kardex.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.ballena.erpalmacen.inventario.kardex.entity.KardexEntity;

import java.util.List;

public interface KardexRepository extends JpaRepository<KardexEntity, Long> {

    Page<KardexEntity> findByProductoIdOrderByFechaMovimientoAsc(Long productoId, Pageable pageable);

    Page<KardexEntity> findByProductoIdAndAlmacenIdOrderByFechaMovimientoAsc(
            Long productoId,
            Long almacenId,
            Pageable pageable
    );

    List<KardexEntity> findByMovimientoId(Long movimientoId);
}
