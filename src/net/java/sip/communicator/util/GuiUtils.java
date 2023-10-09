/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.awt.Component;
import java.awt.Insets;
import java.awt.Toolkit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;

/**
 * The <tt>StringUtils</tt> class is used through this ui implementation for
 * some special operations with strings.
 *
 * Note that this class is NOT thread safe - methods should be called from a
 * single thread (the EDT ideally) otherwise dates may get corrupted.
 */
public class GuiUtils
{
    /**
     * The configuration service.
     */
    private static ConfigurationService configService =
        UtilActivator.getConfigurationService();

    /**
     * If true, we return compact dates and times (i.e. dates with no year,
     * times with no seconds and durations with no zeros).
     */
    private static boolean useCompactDateTime =
        configService.user().getBoolean(
            "net.java.sip.communicator.impl.gui.USE_COMPACT_DATETIME", false);

    private static final Calendar c1 = Calendar.getInstance();

    private static final Calendar c2 = Calendar.getInstance();

    /**
     * Number of milliseconds in a second.
     */
    public static final long MILLIS_PER_SECOND = 1000;

    /**
     * Number of milliseconds in a standard minute.
     */
    public static final long MILLIS_PER_MINUTE = 60 * MILLIS_PER_SECOND;

    /**
     * Number of milliseconds in a standard hour.
     */
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    /**
     * Number of milliseconds in a standard day.
     */
    public static final long MILLIS_PER_DAY = 24 * MILLIS_PER_HOUR;

    /**
     * Regex matching HTML hyperlink tag
     */
    private static final Pattern HYPERLINK_TAG_PATTERN  = Pattern.compile("<A[^<;>;]+?>");

    /**
     * Replaces some chars that are special in a regular expression.
     * @param text The initial text.
     * @return the formatted text
     */
    public static String replaceSpecialRegExpChars(String text)
    {
        // First escape all backslashes by replacing them with
        // double-backslash (note the extra backslashes below are needed
        // because in the strings used in the replace command itself also need
        // to be escaped).
        String escapedText = text.replace("\\", "\\\\");

        // Then make sure all other special characters are escaped by
        // preceding them with backslash (note that 4 backslashes are required
        // here, as the replaceAll command takes a regular expression, so the
        // backslash needs to be escaped both in the regular expression and in
        // the java string).
        return escapedText.replaceAll("([.()^&$*|])", "\\\\$1");
    }

    /**
     * Compares the two dates. The comparison is based only on the day, month
     * and year values. Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is
     * after the second one.
     * @param date1 the first date to compare
     * @param date2 the second date to compare with
     * @return Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is
     * after the second one
     */
    public static int compareDatesOnly(long date1, long date2)
    {
        c1.setTimeInMillis(date1);
        c2.setTimeInMillis(date2);

        int day1 = c1.get(Calendar.DAY_OF_MONTH);
        int month1 = c1.get(Calendar.MONTH);
        int year1 = c1.get(Calendar.YEAR);

        int day2 = c2.get(Calendar.DAY_OF_MONTH);
        int month2 = c2.get(Calendar.MONTH);
        int year2 = c2.get(Calendar.YEAR);

        if (year1 < year2)
        {
            return -1;
        }
        else if (year1 == year2)
        {
            if (month1 < month2)
                return -1;
            else if (month1 == month2)
            {
                return Integer.compare(day1, day2);
            }
            else
                return 1;
        }
        else
        {
            return 1;
        }
    }

    /**
     * Compares the two dates. The comparison is based only on the day, month
     * and year values. Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is
     * after the second one.
     * @param date1 the first date to compare
     * @param date2 the second date to compare with
     * @return Returns 0 if the two dates are equals, a value < 0 if
     * the first date is before the second one and > 0 if the first date is
     * after the second one
     */
    public static int compareDatesOnly(Date date1, Date date2)
    {
        return compareDatesOnly(date1.getTime(), date2.getTime());
    }

