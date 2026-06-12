package pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.entity.TipoArticuloEntity;

public interface TipoArticuloRepository extends JpaRepository<TipoArticuloEntity, Long> {

    Page<TipoArticuloEntity> findByActivo(Boolean activo, Pageable pageable);

    @Query("""
            select t
            from TipoArticuloEntity t
            where lower(t.codigo) = lower(:codigo)
            """)
    java.util.Optional<TipoArticuloEntity> findByCodigoIgnoreCase(@Param("codigo") String codigo);

    @Query("""
            select count(t) > 0
            from TipoArticuloEntity t
            where lower(t.codigo) = lower(:codigo)
            """)
    boolean existsByCodigoIgnoreCase(@Param("codigo") String codigo);

    @Query("""
            select count(t) > 0
            from TipoArticuloEntity t
            where lower(t.codigo) = lower(:codigo)
              and t.id <> :id
            """)
    boolean existsByCodigoIgnoreCaseAndIdNot(@Param("codigo") String codigo, @Param("id") Long id);

    @Query("""
            select count(t) > 0
            from TipoArticuloEntity t
            where lower(t.nombre) = lower(:nombre)
            """)
    boolean existsByNombreIgnoreCase(@Param("nombre") String nombre);

    @Query("""
            select count(t) > 0
            from TipoArticuloEntity t
            where lower(t.nombre) = lower(:nombre)
              and t.id <> :id
            """)
    boolean existsByNombreIgnoreCaseAndIdNot(@Param("nombre") String nombre, @Param("id") Long id);

    @Query("""
            select t
            from TipoArticuloEntity t
            where lower(t.codigo) like lower(concat('%', :texto, '%'))
               or lower(t.nombre) like lower(concat('%', :texto, '%'))
               or lower(coalesce(t.descripcion, '')) like lower(concat('%', :texto, '%'))
            """)
    Page<TipoArticuloEntity> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            select t
            from TipoArticuloEntity t
            where t.activo = :activo
              and (
                   lower(t.codigo) like lower(concat('%', :texto, '%'))
                or lower(t.nombre) like lower(concat('%', :texto, '%'))
                or lower(coalesce(t.descripcion, '')) like lower(concat('%', :texto, '%'))
              )
            """)
    Page<TipoArticuloEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
