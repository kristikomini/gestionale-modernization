package it.kristikomini.modern.acl.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The legacy policy as seen on the SOAP wire. Note {@code dataInizio} is a String (the legacy
 * representation) — the ACL mapper parses it into a {@code LocalDate}. Everything ugly about the
 * legacy model is confined to this package.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Policy", namespace = "http://legacy.kristikomini.it/policy")
public class SoapPolicy {

    private String numeroPolizza;
    private String codiceFiscaleContraente;
    private String dataInizio;
    private BigDecimal premioAnnuo;
    private List<SoapCoverage> coperture = new ArrayList<>();

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

    public List<SoapCoverage> getCoperture() {
        return coperture;
    }

    public void setCoperture(List<SoapCoverage> coperture) {
        this.coperture = coperture;
    }
}
