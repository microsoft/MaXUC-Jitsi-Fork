/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol.media;

import static org.jitsi.util.Hasher.logHasher;

import java.net.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;

import net.java.sip.communicator.service.netaddr.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * <tt>TransportManager</tt>s are responsible for allocating ports, gathering
 * local candidates and managing ICE whenever we are using it.
 *
 * @param <U> the peer extension class like for example <tt>CallPeerSipImpl</tt>
 * or <tt>CallPeerJabberImpl</tt>
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Sebastien Vincent
 */
public abstract class TransportManager<U extends MediaAwareCallPeer<?, ?, ?>>
{
    /**
     * The <tt>Logger</tt> used by the <tt>TransportManager</tt>
     * class and its instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(TransportManager.class);

    /**
     * The port tracker that we should use when binding generic media streams.
     * <p>
     * Initialized by {@link #initializePortNumbers()}.
     * </p>
     */
    private static PortTracker defaultPortTracker = new PortTracker(5000, 6000);

    /**
     * The port tracker that we should use when binding video media streams.
     * <p>
     * Potentially initialized by {@link #initializePortNumbers()} if the
     * necessary properties are set.
     * </p>
     */
    private static PortTracker videoPortTracker = null;

    /**
     * The port tracker that we should use when binding data media streams.
     * <p>
     * Potentially initialized by {@link #initializePortNumbers()} if the
     * necessary properties are set
     * </p>
     */
    private static PortTracker audioPortTracker = null;

    /**
     * RTP audio DSCP configuration property name.
     */
    private static final String RTP_AUDIO_DSCP_PROPERTY =
        "net.java.sip.communicator.impl.protocol.RTP_AUDIO_DSCP";

    /**
     * RTP video DSCP configuration property name.
     */
    private static final String RTP_VIDEO_DSCP_PROPERTY =
        "net.java.sip.communicator.impl.protocol.RTP_VIDEO_DSCP";

    /**
     * The {@link MediaAwareCallPeer} whose traffic we will be taking care of.
     */
    private U callPeer;

    /**
     * The RTP/RTCP socket couples that this <tt>TransportManager</tt> uses to
     * send and receive media flows through indexed by <tt>MediaType</tt>
     * (ordinal).
     */
    private final StreamConnector[] streamConnectors
        = new StreamConnector[MediaType.values().length];

    /**
     * Creates a new instance of this transport manager, binding it to the
     * specified peer.
     *
     * @param callPeer the {@link MediaAwareCallPeer} whose traffic we will be
     * taking care of.
     */
    protected TransportManager(U callPeer)
    {
        this.callPeer = callPeer;
    }

    /**
     * Returns the <tt>StreamConnector</tt> instance that this media handler
     * should use for streams of the specified <tt>mediaType</tt>. The method
     * would also create a new <tt>StreamConnector</tt> if no connector has
     * been initialized for this <tt>mediaType</tt> yet or in case one
     * of its underlying sockets has been closed.
     *
     * @param mediaType the <tt>MediaType</tt> that we'd like to create a
     * connector for.
     * @return this media handler's <tt>StreamConnector</tt> for the specified
     * <tt>mediaType</tt>.
     *
     * @throws OperationFailedException in case we failed to initialize our
     * connector.
     */
    public StreamConnector getStreamConnector(MediaType mediaType)
        throws OperationFailedException
    {
        int streamConnectorIndex = mediaType.ordinal();
        StreamConnector streamConnector
            = streamConnectors[streamConnectorIndex];

        if((streamConnector == null)
            || (streamConnector.getProtocol() == StreamConnector.Protocol.UDP))
        {
            DatagramSocket controlSocket;

            if((streamConnector == null)
                || streamConnector.getDataSocket().isClosed()
                || (((controlSocket = streamConnector.getControlSocket())
                        != null)
                    && controlSocket.isClosed()))
            {
                streamConnectors[streamConnectorIndex]
                    = streamConnector
                    = createStreamConnector(mediaType);
            }
        }
        else if(streamConnector.getProtocol() == StreamConnector.Protocol.TCP)
        {
            Socket controlTCPSocket;

            if(streamConnector.getDataTCPSocket().isClosed()
                || (((controlTCPSocket = streamConnector.getControlTCPSocket())
                        != null)
                    && controlTCPSocket.isClosed()))
            {
                streamConnectors[streamConnectorIndex]
                    = streamConnector
                    = createStreamConnector(mediaType);
            }
        }
        return streamConnector;
    }

