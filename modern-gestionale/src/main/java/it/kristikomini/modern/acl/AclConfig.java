package it.kristikomini.modern.acl;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryRegistry;
import it.kristikomini.modern.acl.soap.PolicyServiceSoap;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Anti-Corruption Layer. Guarded by {@code legacy.policy-service.enabled=true} so the
 * default application boots without a live legacy SOAP endpoint (the migrated routes are served
 * from the modern database). Enabling it activates the strangler path that proxies to legacy.
 */
@Configuration
@ConditionalOnProperty(prefix = "legacy.policy-service", name = "enabled", havingValue = "true")
public class AclConfig {

    /**
     * A CXF JAX-WS client proxy for the legacy SOAP service. {@code JaxWsProxyFactoryBean.create()}
     * builds a dynamic proxy over {@link PolicyServiceSoap}; it does not connect until a method is
     * called, so this is safe to construct at startup.
     */
    @Bean
    public PolicyServiceSoap legacyPolicyServiceSoap(
            @org.springframework.beans.factory.annotation.Value("${legacy.policy-service.endpoint-url}") String endpointUrl) {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(PolicyServiceSoap.class);
        factory.setAddress(endpointUrl);
        return (PolicyServiceSoap) factory.create();
    }

    @Bean
    public LegacyPolicyClient legacyPolicyClient(PolicyServiceSoap port,
                                                 LegacyPolicyMapper mapper,
                                                 CircuitBreakerRegistry circuitBreakerRegistry,
                                                 RetryRegistry retryRegistry) {
        return new ResilientLegacyPolicyClient(port, mapper, circuitBreakerRegistry, retryRegistry);
    }
}
