package pe.com.ballena.erpalmacen.almacen.almacenes.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.dto.AlmacenCreateRequest;
import pe.com.ballena.erpalmacen.almacen.almacenes.dto.AlmacenResponse;
import pe.com.ballena.erpalmacen.almacen.almacenes.dto.AlmacenUpdateRequest;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.almacenes.repository.AlmacenRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.util.Locale;

@Service
public class AlmacenService {

    private final AlmacenRepository almacenRepository;

    public AlmacenService(AlmacenRepository almacenRepository) {
        this.almacenRepository = almacenRepository;
    }

    @Transactional(readOnly = true)
    public Page<AlmacenResponse> listar(String texto, Boolean activo, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null) {
            return almacenRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return almacenRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return almacenRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        return almacenRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public AlmacenResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public AlmacenResponse crear(AlmacenCreateRequest request) {
        String codigo = normalizeCode(request.codigo());
        if (almacenRepository.existsByCodigo(codigo)) {
            throw new BusinessException("Ya existe un almacen con el codigo indicado");
        }

        AlmacenEntity almacen = new AlmacenEntity();
        almacen.setCodigo(codigo);
        almacen.setNombre(cleanRequired(request.nombre()));
        almacen.setDireccion(clean(request.direccion()));
        almacen.setResponsable(clean(request.responsable()));
        almacen.setActivo(true);
        return toResponse(almacenRepository.save(almacen));
    }

    @Transactional
    public AlmacenResponse actualizar(Long id, AlmacenUpdateRequest request) {
        AlmacenEntity almacen = findById(id);
        String codigo = normalizeCode(request.codigo());
        if (almacenRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new BusinessException("Ya existe un almacen con el codigo indicado");
        }

        almacen.setCodigo(codigo);
        almacen.setNombre(cleanRequired(request.nombre()));
        almacen.setDireccion(clean(request.direccion()));
        almacen.setResponsable(clean(request.responsable()));
        return toResponse(almacen);
    }

    @Transactional
    public AlmacenResponse desactivar(Long id) {
        AlmacenEntity almacen = findById(id);
        almacen.setActivo(false);
        return toResponse(almacen);
    }

    @Transactional
    public AlmacenResponse activar(Long id) {
        AlmacenEntity almacen = findById(id);
        almacen.setActivo(true);
        return toResponse(almacen);
    }

    private AlmacenEntity findById(Long id) {
        return almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacen no encontrado"));
    }

    private AlmacenResponse toResponse(AlmacenEntity almacen) {
        return new AlmacenResponse(
                almacen.getId(),
                almacen.getCodigo(),
                almacen.getNombre(),
                almacen.getDireccion(),
                almacen.getResponsable(),
                almacen.isActivo(),
                almacen.getCreadoEn(),
                almacen.getActualizadoEn()
        );
    }

    private String normalizeCode(String value) {
        return cleanRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeSearch(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue;
    }

    private String cleanRequired(String value) {
        return value == null ? null : value.trim();
    }

    private String clean(String value) {
        return value == null ? null : value.trim();
    }
}
