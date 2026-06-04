package pe.com.ballena.erpalmacen.usuarios.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.com.ballena.erpalmacen.usuarios.entity.RolEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RolRepository extends JpaRepository<RolEntity, Long> {

    Optional<RolEntity> findByNombre(String nombre);

    boolean existsByNombre(String nombre);

    @Override
    @EntityGraph(attributePaths = "permisos")
    Optional<RolEntity> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "permisos")
    List<RolEntity> findAll();

    @EntityGraph(attributePaths = "permisos")
    List<RolEntity> findByIdIn(Set<Long> ids);
}
