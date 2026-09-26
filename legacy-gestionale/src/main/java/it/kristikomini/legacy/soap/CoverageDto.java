package it.kristikomini.legacy.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;
import java.math.BigDecimal;

/**
 * The coverage as it appears on the SOAP wire. This is the <i>legacy</i> model — note it is a
 * mutable JavaBean with a no-arg constructor and setters, because that is what JAXB and the old
 * code expect. The modern Anti-Corruption Layer maps this into a clean immutable record.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "Coverage", propOrder = {"codice", "descrizione", "importoAssicurato"})
public class CoverageDto {

    /** Legacy field names are Italian and abbreviated — deliberately kept as inherited. */
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
