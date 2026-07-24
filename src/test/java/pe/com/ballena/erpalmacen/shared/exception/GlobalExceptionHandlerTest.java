package pe.com.ballena.erpalmacen.shared.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import pe.com.ballena.erpalmacen.shared.response.ApiErrorResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void optimisticLockDevuelveConflict() {
        var response = handler.handleConcurrencyConflict(
                new ObjectOptimisticLockingFailureException("MovimientoInventarioEntity", 1L),
                request()
        );

        assertConflictResponse(response.getStatusCode().value(), response.getBody());
    }

    @Test
    void pessimisticLockDevuelveConflict() {
        var response = handler.handleConcurrencyConflict(
                new PessimisticLockingFailureException("No se pudo obtener el bloqueo"),
                request()
        );

        assertConflictResponse(response.getStatusCode().value(), response.getBody());
    }

    @Test
    void cannotAcquireLockDevuelveConflict() {
        var response = handler.handleConcurrencyConflict(
                new CannotAcquireLockException("No se pudo adquirir el bloqueo"),
                request()
        );

        assertConflictResponse(response.getStatusCode().value(), response.getBody());
    }

    @Test
    void dataIntegrityViolationDevuelveConflict() {
        var response = handler.handleDataIntegrityViolation(
                new DataIntegrityViolationException("Clave unica duplicada"),
                request()
        );

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().error()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
        assertThat(response.getBody().message()).contains("conflicto de datos");
        assertThat(response.getBody().path()).isEqualTo("/api/inventario/movimientos/1/confirmar");
    }

    @Test
    void errorNoClasificadoSigueDevuelveInternalServerError() {
        var response = handler.handleUnexpectedException(new RuntimeException("Error no esperado"), request());

        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().message()).isEqualTo("Error interno del servidor");
    }

    private void assertConflictResponse(int statusCode, ApiErrorResponse body) {
        assertThat(statusCode).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(body.error()).isEqualTo(HttpStatus.CONFLICT.getReasonPhrase());
        assertThat(body.message()).contains("modificado por otro usuario");
        assertThat(body.path()).isEqualTo("/api/inventario/movimientos/1/confirmar");
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/inventario/movimientos/1/confirmar");
        return request;
    }
}
