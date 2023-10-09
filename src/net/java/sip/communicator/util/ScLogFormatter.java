/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import static net.java.sip.communicator.util.PrivacyUtils.REDACTED;
import static org.jitsi.util.SanitiseUtils.sanitise;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import java.util.regex.*;

/**
 * Print a brief summary of the LogRecord in a human readable. The summary will
 * typically be on a single line (unless it's too long :) ... what I meant to
 * say is that we don't add any line breaks).
 *
 * @author Emil Ivov
 */

public class ScLogFormatter
    extends java.util.logging.Formatter
{
    /**
     * Contains all classes which hold methods specific to logging. Allows a
     * method to easily assess whether any given frame is a 'logger class'.
     * Used to infer which method called for a log, by moving up a stack
     * frame by frame.
     */
    private static final Set<String> LOGGER_CLASSES = new HashSet<>();
    static
    {
        LOGGER_CLASSES.add("gov.nist.core.CommonLogger");
        LOGGER_CLASSES.add("net.java.sip.communicator.util.Logger");
        LOGGER_CLASSES.add("net.java.sip.communicator.util.ContactLogger");
        LOGGER_CLASSES.add("net.java.sip.communicator.util.ProcessLogger");
        LOGGER_CLASSES.add("net.sf.fmj.media.Log");
        LOGGER_CLASSES.add("net.java.sip.communicator.impl.protocol.sip.SipLogger");
        LOGGER_CLASSES.add("org.jitsi.util.Logger");
    }

    /**
     * The longest message that we allow to log.  If it's too big then we might
     * get OOMs, which would be bad.
     */
    private static final int MAX_MESSAGE_LENGTH = 1_000_000;

    /**
     * Matches sensitive details that needs to be replaced.
     * Does not match "password=*" for replacement because it indicates that the
     * password is not being provided as a part of the request to the
     * CommPortal API login. Any other password must be matched for replacement.
     */
    private static final Pattern STRIP_SENSITIVE_DETAILS =
        Pattern.compile("(encrypted=|passkey=|/session|password=((?!\\*)(?=.)|(?=\\*([^&\n/?]+))))([^&\n/?]+)",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SENSITIVE_STACK_TRACE =
        Pattern.compile("^(org\\.jivesoftware\\.smack)",
                        Pattern.CASE_INSENSITIVE);

    /**
     * Pattern to find peer ID, which can be a chat address, a chat room address
     * or chat room participant address (with resource part), in Smack exceptions.
     * Using <a href="https://www.rfc-editor.org/rfc/rfc3986#section-3.3">Uniform Resource Identifier (URI): Generic Syntax</a>
     */
    private static final Pattern PEER_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9!#$%&'*+-/=?^_`{|}~.]+@[A-Za-z0-9-.]+[A-Za-z0-9-._~!$&'()*+,;=/]*");

    private static String lineSeparator = System.getProperty("line.separator");
    private static DecimalFormat twoDigFmt = new DecimalFormat("00");
    private static DecimalFormat threeDigFmt = new DecimalFormat("000");
    private static DecimalFormat fourDigFmt = new DecimalFormat("0000");

    /**
     * Format the given LogRecord.
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    public synchronized String format(LogRecord record)
    {
        StringBuffer sb = new StringBuffer();

        //current time
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(record.getMillis());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minutes = cal.get(Calendar.MINUTE);
        int seconds = cal.get(Calendar.SECOND);
        int millis = cal.get(Calendar.MILLISECOND);

        sb.append(fourDigFmt.format(year)).append('-');
        sb.append(twoDigFmt.format(month)).append('-');
        sb.append(twoDigFmt.format(day)).append(' ');
        sb.append(twoDigFmt.format(hour)).append(':');
        sb.append(twoDigFmt.format(minutes)).append(':');
        sb.append(twoDigFmt.format(seconds)).append('.');
        sb.append(threeDigFmt.format(millis)).append(' ');

        // Inferring the call returns the line number and sets the correct
        // source class
        int lineNumber = inferCaller(record);
        String logClass;
        if ((record.getSourceClassName() != null) &&
            (record.getSourceClassName().trim().length() != 0))
        {
            logClass = record.getSourceClassName();
        }
        else
        {
            logClass = getLoggerName(record);
        }

        // Don't log the class, thread, method and level for HTTP header
        // and XMPP logs as it's not very useful spam.
        if (!DefaultFileHandler.isHttpHeaderLog(record) &&
            !DefaultFileHandler.isXmppLog(record))
        {
            //log level
            sb.append(record.getLevel().getName());
            sb.append(": ");

            // Thread ID
            sb.append("[" + record.getThreadID() + "] ");

            // Caller class and method (or logger if we don't have a class).  We
            // strip any leading net.java.sip.communicator to reduce the length of
            // log lines for clarity.

            if (logClass != null && logClass.startsWith("net.java.sip.communicator."))
            {
                // Strip out the leading net.java.sip.communicator
                logClass = logClass.substring("net.java.sip.communicator."
                    .length());
            }

            sb.append(logClass);

            if (record.getSourceMethodName() != null)
            {
                sb.append(".");
                sb.append(record.getSourceMethodName());

                //include the line number if we have it.
                if(lineNumber != -1)
                    sb.append("().").append(Integer.toString(lineNumber));
                else
                    sb.append("()");
            }
            sb.append(" ");
        }

        String message = record.getMessage();

        if (message != null && message.length() > MAX_MESSAGE_LENGTH)
        {
            // Message is too long.  Don't use substring here, as that will
            // create a new string which may push us over the edge.  Instead,
            // just append the first MAX_MESSAGE_LENGTH chars
            message = new StringBuilder("TRUNCATED!: ")
                             .append(message, 0, MAX_MESSAGE_LENGTH).toString();
        }

        if (message != null)
        {
            // Don't log sensitive details:
            message = STRIP_SENSITIVE_DETAILS.matcher(message).replaceAll("$1" + REDACTED);
        }

        sb.append(message);
        sb.append(lineSeparator);
        if (record.getThrown() != null)
        {
            try
            {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                record.getThrown().printStackTrace(pw);
                pw.close();
                String exceptionClassName = record.getThrown().getCause() != null
                        ? record.getThrown().getCause().getClass().getCanonicalName()
                        : record.getThrown().getClass().getCanonicalName();
                if (SENSITIVE_STACK_TRACE.matcher(exceptionClassName).find())
                {
                    sb.append(findAndSanitisePeerId(sw.toString()));
                }
                else
                {
                    sb.append(sw);
                }
            }
            catch (Exception ex)
            {
            }
        }
        return sb.toString();
    }

    /**
     * Get the name of this logger
     *
     * @param record The log record we are looking at
     * @return the name of the logger
     */
    protected String getLoggerName(LogRecord record)
    {
        String loggerName = record.getLoggerName();

        if(loggerName == null)
            loggerName = record.getSourceClassName();
        return loggerName;
    }

    /**
     * Try to extract the name of the class and method that called the current
     * log statement.
     *
     * @param record the logrecord where class and method name should be stored.
     *
     * @return the line number that the call was made from in the caller.
     */
    private int inferCaller(LogRecord record)
    {
        // Get the stack trace.
        StackTraceElement[] stack = (new Throwable()).getStackTrace();

        //the line number that the caller made the call from
        int lineNumber = -1;

        // First, search back to a method in the SIP Communicator, libjitsi,
        // FMJ or JSIP logger classes.
        int ix = 0;
        while (ix < stack.length)
        {
            StackTraceElement frame = stack[ix];
            String cname = frame.getClassName();
            if (LOGGER_CLASSES.contains(cname))
            {
                break;
            }
            ix++;
        }

        // Now search for the first frame before the SIP Communicator,
        // libjitsi, FMJ or JSIP logger classes.
        while (ix < stack.length)
        {
            StackTraceElement frame = stack[ix];
            lineNumber = stack[ix].getLineNumber();
            String cname = frame.getClassName();
            if (!LOGGER_CLASSES.contains(cname))
            {
                // We've found the relevant frame.
                record.setSourceClassName(cname);
                record.setSourceMethodName(frame.getMethodName());
                break;
            }
            ix++;
        }

        return lineNumber;
    }

    private static String findAndSanitisePeerId(String value)
    {
        return sanitise(value, PEER_ID_PATTERN, PrivacyUtils::sanitisePeerId);
    }
}
