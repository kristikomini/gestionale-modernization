package it.kristikomini.modern.acl;

import it.kristikomini.modern.acl.soap.SoapCoverage;
import it.kristikomini.modern.acl.soap.SoapPolicy;
import it.kristikomini.modern.policy.CoverageResponse;
import it.kristikomini.modern.policy.PolicyResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Translates the legacy SOAP model into the clean modern domain — the actual "anti-corruption".
 * All the legacy quirks are normalised here and nowhere else:
 * <ul>
 *   <li>the string {@code dataInizio} is parsed into a real {@link LocalDate} with an
 *       immutable, thread-safe {@link DateTimeFormatter} — the fix for the legacy shared
 *       {@code SimpleDateFormat} race;</li>
 *   <li>Italian, abbreviated field names become the modern vocabulary.</li>
 * </ul>
 * Hand-written (not MapStruct) because the date parsing is custom logic worth reading.
 */
@Component
public class LegacyPolicyMapper {

    private static final DateTimeFormatter LEGACY_DATE = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    public PolicyResponse toResponse(SoapPolicy soap) {
        if (soap == null) {
            return null;
        }
        List<CoverageResponse> coverages = soap.getCoperture().stream()
                .map(this::toResponse)
                .toList();
        return new PolicyResponse(
                soap.getNumeroPolizza(),
                soap.getCodiceFiscaleContraente(),
                parseDate(soap.getDataInizio()),
                soap.getPremioAnnuo(),
                coverages);
    }

    private CoverageResponse toResponse(SoapCoverage soap) {
        return new CoverageResponse(soap.getCodice(), soap.getDescrizione(), soap.getImportoAssicurato());
    }

    private LocalDate parseDate(String value) {
        return value == null ? null : LocalDate.parse(value, LEGACY_DATE);
    }
}
