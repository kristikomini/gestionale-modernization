package it.kristikomini.legacy.soap;

import it.kristikomini.legacy.policy.Coverage;
import it.kristikomini.legacy.policy.Policy;
import it.kristikomini.legacy.policy.PolicyRepository;

import javax.jws.WebService;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * SOAP endpoint implementation.
 *
 * <p><b>Intentional legacy bug — a shared {@link SimpleDateFormat}.</b> {@code SimpleDateFormat}
 * is <i>not thread-safe</i>, and a JAX-WS endpoint is a shared singleton called concurrently by
 * many clients. Two threads formatting a date through {@link #DATE_FORMAT} at the same time can
 * interleave its internal calendar state and produce a corrupted or wrong string. It "works on my
 * machine" because it only fails under concurrency — which is exactly why the characterisation
 * test drives it from many threads before the modern side migrates to {@code java.time}
 * (immutable, thread-safe). Do not "fix" this here; it is the documented starting point.
 */
@WebService(
        endpointInterface = "it.kristikomini.legacy.soap.PolicyService",
        targetNamespace = "http://legacy.kristikomini.it/policy",
        serviceName = "PolicyService")
public class PolicyServiceImpl implements PolicyService {

    /** Shared, mutable, not thread-safe. The bug. */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private final EntityManagerFactory emf;

    public PolicyServiceImpl(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public PolicyDto getPolicy(String policyNumber) {
        EntityManager em = emf.createEntityManager();
        try {
            PolicyRepository repo = new PolicyRepository(em);
            Policy policy = repo.findByPolicyNumber(policyNumber);
            return policy == null ? null : toDto(policy);
        } finally {
            em.close();
        }
    }

    @Override
    public List<PolicyDto> listPolicies() {
        EntityManager em = emf.createEntityManager();
        try {
            PolicyRepository repo = new PolicyRepository(em);
            List<Policy> policies = repo.findAll();
            List<PolicyDto> dtos = new ArrayList<PolicyDto>();
            for (Policy p : policies) {
                // Mapping reads p.getCoverages() → triggers the N+1 select per policy.
                dtos.add(toDto(p));
            }
            return dtos;
        } finally {
            em.close();
        }
    }

    @Override
    public List<PolicyDto> findByFiscalCode(String fiscalCode) {
        EntityManager em = emf.createEntityManager();
        try {
            PolicyRepository repo = new PolicyRepository(em);
            List<PolicyDto> dtos = new ArrayList<PolicyDto>();
            for (Policy p : repo.findByHolderFiscalCode(fiscalCode)) {
                dtos.add(toDto(p));
            }
            return dtos;
        } finally {
            em.close();
        }
    }

    private PolicyDto toDto(Policy p) {
        PolicyDto dto = new PolicyDto();
        dto.setNumeroPolizza(p.getPolicyNumber());
        dto.setCodiceFiscaleContraente(p.getHolderFiscalCode());
        // Shared, non-thread-safe formatter used from a shared endpoint — the concurrency bug.
        dto.setDataInizio(DATE_FORMAT.format(p.getStartDate()));
        dto.setPremioAnnuo(p.getAnnualPremium());
        List<CoverageDto> coperture = new ArrayList<CoverageDto>();
        for (Coverage c : p.getCoverages()) { // lazy load → N+1
            CoverageDto cd = new CoverageDto();
            cd.setCodice(c.getCode());
            cd.setDescrizione(c.getDescription());
            cd.setImportoAssicurato(c.getInsuredAmount());
            coperture.add(cd);
        }
        dto.setCoperture(coperture);
        return dto;
    }
}
