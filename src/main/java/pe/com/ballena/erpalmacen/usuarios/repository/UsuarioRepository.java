package pe.com.ballena.erpalmacen.usuarios.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long>, JpaSpecificationExecutor<UsuarioEntity> {

    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    Optional<UsuarioEntity> findByUsername(String username);

    @Override
    @EntityGraph(attributePaths = {"roles", "roles.permisos"})
    Optional<UsuarioEntity> findById(Long id);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
            select count(distinct u.id)
            from UsuarioEntity u
            join u.roles r
            where u.activo = true
              and r.activo = true
              and r.nombre = :rolNombre
            """)
    long countActiveUsersByRoleName(@Param("rolNombre") String rolNombre);
}
