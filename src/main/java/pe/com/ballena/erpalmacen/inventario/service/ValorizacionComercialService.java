package pe.com.ballena.erpalmacen.inventario.service;

import org.springframework.stereotype.Service;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class ValorizacionComercialService {

    public static final int MONEY_SCALE = 2;
    public static final int PERCENT_SCALE = 2;
    public static final BigDecimal IGV_RATE = new BigDecimal("0.18");
    public static final BigDecimal RECARGO_COMERCIAL_MAXIMO_PCT = new BigDecimal("1000.00");

    private static final BigDecimal CIEN = new BigDecimal("100");

    public Resultado calcular(
            String codigoProducto,
            BigDecimal costoBaseComercialSinIgv,
            BigDecimal recargoComercialPct,
            BigDecimal cantidad
    ) {
        validarCostoBaseComercial(codigoProducto, costoBaseComercialSinIgv);
        validarRecargoComercial(recargoComercialPct);
        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("La cantidad del detalle debe ser mayor que cero");
        }

        BigDecimal base = dinero(costoBaseComercialSinIgv);
        BigDecimal recargo = porcentaje(recargoComercialPct);
        BigDecimal factorRecargo = BigDecimal.ONE.add(recargo.divide(CIEN, 10, RoundingMode.HALF_UP));
        BigDecimal valorVentaUnitarioSinIgv = dinero(base.multiply(factorRecargo));
        BigDecimal igvUnitario = dinero(valorVentaUnitarioSinIgv.multiply(IGV_RATE));
        BigDecimal precioVentaUnitarioConIgv = dinero(valorVentaUnitarioSinIgv.add(igvUnitario));
        BigDecimal valorVentaTotalSinIgv = dinero(valorVentaUnitarioSinIgv.multiply(cantidad));
        BigDecimal igvTotal = dinero(valorVentaTotalSinIgv.multiply(IGV_RATE));
        BigDecimal precioVentaTotalConIgv = dinero(valorVentaTotalSinIgv.add(igvTotal));

        return new Resultado(
                base,
                recargo,
                valorVentaUnitarioSinIgv,
                igvUnitario,
                precioVentaUnitarioConIgv,
                valorVentaTotalSinIgv,
                igvTotal,
                precioVentaTotalConIgv
        );
    }

    public void validarRecargoComercialSiInformado(BigDecimal recargoComercialPct) {
        if (recargoComercialPct != null) {
            validarRecargoComercial(recargoComercialPct);
        }
    }

    private void validarCostoBaseComercial(String codigoProducto, BigDecimal costoBaseComercialSinIgv) {
        String codigo = codigoProducto == null || codigoProducto.isBlank() ? "sin codigo" : codigoProducto;
        if (costoBaseComercialSinIgv == null) {
            throw new BusinessException("El producto " + codigo + " no tiene configurado un costo base comercial sin IGV.");
        }
        if (costoBaseComercialSinIgv.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El producto " + codigo + " tiene un costo base comercial sin IGV invalido.");
        }
    }

    private void validarRecargoComercial(BigDecimal recargoComercialPct) {
        if (recargoComercialPct == null) {
            throw new BusinessException("El recargo comercial es obligatorio para una salida de venta.");
        }
        if (recargoComercialPct.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("El recargo comercial debe ser mayor o igual a cero.");
        }
        if (recargoComercialPct.compareTo(RECARGO_COMERCIAL_MAXIMO_PCT) > 0) {
            throw new BusinessException("El recargo comercial no puede superar 1000.00%.");
        }
    }

    private BigDecimal dinero(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.HALF_UP);
    }

    private BigDecimal porcentaje(BigDecimal value) {
        return value.setScale(PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    public record Resultado(
            BigDecimal costoBaseComercialSinIgv,
            BigDecimal recargoComercialPct,
            BigDecimal valorVentaUnitarioSinIgv,
            BigDecimal igvUnitario,
            BigDecimal precioVentaUnitarioConIgv,
            BigDecimal valorVentaTotalSinIgv,
            BigDecimal igvTotal,
            BigDecimal precioVentaTotalConIgv
    ) {
    }
}
