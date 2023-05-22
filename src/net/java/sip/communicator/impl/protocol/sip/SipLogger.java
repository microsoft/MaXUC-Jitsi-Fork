/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import javax.sip.ListeningPoint;
import javax.sip.SipStack;

import org.jitsi.service.packetlogging.PacketLoggingService;

import gov.nist.core.ServerLogger;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.SipStackImpl;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;
import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.util.Logger;

/**
 * This is a custom StackLogger configured as the gov.nist.javax.sip.STACK_LOGGER for JSIP to use.
 * It writes JSIP log messages to the application log and the SIP CSV.
 *
 * @author Sebastien Mazy
 */
public class SipLogger
    implements StackLogger,
               ServerLogger
{
    /**
     * Logger used to log out JSIP trace - excluding SIP messages, which are
     * logged by the csvLogger.
     */
    private static final Logger logger
        = Logger.getLogger(SipLogger.class);

    private static final boolean LOG_KEEPALIVES_PACKETS = true;

    /**
     * Log SIP messages out to a CSV file.
     */
    private static final Logger csvLogger = Logger.getLogger("jitsi.csvSipLogger");

    /**
     * The format of the human-readable timestamps that are written to the CSV
     * file. It is not locale-dependent so can just use the US locale.
     */
    private static final SimpleDateFormat csvFileTimestampFormat =
            new SimpleDateFormat("yyyy_MM_dd HH:mm:ss.SSS", Locale.US);

    /**
     * SipStack to use.
     */
    private SipStack sipStack;

    /*
     * Implementation of StackLogger
     */

    /**
     * [DISABLED] logs a stack trace. This helps to look at the stack frame.
     */
    public void logStackTrace()
    {
        // Do nothing - stack traces aren't terribly useful when we're not
        // looking for real exceptions, and double the number of lines in
        // FINEST log files.
    }

    /**
     * [DISABLED] logs a stack trace. This helps to look at the stack frame.
     *
     * @param traceLevel currently unused.
     */
    public void logStackTrace(int traceLevel)
    {
        // Do nothing - stack traces aren't terribly useful when we're not
        // looking for real exceptions, and double the number of lines in
        // FINEST log files.
    }

    /**
     * Get the line count in the log stream.
     *
     * @return line count
     */
    public int getLineCount()
    {
        return 0;
    }

    /**
     * Determines whether logging is enabled.
     *
     * @return flag to indicate if logging is enabled.
     */
    public boolean isLoggingEnabled()
    {
        return true;
    }

    /**
     * Return true/false if logging is enabled at a given level.
     *
     * @param logLevel the level that we'd like to check loggability for.
     *
     * @return always <tt>true</tt> regardless of <tt>logLevel</tt>'s value.
     */
    public boolean isLoggingEnabled(int logLevel)
    {
        // always enable trace messages so we can receive packets
        // and log them to packet logging service
        if (logLevel == TRACE_DEBUG)
            return logger.isDebugEnabled();
        if (logLevel == TRACE_MESSAGES)         // same as TRACE_INFO
            return true;
        if (logLevel == TRACE_NONE)
            return false;

        return true;
    }

    /**
     * Log an exception.
     *
     * @param ex the exception that we are to log.
     */
    public void logException(Throwable ex)
    {
        logger.error("Exception in SIP stack: ", ex);
    }

    /**
     * Log an exception.
     *
     * @param ex the exception that we are to log.
     */
    @Override
    public void logException(Exception ex)
    {
        logException((Throwable) ex);
    }

    /**
     * Log an error message.
     *
     * @param message --
     *            error message to log.
     */
    public void logFatalError(String message)
    {
        logger.error(message);
    }

    @Override
    public void logFatalError(String message, Throwable cause)
    {
        logger.error(message, cause);
    }

    /**
     * Log an error message.
     *
     * @param message error message to log.
     */
    public void logError(String message)
    {
        logger.error(message);
    }

    /**
     * Logs an exception and an error message error message.
     *
     * @param message that message that we'd like to log.
     * @param ex the exception that we'd like to log.
     */
    public void logError(String message, Exception ex)
    {
        logger.error(message, ex);
    }

    @Override
    public void logError(String message, Throwable cause)
    {
        logger.error(message, cause);
    }

    /**
     * Log a warning message.
     *
     * @param string the warning that we'd like to log
     */
    public void logWarning(String string)
    {
        logger.warn(string);
    }

    @Override
    public void logWarning(Throwable cause)
    {
        logger.warn("Exception in SIP stack: ", cause);
    }

    @Override
    public void logWarning(String message, Throwable cause)
    {
        logger.warn(message, cause);
    }

    /**
     * Log an info message.
     *
     * @param string the message that we'd like to log.
     */
    public void logInfo(String string)
    {
        logger.info(string);
    }

    @Override
    public void logInfo(String message, Throwable cause)
    {
        logger.info(message, cause);
    }

    /**
     * Log a message into the log file.
     *
     * @param message
     *            message to log into the log file.
     */
    public void logDebug(String message)
    {
        logger.debug(message);
    }

    /**
     * Logs the specified trace with a debuf level.
     *
     * @param message the trace to log.
     */
    public void logTrace(String message)
    {
        logger.debug(message);
    }

    /**
     * Logs the given row of CSV data to the csvLogger.
     * This method does not enforce consistency of row structure; that is the
     * responsibility of the caller.
     *
     * @param row A CSV row containing SIP message data.
     */
    private void writeCsvRow(String row)
    {
        // Add a carriage return to the row before logging it.
        // This is a mild compatibility hack: the logger will break lines with
        // '\n' so this forms a \r\n overall.
        // We need \r\n instead of \n because the presence of \r\n inside SIP
        // message bodies confuses CSV readers into thinking that the rows
        // themselves are CRLF-separated.
        // Thus \n alone is ignored by the reader, so we need to use \r\n.
        csvLogger.info(row + "\r");
    }

    /**
     * Disable logging altogether.
     *
     */
    public void disableLogging() {}

    /**
     * Enable logging (globally).
     */
    public void enableLogging() {}

    /**
     * Logs the build time stamp of the jain-sip reference implementation.
     *
     * @param buildTimeStamp the build time stamp of the jain-sip reference
     * implementation.
     */
    public void setBuildTimeStamp(String buildTimeStamp)
    {
        logger.trace("JAIN-SIP RI build " + buildTimeStamp);
    }

    /**
     * Dummy implementation for {@link ServerLogger#setStackProperties(
     * Properties)}
     */
    public void setStackProperties(Properties stackProperties) {}

    /**
     * Dummy implementation for {@link ServerLogger#closeLogFile()}
     */
    public void closeLogFile() {}

    /**
     * Logs the specified message and details.
     *
     * @param message the message to log
     * @param from the message sender
     * @param to the message addressee
     * @param sender determines whether we are the origin of this message.
     * @param time the date this message was received at.
     */
    public void logMessage(SIPMessage message,
                           InetSocketAddress from,
                           InetSocketAddress to,
                           boolean sender,
                           String transport,
                           long time)
    {
        logMessage(message, time, from, to, sender, transport, null);
    }

    /**
     * Logs the specified message and details.
     *
     * @param message the message to log
     * @param time the date this message was received at.
     * @param from the message sender
     * @param to the message addressee
     * @param sender determines whether we are the origin of this message.
     * @param status message status
     */
    public void logMessage(SIPMessage message,
                           long time,
                           InetSocketAddress from,
                           InetSocketAddress to,
                           boolean sender,
                           String transport,
                           String status)
    {
        InetSocketAddress localAddr = sender ? from : to;
        InetSocketAddress remoteAddr = sender ? to : from;
        Direction direction = sender ? Direction.OUTBOUND : Direction.INBOUND;

        logToCsv(System.currentTimeMillis(),
                 direction,
                 localAddr,
                 remoteAddr,
                 transport,
                 message.toString());

        try
        {
            logPacket(message, sender);
        }
        catch(Throwable e)
        {
            logger.error("Error logging packet", e);
        }
    }

    /**
     * Logs the specified message and details to the packet logging service
     * if enabled.
     *
     * @param message the message to log
     * @param sender determines whether we are the origin of this message.
     */
    private void logPacket(SIPMessage message, boolean sender)
    {
        if (message != null && message.isNullRequest())
        {
            logger.interval("CRLF", "Logging CRLF packet");

            // Only log CRLF keepalive packets while this function is in beta
            if (! LOG_KEEPALIVES_PACKETS)
            {
                return;
            }
        }

        try
        {
            PacketLoggingService packetLogging = SipActivator.getPacketLogging();
            if( packetLogging == null
                || !packetLogging.isLoggingEnabled(
                        PacketLoggingService.ProtocolName.SIP))
                return;

            String transport;

            if (message != null && message.isNullRequest())
            {
                transport = message.getNullTransport();
            }
            else
            {
                transport = message.getTopmostVia().getTransport();
            }

            boolean isTransportUDP = transport.equalsIgnoreCase("UDP");

            byte[] srcAddr;
            int srcPort;
            byte[] dstAddr;
            int dstPort;

            InetAddress localAddress = message.getLocalAddress();
            InetAddress remoteAddress = message.getRemoteAddress();
            int localPort = message.getLocalPort();
            int remotePort = message.getRemotePort();

            // Sometimes the local IP address is not set on the SIP message, in
            // which case we get it from the network address manager service.
            if (localAddress.isAnyLocalAddress())
            {
                localAddress =
                    SipActivator.getNetworkAddressManagerService().getLocalHost(
                        remoteAddress);
            }

            if (sender)
            {
                srcAddr = (localAddress != null) ? localAddress.getAddress() :
                    new byte[4];
                srcPort = localPort;
                dstAddr = (remoteAddress != null) ? remoteAddress.getAddress() :
                    new byte[4];
                dstPort = remotePort;
            }
            else
            {
                srcAddr = (remoteAddress != null) ? remoteAddress.getAddress() :
                    new byte[4];
                srcPort = remotePort;
                dstAddr = (localAddress != null) ? localAddress.getAddress() :
                    new byte[4];
                dstPort = localPort;
            }

            byte[] msg = null;
            if(message instanceof SIPRequest && !message.isNullRequest())
            {
                SIPRequest req = (SIPRequest)message;
                if(req.getMethod().equals(SIPRequest.MESSAGE)
                    && message.getContentTypeHeader() != null
                    && message.getContentTypeHeader()
                        .getContentType().equalsIgnoreCase("text"))
                {
                    int len = req.getContentLength().getContentLength();

                    if(len > 0)
                    {
                        SIPRequest newReq = (SIPRequest)req.clone();

                        byte[] newContent =  new byte[len];
                        Arrays.fill(newContent, (byte)'.');
                        newReq.setMessageContent(newContent);
                        msg = newReq.toString().getBytes("UTF-8");
                    }
                }
            }

            if(msg == null)
            {
                msg = message.toString().getBytes("UTF-8");
            }

            packetLogging.logPacket(
                    PacketLoggingService.ProtocolName.SIP,
                    srcAddr, srcPort,
                    dstAddr, dstPort,
                    isTransportUDP ? PacketLoggingService.TransportName.UDP :
                            PacketLoggingService.TransportName.TCP,
                    sender, msg);
        }
        catch(Throwable e)
        {
            logger.error("Cannot obtain message body", e);
        }
    }

    /**
     * A dummy implementation.
     *
     * @param sipStack ignored;
     */
    public void setSipStack(SipStack sipStack)
    {
        this.sipStack = sipStack;
    }

    /**
     * Returns a logger name.
     *
     * @return a logger name.
     */
    public String getLoggerName()
    {
        return "SIP Communicator JAIN SIP logger.";
    }

    /**
     * Returns a local address to use with the specified TCP destination.
     * The method forces the JAIN-SIP stack to create
     * s and binds (if necessary)
     * and return a socket connected to the specified destination address and
     * port and then return its local address.
     *
     * @param dst the destination address that the socket would need to connect
     *            to.
     * @param dstPort the port number that the connection would be established
     * with.
     * @param localAddress the address that we would like to bind on
     * (null for the "any" address).
     * @param transport the transport that will be used TCP ot TLS
     *
     * @return the SocketAddress that this handler would use when connecting to
     * the specified destination address and port.
     *
     * @throws IOException  if we fail binding the local socket
     */
    public java.net.InetSocketAddress getLocalAddressForDestination(
                    java.net.InetAddress dst,
                    int                  dstPort,
                    java.net.InetAddress localAddress,
                    String transport)
        throws IOException
    {
        if(ListeningPoint.TLS.equalsIgnoreCase(transport))
            return (java.net.InetSocketAddress)(((SipStackImpl)this.sipStack)
                .getLocalAddressForTlsDst(dst, dstPort, localAddress));
        else
            return (java.net.InetSocketAddress)(((SipStackImpl)this.sipStack)
            .getLocalAddressForTcpDst(dst, dstPort, localAddress, 0));
    }

    /**
     * Format the given SIP message details into a CSV row, and write it to
     * the csvLogger.
     *
     * @param timestamp The message send/receive time, in ms.
     * @param direction Whether the message was outbound or inbound.
     * @param clientAddr The address and port of this client.
     * @param serverAddr The address and port of the server.
     * @param transportType The transport protocol (tcp, udp, tls).
     * @param contents The message contents, including headers.
     */
    private void logToCsv(long timestamp,
                          Direction direction,
                          InetSocketAddress clientAddr,
                          InetSocketAddress serverAddr,
                          String transportType,
                          String contents)
    {
        // Don't log CRLFs or other blank messages.
        if (contents.trim().length() == 0)
            return;

        // Sometimes the local IP address is not set on the SIP message, in
        // which case we get it from the network address manager service.
        if (clientAddr.getAddress().isAnyLocalAddress())
        {
            NetworkAddressManagerService nams =
                                 SipActivator.getNetworkAddressManagerService();
            InetAddress host = nams.getLocalHost(serverAddr.getAddress());
            clientAddr = new InetSocketAddress(host, clientAddr.getPort());
        }

        Object[] entries = new Object[]
        {
            csvFileTimestampFormat.format(new Date(timestamp)),
            timestamp,
            direction,
            clientAddr.getAddress().getHostAddress(),  // IP of client
            clientAddr.getPort(),
            serverAddr.getAddress().getHostAddress(),  // IP of server
            serverAddr.getPort(),
            transportType,
            contents,
        };

        writeCsvRow(buildCsvRow(entries));
    }

    /**
     * Utility method to convert an array of objects into a CSV-formatted
     * table row of their string representations.
     * @param entries The objects to be stored in the row.
     * @return The CSV row formed from the string representations of the given
     * entities.
     */
    private static String buildCsvRow(Object[] entries)
    {
        if (entries == null || entries.length == 0)
            return "";

        StringBuilder sb = new StringBuilder();
        for (Object obj : entries)
        {
            String objStr = (obj != null) ? obj.toString() : "";

            // Enclose each entity in quotes so we don't have to escape
            // e.g. commas or newlines.
            sb.append("\"");

            // CSV has several mechanisms for escaping double quotes.
            // Here we use the convention of escaping quotes by repeating them.
            sb.append(objStr.replace("\"", "\"\""));

            sb.append("\"");
            sb.append(',');
        }

        // Remove the final ','.
        return sb.substring(0, sb.length() - 1);
    }
}
