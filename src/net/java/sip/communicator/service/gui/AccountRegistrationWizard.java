/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.gui;

import java.util.*;

import net.java.sip.communicator.service.gui.internal.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * The <tt>AccountRegistrationWizard</tt> is meant to provide a wizard which
 * will guide the user through a protocol account registration. Each
 * <tt>AccountRegistrationWizard</tt> should provide a set of
 * <tt>WizardPage</tt>s, an icon, the name and the description of the
 * corresponding protocol.
 * <p>
 * Note that the <tt>AccountRegistrationWizard</tt> is NOT a real wizard, it
 * doesn't handle wizard events. Each UI Service implementation should provide
 * its own wizard UI control, which should manage all the events, panels and
 * buttons, etc.
 * <p>
 * It depends on the wizard implementation in the UI for whether or not a
 * summary will be shown to the user before "Finish".
 *
 * @author Yana Stamcheva
 */
public abstract class AccountRegistrationWizard
{
    /**
     * The parent wizard container.
     */
    private WizardContainer wizardContainer;

    /**
     * Returns the protocol icon that will be shown on the left of the protocol
     * name in the list, where user will choose the protocol to register to.
     *
     * @return a short description of the protocol.
     */
    public abstract BufferedImageFuture getIcon();

    /**
     * Returns the protocol display name that will be shown in the list,
     * where user will choose the protocol to register to.
     *
     * @return the protocol name.
     */
    public abstract String getProtocolName();

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like. For example: john@jabber.org.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public abstract String getUserNameExample();

    /**
     * Returns the set of <tt>WizardPage</tt>-s for this
     * wizard.
     *
     * @return the set of <tt>WizardPage</tt>-s for this
     * wizard.
     */
    public abstract Iterator<WizardPage> getPages();

    /**
     * Returns a set of key-value pairs that will represent the summary for
     * this wizard.
     *
     * @return a set of key-value pairs that will represent the summary for
     * this wizard.
     */
    public abstract Iterator<Map.Entry<String, String>> getSummary();

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public abstract ProtocolProviderService signin()
        throws OperationFailedException;

    /**
     * Defines the operations that will be executed when the user clicks on
     * the wizard "Signin" button.
     *
     * @param userName the user name to sign in with
     * @param password the password to sign in with
     * @return the created <tt>ProtocolProviderService</tt> corresponding to the
     * new account
     * @throws OperationFailedException if the operation didn't succeed
     */
    public abstract ProtocolProviderService signin(  String userName,
                                            String password)
        throws OperationFailedException;

    /**
     * Indicates that the account corresponding to the given
     * <tt>protocolProvider</tt> has been removed.
     * @param protocolProvider the protocol provider that has been removed
     */
    public void accountRemoved(ProtocolProviderService protocolProvider) {}

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return false;
    }

    /**
     * Defines the operation that will be executed when user clicks on the
     * "Sign up" link.
     *
     * @throws UnsupportedOperationException if the web sign up operation is
     * not supported by the current implementation.
     */
    public void webSignup() throws UnsupportedOperationException {}

    /**
     * Returns a simple account registration form that would be the first form
     * shown to the user. Only if the user needs more settings she'll choose
     * to open the advanced wizard, consisted by all pages.
     *
     * @param isCreateAccount indicates if the simple form should be opened as
     * a create account form or as a login form
     * @return a simple account registration form
     */
    public abstract Object getSimpleForm(boolean isCreateAccount);

    /**
     * Returns the wizard container, where all pages are added.
     *
     * @return the wizard container, where all pages are added
     */
    public WizardContainer getWizardContainer()
    {
        return wizardContainer;
    }

    /**
     * Sets the wizard container, where all pages are added.
     *
     * @param wizardContainer the wizard container, where all pages are added
     */
    protected void setWizardContainer(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;
    }

    /**
     * Indicates if this wizard is for the preferred protocol.
     *
     * @return <tt>true</tt> if this wizard corresponds to the preferred
     * protocol, otherwise returns <tt>false</tt>
     */
    public boolean isPreferredProtocol()
    {
        // Check for preferred account through the PREFERRED_ACCOUNT_WIZARD
        // property.
        String prefWName = GuiServiceActivator.getResources().
            getSettingsString("impl.gui.PREFERRED_ACCOUNT_WIZARD");

        if(prefWName != null && prefWName.length() > 0
            && prefWName.equals(this.getClass().getName()))
            return true;

        return false;
    }

    /**
     * Indicates if a wizard is hidden. This may be used if we don't want that
     * a wizard appears in the list of available networks.
     *
     * @return <tt>true</tt> to indicate that a wizard is hidden, <tt>false</tt>
     * otherwise
     */
    public boolean isHidden()
    {
        return false;
    }
}
