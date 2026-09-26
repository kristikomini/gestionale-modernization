package it.kristikomini.modern.policy;

/** Thrown when a policy number does not exist — translated to an RFC 7807 404 by the advice. */
public class PolicyNotFoundException extends RuntimeException {

    public PolicyNotFoundException(String policyNumber) {
        super("No policy with number '" + policyNumber + "'");
    }
}
