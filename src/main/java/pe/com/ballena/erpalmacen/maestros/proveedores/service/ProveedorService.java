package pe.com.ballena.erpalmacen.maestros.proveedores.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.proveedores.dto.ProveedorCreateRequest;
import pe.com.ballena.erpalmacen.maestros.proveedores.dto.ProveedorResponse;
import pe.com.ballena.erpalmacen.maestros.proveedores.dto.ProveedorUpdateRequest;
import pe.com.ballena.erpalmacen.maestros.proveedores.entity.ProveedorEntity;
import pe.com.ballena.erpalmacen.maestros.proveedores.repository.ProveedorRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;
import pe.com.ballena.erpalmacen.shared.exception.ResourceNotFoundException;

import java.util.Locale;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedorRepository;

    public ProveedorService(ProveedorRepository proveedorRepository) {
        this.proveedorRepository = proveedorRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProveedorResponse> listar(String texto, Boolean activo, Pageable pageable) {
        String textoNormalizado = normalizeSearch(texto);
        if (textoNormalizado == null && activo == null) {
            return proveedorRepository.findAll(pageable).map(this::toResponse);
        }
        if (textoNormalizado == null) {
            return proveedorRepository.findByActivo(activo, pageable).map(this::toResponse);
        }
        if (activo == null) {
            return proveedorRepository.buscarPorTexto(textoNormalizado, pageable).map(this::toResponse);
        }
        return proveedorRepository.buscarPorTextoYActivo(textoNormalizado, activo, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public ProveedorResponse crear(ProveedorCreateRequest request) {
        String numeroDocumento = normalizeCode(request.numeroDocumento());
        if (proveedorRepository.existsByNumeroDocumento(numeroDocumento)) {
            throw new BusinessException("Ya existe un proveedor con el numero de documento indicado");
        }

        ProveedorEntity proveedor = new ProveedorEntity();
        proveedor.setTipoDocumento(normalizeCode(request.tipoDocumento()));
        proveedor.setNumeroDocumento(numeroDocumento);
        proveedor.setRazonSocial(normalizeText(request.razonSocial()));
        proveedor.setNombreComercial(normalizeText(request.nombreComercial()));
        proveedor.setDireccion(normalizeText(request.direccion()));
        proveedor.setTelefono(clean(request.telefono()));
        proveedor.setEmail(normalizeEmail(request.email()));
        proveedor.setActivo(true);
        return toResponse(proveedorRepository.save(proveedor));
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorUpdateRequest request) {
        ProveedorEntity proveedor = findById(id);
        String numeroDocumento = normalizeCode(request.numeroDocumento());
        if (proveedorRepository.existsByNumeroDocumentoAndIdNot(numeroDocumento, id)) {
            throw new BusinessException("Ya existe un proveedor con el numero de documento indicado");
        }

        proveedor.setTipoDocumento(normalizeCode(request.tipoDocumento()));
        proveedor.setNumeroDocumento(numeroDocumento);
        proveedor.setRazonSocial(normalizeText(request.razonSocial()));
        proveedor.setNombreComercial(normalizeText(request.nombreComercial()));
        proveedor.setDireccion(normalizeText(request.direccion()));
        proveedor.setTelefono(clean(request.telefono()));
        proveedor.setEmail(normalizeEmail(request.email()));
        return toResponse(proveedor);
    }

    @Transactional
    public ProveedorResponse desactivar(Long id) {
        ProveedorEntity proveedor = findById(id);
        proveedor.setActivo(false);
        return toResponse(proveedor);
    }

    @Transactional
    public ProveedorResponse activar(Long id) {
        ProveedorEntity proveedor = findById(id);
        proveedor.setActivo(true);
        return toResponse(proveedor);
    }

    private ProveedorEntity findById(Long id) {
        return proveedorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Proveedor no encontrado"));
    }

    private ProveedorResponse toResponse(ProveedorEntity proveedor) {
        return new ProveedorResponse(
                proveedor.getId(),
                proveedor.getTipoDocumento(),
                proveedor.getNumeroDocumento(),
                proveedor.getRazonSocial(),
                proveedor.getNombreComercial(),
                proveedor.getDireccion(),
                proveedor.getTelefono(),
                proveedor.getEmail(),
                proveedor.isActivo(),
                proveedor.getCreadoEn(),
                proveedor.getActualizadoEn()
        );
    }

    private String normalizeCode(String value) {
        return cleanRequired(value).toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String value) {
        String cleanValue = clean(value);
        return cleanValue == null || cleanValue.isBlank() ? null : cleanValue.toLowerCase(Locale.ROOT);
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
