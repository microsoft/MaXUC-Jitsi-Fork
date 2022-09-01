/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.sipconstants;

import net.java.sip.communicator.util.*;

/**
 * The <tt>SipPresenceTypeEnum</tt> gives access to presence methods for the
 * SIP protocol e.g. SIMPLE, dialog
 */
public enum SipPresenceTypeEnum
{
    /**
     * the SIP SIMPLE presence type
     */
    SIMPLE,
    /**
     * the SIP dialog presence type
     */
    DIALOG;

    /**
     * The <tt>Logger</tt> used by the <tt>SipPresenecTypeEnum</tt> class and
     * its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(SipPresenceTypeEnum.class);

    /**
     * Returns the enum value matching a string name.  This is
     * case-insensitive, and returns SIMPLE if there is no mapping for the
     * name.
     *
     * @param name the name of the enum value
     * @return the enum value
     */
    public static SipPresenceTypeEnum getEnum(String name)
    {
        SipPresenceTypeEnum value;

        if (name == null)
        {
            logger.warn("Account presence type is null");
            value = SIMPLE;
        }
        else
        {
            try
            {
                value = valueOf(name.toUpperCase());
            }
            catch (IllegalArgumentException e)
            {
                // If the stored value is invalid, log and use SIP SIMPLE.
                logger.warn(
                    "Invalid value stored for account presence type: " + name);
                value = SIMPLE;
            }
        }

        return value;
    }
}
