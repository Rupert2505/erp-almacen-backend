package pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;

public interface UnidadMedidaRepository extends JpaRepository<UnidadMedidaEntity, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    Page<UnidadMedidaEntity> findByActivo(Boolean activo, Pageable pageable);

    @Query("""
            select u
            from UnidadMedidaEntity u
            where lower(u.codigo) like lower(concat('%', :texto, '%'))
               or lower(u.nombre) like lower(concat('%', :texto, '%'))
               or lower(u.abreviatura) like lower(concat('%', :texto, '%'))
            """)
    Page<UnidadMedidaEntity> buscarPorTexto(
            @Param("texto") String texto,
            Pageable pageable
    );

    @Query("""
            select u
            from UnidadMedidaEntity u
            where (lower(u.codigo) like lower(concat('%', :texto, '%'))
                or lower(u.nombre) like lower(concat('%', :texto, '%'))
                or lower(u.abreviatura) like lower(concat('%', :texto, '%')))
              and u.activo = :activo
            """)
    Page<UnidadMedidaEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
