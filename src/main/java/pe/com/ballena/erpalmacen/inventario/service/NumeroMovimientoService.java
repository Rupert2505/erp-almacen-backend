package pe.com.ballena.erpalmacen.inventario.service;

import org.springframework.stereotype.Service;
import pe.com.ballena.erpalmacen.inventario.movimientos.repository.MovimientoInventarioRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class NumeroMovimientoService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int RANDOM_LENGTH = 5;
    private static final int MAX_ATTEMPTS = 10;

    private final SecureRandom secureRandom = new SecureRandom();
    private final MovimientoInventarioRepository movimientoInventarioRepository;

    public NumeroMovimientoService(MovimientoInventarioRepository movimientoInventarioRepository) {
        this.movimientoInventarioRepository = movimientoInventarioRepository;
    }

    public String generar() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String numero = "MOV-" + LocalDateTime.now().format(FORMATTER) + "-" + randomSuffix();
            if (!movimientoInventarioRepository.existsByNumero(numero)) {
                return numero;
            }
        }
        throw new IllegalStateException("No se pudo generar un numero de movimiento unico");
    }

    private String randomSuffix() {
        StringBuilder builder = new StringBuilder(RANDOM_LENGTH);
        for (int i = 0; i < RANDOM_LENGTH; i++) {
            builder.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return builder.toString();
    }
}
