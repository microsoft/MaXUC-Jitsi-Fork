/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.sip;

import static gov.nist.core.Separators.NEWLINE;
import static java.util.stream.Collectors.joining;
import static org.jitsi.util.Hasher.logHasher;
import static org.jitsi.util.SanitiseUtils.sanitise;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.sip.SipStack;

import gov.nist.core.ServerLogger;
import gov.nist.core.StackLogger;
import gov.nist.javax.sip.header.Via;
import gov.nist.javax.sip.message.SIPMessage;
import gov.nist.javax.sip.message.SIPRequest;

import net.java.sip.communicator.service.netaddr.NetworkAddressManagerService;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.packetlogging.PacketLoggingService;

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
     * SIP methods. Used for identifying lines in SIP messages that may need
     * to be sanitised for logging.
     */
    private static final List<String> SIP_METHODS = List.of("INVITE",
                                                            "ACK",
                                                            "CANCEL",
                                                            "BYE",
                                                            "REGISTER",
                                                            "OPTIONS",
                                                            "PRACK",
                                                            "UPDATE",
                                                            "SUBSCRIBE",
                                                            "NOTIFY",
                                                            "REFER",
                                                            "INFO",
                                                            "PUBLISH");

    private static final List<String> CONTACT_TAGS  = List.of("From",
                                                              "To",
                                                              "Contact",
                                                              "Referred-By",
                                                              "Refer-To",
                                                              "Diversion");

    private static final List<String> ROUTE_TAGS  = List.of("Call-ID",
                                                            "Call-Info",
                                                            "Route",
                                                            "Via",
                                                            "WWW-Authenticate");

    /**
     * Regex for IPv4 Addresses (needed for sanitising logged SIP messages).
     */
    private static final Pattern IPV4_ADDR_PATTERN = Pattern
            .compile("\\d+\\.\\d+\\.\\d+\\.\\d+");

    /**
     * Regex for contact names (in quotation marks) logged in SIP messages.
     */
    private static final Pattern CONTACT_PATTERN = Pattern
            .compile("(?<=\")(.+)(?=\")");

    /**
     * Regex for user ID in SIP headers.
     * They appear as sip:user@[domain or IP address]. This regex selects "user".
     */
    private static final Pattern SIP_USER_PATTERN = Pattern
            .compile("(?<=sip:)(.+)(?=@)");

    /**
     * Regex for a domain labels alphanumeric part. A domain label may contain between 1 and 63 characters (octets, more
     * precisely). That includes alphanumeric characters, hyphens, or unicode characters (for IDNs).
     * NOTE: using the Pattern.UNICODE_CHARACTER_CLASS in this case is not valid because \w then also matches '_', and
     * other unicode characters
     *
     * @see <a href=https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/regex/Pattern.html>Pattern
     * Class javadoc</a>
     * @see <a href=https://unicode.org/reports/tr18/#property_syntax>Unicode Regular Expressions</a>
     */
    private static final String DOMAIN_PART = "[A-Za-z0-9-\\p{L}\\p{N}]{1,63}";

    /**
     * Regex for a domain label The following regex uses a positive lookahead (?=) to count the length of the label, and
     * then uses a non-capturing group to match it.
     */
    private static final String DOMAIN_LABEL = "(?=" + DOMAIN_PART + ")(?:" + DOMAIN_PART + ")";

    /**
     * Regex for domains in SIP headers, contacts, and SIP messages, depending on the terminator. Selects a domain
     * consisting of domain labels delimited by a dot. The domain must start with @, realm=", uri=", sip: and be
     * terminated with :, "", ; or \s. A FQDN may also include a trailing dot.
     */
    private static final Pattern DOMAIN_PATTERN = Pattern
            .compile("(?<=@|uri=\"sip:|realm=\"|sip:)" + "((?:" + DOMAIN_LABEL + "\\.)*" + DOMAIN_LABEL + "\\.?)"
                     + "(?=:|\"|;|>|\\s)");

    /**
     * Regex for the 'register_acc' field of the Contact header. This domain is underscore-delimited. It appears as
     * register_acc=domain; or register_acc=domain>
     */
    private static final Pattern REGISTER_ACC_DOMAIN_PATTERN = Pattern
            .compile("(?<=registering_acc=)((?:" + DOMAIN_LABEL + "_)*" + DOMAIN_LABEL + "_?)(?=>|;)");

    /**
     * Regex for digest username in the authorisation SIP header.
     * Valid matches: Digest username="+12345678", Digest username="12345678",
     * where only the username is highlighted, with or without "+" prefix
     * Note: "." character at the end of the matching part of the pattern seems like it could be a surplus.
     */
    private static final Pattern DIGEST_USER_PATTERN = Pattern
            .compile("(?<=Digest username=\")\\+?[\\w]+.(?=\")");

    /**
     * SIP messages with non-zero Content Length contain lines
     * of the form "SYMBOL=...". This regex selects them for sanitization.
     * Lines which begin with these symbols are the ones that can contain
     * subscriber numbers and IP addresses.
     */
    private static final List<String> SENSITIVE_SIP_CONTENT_FIELDS = List.of("c=", "o=");

    /**
     * Regex to pick out the subscriber DN from content messages of the form
     * "o={subscriber_dn} ..."
     */
    private static final Pattern SUBSCRIBER_DN_PATTERN = Pattern.compile("(?<=o=)\\+?([0-9]+)");

    /**
     * Regex to pick out the branch from content messages of the form
     * "branch={branch_param} ..."
     */
    private static final Pattern VIA_BRANCH_PATTERN = Pattern.compile("(?<=branch=)(.+)(?=$)");

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
        // 'FINEST' log files.
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
        // 'FINEST' log files.
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
        // always enable trace messages, so we can receive packets
        // and log them to packet logging service
        if (logLevel == TRACE_DEBUG)
            return logger.isDebugEnabled();
        if (logLevel == TRACE_MESSAGES)         // same as TRACE_INFO
            return true;
        return (logLevel != TRACE_NONE);
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
     * Logs the specified trace with a debug level.
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
        // Thus, \n alone is ignored by the reader, so we need to use \r\n.
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
                 sanitiseSipMessage(message));

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
     * Removes Personal Data in SIP messages before logging. Processes each
     * message line by line.
     */
    private static String sanitiseSipMessage(SIPMessage message)
    {
        // We are processing a SIP message.
        // By definition, these use a fixed CR+LF separator, not an OS dependent value
        return Arrays.stream(message.toString().split(NEWLINE, -1))
                .map(SipLogger::processSipMessageLines)
                .collect(joining(NEWLINE));
    }

    /**
     * Sanitises contact names i.e. "General 1234".
     */
    private static String sanitiseContactName(String line)
    {
        return sanitise(line, CONTACT_PATTERN);
    }

    /**
     * Sanitises the "Digest username" field exposed in some SIP messages.
     */
    private static String sanitiseDigestUser(String line)
    {
        return sanitise(line, DIGEST_USER_PATTERN);
    }

    /**
     * Sanitises IPv4 addresses in SIP messages.
     */
    private static String sanitiseIPv4Address(String line)
    {
        return sanitise(line, IPV4_ADDR_PATTERN);
    }

    /**
     * Sanitises lines of the form "sip:[user]@..." to
     * "sip:[HASHED USER]@...".
     */
    private static String sanitiseSipUser(String line)
    {
        return sanitise(line, SIP_USER_PATTERN);
    }

    /**
     * Sanitises domains from the SIP messages.
     */
    private static String sanitiseDomain(String line)
    {
        return sanitise(line, DOMAIN_PATTERN);
    }

    /**
     * Sanitises the register_acc field
     */
    private static String sanitiseRegisterAccDomain(String line)
    {
        return sanitise(line, REGISTER_ACC_DOMAIN_PATTERN);
    }

    /**
     * Sanitises lines of the form "o=0123456 ..." to "o=[HASHED DN] ...".
     */
    private static String sanitiseSubscriberDn(String line)
    {
        return sanitise(line, SUBSCRIBER_DN_PATTERN);
    }

    /**
     * Sanitises branch part of the line of the form "branch=0a1bc3456 ..." to "branch=[HASHED DN] ...".
     */
    private static String sanitiseViaBranch(String line)
    {
        return sanitise(line, VIA_BRANCH_PATTERN);
    }

    private static String sanitiseContentId(String line)
    {
        return splitAndSanitize(line, "<");
    }

    private static String sanitiseGeolocation(String line)
    {
        return splitAndSanitize(line, "<cid:");
    }

    private static String splitAndSanitize(String line, String firstToken)
    {
        String[] parts = line.split(firstToken + "|@|>");
        if (parts.length == 3)
        {
            line = parts[0] + firstToken + logHasher(parts[1]) + "@" + logHasher(parts[2]) + ">";
        }
        return line;
    }

    private static String sanitiseGeoXml(String line)
    {
        // The civic address used for the geolocation xml contains many individual
        // elements each of which is private information.
        // First we mask off the outer ca:civicAddress tags
        final String CIVIC_ADDRESS = "ca:civicAddress";
        final String CIVIC_ADDRESS_MASKED = CIVIC_ADDRESS.toUpperCase(Locale.ROOT);

        line = line.replace(CIVIC_ADDRESS, CIVIC_ADDRESS_MASKED);

        // Find all the remaining elements in the ca namespace
        //  - a tag consisting of ca namespace and a word
        //  - then the element contents
        //  - then the matching closing tag using a back reference
        final Pattern pattern = Pattern.compile("(<ca:(\\w+)>)(.+)(</ca:\\2>)");
        final Matcher matcher = pattern.matcher(line);

        // Hash the contents of each element - group 2 was a nested group so not needed
        line = matcher.replaceAll(mr -> mr.group(1) + logHasher(mr.group(3)) + mr.group(4));

        // Undo the masking of the outer civicAddress
        line =  line.replace(CIVIC_ADDRESS_MASKED, CIVIC_ADDRESS);

        // We also have personal info in some of the attributes
        for (String first : List.of("entity=\"pres:", "id=\""))
        {
            line = Pattern.compile( first + "(.+?)@(.+?)(\")")
                    .matcher(line)
                    .replaceAll(mr -> first + logHasher(mr.group(1)) + "@" + logHasher(mr.group(2)) + mr.group(3));
        }

        return line;
    }

    /**
     * Sanitises each line according to the specific Personal Data it could
     * contain.
     */
    private static String processSipMessageLines(String line)
    {
        List<Function<String, String>> sanitisers = null;
        if (CONTACT_TAGS.stream().anyMatch(line::startsWith))
        {
            sanitisers = List.of(SipLogger::sanitiseContactName,
                                 SipLogger::sanitiseIPv4Address,
                                 SipLogger::sanitiseSipUser,
                                 SipLogger::sanitiseDomain,
                                 SipLogger::sanitiseRegisterAccDomain);
        }
        else if (ROUTE_TAGS.stream().anyMatch(line::startsWith))
        {
            sanitisers = List.of(SipLogger::sanitiseIPv4Address,
                                 SipLogger::sanitiseViaBranch,
                                 SipLogger::sanitiseDomain);
        }
        else if (SIP_METHODS.stream().anyMatch(line::startsWith))
        {
            sanitisers = List.of(SipLogger::sanitiseIPv4Address,
                                 SipLogger::sanitiseSipUser,
                                 SipLogger::sanitiseDomain);
        }
        else if (line.startsWith("Authorization"))
        {
            sanitisers = List.of(SipLogger::sanitiseIPv4Address,
                                 SipLogger::sanitiseSipUser,
                                 SipLogger::sanitiseDigestUser,
                                 SipLogger::sanitiseDomain);
        }
        else if (line.startsWith("<dialog-info"))
        {
            sanitisers = List.of(SipLogger::sanitiseSipUser,
                                 SipLogger::sanitiseDomain);
        }
        else if (SENSITIVE_SIP_CONTENT_FIELDS.stream().anyMatch(line::startsWith))
        {
            sanitisers = List.of(SipLogger::sanitiseIPv4Address,
                                 SipLogger::sanitiseSubscriberDn,
                                 SipLogger::sanitiseDomain);
        }
        else if (line.startsWith("Content-ID:"))
        {
            sanitisers = List.of(SipLogger::sanitiseContentId);
        }
        else if (line.startsWith("Geolocation:"))
        {
            sanitisers = List.of(SipLogger::sanitiseGeolocation);
        }
        else if (line.startsWith("<?xml"))
        {
            // We make the assumption than any xml is only going to be the geolocation one!
            sanitisers = List.of(SipLogger::sanitiseGeoXml);
        }

        return sanitisers == null ? line : useMultipleSanitisers(sanitisers).apply(line);
    }

    /**
     * Utility method that stacks multiple sanitisers.
     * @return a single function that applies all the
     * supplied {@code sanitisers} at once.
     */
    private static Function<String, String> useMultipleSanitisers(
            List<Function<String, String>> sanitisers)
    {
        return sanitisers.stream().reduce(Function.identity(), Function::andThen);
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

            // Only log CRLF keep-alive packets while this function is in beta
            if (! LOG_KEEPALIVES_PACKETS)
            {
                return;
            }
        }

        try
        {
            PacketLoggingService packetLogging = SipActivator.getPacketLogging();
            if (packetLogging == null
                || !packetLogging.isLoggingEnabled(
                        PacketLoggingService.ProtocolName.SIP))
                return;

            String transport = message.getNullTransport();
            if (!message.isNullRequest())
            {
                Via via = message.getTopmostVia();
                if (via != null)
                {
                    transport = via.getTransport();
                }
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
            if (message instanceof SIPRequest && !message.isNullRequest())
            {
                SIPRequest req = (SIPRequest)message;
                if (req.getMethod().equals(SIPRequest.MESSAGE)
                    && message.getContentTypeHeader() != null
                    && message.getContentTypeHeader()
                        .getContentType().equalsIgnoreCase("text"))
                {
                    int len = req.getContentLength().getContentLength();

                    if (len > 0)
                    {
                        SIPRequest newReq = (SIPRequest)req.clone();

                        byte[] newContent =  new byte[len];
                        Arrays.fill(newContent, (byte)'.');
                        newReq.setMessageContent(newContent);
                        msg = newReq.toString().getBytes(StandardCharsets.UTF_8);
                    }
                }
            }

            if (msg == null)
            {
                msg = message.toString().getBytes(StandardCharsets.UTF_8);
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
        // sipStack is not used in this class
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
            logHasher(clientAddr.getAddress().getHostAddress()),  // IP of client
            clientAddr.getPort(),
            logHasher(serverAddr.getAddress().getHostAddress()),  // IP of server
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

            // Enclose each entity in quotes, so we don't have to escape
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
