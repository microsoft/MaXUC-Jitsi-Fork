// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

/**
 * Class containing constants used both in AccessionDesktop and jitsi plugins
 */
public class CommonConstants
{
    /**
     * Value indicating using VOIP for outgoing calls.
     */
    public static final String OUTGOING_CALL_WITH_SIP = "SIP";
    /**
     * Value indicating using click to dial for outgoing calls.
     */
    public static final String OUTGOING_CALL_WITH_CTD = "CTD_CommPortal";
    /**
     * Value indicating ask before making outgoing calls.
     */
    public static final String OUTGOING_CALL_WITH_ASK = "ask";
    /**
     * Value indicating click to dial call on ask.
     */
    public static final String OUTGOING_CTD_WITH_ASK = "null";
    /**
     * Value indicating click to dial call on account phone.
     */
    public static final String OUTGOING_CTD_WITH_ACCOUNT = "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";
    /**
     * Value indicating click to dial call on other phone number.
     */
    public static final String OUTGOING_CTD_WITH_OTHER_NUMBER = "net.java.sip.communicator.impl.protocol.commportal.ctd.myphones";
}
