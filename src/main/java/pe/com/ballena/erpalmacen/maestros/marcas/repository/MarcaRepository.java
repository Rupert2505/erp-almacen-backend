package pe.com.ballena.erpalmacen.maestros.marcas.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.marcas.entity.MarcaEntity;

public interface MarcaRepository extends JpaRepository<MarcaEntity, Long> {

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    Page<MarcaEntity> findByActivo(Boolean activo, Pageable pageable);

    @Query("""
            select m
            from MarcaEntity m
            where lower(m.codigo) like lower(concat('%', :texto, '%'))
               or lower(m.nombre) like lower(concat('%', :texto, '%'))
            """)
    Page<MarcaEntity> buscarPorTexto(
            @Param("texto") String texto,
            Pageable pageable
    );

    @Query("""
            select m
            from MarcaEntity m
            where (lower(m.codigo) like lower(concat('%', :texto, '%'))
                or lower(m.nombre) like lower(concat('%', :texto, '%')))
              and m.activo = :activo
            """)
    Page<MarcaEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
