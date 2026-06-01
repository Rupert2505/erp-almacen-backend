package pe.com.ballena.erpalmacen.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.ballena.erpalmacen.usuarios.entity.PermisoEntity;

import java.util.Optional;

public interface PermisoRepository extends JpaRepository<PermisoEntity, Long> {

    Optional<PermisoEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);
}
