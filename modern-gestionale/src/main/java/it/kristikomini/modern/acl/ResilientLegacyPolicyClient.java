package it.kristikomini.modern.acl;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import it.kristikomini.modern.acl.soap.PolicyServiceSoap;
import it.kristikomini.modern.acl.soap.SoapPolicy;
import it.kristikomini.modern.gateway.LegacyUnavailableException;
import it.kristikomini.modern.policy.PolicyResponse;

import java.util.List;
import java.util.function.Supplier;

/**
 * The Anti-Corruption Layer client: consumes the legacy SOAP port, protects every call with
 * Resilience4j, and returns clean modern records.
 *
 * <p><b>Decoration order — retry wraps circuit breaker.</b> Each retry attempt passes through the
 * breaker, so the breaker counts individual call failures and can open mid-retry; once it is open,
 * further attempts fail fast with {@link CallNotPermittedException} (which the retry is configured
 * to ignore), and the fallback converts it to {@link LegacyUnavailableException} → a clean 503.
 * The alternative (breaker outside retry) would hide N transient failures as one breaker call.
 *
 * <p>Not annotation-driven on purpose: decorating explicitly with the injected registries makes
 * the mechanism visible and lets it be unit-tested without a Spring context (see the test).
 *
 * <p>The named instances {@code legacyPolicyClient} are configured in {@code application.yml}.
 */
public class ResilientLegacyPolicyClient implements LegacyPolicyClient {

    private final PolicyServiceSoap port;
    private final LegacyPolicyMapper mapper;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public ResilientLegacyPolicyClient(PolicyServiceSoap port,
                                       LegacyPolicyMapper mapper,
                                       CircuitBreakerRegistry circuitBreakerRegistry,
                                       RetryRegistry retryRegistry) {
        this.port = port;
        this.mapper = mapper;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("legacyPolicyClient");
        this.retry = retryRegistry.retry("legacyPolicyClient");
    }

    @Override
    public List<PolicyResponse> listPolicies() {
        List<SoapPolicy> soap = protect(port::listPolicies);
        return soap.stream().map(mapper::toResponse).toList();
    }

    @Override
    public PolicyResponse getPolicy(String policyNumber) {
        return mapper.toResponse(protect(() -> port.getPolicy(policyNumber)));
    }

    /** Runs {@code call} through the breaker, then retry, mapping any failure to a clean 503 signal. */
    private <T> T protect(Supplier<T> call) {
        Supplier<T> decorated = Retry.decorateSupplier(retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, call));
        try {
            return decorated.get();
        } catch (CallNotPermittedException e) {
            throw new LegacyUnavailableException("Legacy policy service circuit is open", e);
        } catch (RuntimeException e) {
            throw new LegacyUnavailableException("Legacy policy service call failed", e);
        }
    }
}
