package it.kristikomini.legacy.soap;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import java.util.List;

/**
 * The SOAP service endpoint interface (SEI). {@code @WebService} on an interface makes it the
 * contract from which the WSDL is generated. This is the JAX-WS ({@code javax.jws}) API — the
 * kind of "SOAP service you inherited" that the modern module wraps behind an Anti-Corruption
 * Layer and re-exposes as clean REST.
 */
@WebService(name = "PolicyService", targetNamespace = "http://legacy.kristikomini.it/policy")
public interface PolicyService {

    @WebMethod
    PolicyDto getPolicy(@WebParam(name = "numeroPolizza") String policyNumber);

    @WebMethod
    List<PolicyDto> listPolicies();

    @WebMethod
    List<PolicyDto> findByFiscalCode(@WebParam(name = "codiceFiscale") String fiscalCode);
}
