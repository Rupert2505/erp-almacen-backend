package pe.com.ballena.erpalmacen.maestros.familias.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaCorrelativoEntity;

import java.util.Optional;

public interface FamiliaCorrelativoRepository extends JpaRepository<FamiliaCorrelativoEntity, Long> {

    Optional<FamiliaCorrelativoEntity> findByFamiliaId(Long familiaId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select c
            from FamiliaCorrelativoEntity c
            where c.familia.id = :familiaId
            """)
    Optional<FamiliaCorrelativoEntity> findByFamiliaIdForUpdate(@Param("familiaId") Long familiaId);
}