    /**
     * Formats the given date as: Month DD, YYYY and appends it to the given
     * <tt>dateStrBuf</tt> string buffer.
     * If the USE_COMPACT_DATETIME property is set, the year is omitted from
     * the date.
     * @param date the date to format
     * @return the formatted date string
     */
    public static String formatDate(Date date)
    {
        return formatDate(date.getTime());
    }

    /**
     * Formats the given date as: Month DD, YYYY.
     * If the USE_COMPACT_DATETIME property is set, the year is omitted from
     * the date.
     * @param date the date to format
     * @return the formatted date string
     */
    public static String formatDate(final long date)
    {
        StringBuilder strBuilder = new StringBuilder();

        formatDate(date, strBuilder);
        return strBuilder.toString();
    }

    /**
     * Formats the given date as: Month DD, YYYY and appends it to the given
     * <tt>dateStrBuf</tt> string buffer.
     * If the USE_COMPACT_DATETIME property is set, the year is omitted from
     * the date.
     * @param date the date to format
     * @param dateStrBuilder the <tt>StringBuilder</tt>, where to append the
     * formatted date
     */
    public static void formatDate(long date, StringBuilder dateStrBuilder)
    {
        c1.setTimeInMillis(date);

        formatDate(dateStrBuilder);
    }

    /**
     * Formats the given date as: Month DD, YYYY and appends it to the given
     * <tt>dateStrBuf</tt> string buffer.
     * If the USE_COMPACT_DATETIME parameter is set, the year is omitted from
     * the date.
     * @param date the date to format
     * @param dateStrBuilder the <tt>StringBuilder</tt>, where to append the
     * formatted date
     */
    public static void formatDate(Date date, StringBuilder dateStrBuilder)
    {
        c1.setTime(date);

        formatDate(dateStrBuilder);
    }

    private static void formatDate(StringBuilder dateStrBuilder)
    {
        dateStrBuilder.append(GuiUtils.processMonth(c1.get(Calendar.MONTH)));
        dateStrBuilder.append(' ');
        GuiUtils.formatTime(c1.get(Calendar.DAY_OF_MONTH), dateStrBuilder);

        c2.setTimeInMillis(System.currentTimeMillis());
        int currentYear = c2.get(Calendar.YEAR);
        int formatYear = c1.get(Calendar.YEAR);

        // If we're not using compact date and time, add the year to the date.
        // However, if the date is from previous years add it anyway
        if (!useCompactDateTime || (formatYear < currentYear))
        {
            dateStrBuilder.append(", ");
            GuiUtils.formatTime(formatYear, dateStrBuilder);
        }
    }

    /**
     * Formats the time for the given date. The result format is the following:
     * [Hour]:[Minute]:[Second]. For example: 12:25:30.
     * If the USE_COMPACT_DATETIME parameter is set, the seconds are omitted
     * from the time.
     * @param date the date to format
     * @return the formatted time string
     */
    public static String formatTime(Date date)
    {
        return formatTime(date.getTime());
    }

    /**
     * Formats the time for the given date. The result format is the following:
     * [Hour]:[Minute]:[Second]. For example: 12:25:30.
     * If the USE_COMPACT_DATETIME parameter is set, the seconds are omitted
     * from the time.
     *
     * @param time the time to format
     * @return the formatted time string
     */
    public static String formatTime(long time)
    {
        c1.setTimeInMillis(time);

        StringBuilder timeStrBuilder = new StringBuilder();

        GuiUtils.formatTime(c1.get(Calendar.HOUR_OF_DAY), timeStrBuilder);
        timeStrBuilder.append(':');
        GuiUtils.formatTime(c1.get(Calendar.MINUTE), timeStrBuilder);

        // If we're not using compact date and time, add the seconds to the
        // time.
        if(!useCompactDateTime)
        {
            timeStrBuilder.append(':');
            GuiUtils.formatTime(c1.get(Calendar.SECOND), timeStrBuilder);
        }

        return timeStrBuilder.toString();
    }

