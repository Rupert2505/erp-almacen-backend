package pe.com.ballena.erpalmacen.inventario.service;

import org.junit.jupiter.api.Test;
import pe.com.ballena.erpalmacen.shared.exception.BusinessException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CostoInventariableServiceTest {

    private final CostoInventariableService service = new CostoInventariableService();

    @Test
    void igvRecuperableNoFormaParteDelCostoInventariable() {
        var resultado = service.calcularDetalleEntrada(
                new BigDecimal("10"),
                null,
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                true
        );

        assertThat(resultado.costoUnitarioInventariable()).isEqualByComparingTo("100.00");
        assertThat(resultado.montoIgvUnitario()).isEqualByComparingTo("18.000000");
        assertThat(resultado.precioUnitarioConIgv()).isEqualByComparingTo("118.000000");
        assertThat(resultado.totalInventariable()).isEqualByComparingTo("1000.000000");
        assertThat(resultado.totalConIgv()).isEqualByComparingTo("1180.000000");
    }

    @Test
    void igvNoRecuperableFormaParteDelCostoInventariable() {
        var resultado = service.calcularDetalleEntrada(
                new BigDecimal("10"),
                null,
                new BigDecimal("100.00"),
                new BigDecimal("18.00"),
                false
        );

        assertThat(resultado.costoUnitarioInventariable()).isEqualByComparingTo("118.000000");
        assertThat(resultado.totalInventariable()).isEqualByComparingTo("1180.000000");
    }

    @Test
    void tasaCeroNoCambiaCostoInventariable() {
        assertThat(service.calcularCostoInventariable(new BigDecimal("100.00"), BigDecimal.ZERO, true))
                .isEqualByComparingTo("100.00");
        assertThat(service.calcularCostoInventariable(new BigDecimal("100.00"), BigDecimal.ZERO, false))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void calculaDecimalesConBigDecimalSinErroresBinarios() {
        var resultado = service.calcularDetalleEntrada(
                new BigDecimal("7"),
                null,
                new BigDecimal("13.37"),
                new BigDecimal("18"),
                false
        );

        assertThat(resultado.montoIgvUnitario()).isEqualByComparingTo("2.406600");
        assertThat(resultado.precioUnitarioConIgv()).isEqualByComparingTo("15.776600");
        assertThat(resultado.costoUnitarioInventariable()).isEqualByComparingTo("15.776600");
        assertThat(resultado.totalInventariable()).isEqualByComparingTo("110.436200");
    }

    @Test
    void rechazaValoresNegativos() {
        assertThatThrownBy(() -> service.calcularDetalleEntrada(
                BigDecimal.ZERO,
                null,
                new BigDecimal("1"),
                BigDecimal.ZERO,
                true
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("cantidad");

        assertThatThrownBy(() -> service.calcularDetalleEntrada(
                BigDecimal.ONE,
                null,
                new BigDecimal("-1"),
                BigDecimal.ZERO,
                true
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("valor unitario");

        assertThatThrownBy(() -> service.calcularDetalleEntrada(
                BigDecimal.ONE,
                null,
                BigDecimal.ONE,
                new BigDecimal("-18"),
                true
        )).isInstanceOf(BusinessException.class)
                .hasMessageContaining("tasa IGV");
    }
}
