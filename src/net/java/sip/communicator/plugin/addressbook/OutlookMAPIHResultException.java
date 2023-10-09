/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.addressbook;

/**
 * Represents a specific Microsoft Outlook MAPI <tt>HRESULT</tt> as an
 * <tt>Exception</tt>.
 */
public class OutlookMAPIHResultException
        extends Exception
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Initializes a new <tt>OutlookMAPIHResultException</tt> instance which
     * is to represent a specific <tt>HRESULT</tt>.
     *
     * @param hResult the <tt>HRESULT</tt> to be represented by the new instance
     */
    public OutlookMAPIHResultException(long hResult)
    {
        this(hResult, toString(hResult));
    }

    /**
     * Initializes a new <tt>OutlookMAPIHResultException</tt> instance which
     * is to represent a specific <tt>HRESULT</tt> and to provide a specific
     * <tt>String</tt> message.
     *
     * @param hResult the <tt>HRESULT</tt> to be represented by the new instance
     * @param message the <tt>String</tt> message to be provided by the new
     * instance
     */
    public OutlookMAPIHResultException(long hResult, String message)
    {
        super(message);
    }

    /**
     * Initializes a new <tt>OutlookMAPIHResultException</tt> instance with a
     * specific <tt>String</tt> message.
     *
     * @param message the <tt>String</tt> message to be provided by the new
     * instance
     */
    public OutlookMAPIHResultException(String message)
    {
        this(0, message);
    }

    /**
     * Converts a specific <tt>HRESULT</tt> to a touch more readable
     * <tt>String</tt> in accord with the rule of constructing MAPI
     * <tt>HRESULT</tt> values.
     *
     * @param hResult the <tt>HRESULT</tt> to convert
     * @return a <tt>String</tt> which represents the specified <tt>hResult</tt>
     * in a touch more readable form
     */
    private static String toString(long hResult)
    {
        if (hResult == 0)
        {
            return "S_OK";
        }

        StringBuilder s = new StringBuilder("MAPI_");

        s.append(((hResult & 0x80000000L) == 0) ? 'W' : 'E');
        s.append("_0x");
        s.append(Long.toHexString(hResult & 0xFFFL));
        s.append(" (" + Long.toHexString(hResult) + ")");
        return s.toString();
    }
}