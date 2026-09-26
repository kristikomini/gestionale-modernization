package it.kristikomini.modern;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The modern side of the strangler: Spring Boot 3 / Java 21, {@code jakarta.*} namespace.
 *
 * <p>It hosts the migrated endpoints (clean REST + OpenAPI), the Anti-Corruption Layer that
 * consumes the legacy SOAP service, and — as routes are strangled — becomes the system of record.
 */
@SpringBootApplication
public class ModernGestionaleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ModernGestionaleApplication.class, args);
    }
}
