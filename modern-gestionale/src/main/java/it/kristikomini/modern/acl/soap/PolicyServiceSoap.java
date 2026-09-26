package it.kristikomini.modern.acl.soap;

import jakarta.jws.WebMethod;
import jakarta.jws.WebParam;
import jakarta.jws.WebService;

import java.util.List;

/**
 * The modern-side ({@code jakarta.jws}) view of the legacy SOAP contract. Its namespace and
 * operation names mirror the legacy {@code PolicyService} so a CXF client proxy built from it is
 * wire-compatible with the deployed legacy endpoint.
 *
 * <p>This interface is also the ACL's transport seam: {@code ResilientLegacyPolicyClient} depends
 * on it, so tests mock it directly and never need a live SOAP server.
 */
@WebService(name = "PolicyService", targetNamespace = "http://legacy.kristikomini.it/policy")
public interface PolicyServiceSoap {

    @WebMethod
    SoapPolicy getPolicy(@WebParam(name = "numeroPolizza") String policyNumber);

    @WebMethod
    List<SoapPolicy> listPolicies();
}
