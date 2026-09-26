package it.kristikomini.modern.acl;

import it.kristikomini.modern.policy.PolicyResponse;

import java.util.List;

/**
 * Anti-Corruption Layer boundary (see {@code docs/ANTI-CORRUPTION-LAYER.md}).
 *
 * <p>This interface speaks the <b>modern</b> domain — it returns {@link PolicyResponse} records,
 * not legacy SOAP DTOs. Its implementation is the only place that knows about the legacy WSDL, the
 * {@code javax} types and the string-typed dates; it consumes the legacy SOAP service, wraps every
 * call in Resilience4j (circuit breaker / retry / time limiter), and maps the result into these
 * clean records. On failure it throws
 * {@link it.kristikomini.modern.gateway.LegacyUnavailableException} from its fallback.
 *
 * <p>During the strangler migration, routes not yet owning their data in the modern DB are served
 * through this client; once a route's data is migrated, its ACL path is deleted.
 */
public interface LegacyPolicyClient {

    List<PolicyResponse> listPolicies();

    PolicyResponse getPolicy(String policyNumber);
}
