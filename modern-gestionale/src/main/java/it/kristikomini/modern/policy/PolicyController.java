package it.kristikomini.modern.policy;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The modern REST endpoint — the migrated replacement for the legacy SOAP {@code PolicyService}.
 * It is deliberately thin: it delegates to {@link PolicyQueryService}, which owns the strangler
 * decision of whether each read comes from the modern DB or the legacy system via the ACL. The
 * characterisation test guarantees the payload matches what legacy consumers expect.
 */
@RestController
@RequestMapping("/policies")
public class PolicyController {

    private final PolicyQueryService policies;

    public PolicyController(PolicyQueryService policies) {
        this.policies = policies;
    }

    @GetMapping
    public List<PolicyResponse> list() {
        return policies.list();
    }

    @GetMapping("/{policyNumber}")
    public PolicyResponse byNumber(@PathVariable String policyNumber) {
        return policies.byNumber(policyNumber);
    }

    @GetMapping(params = "fiscalCode")
    public List<PolicyResponse> byFiscalCode(@RequestParam String fiscalCode) {
        return policies.byFiscalCode(fiscalCode);
    }
}
