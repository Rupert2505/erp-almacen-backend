package pe.com.ballena.erpalmacen.maestros.subfamilias.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.subfamilias.entity.SubfamiliaEntity;

import java.util.Optional;

public interface SubfamiliaRepository extends JpaRepository<SubfamiliaEntity, Long> {

    @Override
    @EntityGraph(attributePaths = "familia")
    Page<SubfamiliaEntity> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "familia")
    Optional<SubfamiliaEntity> findById(Long id);

    @EntityGraph(attributePaths = "familia")
    Page<SubfamiliaEntity> findByActivo(Boolean activo, Pageable pageable);

    @EntityGraph(attributePaths = "familia")
    Page<SubfamiliaEntity> findByFamiliaId(Long familiaId, Pageable pageable);

    @EntityGraph(attributePaths = "familia")
    Page<SubfamiliaEntity> findByFamiliaIdAndActivo(Long familiaId, Boolean activo, Pageable pageable);

    @Query("""
            select count(s) > 0
            from SubfamiliaEntity s
            where s.familia.id = :familiaId
              and lower(s.nombre) = lower(:nombre)
            """)
    boolean existsByFamiliaIdAndNombreIgnoreCase(
            @Param("familiaId") Long familiaId,
            @Param("nombre") String nombre
    );

    @Query("""
            select count(s) > 0
            from SubfamiliaEntity s
            where s.familia.id = :familiaId
              and lower(s.nombre) = lower(:nombre)
              and s.id <> :id
            """)
    boolean existsByFamiliaIdAndNombreIgnoreCaseAndIdNot(
            @Param("familiaId") Long familiaId,
            @Param("nombre") String nombre,
            @Param("id") Long id
    );

    @Query("""
            select s
            from SubfamiliaEntity s
            join s.familia f
            where lower(s.nombre) like lower(concat('%', :texto, '%'))
               or lower(f.nombre) like lower(concat('%', :texto, '%'))
            """)
    @EntityGraph(attributePaths = "familia")
    Page<SubfamiliaEntity> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            select s
            from SubfamiliaEntity s
            join s.familia f
            where (lower(s.nombre) like lower(concat('%', :texto, '%'))
                or lower(f.nombre) like lower(concat('%', :texto, '%')))
              and s.activo = :activo
            """)
    @EntityGraph(attributePaths = "familia")
    Page<SubfamiliaEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
