package it.kristikomini.modern.acl.soap;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

import java.math.BigDecimal;

/**
 * The legacy coverage as seen on the SOAP wire, expressed with the {@code jakarta.xml.bind}
 * namespace on the modern side. This lives inside the ACL package because it is legacy vocabulary
 * (Italian, abbreviated) that must not escape into the modern domain.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Coverage", namespace = "http://legacy.kristikomini.it/policy")
public class SoapCoverage {

    private String codice;
    private String descrizione;
    private BigDecimal importoAssicurato;

    public String getCodice() {
        return codice;
    }

    public void setCodice(String codice) {
        this.codice = codice;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public BigDecimal getImportoAssicurato() {
        return importoAssicurato;
    }

    public void setImportoAssicurato(BigDecimal importoAssicurato) {
        this.importoAssicurato = importoAssicurato;
    }
}
