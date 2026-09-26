package it.kristikomini.modern.policy;

import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The modern REST endpoint — the migrated replacement for the legacy SOAP {@code PolicyService}.
 * When the gateway flips {@code GET /policies} to modern, this handler serves it; the
 * characterisation test guarantees the payload still matches what legacy consumers expect.
 */
@RestController
@RequestMapping("/policies")
public class PolicyController {

    private final PolicyRepository repository;
    private final PolicyMapper mapper;

    public PolicyController(PolicyRepository repository, PolicyMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Lists policies with coverages in a single query (N+1 fixed). */
    @GetMapping
    @Transactional(readOnly = true)
    public List<PolicyResponse> list() {
        return mapper.toResponseList(repository.findAllWithCoverages());
    }

    @GetMapping("/{policyNumber}")
    @Transactional(readOnly = true)
    public ResponseEntity<PolicyResponse> byNumber(@PathVariable String policyNumber) {
        return repository.findByPolicyNumber(policyNumber)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new PolicyNotFoundException(policyNumber));
    }

    @GetMapping(params = "fiscalCode")
    @Transactional(readOnly = true)
    public List<PolicyResponse> byFiscalCode(@RequestParam String fiscalCode) {
        return mapper.toResponseList(repository.findByHolderFiscalCode(fiscalCode));
    }
}
