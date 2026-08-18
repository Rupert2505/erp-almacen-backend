package pe.com.ballena.erpalmacen.inventario.service;

import org.springframework.stereotype.Service;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CostoInventariableService {

    public static final int COST_SCALE = 6;
    private static final BigDecimal CIEN = new BigDecimal("100");

    public BigDecimal calcularCostoInventariable(
            BigDecimal valorUnitarioSinIgv,
            BigDecimal tasaIgv,
            boolean igvRecuperable
    ) {
        validarNoNegativo(valorUnitarioSinIgv, "El valor unitario sin IGV no puede ser negativo");
        validarNoNegativo(tasaIgv, "La tasa IGV no puede ser negativa");

        BigDecimal tasa = tasaIgv == null ? BigDecimal.ZERO : tasaIgv;
        if (Boolean.TRUE.equals(igvRecuperable) || tasa.compareTo(BigDecimal.ZERO) == 0) {
            return valorUnitarioSinIgv;
        }
        return valorUnitarioSinIgv.add(calcularIgvUnitario(valorUnitarioSinIgv, tasa));
    }

    public Resultado calcularDetalleEntrada(
            BigDecimal cantidad,
            BigDecimal costoUnitarioDirecto,
            BigDecimal valorUnitarioSinIgv,
            BigDecimal tasaIgv,
            Boolean igvRecuperable
    ) {
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La cantidad del detalle debe ser mayor que cero");
        }

        if (valorUnitarioSinIgv == null) {
            validarNoNegativo(costoUnitarioDirecto, "El costo unitario no puede ser negativo");
            return new Resultado(
                    null,
                    null,
                    null,
                    null,
                    null,
                    costoUnitarioDirecto,
                    calcularValor(cantidad, costoUnitarioDirecto),
                    null,
                    null
            );
        }

        BigDecimal tasa = tasaIgv == null ? BigDecimal.ZERO : tasaIgv;
        boolean recuperable = igvRecuperable == null || igvRecuperable;
        validarNoNegativo(valorUnitarioSinIgv, "El valor unitario sin IGV no puede ser negativo");
        validarNoNegativo(tasa, "La tasa IGV no puede ser negativa");

        BigDecimal igvUnitario = calcularIgvUnitario(valorUnitarioSinIgv, tasa);
        BigDecimal precioUnitarioConIgv = valorUnitarioSinIgv.add(igvUnitario);
        BigDecimal costoInventariable = calcularCostoInventariable(valorUnitarioSinIgv, tasa, recuperable);

        return new Resultado(
                valorUnitarioSinIgv,
                tasa,
                igvUnitario,
                precioUnitarioConIgv,
                recuperable,
                costoInventariable,
                calcularValor(cantidad, costoInventariable),
                calcularValor(cantidad, precioUnitarioConIgv),
                calcularValor(cantidad, igvUnitario)
        );
    }

    private BigDecimal calcularIgvUnitario(BigDecimal valorUnitarioSinIgv, BigDecimal tasaIgv) {
        return valorUnitarioSinIgv
                .multiply(tasaIgv)
                .divide(CIEN, COST_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularValor(BigDecimal cantidad, BigDecimal costoUnitario) {
        if (cantidad == null || costoUnitario == null) {
            return null;
        }
        return cantidad.multiply(costoUnitario).setScale(COST_SCALE, RoundingMode.HALF_UP);
    }

    private void validarNoNegativo(BigDecimal valor, String mensaje) {
        if (valor != null && valor.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(mensaje);
        }
    }

    public record Resultado(
            BigDecimal valorUnitarioSinIgv,
            BigDecimal tasaIgv,
            BigDecimal montoIgvUnitario,
            BigDecimal precioUnitarioConIgv,
            Boolean igvRecuperable,
            BigDecimal costoUnitarioInventariable,
            BigDecimal totalInventariable,
            BigDecimal totalConIgv,
            BigDecimal montoIgvTotal
    ) {
    }
}
