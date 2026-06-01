package pe.com.ballena.erpalmacen.maestros.marcas.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.marcas.dto.MarcaCreateRequest;
import pe.com.ballena.erpalmacen.maestros.marcas.dto.MarcaResponse;
import pe.com.ballena.erpalmacen.maestros.marcas.dto.MarcaUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.marcas.entity.MarcaEntity;
import pe.com.ballena.erpalmacen.maestros.marcas.repository.MarcaRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.util.Locale;

@Service
public class MarcaService {

    private final MarcaRepository marcaRepository;

    public MarcaService(MarcaRepository marcaRepository) {
        this.marcaRepository = marcaRepository;
    }

    @Transactional(readOnly = true)
    public Page<MarcaResponse> listar(String texto, Boolean activo, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null) {
            return marcaRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return marcaRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return marcaRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        return marcaRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public MarcaResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public MarcaResponse crear(MarcaCreateRequest request) {
        String codigo = normalizeCode(request.codigo());
        if (marcaRepository.existsByCodigo(codigo)) {
            throw new BusinessException("Ya existe una marca con el codigo indicado");
        }

        MarcaEntity marca = new MarcaEntity();
        marca.setCodigo(codigo);
        marca.setNombre(clean(request.nombre()));
        marca.setDescripcion(clean(request.descripcion()));
        marca.setActivo(true);
        return toResponse(marcaRepository.save(marca));
    }

    @Transactional
    public MarcaResponse actualizar(Long id, MarcaUpdateRequest request) {
        MarcaEntity marca = findById(id);
        String codigo = normalizeCode(request.codigo());
        if (marcaRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new BusinessException("Ya existe una marca con el codigo indicado");
        }

        marca.setCodigo(codigo);
        marca.setNombre(clean(request.nombre()));
        marca.setDescripcion(clean(request.descripcion()));
        return toResponse(marca);
    }

    @Transactional
    public MarcaResponse desactivar(Long id) {
        MarcaEntity marca = findById(id);
        marca.setActivo(false);
        return toResponse(marca);
    }

    @Transactional
    public MarcaResponse activar(Long id) {
        MarcaEntity marca = findById(id);
        marca.setActivo(true);
        return toResponse(marca);
    }

    private MarcaEntity findById(Long id) {
        return marcaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Marca no encontrada"));
    }

    private MarcaResponse toResponse(MarcaEntity marca) {
        return new MarcaResponse(
                marca.getId(),
                marca.getCodigo(),
                marca.getNombre(),
                marca.getDescripcion(),
                marca.isActivo(),
                marca.getCreadoEn(),
                marca.getActualizadoEn()
        );
    }

    private String normalizeCode(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
