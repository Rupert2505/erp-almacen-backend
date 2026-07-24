package pe.com.ballena.erpalmacen.maestros.categorias.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.categorias.dto.CategoriaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.categorias.dto.CategoriaResponse;
import pe.com.ballena.erpalmacen.maestros.categorias.dto.CategoriaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.categorias.entity.CategoriaEntity;
import pe.com.ballena.erpalmacen.maestros.categorias.repository.CategoriaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.util.Locale;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public Page<CategoriaResponse> listar(String texto, Boolean activo, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null) {
            return categoriaRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return categoriaRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return categoriaRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        return categoriaRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public CategoriaResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public CategoriaResponse crear(CategoriaCreateRequest request) {
        String codigo = normalizeCode(request.codigo());
        if (categoriaRepository.existsByCodigo(codigo)) {
            throw new BusinessException("Ya existe una categoria con el codigo indicado");
        }

        CategoriaEntity categoria = new CategoriaEntity();
        categoria.setCodigo(codigo);
        categoria.setNombre(normalizeText(request.nombre()));
        categoria.setDescripcion(normalizeText(request.descripcion()));
        categoria.setActivo(true);
        return toResponse(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse actualizar(Long id, CategoriaUpdateRequest request) {
        CategoriaEntity categoria = findById(id);
        String codigo = normalizeCode(request.codigo());
        if (categoriaRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new BusinessException("Ya existe una categoria con el codigo indicado");
        }

        categoria.setCodigo(codigo);
        categoria.setNombre(normalizeText(request.nombre()));
        categoria.setDescripcion(normalizeText(request.descripcion()));
        return toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse desactivar(Long id) {
        CategoriaEntity categoria = findById(id);
        categoria.setActivo(false);
        return toResponse(categoria);
    }

    @Transactional
    public CategoriaResponse activar(Long id) {
        CategoriaEntity categoria = findById(id);
        categoria.setActivo(true);
        return toResponse(categoria);
    }

    private CategoriaEntity findById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada"));
    }

    private CategoriaResponse toResponse(CategoriaEntity categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getCodigo(),
                categoria.getNombre(),
                categoria.getDescripcion(),
                categoria.isActivo(),
                categoria.getCreadoEn(),
                categoria.getActualizadoEn()
        );
    }

    private String normalizeCode(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
