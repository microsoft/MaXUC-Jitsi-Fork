/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip.xcap;

import net.java.sip.communicator.impl.protocol.sip.xcap.model.commonpolicy.*;

/**
 * XCAP pres-rules client interface.
 * <p/>
 * Compliant with rfc4745, rfc5025
 *
 * @author Grigorii Balutsel
 */
public interface PresRulesClient
{
    /**
     * Pres-rules uri format
     */
    String DOCUMENT_FORMAT = "pres-rules/users/%2s/presrules";

    /**
     * Pres-rules content type
     */
    String CONTENT_TYPE = "application/auth-policy+xml";

    /**
     * Pres-rules namespace
     */
    String NAMESPACE = "urn:ietf:params:xml:ns:pres-rules";

    /**
     * Puts the pres-rules to the server.
     *
     * @param presRules the pres-rules to be saved on the server.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    void putPresRules(RulesetType presRules)
            throws XCapException;

    /**
     * Gets the pres-rules from the server.
     *
     * @return the pres-rules.
     * @throws IllegalStateException if the user has not been connected.
     * @throws XCapException         if there is some error during operation.
     */
    RulesetType getPresRules()
            throws XCapException;
}
