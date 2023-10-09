/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.awt.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>FirstWizardPage</tt> is the page, where user could enter the user
 * ID and the password of the account.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public class FirstWizardPage
    extends TransparentPanel
    implements  WizardPage
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Identifier of the first page.
     */
    public static final String FIRST_PAGE_IDENTIFIER = "FirstPageIdentifier";

    private Object nextPageIdentifier = WizardPage.SUMMARY_PAGE_IDENTIFIER;

    private final JabberAccountRegistrationWizard wizard;

    private final JabberAccountRegistrationForm registrationForm;

    private boolean isCommitted = false;

    /**
     * Creates an instance of <tt>FirstWizardPage</tt>.
     *
     * @param wizard the parent wizard
     */
    public FirstWizardPage(JabberAccountRegistrationWizard wizard)
    {
        super(new BorderLayout());

        this.wizard = wizard;

        this.registrationForm = new JabberAccountRegistrationForm(wizard);

        this.add(registrationForm);
    }

    /**
     * Implements the <code>WizardPage.getIdentifier</code> to return this
     * page identifier.
     *
     * @return the id of the first wizard page.
     */
    public Object getIdentifier()
    {
        return FIRST_PAGE_IDENTIFIER;
    }

    /**
     * Implements the <code>WizardPage.getNextPageIdentifier</code> to return
     * the next page identifier - the summary page.
     *
     * @return the id of the next wizard page.
     */
    public Object getNextPageIdentifier()
    {
        return nextPageIdentifier;
    }

    /**
     * Implements the <code>WizardPage.getBackPageIdentifier</code> to return
     * the back identifier. In this case it's null because this is the first
     * wizard page.
     *
     * @return the identifier of the previous wizard page
     */
    public Object getBackPageIdentifier()
    {
        return null;
    }

    /**
     * Implements the <code>WizardPage.getWizardForm</code> to return this
     * panel.
     *
     * @return this wizard page.
     */
    public Object getWizardForm()
    {
        registrationForm.init();

        return this;
    }

    /**
     * Before this page is displayed enables or disables the "Next" wizard
     * button according to whether the User ID field is empty.
     */
    public void pageShowing()
    {
        wizard.getWizardContainer().setBackButtonEnabled(false);
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     */
    public void commitPage()
    {
        isCommitted
            = registrationForm.commitPage(wizard.getRegistration());

        nextPageIdentifier = SUMMARY_PAGE_IDENTIFIER;
    }

    public void pageHiding() {}

    /**
     * Dummy implementation
     */
    public void pageShown() {}

    /**
     * Dummy implementation
     */
    public void pageBack() {}

    /**
     * Returns the simple form.
     * @return the simple form
     */
    public Object getSimpleForm()
    {
        return registrationForm.getSimpleForm();
    }

    /**
     * Indicates if this page has been already committed.
     * @return <tt>true</tt> if this page has been already committed,
     * <tt>false</tt> - otherwise
     */
    public boolean isCommitted()
    {
        return isCommitted;
    }
}
