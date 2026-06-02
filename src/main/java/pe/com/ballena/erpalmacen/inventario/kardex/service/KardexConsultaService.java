package pe.com.ballena.erpalmacen.inventario.kardex.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.inventario.kardex.dto.KardexResponse;
import pe.com.ballena.erpalmacen.inventario.kardex.entity.KardexEntity;
import pe.com.ballena.erpalmacen.inventario.kardex.repository.KardexRepository;
import pe.com.ballena.erpalmacen.inventario.shared.TipoMovimientoInventario;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.usuarios.entity.UsuarioEntity;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class KardexConsultaService {

    private final KardexRepository kardexRepository;

    public KardexConsultaService(KardexRepository kardexRepository) {
        this.kardexRepository = kardexRepository;
    }

    @Transactional(readOnly = true)
    public Page<KardexResponse> listar(
            Long productoId,
            Long almacenId,
            Long ubicacionId,
            TipoMovimientoInventario tipoMovimiento,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            String texto,
            Pageable pageable
    ) {
        return kardexRepository.findAll(
                buildSpecification(productoId, almacenId, ubicacionId, tipoMovimiento, fechaDesde, fechaHasta, texto),
                pageable
        ).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<KardexResponse> listarPorProducto(
            Long productoId,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Pageable pageable
    ) {
        return listar(productoId, null, null, null, fechaDesde, fechaHasta, null, pageable);
    }

    @Transactional(readOnly = true)
    public Page<KardexResponse> listarPorProductoYAlmacen(
            Long productoId,
            Long almacenId,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Pageable pageable
    ) {
        return listar(productoId, almacenId, null, null, fechaDesde, fechaHasta, null, pageable);
    }

    @Transactional(readOnly = true)
    public List<KardexResponse> listarPorMovimiento(Long movimientoId) {
        return kardexRepository.findByMovimientoId(movimientoId).stream()
                .sorted(Comparator.comparing(KardexEntity::getFechaMovimiento).thenComparing(KardexEntity::getId))
                .map(this::toResponse)
                .toList();
    }

    private Specification<KardexEntity> buildSpecification(
            Long productoId,
            Long almacenId,
            Long ubicacionId,
            TipoMovimientoInventario tipoMovimiento,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            String texto
    ) {
        String textoNormalizado = normalizeSearch(texto);
        return (root, query, criteriaBuilder) -> {
            java.util.ArrayList<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (productoId != null) {
                predicates.add(criteriaBuilder.equal(root.get("producto").get("id"), productoId));
            }
            if (almacenId != null) {
                predicates.add(criteriaBuilder.equal(root.get("almacen").get("id"), almacenId));
            }
            if (ubicacionId != null) {
                predicates.add(criteriaBuilder.equal(root.get("ubicacion").get("id"), ubicacionId));
            }
            if (tipoMovimiento != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipoMovimiento"), tipoMovimiento));
            }
            if (fechaDesde != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("fechaMovimiento"), fechaDesde));
            }
            if (fechaHasta != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("fechaMovimiento"), fechaHasta));
            }
            if (textoNormalizado != null) {
                String pattern = "%" + textoNormalizado.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("numeroMovimiento")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("observacion")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("producto").get("codigo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("producto").get("nombre")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("almacen").get("codigo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("almacen").get("nombre")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ubicacion").get("codigo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("ubicacion").get("nombre")), pattern)
                ));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private KardexResponse toResponse(KardexEntity kardex) {
        ProductoEntity producto = kardex.getProducto();
        AlmacenEntity almacen = kardex.getAlmacen();
        UbicacionAlmacenEntity ubicacion = kardex.getUbicacion();
        UsuarioEntity usuario = kardex.getUsuario();

        return new KardexResponse(
                kardex.getId(),
                kardex.getMovimiento().getId(),
                kardex.getMovimientoDetalle().getId(),
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                almacen.getId(),
                almacen.getCodigo(),
                almacen.getNombre(),
                ubicacion == null ? null : ubicacion.getId(),
                ubicacion == null ? null : ubicacion.getCodigo(),
                ubicacion == null ? null : ubicacion.getNombre(),
                kardex.getFechaMovimiento(),
                kardex.getTipoMovimiento(),
                kardex.getNumeroMovimiento(),
                kardex.getEntrada(),
                kardex.getSalida(),
                kardex.getSaldoAnterior(),
                kardex.getSaldoFinal(),
                kardex.getCostoUnitario(),
                kardex.getCostoPromedio(),
                usuario.getId(),
                usuario.getUsername(),
                kardex.getObservacion(),
                kardex.getCreadoEn()
        );
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
