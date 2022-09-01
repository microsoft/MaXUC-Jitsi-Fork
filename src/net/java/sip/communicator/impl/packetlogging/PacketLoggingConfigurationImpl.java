/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.packetlogging;

import org.jitsi.service.configuration.*;
import org.jitsi.service.packetlogging.*;

/**
 * Extends PacketLoggingConfiguration by storing and loading values from
 * configuration service.
 *
 * @author Damian Minkov
 */
public class PacketLoggingConfigurationImpl
    extends PacketLoggingConfiguration
{
    /**
     * Creates new PacketLoggingConfiguration and load values from
     * configuration service and if missing uses already defined
     * default values.
     */
    PacketLoggingConfigurationImpl()
    {
        // load values from config service
        ConfigurationService configService =
                PacketLoggingActivator.getConfigurationService();

        super.setGlobalLoggingEnabled(
            configService.global().getBoolean(
                PACKET_LOGGING_ENABLED_PROPERTY_NAME,
                isGlobalLoggingEnabled()));
        super.setSipLoggingEnabled(
            configService.global().getBoolean(
                PACKET_LOGGING_SIP_ENABLED_PROPERTY_NAME,
                isSipLoggingEnabled()));
        super.setJabberLoggingEnabled(
            configService.global().getBoolean(
                PACKET_LOGGING_JABBER_ENABLED_PROPERTY_NAME,
                isJabberLoggingEnabled()));
        super.setRTPLoggingEnabled(
            configService.global().getBoolean(
                PACKET_LOGGING_RTP_ENABLED_PROPERTY_NAME,
                isRTPLoggingEnabled()));
        super.setLimit(
            configService.global().getLong(
                PACKET_LOGGING_FILE_SIZE_PROPERTY_NAME,
                getLimit()));
        super.setLogfileCount(
            configService.global().getInt(
                PACKET_LOGGING_FILE_COUNT_PROPERTY_NAME,
                getLogfileCount()));
    }

    /**
     * Change whether packet logging is enabled and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public void setGlobalLoggingEnabled(boolean enabled)
    {
        super.setGlobalLoggingEnabled(enabled);

        PacketLoggingActivator.getConfigurationService().global().setProperty(
            PACKET_LOGGING_ENABLED_PROPERTY_NAME, enabled);
    }

    /**
     * Change whether packet logging for sip protocol is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public void setSipLoggingEnabled(boolean enabled)
    {
        super.setSipLoggingEnabled(enabled);

        PacketLoggingActivator.getConfigurationService().global().setProperty(
            PACKET_LOGGING_SIP_ENABLED_PROPERTY_NAME,
            enabled);
    }

    /**
     * Change whether packet logging for jabber protocol is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public void setJabberLoggingEnabled(boolean enabled)
    {
        super.setJabberLoggingEnabled(enabled);

        PacketLoggingActivator.getConfigurationService().global().setProperty(
            PACKET_LOGGING_JABBER_ENABLED_PROPERTY_NAME,
            enabled);
    }

    /**
     * Change whether packet logging for RTP is enabled
     * and save it in configuration.
     * @param enabled <tt>true</tt> if we enable it.
     */
    public void setRTPLoggingEnabled(boolean enabled)
    {
        super.setRTPLoggingEnabled(enabled);

        PacketLoggingActivator.getConfigurationService().global().setProperty(
            PACKET_LOGGING_RTP_ENABLED_PROPERTY_NAME,
            enabled);
    }

    /**
     * Changes the file size limit.
     * @param limit the new limit size.
     */
    public void setLimit(long limit)
    {
        super.setLimit(limit);

        PacketLoggingActivator.getConfigurationService().global().setProperty(
                PACKET_LOGGING_FILE_SIZE_PROPERTY_NAME,
                limit);
    }

    /**
     * Changes file count.
     * @param logfileCount the new file count.
     */
    public void setLogfileCount(int logfileCount)
    {
        super.setLogfileCount(logfileCount);

        PacketLoggingActivator.getConfigurationService().global().setProperty(
                PACKET_LOGGING_FILE_COUNT_PROPERTY_NAME,
                logfileCount);
    }
}
