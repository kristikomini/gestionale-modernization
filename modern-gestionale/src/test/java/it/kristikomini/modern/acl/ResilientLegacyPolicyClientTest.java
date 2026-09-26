package it.kristikomini.modern.acl;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import it.kristikomini.modern.acl.soap.PolicyServiceSoap;
import it.kristikomini.modern.gateway.LegacyUnavailableException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for the ACL resilience — no Spring context, no Docker, no live SOAP endpoint: a mock
 * port plus Resilience4j registries built in-test. Proves the two behaviours the ACL exists for.
 */
class ResilientLegacyPolicyClientTest {

    private final PolicyServiceSoap port = mock(PolicyServiceSoap.class);
    private final LegacyPolicyMapper mapper = new LegacyPolicyMapper();

    @Test
    void retriesTransientFailuresThenSurfacesCleanError() {
        // Breaker that never opens in this test → isolates retry behaviour.
        CircuitBreakerRegistry cbr = CircuitBreakerRegistry.of(
                CircuitBreakerConfig.custom().minimumNumberOfCalls(100).build());
        RetryRegistry rr = RetryRegistry.of(RetryConfig.custom()
                .maxAttempts(3).waitDuration(Duration.ofMillis(1)).build());
        var client = new ResilientLegacyPolicyClient(port, mapper, cbr, rr);

        when(port.listPolicies()).thenThrow(new RuntimeException("SOAP backend down"));

        assertThatThrownBy(client::listPolicies)
                .isInstanceOf(LegacyUnavailableException.class);

        // Retried up to maxAttempts before giving up.
        verify(port, times(3)).listPolicies();
    }

    @Test
    void opensCircuitAndFailsFastWithoutCallingLegacyAgain() {
        // Breaker opens after a single failed call; retry disabled so we observe the breaker alone.
        CircuitBreakerRegistry cbr = CircuitBreakerRegistry.of(CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(1)
                .minimumNumberOfCalls(1)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(60))
                .build());
        RetryRegistry rr = RetryRegistry.of(RetryConfig.custom().maxAttempts(1).build());
        var client = new ResilientLegacyPolicyClient(port, mapper, cbr, rr);

        when(port.listPolicies()).thenThrow(new RuntimeException("SOAP backend down"));

        // First call fails through to the backend and trips the breaker.
        assertThatThrownBy(client::listPolicies).isInstanceOf(LegacyUnavailableException.class);
        assertThat(cbr.circuitBreaker("legacyPolicyClient").getState())
                .isEqualTo(CircuitBreaker.State.OPEN);

        // Second call must fail fast: caused by CallNotPermittedException, backend NOT called again.
        assertThatThrownBy(client::listPolicies)
                .isInstanceOf(LegacyUnavailableException.class)
                .hasCauseInstanceOf(CallNotPermittedException.class);
        verify(port, times(1)).listPolicies();
    }
}
