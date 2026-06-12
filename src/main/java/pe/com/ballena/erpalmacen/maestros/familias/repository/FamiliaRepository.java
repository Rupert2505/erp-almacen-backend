package pe.com.ballena.erpalmacen.maestros.familias.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;

public interface FamiliaRepository extends JpaRepository<FamiliaEntity, Long> {

    Page<FamiliaEntity> findByActivo(Boolean activo, Pageable pageable);

    @Query("""
            select count(f) > 0
            from FamiliaEntity f
            where lower(f.nombre) = lower(:nombre)
            """)
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    @Query("""
            select count(f) > 0
            from FamiliaEntity f
            where lower(f.nombre) = lower(:nombre)
              and f.id <> :id
            """)
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);

    @Query("""
            select f
            from FamiliaEntity f
            where lower(f.nombre) like lower(concat('%', :texto, '%'))
            """)
    Page<FamiliaEntity> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            select f
            from FamiliaEntity f
            where lower(f.nombre) like lower(concat('%', :texto, '%'))
              and f.activo = :activo
            """)
    Page<FamiliaEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
