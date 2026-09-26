package it.kristikomini.legacy;

import it.kristikomini.legacy.soap.CoverageDto;
import it.kristikomini.legacy.soap.PolicyDto;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;
import java.math.BigDecimal;

import static org.junit.Assert.assertTrue;

/**
 * Characterisation test — pins the legacy SOAP <b>wire format</b> as it is today.
 *
 * <p>It does not assert what the XML <i>should</i> look like; it records what it <i>does</i> look
 * like (Italian element names, date as a string). When the modern Anti-Corruption Layer maps this
 * model into the clean domain, this test still passes against the legacy DTOs — proving the
 * migration did not silently change the contract the legacy consumers depend on.
 *
 * <p>No database and no Docker: pure JAXB, so it runs on every {@code mvn test}.
 */
public class PolicyDtoXmlCharacterisationTest {

    @Test
    public void marshalsTheLegacyWireFormat() throws Exception {
        PolicyDto dto = new PolicyDto();
        dto.setNumeroPolizza("POL-2024-0001");
        dto.setCodiceFiscaleContraente("RSSMRA80A01H501U");
        dto.setDataInizio("2024-01-15"); // date as a string — the legacy representation
        dto.setPremioAnnuo(new BigDecimal("450.00"));
        CoverageDto cov = new CoverageDto();
        cov.setCodice("RCA");
        cov.setDescrizione("Responsabilita civile auto");
        cov.setImportoAssicurato(new BigDecimal("6000000.00"));
        dto.getCoperture().add(cov);

        JAXBContext ctx = JAXBContext.newInstance(PolicyDto.class);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        StringWriter sw = new StringWriter();
        m.marshal(dto, sw);
        String xml = sw.toString();

        // The pinned contract: element names and the string-typed date.
        assertTrue("numeroPolizza element present", xml.contains("<numeroPolizza>POL-2024-0001</numeroPolizza>"));
        assertTrue("codiceFiscaleContraente present", xml.contains("<codiceFiscaleContraente>RSSMRA80A01H501U</codiceFiscaleContraente>"));
        assertTrue("dataInizio is a string yyyy-MM-dd", xml.contains("<dataInizio>2024-01-15</dataInizio>"));
        assertTrue("premioAnnuo present", xml.contains("<premioAnnuo>450.00</premioAnnuo>"));
        assertTrue("nested coperture/codice present", xml.contains("<codice>RCA</codice>"));
    }
}
