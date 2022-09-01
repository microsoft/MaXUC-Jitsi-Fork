/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * The class is used whenever user credentials for a particular realm (site
 * server or service) are necessary
 * @author Emil Ivov <emcho@dev.java.net>
 * @version 1.0
 */

public class UserCredentials
{
    /**
     * The user name.
     */
    private String  userName     = null;

    /**
     * The user password.
     */
    private char[]  password     = null;

    /**
     * If we will store the password persistently.
     */
    private boolean storePassword = false;

    /**
     * Sets the name of the user that these credentials relate to.
     * @param userName the name of the user that these credentials relate to.
     */
    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    /**
     * Returns the name of the user that these credentials relate to.
     * @return the user name.
     */
    public String getUserName()
    {
        return this.userName;
    }

    /**
     * Sets a password associated with this set of credentials.
     *
     * @param passwd the password associated with this set of credentials.
     */
    public void setPassword(char[] passwd)
    {
        this.password = passwd;
    }

    /**
     * Returns a password associated with this set of credentials.
     *
     * @return a password associated with this set of credentials.
     */
    public char[] getPassword()
    {
        return password;
    }

    /**
     * Returns a String containing the password associated with this set of
     * credentials.
     *
     * @return a String containing the password associated with this set of
     * credentials.
     */
    public String getPasswordAsString()
    {
        return new String(password);
    }

    /**
     * Specifies whether or not the password associated with this credentials
     * object is to be stored persistently (insecure!) or not.
     * <p>
     * @param storePassword indicates whether passwords contained by this
     * credentials object are to be stored persistently.
     */
    public void setPasswordPersistent(boolean storePassword)
    {
        this.storePassword = storePassword;
    }

    /**
     * Determines whether or not the password associated with this credentials
     * object is to be stored persistently (insecure!) or not.
     * <p>
     * @return true if the underlying protocol provider is to persistently
     * (and possibly insecurely) store the password and false otherwise.
     */
    public boolean isPasswordPersistent()
    {
        return storePassword;
    }
}
