package pe.com.ballena.erpalmacen.maestros.productos.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaRepository;
import pe.com.ballena.erpalmacen.maestros.marcas.entity.MarcaEntity;
import pe.com.ballena.erpalmacen.maestros.marcas.repository.MarcaRepository;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoCreateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoResponse;
import pe.com.ballena.erpalmacen.maestros.productos.dto.ProductoUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.productos.entity.ProductoEntity;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.maestros.subfamilias.entity.SubfamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.subfamilias.repository.SubfamiliaRepository;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.entity.TipoArticuloEntity;
import pe.com.ballena.erpalmacen.maestros.tiposarticulo.repository.TipoArticuloRepository;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.entity.UnidadMedidaEntity;
import pe.com.ballena.erpalmacen.maestros.unidadesmedida.repository.UnidadMedidaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.math.BigDecimal;
import java.util.Locale;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final FamiliaRepository familiaRepository;
    private final SubfamiliaRepository subfamiliaRepository;
    private final MarcaRepository marcaRepository;
    private final TipoArticuloRepository tipoArticuloRepository;
    private final UnidadMedidaRepository unidadMedidaRepository;
    private final ProductoCodigoService productoCodigoService;

    public ProductoService(
            ProductoRepository productoRepository,
            FamiliaRepository familiaRepository,
            SubfamiliaRepository subfamiliaRepository,
            MarcaRepository marcaRepository,
            TipoArticuloRepository tipoArticuloRepository,
            UnidadMedidaRepository unidadMedidaRepository,
            ProductoCodigoService productoCodigoService
    ) {
        this.productoRepository = productoRepository;
        this.familiaRepository = familiaRepository;
        this.subfamiliaRepository = subfamiliaRepository;
        this.marcaRepository = marcaRepository;
        this.tipoArticuloRepository = tipoArticuloRepository;
        this.unidadMedidaRepository = unidadMedidaRepository;
        this.productoCodigoService = productoCodigoService;
    }

    @Transactional(readOnly = true)
    public Page<ProductoResponse> listar(
            String texto,
            Boolean activo,
            Long tipoArticuloId,
            Long familiaId,
            Long subfamiliaId,
            Long marcaId,
            Long unidadMedidaId,
            Pageable pageable
    ) {
        return productoRepository.findAll(
                        buildSpecification(texto, activo, tipoArticuloId, familiaId, subfamiliaId, marcaId, unidadMedidaId),
                        aplicarOrdenPorDefecto(pageable)
                )
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ProductoResponse crear(ProductoCreateRequest request) {
        ProductoEntity producto = new ProductoEntity();
        applyValues(producto, request);
        producto.setCodigo(productoCodigoService.generarCodigo(producto.getFamilia()));
        producto.setActivo(true);
        return toResponse(productoRepository.save(producto));
    }

    @Transactional
    public ProductoResponse actualizar(Long id, ProductoUpdateRequest request) {
        ProductoEntity producto = findById(id);
        String codigo = normalizeOptionalCode(request.codigo());
        if (codigo != null && !codigo.equals(producto.getCodigo())) {
            throw new BusinessException("El codigo del producto no se puede modificar");
        }

        applyValues(producto, request);
        return toResponse(producto);
    }

    @Transactional(readOnly = true)
    public String obtenerSiguienteCodigo(Long familiaId) {
        FamiliaEntity familia = findFamiliaActiva(familiaId);
        return productoCodigoService.obtenerSiguienteCodigo(familia);
    }

    @Transactional
    public ProductoResponse desactivar(Long id) {
        ProductoEntity producto = findById(id);
        producto.setActivo(false);
        return toResponse(producto);
    }

    @Transactional
    public ProductoResponse activar(Long id) {
        ProductoEntity producto = findById(id);
        producto.setActivo(true);
        return toResponse(producto);
    }

    private void applyValues(ProductoEntity producto, ProductoCreateRequest request) {
        BigDecimal stockMinimo = request.stockMinimo() == null ? BigDecimal.ZERO : request.stockMinimo();
        validateAmounts(stockMinimo, request.stockMaximo(), request.costoReferencial());
        TipoArticuloEntity tipoArticulo = findTipoArticuloActivo(request.tipoArticuloId());
        FamiliaEntity familia = findFamiliaActiva(request.familiaId());
        SubfamiliaEntity subfamilia = findSubfamiliaActiva(request.subfamiliaId(), familia);

        producto.setNombre(normalizeText(request.nombre()));
        producto.setDescripcion(normalizeText(request.descripcion()));
        producto.setTipoArticulo(tipoArticulo);
        producto.setFamilia(familia);
        producto.setSubfamilia(subfamilia);
        producto.setMarca(findMarcaActiva(request.marcaId()));
        producto.setUnidadMedida(findUnidadMedidaActiva(request.unidadMedidaId()));
        producto.setStockMinimo(stockMinimo);
        producto.setStockMaximo(request.stockMaximo());
        producto.setCostoReferencial(request.costoReferencial());
        producto.setControlaLote(Boolean.TRUE.equals(request.controlaLote()));
        producto.setControlaSerie(Boolean.TRUE.equals(request.controlaSerie()));
    }

    private void applyValues(ProductoEntity producto, ProductoUpdateRequest request) {
        BigDecimal stockMinimo = request.stockMinimo() == null ? BigDecimal.ZERO : request.stockMinimo();
        validateAmounts(stockMinimo, request.stockMaximo(), request.costoReferencial());
        TipoArticuloEntity tipoArticulo = findTipoArticuloActivo(request.tipoArticuloId());
        FamiliaEntity familia = findFamiliaActiva(request.familiaId());
        SubfamiliaEntity subfamilia = findSubfamiliaActiva(request.subfamiliaId(), familia);

        producto.setNombre(normalizeText(request.nombre()));
        producto.setDescripcion(normalizeText(request.descripcion()));
        producto.setTipoArticulo(tipoArticulo);
        producto.setFamilia(familia);
        producto.setSubfamilia(subfamilia);
        producto.setMarca(findMarcaActiva(request.marcaId()));
        producto.setUnidadMedida(findUnidadMedidaActiva(request.unidadMedidaId()));
        producto.setStockMinimo(stockMinimo);
        producto.setStockMaximo(request.stockMaximo());
        producto.setCostoReferencial(request.costoReferencial());
        producto.setControlaLote(Boolean.TRUE.equals(request.controlaLote()));
        producto.setControlaSerie(Boolean.TRUE.equals(request.controlaSerie()));
    }

    private void validateAmounts(BigDecimal stockMinimo, BigDecimal stockMaximo, BigDecimal costoReferencial) {
        if (stockMinimo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El stock minimo no puede ser negativo");
        }
        if (stockMaximo != null && stockMaximo.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El stock maximo no puede ser negativo");
        }
        if (costoReferencial != null && costoReferencial.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El costo referencial no puede ser negativo");
        }
        if (stockMaximo != null && stockMaximo.compareTo(stockMinimo) < 0) {
            throw new BusinessException("El stock maximo debe ser mayor o igual al stock minimo");
        }
    }

    private ProductoEntity findById(Long id) {
        return productoRepository.findDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));
    }

    private Specification<ProductoEntity> buildSpecification(
            String texto,
            Boolean activo,
            Long tipoArticuloId,
            Long familiaId,
            Long subfamiliaId,
            Long marcaId,
            Long unidadMedidaId
    ) {
        String textoNormalizado = normalizeSearch(texto);
        return (root, query, criteriaBuilder) -> {
            if (query != null && ProductoEntity.class.equals(query.getResultType())) {
                root.fetch("categoria", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("tipoArticulo", jakarta.persistence.criteria.JoinType.INNER);
                root.fetch("familia", jakarta.persistence.criteria.JoinType.INNER);
                root.fetch("subfamilia", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("marca", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("unidadMedida", jakarta.persistence.criteria.JoinType.INNER);
                query.distinct(true);
            }

            java.util.ArrayList<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
            if (textoNormalizado != null) {
                String pattern = "%" + textoNormalizado.toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("codigo")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("descripcion")), pattern)
                ));
            }
            if (activo != null) {
                predicates.add(criteriaBuilder.equal(root.get("activo"), activo));
            }
            if (tipoArticuloId != null) {
                predicates.add(criteriaBuilder.equal(root.get("tipoArticulo").get("id"), tipoArticuloId));
            }
            if (familiaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("familia").get("id"), familiaId));
            }
            if (subfamiliaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("subfamilia").get("id"), subfamiliaId));
            }
            if (marcaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("marca").get("id"), marcaId));
            }
            if (unidadMedidaId != null) {
                predicates.add(criteriaBuilder.equal(root.get("unidadMedida").get("id"), unidadMedidaId));
            }
            return criteriaBuilder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private TipoArticuloEntity findTipoArticuloActivo(Long id) {
        if (id == null) {
            throw new BusinessException("El tipo de articulo es obligatorio");
        }
        TipoArticuloEntity tipoArticulo = tipoArticuloRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de articulo no encontrado"));
        if (!tipoArticulo.isActivo()) {
            throw new BusinessException("No se puede usar un tipo de articulo inactivo");
        }
        return tipoArticulo;
    }

    private FamiliaEntity findFamiliaActiva(Long id) {
        if (id == null) {
            throw new BusinessException("La familia es obligatoria");
        }
        FamiliaEntity familia = familiaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Familia no encontrada"));
        if (!familia.isActivo()) {
            throw new BusinessException("No se puede usar una familia inactiva");
        }
        return familia;
    }

    private SubfamiliaEntity findSubfamiliaActiva(Long id, FamiliaEntity familia) {
        if (id == null) {
            return null;
        }
        SubfamiliaEntity subfamilia = subfamiliaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subfamilia no encontrada"));
        if (!subfamilia.isActivo()) {
            throw new BusinessException("No se puede usar una subfamilia inactiva");
        }
        if (!subfamilia.getFamilia().getId().equals(familia.getId())) {
            throw new BusinessException("La subfamilia no pertenece a la familia seleccionada");
        }
        return subfamilia;
    }

    private MarcaEntity findMarcaActiva(Long id) {
        if (id == null) {
            return null;
        }
        MarcaEntity marca = marcaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
        if (!marca.isActivo()) {
            throw new BusinessException("No se puede usar una marca inactiva");
        }
        return marca;
    }

    private UnidadMedidaEntity findUnidadMedidaActiva(Long id) {
        if (id == null) {
            throw new BusinessException("La unidad de medida es obligatoria");
        }
        UnidadMedidaEntity unidad = unidadMedidaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de medida no encontrada"));
        if (!unidad.isActivo()) {
            throw new BusinessException("No se puede usar una unidad de medida inactiva");
        }
        return unidad;
    }

    private ProductoResponse toResponse(ProductoEntity producto) {
        TipoArticuloEntity tipoArticulo = producto.getTipoArticulo();
        FamiliaEntity familia = producto.getFamilia();
        SubfamiliaEntity subfamilia = producto.getSubfamilia();
        MarcaEntity marca = producto.getMarca();
        UnidadMedidaEntity unidad = producto.getUnidadMedida();

        return new ProductoResponse(
                producto.getId(),
                producto.getCodigo(),
                producto.getNombre(),
                producto.getDescripcion(),
                tipoArticulo.getId(),
                tipoArticulo.getCodigo(),
                tipoArticulo.getNombre(),
                familia.getId(),
                familia.getPrefijo(),
                familia.getNombre(),
                subfamilia == null ? null : subfamilia.getId(),
                subfamilia == null ? null : subfamilia.getNombre(),
                marca == null ? null : marca.getId(),
                marca == null ? null : marca.getNombre(),
                unidad.getId(),
                unidad.getNombre(),
                producto.getStockMinimo(),
                producto.getStockMaximo(),
                producto.getCostoReferencial(),
                producto.isControlaLote(),
                producto.isControlaSerie(),
                producto.isActivo(),
                producto.getCreadoEn(),
                producto.getActualizadoEn()
        );
    }

    private String normalizeOptionalCode(String value) {
        String codigo = clean(value);
        return codigo == null || codigo.isBlank() ? null : codigo.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private Pageable aplicarOrdenPorDefecto(Pageable pageable) {
        Sort ordenRecientesPrimero = Sort.by(Sort.Direction.DESC, "creadoEn", "id");
        if (pageable == null) {
            return PageRequest.of(0, 20, ordenRecientesPrimero);
        }
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), ordenRecientesPrimero);
    }

    private String cleanRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
