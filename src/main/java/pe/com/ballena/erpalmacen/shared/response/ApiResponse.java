package pe.com.ballena.erpalmacen.shared.response;

import java.time.Instant;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(T data) {
        return ok("Operación realizada correctamente", data);
    }

    public static ApiResponse<Void> empty(String message) {
        return new ApiResponse<>(true, message, null, Instant.now());
    }
}
