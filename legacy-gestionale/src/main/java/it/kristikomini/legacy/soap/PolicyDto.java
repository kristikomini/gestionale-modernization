package it.kristikomini.legacy.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The policy as it appears on the SOAP wire.
 *
 * <p>Two "legacy XML model" traits the Anti-Corruption Layer has to normalise:
 * <ul>
 *   <li>{@link #dataInizio} is a <b>String</b>, not a date type — formatted {@code yyyy-MM-dd}
 *       by a shared {@code SimpleDateFormat}. The ACL parses it back into {@code LocalDate}.</li>
 *   <li>Italian, abbreviated field names that do not match the modern domain vocabulary.</li>
 * </ul>
 */
@XmlRootElement(name = "policy")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Policy", propOrder = {"numeroPolizza", "codiceFiscaleContraente", "dataInizio", "premioAnnuo", "coperture"})
public class PolicyDto {

    private String numeroPolizza;
    private String codiceFiscaleContraente;
    /** Date as a formatted string — the legacy representation. */
    private String dataInizio;
    private BigDecimal premioAnnuo;
    private List<CoverageDto> coperture = new ArrayList<CoverageDto>();

    public String getNumeroPolizza() {
        return numeroPolizza;
    }

    public void setNumeroPolizza(String numeroPolizza) {
        this.numeroPolizza = numeroPolizza;
    }

    public String getCodiceFiscaleContraente() {
        return codiceFiscaleContraente;
    }

    public void setCodiceFiscaleContraente(String codiceFiscaleContraente) {
        this.codiceFiscaleContraente = codiceFiscaleContraente;
    }

    public String getDataInizio() {
        return dataInizio;
    }

    public void setDataInizio(String dataInizio) {
        this.dataInizio = dataInizio;
    }

    public BigDecimal getPremioAnnuo() {
        return premioAnnuo;
    }

    public void setPremioAnnuo(BigDecimal premioAnnuo) {
        this.premioAnnuo = premioAnnuo;
    }

    public List<CoverageDto> getCoperture() {
        return coperture;
    }

    public void setCoperture(List<CoverageDto> coperture) {
        this.coperture = coperture;
    }
}
