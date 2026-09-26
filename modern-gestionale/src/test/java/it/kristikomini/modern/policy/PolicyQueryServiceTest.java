package it.kristikomini.modern.policy;

import it.kristikomini.modern.acl.LegacyPolicyClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Proves the strangler route decision: when a route is migrated the modern DB is used and the
 * legacy ACL is never touched; when it is not, the read is served through the ACL and the DB is
 * never touched. This is the behaviour a route flip changes.
 */
class PolicyQueryServiceTest {

    private final PolicyRepository repository = mock(PolicyRepository.class);
    private final PolicyMapper mapper = mock(PolicyMapper.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<LegacyPolicyClient> legacyProvider = mock(ObjectProvider.class);
    private final LegacyPolicyClient legacyClient = mock(LegacyPolicyClient.class);

    private PolicyResponse sample() {
        return new PolicyResponse("POL-1", "CF1", LocalDate.now(), new BigDecimal("1.00"), List.of());
    }

    @Test
    void migratedRouteReadsModernDbAndNeverTouchesLegacy() {
        var service = new PolicyQueryService(repository, mapper, legacyProvider, true);
        when(repository.findAllWithCoverages()).thenReturn(List.of());
        when(mapper.toResponseList(any())).thenReturn(List.of(sample()));

        List<PolicyResponse> result = service.list();

        assertThat(result).hasSize(1);
        verify(repository).findAllWithCoverages();
        verifyNoInteractions(legacyProvider, legacyClient);
    }

    @Test
    void notYetMigratedRouteReadsThroughLegacyAclAndNeverTouchesDb() {
        var service = new PolicyQueryService(repository, mapper, legacyProvider, false);
        when(legacyProvider.getIfAvailable(any())).thenReturn(legacyClient);
        when(legacyClient.listPolicies()).thenReturn(List.of(sample(), sample()));

        List<PolicyResponse> result = service.list();

        assertThat(result).hasSize(2);
        verify(legacyClient).listPolicies();
        verify(repository, never()).findAllWithCoverages();
    }

    @Test
    void failsClearlyWhenNotMigratedButAclDisabled() {
        var service = new PolicyQueryService(repository, mapper, legacyProvider, false);
        // Simulate ObjectProvider with no bean: it invokes the default supplier, which throws.
        when(legacyProvider.getIfAvailable(any())).thenAnswer(inv -> {
            Supplier<LegacyPolicyClient> defaultSupplier = inv.getArgument(0);
            return defaultSupplier.get();
        });

        try {
            service.list();
            assertThat(false).as("expected IllegalStateException").isTrue();
        } catch (IllegalStateException expected) {
            assertThat(expected).hasMessageContaining("legacy.policy-service.enabled=true");
        }
    }
}
