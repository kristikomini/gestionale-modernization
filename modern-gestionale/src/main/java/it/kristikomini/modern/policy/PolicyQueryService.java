package it.kristikomini.modern.policy;

import it.kristikomini.modern.acl.LegacyPolicyClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The strangler-fig route decision, expressed in code: for each read it picks the source —
 * the <b>modern database</b> once the route is migrated, or the <b>legacy system via the ACL</b>
 * while it is not. Flipping {@code strangler.policies.migrated} moves the route with no change to
 * either backend.
 *
 * <p>(The scaffold ARCHITECTURE.md also describes an edge gateway; this application-level router is
 * the same decision realised where it can be unit-tested. An edge gateway is the deployment-time
 * alternative when the two backends are separate processes.)
 */
@Service
public class PolicyQueryService {

    private final PolicyRepository repository;
    private final PolicyMapper mapper;
    private final ObjectProvider<LegacyPolicyClient> legacyClient;
    private final boolean migrated;

    public PolicyQueryService(PolicyRepository repository,
                              PolicyMapper mapper,
                              ObjectProvider<LegacyPolicyClient> legacyClient,
                              @Value("${strangler.policies.migrated:true}") boolean migrated) {
        this.repository = repository;
        this.mapper = mapper;
        this.legacyClient = legacyClient;
        this.migrated = migrated;
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> list() {
        return migrated
                ? mapper.toResponseList(repository.findAllWithCoverages())
                : legacy().listPolicies();
    }

    @Transactional(readOnly = true)
    public PolicyResponse byNumber(String policyNumber) {
        if (migrated) {
            return repository.findByPolicyNumber(policyNumber)
                    .map(mapper::toResponse)
                    .orElseThrow(() -> new PolicyNotFoundException(policyNumber));
        }
        PolicyResponse fromLegacy = legacy().getPolicy(policyNumber);
        if (fromLegacy == null) {
            throw new PolicyNotFoundException(policyNumber);
        }
        return fromLegacy;
    }

    @Transactional(readOnly = true)
    public List<PolicyResponse> byFiscalCode(String fiscalCode) {
        // This query only exists on the modern side; the legacy SOAP contract does not expose it.
        return mapper.toResponseList(repository.findByHolderFiscalCode(fiscalCode));
    }

    /** The ACL bean only exists when legacy.policy-service.enabled=true. */
    private LegacyPolicyClient legacy() {
        return legacyClient.getIfAvailable(() -> {
            throw new IllegalStateException(
                    "strangler.policies.migrated=false but the legacy ACL is disabled "
                            + "(set legacy.policy-service.enabled=true)");
        });
    }
}
