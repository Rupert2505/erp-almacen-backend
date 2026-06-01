package pe.com.ballena.erpalmacen.auth.dto;

import java.util.List;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String username,
        String nombres,
        String apellidos,
        String email,
        List<String> roles,
        List<String> permisos
) {
}