    /**
     * Closes the existing <tt>StreamConnector</tt>, if any, associated with a
     * specific <tt>MediaType</tt> and removes its reference from this
     * <tt>TransportManager</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> associated with the
     * <tt>StreamConnector</tt> to close
     */
    public void closeStreamConnector(MediaType mediaType)
    {
        int index = mediaType.ordinal();
        StreamConnector streamConnector = streamConnectors[index];

        if (streamConnector != null)
        {
            closeStreamConnector(mediaType, streamConnector);
            streamConnectors[index] = null;
        }
    }

    /**
     * Closes a specific <tt>StreamConnector</tt> associated with a specific
     * <tt>MediaType</tt>. If this <tt>TransportManager</tt> has a reference to
     * the specified <tt>streamConnector</tt>, it remains. Allows extenders to
     * override and perform additional customizations to the closing of the
     * specified <tt>streamConnector</tt>.
     *
     * @param mediaType the <tt>MediaType</tt> associated with the specified
     * <tt>streamConnector</tt>
     * @param streamConnector the <tt>StreamConnector</tt> to be closed
     * @see #closeStreamConnector(MediaType)
     */
    protected void closeStreamConnector(
            MediaType mediaType,
            StreamConnector streamConnector)
    {
        /*
         * XXX The connected owns the sockets so it is important that it
         * decides whether to close them i.e. this TransportManager is not
         * allowed to explicitly close the sockets by itself.
         */
        streamConnector.close();
    }

    /**
     * Creates a media <tt>StreamConnector</tt>. The method takes into account
     * the minimum and maximum media port boundaries.
     *
     * @param mediaType the <tt>MediaType</tt> of the stream for which a new
     * <tt>StreamConnector</tt> is to be created
     * @return a new <tt>StreamConnector</tt>.
     *
     * @throws OperationFailedException if the binding of the sockets fails.
     */
    protected StreamConnector createStreamConnector(MediaType mediaType)
        throws OperationFailedException
    {
        NetworkAddressManagerService nam
            = ProtocolMediaActivator.getNetworkAddressManagerService();
        InetAddress intendedDestination = getIntendedDestination(getCallPeer());
        InetAddress localHostForPeer = nam.getLocalHost(intendedDestination);

        //make sure our port numbers reflect the configuration service settings
        initializePortNumbers();

        PortTracker portTracker = getPortTracker(mediaType);

        //create the RTP socket.
        DatagramSocket rtpSocket = null;
        try
        {
            rtpSocket = nam.createDatagramSocket(
                localHostForPeer, portTracker.getPort(),
                portTracker.getMinPort(), portTracker.getMaxPort());
        }
        catch (Exception exc)
        {
            throw new OperationFailedException(
                "Failed to allocate the network ports necessary for the call.",
                OperationFailedException.INTERNAL_ERROR, exc);
        }

        // Make sure that next time we don't try to bind on occupied ports.
        // Use the next port number up, unless that's out of the configured port
        // range; in which case we wrap back to the minimum configured port.
        portTracker.setNextPort(rtpSocket.getLocalPort() + 1);

        //create the RTCP socket, preferably on the port following our RTP one.
        DatagramSocket rtcpSocket = null;
        try
        {
            rtcpSocket = nam.createDatagramSocket(
                localHostForPeer, portTracker.getPort(),
                portTracker.getMinPort(), portTracker.getMaxPort());
        }
        catch (Exception exc)
        {
           throw new OperationFailedException(
                "Failed to allocate the network ports necessary for the call.",
                OperationFailedException.INTERNAL_ERROR,
                exc);
        }

        // Make sure that next time we don't try to bind on occupied ports.
        // Use the next port number up, unless that's out of the configured port
        // range; in which case we wrap back to the minimum configured port.
        portTracker.setNextPort(rtcpSocket.getLocalPort() + 1);

        return new DefaultStreamConnector(rtpSocket, rtcpSocket);
    }

