package pe.com.ballena.erpalmacen.shared.response;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        boolean success,
        int status,
        String error,
        String message,
        String path,
        Instant timestamp,
        Map<String, String> fieldErrors
) {

    public static ApiErrorResponse of(int status, String error, String message, String path) {
        return new ApiErrorResponse(false, status, error, message, path, Instant.now(), Map.of());
    }

    public static ApiErrorResponse withFieldErrors(
            int status,
            String error,
            String message,
            String path,
            Map<String, String> fieldErrors
    ) {
        return new ApiErrorResponse(false, status, error, message, path, Instant.now(), fieldErrors);
    }
}
