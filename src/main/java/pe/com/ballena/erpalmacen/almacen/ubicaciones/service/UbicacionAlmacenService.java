package pe.com.ballena.erpalmacen.almacen.ubicaciones.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.almacen.almacenes.entity.AlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.almacenes.repository.AlmacenRepository;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.dto.UbicacionAlmacenCreateRequest;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.dto.UbicacionAlmacenResponse;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.dto.UbicacionAlmacenUpdateRequest;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.entity.UbicacionAlmacenEntity;
import pe.com.ballena.erpalmacen.almacen.ubicaciones.repository.UbicacionAlmacenRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.util.Locale;

@Service
public class UbicacionAlmacenService {

    private final UbicacionAlmacenRepository ubicacionRepository;
    private final AlmacenRepository almacenRepository;

    public UbicacionAlmacenService(
            UbicacionAlmacenRepository ubicacionRepository,
            AlmacenRepository almacenRepository
    ) {
        this.ubicacionRepository = ubicacionRepository;
        this.almacenRepository = almacenRepository;
    }

    @Transactional(readOnly = true)
    public Page<UbicacionAlmacenResponse> listar(String texto, Boolean activo, Long almacenId, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null && almacenId == null) {
            return ubicacionRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null && activo == null) {
            return ubicacionRepository.findByAlmacen_Id(almacenId, pageable).map(this::toResponse);
        }
        if (textoNormalizado == null && almacenId == null) {
            return ubicacionRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return ubicacionRepository.findByAlmacen_IdAndActivo(almacenId, activo, pageable).map(this::toResponse);
        }
        if (activo == null && almacenId == null) {
            return ubicacionRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        if (almacenId == null) {
            return ubicacionRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return ubicacionRepository.buscarPorTextoYAlmacen(textoNormalizado, almacenId, pageable).map(this::toResponse);
        }
        return ubicacionRepository.buscarPorTextoAlmacenYActivo(textoNormalizado, almacenId, activo, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<UbicacionAlmacenResponse> listarPorAlmacen(Long almacenId, String texto, Boolean activo, Pageable pageable) {
        return listar(texto, activo, almacenId, pageable);
    }

    @Transactional(readOnly = true)
    public UbicacionAlmacenResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public UbicacionAlmacenResponse crear(UbicacionAlmacenCreateRequest request) {
        AlmacenEntity almacen = findAlmacenActivo(request.almacenId());
        String codigo = normalizeCode(request.codigo());
        if (ubicacionRepository.existsByAlmacen_IdAndCodigo(almacen.getId(), codigo)) {
            throw new BusinessException("Ya existe una ubicacion con el codigo indicado en el almacen");
        }

        UbicacionAlmacenEntity ubicacion = new UbicacionAlmacenEntity();
        ubicacion.setAlmacen(almacen);
        ubicacion.setCodigo(codigo);
        ubicacion.setNombre(normalizeText(request.nombre()));
        ubicacion.setDescripcion(normalizeText(request.descripcion()));
        ubicacion.setActivo(true);
        return toResponse(ubicacionRepository.save(ubicacion));
    }

    @Transactional
    public UbicacionAlmacenResponse actualizar(Long id, UbicacionAlmacenUpdateRequest request) {
        UbicacionAlmacenEntity ubicacion = findById(id);
        AlmacenEntity almacen = findAlmacenActivo(request.almacenId());
        String codigo = normalizeCode(request.codigo());
        if (ubicacionRepository.existsByAlmacen_IdAndCodigoAndIdNot(almacen.getId(), codigo, id)) {
            throw new BusinessException("Ya existe una ubicacion con el codigo indicado en el almacen");
        }

        ubicacion.setAlmacen(almacen);
        ubicacion.setCodigo(codigo);
        ubicacion.setNombre(normalizeText(request.nombre()));
        ubicacion.setDescripcion(normalizeText(request.descripcion()));
        return toResponse(ubicacion);
    }

    @Transactional
    public UbicacionAlmacenResponse desactivar(Long id) {
        UbicacionAlmacenEntity ubicacion = findById(id);
        ubicacion.setActivo(false);
        return toResponse(ubicacion);
    }

    @Transactional
    public UbicacionAlmacenResponse activar(Long id) {
        UbicacionAlmacenEntity ubicacion = findById(id);
        ubicacion.setActivo(true);
        return toResponse(ubicacion);
    }

    private UbicacionAlmacenEntity findById(Long id) {
        return ubicacionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ubicacion de almacen no encontrada"));
    }

    private AlmacenEntity findAlmacenActivo(Long id) {
        AlmacenEntity almacen = almacenRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Almacen no encontrado"));
        if (!almacen.isActivo()) {
            throw new BusinessException("No se puede usar un almacen inactivo");
        }
        return almacen;
    }

    private UbicacionAlmacenResponse toResponse(UbicacionAlmacenEntity ubicacion) {
        AlmacenEntity almacen = ubicacion.getAlmacen();
        return new UbicacionAlmacenResponse(
                ubicacion.getId(),
                almacen.getId(),
                almacen.getCodigo(),
                almacen.getNombre(),
                ubicacion.getCodigo(),
                ubicacion.getNombre(),
                ubicacion.getDescripcion(),
                ubicacion.isActivo(),
                ubicacion.getCreadoEn(),
                ubicacion.getActualizadoEn()
        );
    }

    private String normalizeCode(String value) {
        return cleanRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toUpperCase(Locale.ROOT);
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