    /**
     * (Re)Sets the all the port allocators to reflect current values specified
     * in the <tt>ConfigurationService</tt>. Calling this method may very well
     * result in creating new port allocators or destroying exising ones.
     */
    protected static void initializePortNumbers()
    {
        //try the default tracker first
        ConfigurationService configuration
            = ProtocolMediaActivator.getConfigurationService();
        String minPortNumberStr = configuration.global().getString(
                OperationSetBasicTelephony.MIN_MEDIA_PORT_NUMBER_PROPERTY_NAME);

        String maxPortNumberStr = configuration.global().getString(
                OperationSetBasicTelephony.MAX_MEDIA_PORT_NUMBER_PROPERTY_NAME);

        //try to send the specified range. If there's no specified range in
        //configuration, we'll just leave the tracker as it is: [5000 to 6000]
        defaultPortTracker.tryRange(minPortNumberStr, maxPortNumberStr);

        //try the VIDEO tracker
        minPortNumberStr = configuration.global().getString(
            OperationSetBasicTelephony.MIN_VIDEO_PORT_NUMBER_PROPERTY_NAME);

        maxPortNumberStr = configuration.global().getString(
            OperationSetBasicTelephony.MAX_VIDEO_PORT_NUMBER_PROPERTY_NAME);

        //try to send the specified range. If there's no specified range in
        //configuration, we'll just leave this tracker to null
        videoPortTracker
            = PortTracker.createTracker(minPortNumberStr, maxPortNumberStr);

        //try the AUDIO tracker
        minPortNumberStr = configuration.global().getString(
            OperationSetBasicTelephony.MIN_AUDIO_PORT_NUMBER_PROPERTY_NAME);

        maxPortNumberStr = configuration.global().getString(
            OperationSetBasicTelephony.MAX_AUDIO_PORT_NUMBER_PROPERTY_NAME);

        //try to send the specified range. If there's no specified range in
        //configuration, we'll just leave this tracker to null
        audioPortTracker
            = PortTracker.createTracker(minPortNumberStr, maxPortNumberStr);
    }

    /**
     * Returns the <tt>InetAddress</tt> that we are using in one of our
     * <tt>StreamConnector</tt>s or, in case we don't have any connectors yet
     * the address returned by the our network address manager as the best local
     * address to use when contacting the <tt>CallPeer</tt> associated with this
     * <tt>MediaHandler</tt>. This method is primarily meant for use with the
     * o= and c= fields of a newly created session description. The point is
     * that we create our <tt>StreamConnector</tt>s when constructing the media
     * descriptions so we already have a specific local address assigned to them
     * at the time we get ready to create the c= and o= fields. It is therefore
     * better to try and return one of these addresses before trying the net
     * address manager again and running the slight risk of getting a different
     * address.
     *
     * @return an <tt>InetAddress</tt> that we use in one of the
     * <tt>StreamConnector</tt>s in this class.
     */
    public InetAddress getLastUsedLocalHost()
    {
        InetAddress localAddr;
        for (MediaType mediaType : MediaType.values())
        {
            StreamConnector streamConnector
                = streamConnectors[mediaType.ordinal()];

            if (streamConnector != null)
            {
                localAddr = streamConnector.getDataSocket().getLocalAddress();
                logger.debug("Found existing local address: " + logHasher(localAddr));
                if (!localAddr.isAnyLocalAddress())
                {
                    return localAddr;
                }

                logger.debug("Existing address was the ANY address - ask NAM");
            }
        }

        NetworkAddressManagerService nam
            = ProtocolMediaActivator.getNetworkAddressManagerService();
        InetAddress intendedDestination = getIntendedDestination(getCallPeer());
        localAddr = nam.getLocalHost(intendedDestination);

        logger.debug("Returning local address: " + logHasher(localAddr));
        return localAddr;
    }

