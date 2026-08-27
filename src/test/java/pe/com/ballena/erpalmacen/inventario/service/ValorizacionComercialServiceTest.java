package pe.com.ballena.erpalmacen.inventario.service;

import org.junit.jupiter.api.Test;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValorizacionComercialServiceTest {

    private final ValorizacionComercialService service = new ValorizacionComercialService();

    @Test
    void calculaBaseDoceSetentaYUnoConRecargoTreintaYCinco() {
        var resultado = service.calcular(
                "COD123",
                new BigDecimal("12.71"),
                new BigDecimal("35.00"),
                BigDecimal.ONE
        );

        assertThat(resultado.valorVentaUnitarioSinIgv()).isEqualByComparingTo("17.16");
        assertThat(resultado.igvUnitario()).isEqualByComparingTo("3.09");
        assertThat(resultado.precioVentaUnitarioConIgv()).isEqualByComparingTo("20.25");
    }

    @Test
    void calculaBaseQuinceConRecargosPredefinidos() {
        var veinticinco = service.calcular("COD123", new BigDecimal("15.00"), new BigDecimal("25.00"), BigDecimal.ONE);
        var cien = service.calcular("COD123", new BigDecimal("15.00"), new BigDecimal("100.00"), BigDecimal.ONE);

        assertThat(veinticinco.valorVentaUnitarioSinIgv()).isEqualByComparingTo("18.75");
        assertThat(cien.valorVentaUnitarioSinIgv()).isEqualByComparingTo("30.00");
    }

    @Test
    void calculaRecargoPersonalizadoDecimalYTotales() {
        var resultado = service.calcular(
                "COD123",
                new BigDecimal("10.00"),
                new BigDecimal("42.50"),
                new BigDecimal("3.0000")
        );

        assertThat(resultado.recargoComercialPct()).isEqualByComparingTo("42.50");
        assertThat(resultado.valorVentaUnitarioSinIgv()).isEqualByComparingTo("14.25");
        assertThat(resultado.valorVentaTotalSinIgv()).isEqualByComparingTo("42.75");
        assertThat(resultado.igvTotal()).isEqualByComparingTo("7.70");
        assertThat(resultado.precioVentaTotalConIgv()).isEqualByComparingTo("50.45");
    }

    @Test
    void redondeaConHalfUpYUsaBigDecimal() {
        var resultado = service.calcular(
                "COD123",
                new BigDecimal("0.015"),
                BigDecimal.ZERO,
                BigDecimal.ONE
        );

        assertThat(resultado.costoBaseComercialSinIgv()).isEqualByComparingTo("0.02");
        assertThat(resultado.valorVentaUnitarioSinIgv()).isEqualByComparingTo("0.02");
        assertThat(resultado.igvUnitario()).isEqualByComparingTo("0.00");
    }

    @Test
    void rechazaCostoBaseORecargoInvalidos() {
        assertThatThrownBy(() -> service.calcular("COD123", null, BigDecimal.ZERO, BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("costo base comercial sin IGV");
        assertThatThrownBy(() -> service.calcular("COD123", new BigDecimal("-1.00"), BigDecimal.ZERO, BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("invalido");
        assertThatThrownBy(() -> service.calcular("COD123", BigDecimal.ONE, null, BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("recargo comercial");
        assertThatThrownBy(() -> service.calcular("COD123", BigDecimal.ONE, new BigDecimal("-0.01"), BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("mayor o igual a cero");
        assertThatThrownBy(() -> service.calcular("COD123", BigDecimal.ONE, new BigDecimal("1000.01"), BigDecimal.ONE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1000.00");
    }
}
