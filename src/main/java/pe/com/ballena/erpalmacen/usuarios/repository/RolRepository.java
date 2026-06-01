package pe.com.ballena.erpalmacen.usuarios.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.ballena.erpalmacen.usuarios.entity.RolEntity;

import java.util.Optional;

public interface RolRepository extends JpaRepository<RolEntity, Long> {

    Optional<RolEntity> findByNombre(String nombre);

    boolean existsByNombre(String nombre);
}
