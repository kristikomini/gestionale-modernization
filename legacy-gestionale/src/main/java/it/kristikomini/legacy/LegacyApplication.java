package it.kristikomini.legacy;

import it.kristikomini.legacy.soap.PolicyServiceImpl;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.xml.ws.Endpoint;

/**
 * Bootstraps the legacy system: opens the JPA {@code EntityManagerFactory} and publishes the
 * SOAP endpoint with the JAX-WS reference implementation.
 *
 * <p>{@code Endpoint.publish} starts a lightweight HTTP server and serves the WSDL at
 * {@code <address>?wsdl} — this is how the pre-Spring-Boot world stood a SOAP service up. The
 * modern module's Anti-Corruption Layer consumes this WSDL.
 *
 * <p>Note: {@code javax.xml.ws} was removed from the JDK in Java 11, which is one more reason
 * these systems get stuck — running them on a modern JDK requires the standalone JAX-WS RI on
 * the classpath (the {@code jaxws-rt} dependency), which is what we do here.
 */
public final class LegacyApplication {

    private static final String ENDPOINT_ADDRESS = "http://0.0.0.0:8088/ws/policy";

    private LegacyApplication() {
    }

    public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("legacy-gestionale");
        try {
            PolicyServiceImpl service = new PolicyServiceImpl(emf);
            Endpoint endpoint = Endpoint.publish(ENDPOINT_ADDRESS, service);
            System.out.println("Legacy PolicyService published at " + ENDPOINT_ADDRESS);
            System.out.println("WSDL: " + ENDPOINT_ADDRESS + "?wsdl");

            // Keep the JVM alive while the endpoint serves requests.
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    if (endpoint.isPublished()) {
                        endpoint.stop();
                    }
                    emf.close();
                }
            }));
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (emf.isOpen()) {
                emf.close();
            }
        }
    }
}