    /**
     * Set traffic class (QoS) for the RTP socket.
     *
     * @param target <tt>MediaStreamTarget</tt>
     * @param type the {@link MediaType} of the connector we'd like to set
     * traffic class
     */
    protected void setTrafficClass(MediaStreamTarget target, MediaType type)
    {
        // get traffic class value for RTP audio/video
        int trafficClass = getDSCP(type);

        if(trafficClass <= 0)
        {
            logger.debug("Traffic class is " + trafficClass + "; skipping.");
            return;
        }

        logger.info("Set traffic class for " + type + " to " + trafficClass);
        try
        {
            StreamConnector connector = getStreamConnector(type);

            logger.debug("Waiting for monitor on connector " + connector +
                                              " before setting traffic class.");
            synchronized(connector)
            {
                logger.debug("Got monitor lock; setting traffic class.");
                if(connector.getProtocol() == StreamConnector.Protocol.TCP)
                {
                    connector.getDataTCPSocket().setTrafficClass(trafficClass);
                    logger.trace("TCP data traffic class set.");

                    Socket controlTCPSocket = connector.getControlTCPSocket();

                    if (controlTCPSocket != null)
                    {
                        controlTCPSocket.setTrafficClass(trafficClass);
                        logger.trace("TCP control traffic class set.");
                    }
                    else
                    {
                        logger.trace("No control socket configured.");
                    }
                }
                else
                {
                    /* data port (RTP) */
                    connector.getDataSocket().setTrafficClass(trafficClass);
                    logger.trace("UDP data traffic class set.");

                    /* control port (RTCP) */
                    DatagramSocket controlSocket = connector.getControlSocket();

                    if (controlSocket != null)
                    {
                        controlSocket.setTrafficClass(trafficClass);
                        logger.trace("UDP control traffic class set.");
                    }
                    else
                    {
                        logger.trace("No control socket configured.");
                    }
                }
            }
        }
        catch(Exception e)
        {
            logger.error(
                "Failed to set traffic class for " + type + " to "
                    + trafficClass,
                e);
        }
    }

    /**
     * Gets the SIP traffic class associated with a specific <tt>MediaType</tt>
     * from the configuration.
     *
     * @param type the <tt>MediaType</tt> to get the associated SIP traffic
     * class of
     * @return the SIP traffic class associated with the specified
     * <tt>MediaType</tt> or <tt>0</tt> if not configured
     */
    private int getDSCP(MediaType type)
    {
        String dscpPropertyName;

        switch (type)
        {
            case AUDIO:
                dscpPropertyName = RTP_AUDIO_DSCP_PROPERTY;
                break;
            case VIDEO:
                dscpPropertyName = RTP_VIDEO_DSCP_PROPERTY;
                break;
            default:
                return 0;
        }

        // The phone profile divides the configured value by 4 before sending to the client.
        // That's possibly related to what's a standard value to use for DSCP headers, but all
        // even values should be allowed, so just attempt to use whatever is configured.
        String dscp = (String) ProtocolMediaActivator.getConfigurationService().user().getProperty(dscpPropertyName);

        if (dscp != null && !dscp.equals(""))
        {
            return (int) (Double.parseDouble(dscp) * 4);
        }

        return 0;
    }

    /**
     * Returns the <tt>InetAddress</tt> that is most likely to be used as a
     * next hop when contacting the specified <tt>destination</tt>. This is
     * an utility method that is used whenever we have to choose one of our
     * local addresses to put in the Via, Contact or (in the case of no
     * registrar accounts) From headers.
     *
     * @param peer the CallPeer that we would contact.
     *
     * @return the <tt>InetAddress</tt> that is most likely to be to be used
     * as a next hop when contacting the specified <tt>destination</tt>.
     *
     * @throws IllegalArgumentException if <tt>destination</tt> is not a valid
     * host/ip/fqdn
     */
    protected abstract InetAddress getIntendedDestination(U peer);

