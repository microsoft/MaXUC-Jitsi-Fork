/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

/**
 * Implemented by the user interface, this interface allows a protocol provider
 * to asynchronously demand passwords necessary for authentication against
 * various realms.
 * <p>
 * Or in other (simpler words) this is a callback or a hook that the UI would
 * give a protocol provider so that the protocol provider could
 * requestCredentials() when necessary (when a password is not available for
 * a server, or once it has changed, or re-demand one after a faulty
 * authentication)
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public interface SecurityAuthority
{
    /**
     * Indicates that the reason for obtaining credentials is that an
     * authentication is required.
     */
    int AUTHENTICATION_REQUIRED = 0;

    /**
     * Indicates that the reason for obtaining credentials is that the last time
     * a wrong password has been provided.
     */
    int WRONG_PASSWORD = 1;

    /**
     * Indicates that the reason for obtaining credentials is that the last time
     * a wrong user name has been provided.
     */
    int WRONG_USERNAME = 2;

    /**
     * Indicates that the reason for obtaining credentials is that the last time
     * the connection failed.
     */
    int CONNECTION_FAILED = 3;

    /**
     * Returns a UserCredentials object associated with the specified realm, by
     * specifying the reason of this operation.
     * <p>
     * @param realm The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @param reasonCode indicates the reason for which we're obtaining the
     * credentials.
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    UserCredentials obtainCredentials(String realm,
                                      UserCredentials defaultValues,
                                      int reasonCode);

    /**
     * Returns a UserCredentials object associated with the specified realm, by
     * specifying the reason of this operation.
     * <p>
     * @param realm The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    UserCredentials obtainCredentials(String realm,
                                      UserCredentials defaultValues);

    /**
     * Returns a UserCredentials object associated with the specified realm, by
     * specifying the reason of this operation.
     * <p>
     * @param realm The realm that the credentials are needed for.
     * @param defaultValues the values to propose the user by default
     * @param reasonCode indicates the reason for which we're obtaining the
     * credentials.
     * @param resSuffix A suffix added to the end of the resources to allow
     * resource strings to be over-ridden by the calling code
     * @return The credentials associated with the specified realm or null if
     * none could be obtained.
     */
    UserCredentials obtainCredentials(String realm,
                                      UserCredentials defaultValues,
                                      int reasonCode,
                                      String resSuffix);

    /**
     * Sets the userNameEditable property, which should indicate to the
     * implementations of this interface if the user name could be changed by
     * user or not.
     *
     * @param isUserNameEditable indicates if the user name could be changed by
     * user in the implementation of this interface.
     */
    void setUserNameEditable(boolean isUserNameEditable);

    /**
     * Indicates if the user name is currently editable, i.e. could be changed
     * by user or not.
     *
     * @return <code>true</code> if the user name could be changed,
     * <code>false</code> - otherwise.
     */
    boolean isUserNameEditable();
}
