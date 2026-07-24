package pe.com.ballena.erpalmacen.maestros.personal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.com.ballena.erpalmacen.maestros.personal.entity.PersonalEntity;

public interface PersonalRepository extends JpaRepository<PersonalEntity, Long> {

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);

    Page<PersonalEntity> findByActivo(Boolean activo, Pageable pageable);

    @Query("""
            select p
            from PersonalEntity p
            where lower(p.numeroDocumento) like lower(concat('%', :texto, '%'))
               or lower(p.nombres) like lower(concat('%', :texto, '%'))
               or lower(p.apellidos) like lower(concat('%', :texto, '%'))
               or lower(p.cargo) like lower(concat('%', :texto, '%'))
               or lower(p.area) like lower(concat('%', :texto, '%'))
            """)
    Page<PersonalEntity> buscarPorTexto(@Param("texto") String texto, Pageable pageable);

    @Query("""
            select p
            from PersonalEntity p
            where (lower(p.numeroDocumento) like lower(concat('%', :texto, '%'))
                or lower(p.nombres) like lower(concat('%', :texto, '%'))
                or lower(p.apellidos) like lower(concat('%', :texto, '%'))
                or lower(p.cargo) like lower(concat('%', :texto, '%'))
                or lower(p.area) like lower(concat('%', :texto, '%')))
              and p.activo = :activo
            """)
    Page<PersonalEntity> buscarPorTextoYActivo(
            @Param("texto") String texto,
            @Param("activo") Boolean activo,
            Pageable pageable
    );
}
