/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui;

import java.awt.*;

import net.java.sip.communicator.service.protocol.*;

/**
 *
 * @author Yana Stamcheva
 */
public interface AccountRegistrationForm
{
    /**
     * Returns the protocol icon that will be shown on the left of the protocol
     * name in the list, where user will choose the protocol to register to.
     *
     * @return a short description of the protocol.
     */
    byte[] getListIcon();

    /**
     * Returns the icon that will be shown on the left of the registration form.
     * @return the icon that will be shown on the left of the registration form
     */
    byte[] getIcon();

    /**
     * Returns the protocol name that will be shown in the list, where user
     * will choose the protocol to register to.
     *
     * @return the protocol name.
     */
    String getProtocolName();

    /**
     * Returns a short description of the protocol that will be shown on the
     * right of the protocol name in the list, where user will choose the
     * protocol to register to.
     *
     * @return a short description of the protocol.
     */
    String getProtocolDescription();

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like. For example: john@jabber.org.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    String getUserNameExample();

    /**
     * Loads all data concerning the given <tt>ProtocolProviderService</tt>.
     * This method is meant to be used when a modification in an already
     * created account is needed.
     *
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to
     * load data from.
     */
    void loadAccount(ProtocolProviderService protocolProvider);

    /**
     * Returns the advanced registration form.
     *
     * @return the advanced registration form
     */
    Component getAdvancedForm();

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     * @return the <tt>ProtocolProviderService</tt> that signed in
     */
    ProtocolProviderService signin()
    ;

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     *
     * @param userName the user name to sign in with
     * @param password the password to sign in with
     */
    ProtocolProviderService signin(String userName,
                                   String password)
    ;

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    boolean isWebSignupSupported();

    /**
     * Defines the operation that will be executed when user clicks on the
     * "Sign up" link.
     * @throws UnsupportedOperationException if the web sign up operation is
     * not supported by the current implementation.
     */
    void webSignup() throws UnsupportedOperationException;

    /**
     * Sets the modification property to indicate if this wizard is opened for
     * a modification.
     *
     * @param isModification indicates if this wizard is opened for modification
     * or for creating a new account.
     */
    void setModification(boolean isModification);

    /**
     * Indicates if this wizard is modifying an existing account or is creating
     * a new one.
     *
     * @return <code>true</code> to indicate that this wizard is currently in
     * modification mode, <code>false</code> - otherwise.
     */
    boolean isModification();

    /**
     * Indicates whether this wizard enables the simple "sign in" form shown
     * when the user opens the application for the first time. The simple
     * "sign in" form allows user to configure her account in one click, just
     * specifying her username and password and leaving any other configuration
     * as by default.
     * @return <code>true</code> if the simple "Sign in" form is enabled or
     * <code>false</code> otherwise.
     */
    boolean isSimpleFormEnabled();

    /**
     * Returns a simple account registration form that would be the first form
     * shown to the user. Only if the user needs more settings she'll choose
     * to open the advanced wizard, consisted by all pages.
     *
     * @return a simple account registration form
     */
    Component getSimpleForm();
}
