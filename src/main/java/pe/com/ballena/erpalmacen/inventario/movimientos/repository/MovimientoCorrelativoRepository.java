package pe.com.ballena.erpalmacen.inventario.movimientos.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.inventario.movimientos.entity.MovimientoCorrelativoEntity;

import java.util.Optional;

public interface MovimientoCorrelativoRepository extends JpaRepository<MovimientoCorrelativoEntity, Long> {

    Optional<MovimientoCorrelativoEntity> findByPrefijo(String prefijo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from MovimientoCorrelativoEntity c
            where c.prefijo = :prefijo
            """)
    Optional<MovimientoCorrelativoEntity> findByPrefijoForUpdate(@Param("prefijo") String prefijo);
}
