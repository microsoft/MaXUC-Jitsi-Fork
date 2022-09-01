/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.account;

import java.awt.*;
import java.util.*;
import java.util.Map.Entry;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;

import org.jitsi.service.resources.*;

/**
 * We use this class as a dummy implementation of the
 * <tt>AccountRegistrationWizard</tt> only containing a blank page and not
 * related to a specific protocol. We are using this class so that we could
 * have the NewAccountDialog open without having a specific protocol selected.
 *
 * The point of having this empty page is to avoid users mistakenly filling in
 * data for the default protocol without noticing that it is not really the
 * protocol they had in mind.
 *
 * @author Emil Ivov
 */
class EmptyAccountRegistrationWizard
    extends AccountRegistrationWizard
{
    /**
     * The only page we need in this wizard, containing a prompt for the user
     * to select a wizrd.
     */
    private EmptyAccountRegistrationWizardPage page
                = new EmptyAccountRegistrationWizardPage(this);

    /**
     * A list containing the only page that this dummy wizard has.
     */
    private LinkedList<WizardPage> pages = new LinkedList<>();

    /**
     * Creates the wizard.
     */
    public EmptyAccountRegistrationWizard()
    {
        pages.add(page);
    }

    /**
     * Returns the ID of our only page.
     *
     * @return the ID of our only page
     */
    public Object getFirstPageIdentifier()
    {
        return page.getIdentifier();
    }

    /**
     * Called by the NewAccountDialog protocol combo renderer. We don't have an
     * icon so we return <tt>null</tt>
     *
     * @return <tt>null</tt>;
     */
    public BufferedImageFuture getIcon()
    {
        return null;
    }

    /**
     * Returns the ID of our last and only page.
     *
     * @return the id of our last (and only) page.
     */
    public Object getLastPageIdentifier()
    {
        return EmptyAccountRegistrationWizardPage.FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Returns null since we don't have any images associated with this wizard
     * or no image in our case.
     *
     * return an empty byte[] array.
     */
    public BufferedImageFuture getPageImage()
    {
        return null;
    }

    /**
     * Returns an iterator over a list containing our only page.
     *
     * @return an iterator over a list containing our only page.
     */
    public Iterator<WizardPage> getPages()
    {
        return pages.iterator();
    }

    /**
     * Returns a dummy protocol description.
     *
     * @return a string containing a dummy protocol description.
     */
    public String getProtocolDescription()
    {
        return GuiActivator.getResources()
            .getI18NString("impl.gui.main.account.DUMMY_PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the name of a dummy protocol which is actually a prompt to select
     * a network.
     *
     * @return a string prompting the user to select a network.
     */
    public String getProtocolName()
    {
        return GuiActivator.getResources()
            .getI18NString("impl.gui.main.account.DUMMY_PROTOCOL_NAME");
    }

    /**
     * Returns a simple account registration form that would be the first form
     * shown to the user. Only if the user needs more settings she'll choose
     * to open the advanced wizard, consisted by all pages.
     *
     * @param isCreateAccount indicates if the simple form should be opened as
     * a create account form or as a login form
     * @return a simple account registration form
     */
    public Object getSimpleForm(boolean isCreateAccount)
    {
        return page.getSimpleForm();
    }

    /**
     * Returns a dummy size that we never use here.
     */
    public Dimension getSize()
    {
        return new Dimension(600, 500);
    }

    /**
     * Returns a dummy <tt>Iterator</tt>. Never really called.
     *
     * @return an Empty iterator.
     */
    public Iterator<Entry<String, String>> getSummary()
    {
        return new java.util.LinkedList<Entry<String, String>>().iterator();
    }

    /**
     * Returns an empty string since never used.
     *
     * @return an empty string as we never use this method.
     */
    public String getUserNameExample()
    {
        return "";
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public ProtocolProviderService signin()
    {
        return null;
    }

    /**
     * Empty interface method implementation, unused in the case of the
     * {@link EmptyAccountRegistrationWizard}
     */
    public ProtocolProviderService signin(String userName, String password)
    {
        return null;
    }
}
