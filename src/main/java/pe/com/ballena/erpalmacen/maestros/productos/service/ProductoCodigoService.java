package pe.com.ballena.erpalmacen.maestros.productos.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaCorrelativoEntity;
import pe.com.ballena.erpalmacen.maestros.familias.entity.FamiliaEntity;
import pe.com.ballena.erpalmacen.maestros.familias.repository.FamiliaCorrelativoRepository;
import pe.com.ballena.erpalmacen.maestros.productos.repository.ProductoRepository;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;

@Service
public class ProductoCodigoService {

    private final FamiliaCorrelativoRepository familiaCorrelativoRepository;
    private final ProductoRepository productoRepository;

    public ProductoCodigoService(
            FamiliaCorrelativoRepository familiaCorrelativoRepository,
            ProductoRepository productoRepository
    ) {
        this.familiaCorrelativoRepository = familiaCorrelativoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String generarCodigo(FamiliaEntity familia) {
        FamiliaCorrelativoEntity correlativo = familiaCorrelativoRepository.findByFamiliaIdForUpdate(familia.getId())
                .orElseThrow(() -> new BusinessException("No existe correlativo configurado para la familia seleccionada"));

        long siguiente = correlativo.getUltimoCorrelativo() + 1;
        String codigo = construirCodigo(familia.getPrefijo(), siguiente);
        while (productoRepository.existsByCodigo(codigo)) {
            siguiente++;
            codigo = construirCodigo(familia.getPrefijo(), siguiente);
        }

        correlativo.setUltimoCorrelativo(siguiente);
        return codigo;
    }

    @Transactional(readOnly = true)
    public String obtenerSiguienteCodigo(FamiliaEntity familia) {
        long siguiente = familiaCorrelativoRepository.findByFamiliaId(familia.getId())
                .map(FamiliaCorrelativoEntity::getUltimoCorrelativo)
                .orElse(0L) + 1;

        String codigo = construirCodigo(familia.getPrefijo(), siguiente);
        while (productoRepository.existsByCodigo(codigo)) {
            siguiente++;
            codigo = construirCodigo(familia.getPrefijo(), siguiente);
        }
        return codigo;
    }

    private String construirCodigo(String prefijo, long correlativo) {
        return prefijo + "-" + correlativo;
    }
}
