package pe.com.ballena.erpalmacen;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class ErpAlmacenApplication {

    private static final String DEFAULT_TIME_ZONE = "America/Lima";

    public static void main(String[] args) {
        String timeZoneId = System.getenv().getOrDefault("APP_TIME_ZONE", DEFAULT_TIME_ZONE);
        TimeZone.setDefault(TimeZone.getTimeZone(timeZoneId));
        SpringApplication.run(ErpAlmacenApplication.class, args);
    }
}