    /**
     * Formats the time period duration for the given start date and end date.
     * The return format is as follows:
     * [Hour]:[Minute]:[Second]. For example: 12:25:30.
     * @param startDate the start date
     * @param endDate the end date
     * @return the formatted duration string
     */
    public static String formatTime(Date startDate, Date endDate)
    {
        return formatTime(startDate.getTime(), endDate.getTime());
    }

    /**
     * Formats the time period duration for the given start date and end date.
     * The return format is as follows:
     * [Hour]:[Minute]:[Second]. For example: 12:25:30.
     * @param start the start date in milliseconds
     * @param end the end date in milliseconds
     * @return the formatted duration string
     */
    public static String formatTime(long start, long end)
    {
        long duration = end - start;

        long milPerSec = 1000;
        long milPerMin = milPerSec*60;
        long milPerHour = milPerMin*60;

        long hours = duration / milPerHour;
        long minutes
            = ( duration - hours*milPerHour ) / milPerMin;
        long seconds
            = ( duration - hours*milPerHour - minutes*milPerMin)
                    / milPerSec;

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Formats the time period duration for the given start date and end date,
     * allowing localizations to omit any zero values if they want.
     * Does not properly deal with plurals however!
     * The return format in English is as follows:
     * "- [Hour] hrs, [Minute] mins, [Second] secs"
     * For example: "- 12hrs, 25mins, 30secs"
     *
     * @param startDate the start date in milliseconds
     * @param endDate the end date in milliseconds
     * @return the formatted duration string
     */
    public static String formatCompactDuration(Date startDate, Date endDate)
    {
        long duration = endDate.getTime() - startDate.getTime();

        long milPerSec = 1000;
        long milPerMin = milPerSec*60;
        long milPerHour = milPerMin*60;

        long hours = duration / milPerHour;
        long minutes
            = ( duration - hours*milPerHour ) / milPerMin;
        long seconds
            = ( duration - hours*milPerHour - minutes*milPerMin)
                    / milPerSec;

        ResourceManagementService resources = UtilActivator.getResources();

        String durationResource =
                duration == 0 ? "service.gui.DURATION_ZERO" :
                hours == 0 && minutes == 0 ? "service.gui.DURATION_SECONDS" :
                hours == 0 && seconds == 0 ? "service.gui.DURATION_MINUTES" :
                hours == 0 ? "service.gui.DURATION_MINUTES_SECONDS" :
                minutes == 0 && seconds == 0 ? "service.gui.DURATION_HOURS" :
                minutes == 0 ? "service.gui.DURATION_HOURS_SECONDS" :
                seconds == 0 ? "service.gui.DURATION_HOURS_MINUTES" :
                "service.gui.DURATION_HOURS_MINUTES_SECONDS";

        String durationString = resources.getI18NString(durationResource,
                                                        new String[] {
                                                            Long.toString(hours),
                                                            Long.toString(minutes),
                                                            Long.toString(seconds)
                                                        });

        return durationString;
    }

    /**
     * Gets the display/human-readable string representation of the month with
     * the specified zero-based month number.
     *
     * @param month the zero-based month number
     * @return the corresponding month abbreviation
     */
    private static String processMonth(int month)
    {
        String monthStringKey;

        switch (month)
        {
        case 0: monthStringKey = "service.gui.JANUARY"; break;
        case 1: monthStringKey = "service.gui.FEBRUARY"; break;
        case 2: monthStringKey = "service.gui.MARCH"; break;
        case 3: monthStringKey = "service.gui.APRIL"; break;
        case 4: monthStringKey = "service.gui.MAY"; break;
        case 5: monthStringKey = "service.gui.JUNE"; break;
        case 6: monthStringKey = "service.gui.JULY"; break;
        case 7: monthStringKey = "service.gui.AUGUST"; break;
        case 8: monthStringKey = "service.gui.SEPTEMBER"; break;
        case 9: monthStringKey = "service.gui.OCTOBER"; break;
        case 10: monthStringKey = "service.gui.NOVEMBER"; break;
        case 11: monthStringKey = "service.gui.DECEMBER"; break;
        default: return "";
        }

        return UtilActivator.getResources().getI18NString(monthStringKey);
    }

    /**
     * Adds a 0 in the beginning of one digit numbers.
     *
     * @param time The time parameter could be hours, minutes or seconds.
     * @param timeStrBuilder the <tt>StringBuilder</tt> to which the formatted
     * minutes string is to be appended
     */
    private static void formatTime(int time, StringBuilder timeStrBuilder)
    {
        String timeString = Integer.toString(time);

        if (timeString.length() < 2)
            timeStrBuilder.append('0');
        timeStrBuilder.append(timeString);
    }

    /**
     * Formats the given long to X hour, Y min, Z sec.
     * @param millis the time in milliseconds to format
     * @return the formatted seconds
     */
    public static String formatSeconds(long millis)
    {
        long[] values = new long[4];
        values[0] = millis / MILLIS_PER_DAY;
        values[1] = (millis / MILLIS_PER_HOUR) % 24;
        values[2] = (millis / MILLIS_PER_MINUTE) % 60;
        values[3] = (millis / MILLIS_PER_SECOND) % 60;

        String[] fields = { " d ", " h ", " min ", " sec" };

        StringBuilder buf = new StringBuilder(64);
        boolean valueOutput = false;

        for (int i = 0; i < 4; i++)
        {
            long value = values[i];

            if (value == 0)
            {
                if (valueOutput)
                    buf.append('0').append(fields[i]);
            }
            else
            {
                valueOutput = true;
                buf.append(value).append(fields[i]);
            }
        }

        return buf.toString().trim();
    }

    /**
     * Escapes special HTML characters such as &lt;, &gt;, &amp; and &quot; in
     * the specified message.
     *
     * @param message the message to be processed
     * @return the processed message with escaped special HTML characters
     */
    public static String escapeHTMLChars(String message)
    {
        return message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * Unescapes special HTML character codes from e.g. &lt;, &gt;, &amp; and &quot;
     * to their corresponding UTF-8 characters in the specified message.
     *
     * @param message the message to be processed
     * @return the processed message with unescaped characters
     */
    public static String replaceHTMLCharCodes(String message)
    {
        return message
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#x27;", "'")
            .replace("&#x2F;", "/");
    }

    public static String removePlaintextTags(String message)
    {
        return message
            .replace("<PLAINTEXT>", "")
            .replace("</PLAINTEXT>", "");
    }

    // Only the opening <A> tag appears in HTML passed to the Editor Pane,
    // so sufficient to match on this.
    public static String removeLinkTags(String message)
    {
        Matcher m = HYPERLINK_TAG_PATTERN.matcher(message);

        return m.replaceAll("");
    }

    public static String replaceBreakTags(String message)
    {
        return message.replace("<BR/>&#10;", "\n");
    }

    /**
     * Given a component, returns the maximum usable display height for the
     * component. The value returned takes into account the display the
     * component is located if using mulitple monitors.
     *
     * @param component the component to get the display height for.
     * @return the maximum usable height for the component given its position.
     *      Returns 0 if the input component is null.
     */
    public static int getUsableDisplayHeightForComponent(Component component)
    {
        int displayHeight = 0;

        if (component != null)
        {
            // Need to remove the insets (used for task bar etc.)
            Insets insets = Toolkit.getDefaultToolkit()
                .getScreenInsets(component.getGraphicsConfiguration());

            displayHeight =
                component.getGraphicsConfiguration().getBounds().height -
                insets.top - insets.bottom;
        }

        return displayHeight;
    }
}