    /**
     * Returns the {@link MediaAwareCallPeer} that this transport manager is
     * serving.
     *
     * @return the {@link MediaAwareCallPeer} that this transport manager is
     * serving.
     */
    public U getCallPeer()
    {
        return callPeer;
    }

    /**
     * Returns the port tracker that we are supposed to use when binding ports
     * for the specified {@link MediaType}.
     *
     * @param mediaType the media type that we want to obtain a locator for.
     *
     * @return the port tracker that we are supposed to use when binding ports
     * for the specified {@link MediaType}.
     */
    protected static PortTracker getPortTracker(MediaType mediaType)
    {
        if(MediaType.AUDIO == mediaType && audioPortTracker != null)
            return audioPortTracker;

        if(MediaType.VIDEO == mediaType && videoPortTracker != null)
            return videoPortTracker;

        return defaultPortTracker;
    }

    /**
     * Returns the extended type of the candidate selected if this transport
     * manager is using ICE.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return The extended type of the candidate selected if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public abstract String getICECandidateExtendedType(String streamName);

    /**
     * Returns the current state of ICE processing.
     *
     * @return the current state of ICE processing if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public abstract String getICEState();

    /**
     * Returns the ICE local host address.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local host address if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public abstract InetSocketAddress getICELocalHostAddress(String streamName);

    /**
     * Returns the ICE remote host address.
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote host address if this transport
     * manager is using ICE. Otherwise, returns null.
     */
    public abstract InetSocketAddress getICERemoteHostAddress(
            String streamName);

    /**
     * Returns the ICE local reflexive address (server or peer reflexive).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local reflexive address. May be null if this transport
     * manager is not using ICE or if there is no reflexive address for the
     * local candidate used.
     */
    public abstract InetSocketAddress getICELocalReflexiveAddress(
            String streamName);

    /**
     * Returns the ICE remote reflexive address (server or peer reflexive).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote reflexive address. May be null if this transport
     * manager is not using ICE or if there is no reflexive address for the
     * remote candidate used.
     */
    public abstract InetSocketAddress getICERemoteReflexiveAddress(
            String streamName);

    /**
     * Returns the ICE local relayed address (server or peer relayed).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE local relayed address. May be null if this transport
     * manager is not using ICE or if there is no relayed address for the
     * local candidate used.
     */
    public abstract InetSocketAddress getICELocalRelayedAddress(
            String streamName);

    /**
     * Returns the ICE remote relayed address (server or peer relayed).
     *
     * @param streamName The stream name (AUDIO, VIDEO);
     *
     * @return the ICE remote relayed address. May be null if this transport
     * manager is not using ICE or if there is no relayed address for the
     * remote candidate used.
     */
    public abstract InetSocketAddress getICERemoteRelayedAddress(
            String streamName);

    /**
     * Returns the total harvesting time (in ms) for all harvesters.
     *
     * @return The total harvesting time (in ms) for all the harvesters. 0 if
     * the ICE agent is null, or if the agent has nevers harvested.
     */
    public abstract long getTotalHarvestingTime();

    /**
     * Returns the harvesting time (in ms) for the harvester given in parameter.
     *
     * @param harvesterName The class name if the harvester.
     *
     * @return The harvesting time (in ms) for the harvester given in parameter.
     * 0 if this harvester does not exists, if the ICE agent is null, or if the
     * agent has never harvested with this harvester.
     */
    public abstract long getHarvestingTime(String harvesterName);

    /**
     * Returns the number of harvesting for this agent.
     *
     * @return The number of harvesting for this agent.
     */
    public abstract int getNbHarvesting();

    /**
     * Returns the number of harvesting time for the harvester given in
     * parameter.
     *
     * @param harvesterName The class name if the harvester.
     *
     * @return The number of harvesting time for the harvester given in
     * parameter.
     */
    public abstract int getNbHarvesting(String harvesterName);

}
