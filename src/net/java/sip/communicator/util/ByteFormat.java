/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.text.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.CustomAnnotations.*;

/**
 * Acknowledgment: This file was originally provided by the Ignite Realtime
 * community, and was part of the Spark project (distributed under the terms of
 * the LGPL).
 *
 * A formatter for formatting byte sizes. For example, formatting 12345 bytes
 * results in "12.1 K" and 1234567 results in "1.18 MB".
 *
 * @author Bill Lynch
 */
public class ByteFormat
    extends Format
{
    private static final long serialVersionUID = 0;

    public ByteFormat()
    {}

    /**
     * The service through which we access resources like colors.
     */
    protected static final ResourceManagementService resources
        = UtilActivator.getResources();

    /**
     * Format the given object (must be a Long).
     *
     * @param obj assumed to be the number of bytes as a Long.
     * @param buf the StringBuffer to append to.
     * @param pos field position.
     * @return A formatted string representing the given bytes in more
     * human-readable form.
     */
    public StringBuffer format(Object obj, @NotNull StringBuffer buf, @NotNull FieldPosition pos)
    {
        if (obj instanceof Long)
        {
            long numBytes = (Long) obj;
            if (numBytes < 1024)
            {
                DecimalFormat formatter = new DecimalFormat("#,##0");
                buf.append(formatter.format(
                    (double)numBytes)).append(
                        " " + resources.getI18NString("service.gui.BYTES") );
            }
            else if (numBytes < 1024 * 1024)
            {
                DecimalFormat formatter = new DecimalFormat("#,##0.0");
                buf.append(
                    formatter.format(
                        (double)numBytes / 1024.0)).append(
                            " " + resources.getI18NString("service.gui.KILOBYTES") );
            }
            else if (numBytes < 1024 * 1024 * 1024)
            {
                DecimalFormat formatter = new DecimalFormat("#,##0.0");
                buf.append(
                    formatter.format(
                        (double)numBytes / (1024.0 * 1024.0))).append(
                            " " + resources.getI18NString("service.gui.MEGABYTES") );
            }
            else
            {
                DecimalFormat formatter = new DecimalFormat("#,##0.0");
                buf.append(
                    formatter.format(
                        (double)numBytes / (1024.0 * 1024.0 * 1024.0))).append(
                            " " + resources.getI18NString("service.gui.GIGABYTES") );
            }
        }
        return buf;
    }

    /**
     * In this implementation, returns null always.
     *
     * @param source Source string to parse.
     * @param pos Position to parse from.
     * @return returns null in this implementation.
     */
    public Object parseObject(String source, @NotNull ParsePosition pos)
    {
        return null;
    }
}
