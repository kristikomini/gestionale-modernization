package it.kristikomini.modern.policy;

import it.kristikomini.modern.gateway.ApiExceptionHandler;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice test (no database, no Docker) — runs on every {@code mvn test}. Verifies the
 * REST contract and that a missing policy yields an RFC 7807 {@code ProblemDetail} 404, not a
 * stack trace. Uses the real MapStruct mapper so the entity→record mapping is exercised too.
 */
@WebMvcTest(PolicyController.class)
@Import({ApiExceptionHandler.class, PolicyControllerTest.MapperConfig.class})
class PolicyControllerTest {

    @TestConfiguration
    static class MapperConfig {
        @Bean
        PolicyMapper policyMapper() {
            return Mappers.getMapper(PolicyMapper.class);
        }
    }

    @Autowired
    MockMvc mvc;

    @MockBean
    PolicyRepository repository;

    private PolicyEntity samplePolicy() {
        PolicyEntity p = new PolicyEntity("POL-2024-0001", "RSSMRA80A01H501U",
                LocalDate.of(2024, 1, 15), new BigDecimal("450.00"));
        p.addCoverage(new CoverageEntity("RCA", "Responsabilita civile auto", new BigDecimal("6000000.00")));
        return p;
    }

    @Test
    void listsPoliciesAsJson() throws Exception {
        when(repository.findAllWithCoverages()).thenReturn(List.of(samplePolicy()));

        mvc.perform(get("/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].policyNumber").value("POL-2024-0001"))
                .andExpect(jsonPath("$[0].startDate").value("2024-01-15"))
                .andExpect(jsonPath("$[0].coverages[0].code").value("RCA"));
    }

    @Test
    void unknownPolicyReturnsRfc7807ProblemDetail() throws Exception {
        when(repository.findByPolicyNumber("NOPE")).thenReturn(Optional.empty());

        mvc.perform(get("/policies/NOPE"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.title").value("Policy not found"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
