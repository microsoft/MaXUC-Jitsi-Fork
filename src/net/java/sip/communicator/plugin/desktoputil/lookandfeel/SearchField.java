/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil.lookandfeel;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The field shown on the top of the main window, which allows the user to
 * search for users.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SearchField
    extends SIPCommTextField
    implements  TextFieldChangeListener,
                FilterQueryListener,
                Skinnable
{
    private static final long serialVersionUID = 0L;
    private static final Logger sLog = Logger.getLogger(SearchField.class);

    /**
     * If we're running in QA mode, we can log details of what the user types
     * into the search box, otherwise we can't as that may contain PII.
     */
    private final boolean mQAMode;

    /**
     * Class id key used in UIDefaults.
     */
    private static final String uiClassID =
        SearchField.class.getName() +  "FieldUI";

    /**
     * Config option defining whether to enter should call the number
     */
    private static final String ENTER_CALLS_NUMBER =
                        "net.java.sip.communicator.impl.gui.ENTER_CALLS_NUMBER";

    /**
     * Used to check whether the text entered in the saerch field is a valid
     * number for calls/SMS.
     */
    private final PhoneNumberUtilsService mFormatter =
                                     DesktopUtilActivator.getPhoneNumberUtils();

    // Adds the ui class to UIDefaults.
    static
    {
        UIManager.getDefaults().put(uiClassID,
            SearchFieldUI.class.getName());
    }

    /**
     * The main application window.
     */
    private final SearchFieldContainer container;

    /**
     * The contact list on which we apply the filter.
     */
    private ContactList contactList;

    /**
     * The filter to apply on search.
     */
    private final ContactListSearchFilter searchFilter;

    /**
     * The current filter query.
     */
    private FilterQuery currentFilterQuery = null;

    /**
     * Creates the <tt>SearchField</tt> with the default hint text.
     *
     * @param frame the main application window
     * @param searchFilter the filter to apply on search
     * @param buttonsEnabled indicates if the call, chat and dial pad buttons
     * should be enabled in this search field
     */
    public SearchField(SearchFieldContainer frame,
                       ContactListSearchFilter searchFilter,
                       boolean buttonsEnabled)
    {
        this(frame,
             searchFilter,
             buttonsEnabled,
             DesktopUtilActivator.getResources().getI18NString("service.gui.ENTER_NAME_OR_NUMBER"));
    }

    /**
     * Creates the <tt>SearchField</tt> with custom hint text.
     *
     * @param frame the main application window
     * @param searchFilter the filter to apply on search
     * @param buttonsEnabled indicates if the call, chat and dial pad buttons
     * should be enabled in this search field
     * @param hintText the hint text to display in the field
     */
    public SearchField(SearchFieldContainer frame,
                       ContactListSearchFilter searchFilter,
                       boolean buttonsEnabled,
                       String hintText)
    {
        // Limit the maximum length of the string the user can enter.  This is
        // an arbitrary value but without some limit the user can trigger
        /// out-of-memory errors by entering very long strings.
        super(hintText, 256);

        mQAMode = DesktopUtilActivator.getConfigurationService().global().getBoolean(
            "net.java.sip.communicator.QA_MODE", false);

        this.container = frame;
        this.searchFilter = searchFilter;

        AccessibilityUtils.setDescription(
            this,
            DesktopUtilActivator.getResources()
                        .getI18NString("service.gui.SEARCH_FIELD_DESCRIPTION"));

        if(getUI() instanceof  SearchFieldUI)
        {
            SearchFieldUI searchFieldUI = (SearchFieldUI)getUI();
            searchFieldUI.setDeleteButtonEnabled(true);
            searchFieldUI.setButtonsEnabled(buttonsEnabled);
        }

        this.setBorder(BorderFactory.createEmptyBorder());
        this.setOpaque(false);

        this.setDragEnabled(true);
        this.addTextChangeListener(this);

        InputMap imap
            = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escape");
        ActionMap amap = getActionMap();
        amap.put("escape", new AbstractAction()
        {
            private static final long serialVersionUID = 0L;

            public void actionPerformed(ActionEvent e)
            {
                setText("");

                if (container != null)
                    container.requestFocusInContactList();
            }
        });

        loadSkin();
    }

    /**
     * Enables/disables the call button in the search field.
     *
     * @param buttonEnabled indicates if the call button will be enabled
     */
    public void setDialPadButtonEnabled(boolean buttonEnabled)
    {
        sLog.debug("setDialPadButtonEnabled: " + buttonEnabled);

        if(getUI() instanceof SearchFieldUI)
        {
            SearchFieldUI searchFieldUI = (SearchFieldUI)getUI();
            searchFieldUI.setButtonsEnabled(buttonEnabled);
        }
    }

    /**
     * Handles the change when a char has been inserted in the field.
     */
    public void textInserted()
    {
        sLog.debug("textInserted");

        // Should explicitly check if there's a text, because the default text
        // triggers also an insertUpdate event.
        String filterString = getText();
        String filterStringToLog = mQAMode ? filterString : "<redacted>";
        sLog.info("Text inserted - user searching for string " + filterStringToLog);

        if (filterString == null || filterString.length() <= 0)
            return;

        updateContactListView();

        AccessibilityUtils.setName(this, filterString);
    }

    /**
     * Handles the change when a char has been removed from the field.
     */
    public void textRemoved()
    {
        sLog.debug("textRemoved");

        String filterString = getText();
        sLog.info("Text removed - user searching for string " + filterString);
        updateContactListView();
        AccessibilityUtils.setName(this, filterString);
    }

    /**
     * Returns a valid phone number derived from the search field text.
     * Delimiter characters are removed and then the text is validated.
     * @return a valid phone number, or null if the text doesn't map to one
     */
    public String getPhoneNumber()
    {
        return mFormatter.getValidNumber(
            getText(),
            ConfigurationUtils.getPhoneNumberCallableRegex());
    }

    /**
     * Returns a valid SMS number derived from the search field text.
     * Delimiter characters are removed and then the text is validated.
     * @return a valid SMS number, or null if the text doesn't map to one
     */
    public String getSmsNumber()
    {
        return mFormatter.getValidNumber(
            getText(),
            ConfigurationUtils.getSmsNumberMessagableRegex());
    }

    /**
     * Do not need this for the moment.
     * @param e the <tt>DocumentEvent</tt> that notified us
     */
    public void changedUpdate(DocumentEvent e) {}

    /**
     * Schedules an update if necessary.
     */
    private void updateContactListView()
    {
        sLog.debug("updateContactListView");

        String filterString = getText();

        boolean isDefaultFilter = false;

        searchFilter.setFilterString(filterString.trim());

        // First finish the last filter.
        if (currentFilterQuery != null)
            filterQueryFinished(currentFilterQuery, true);

        if (filterString != null && filterString.length() > 0)
        {
            currentFilterQuery = contactList.applyFilter(searchFilter);
        }
        else
        {
            currentFilterQuery = contactList.applyCurrentFilter();
            isDefaultFilter = true;
        }

        if (currentFilterQuery != null && !currentFilterQuery.isCanceled())
        {
            // If we already have a result here we update the interface.
            // In the case of default filter we don't need to know if the
            // query has succeeded, as even if it isn't we would like to
            // remove the unknown contact view.
            if (isDefaultFilter || currentFilterQuery.isSucceeded())
                enableUnknownContactView(false);
            else
                // Otherwise we will listen for events for changes in status
                // of this query.
                currentFilterQuery.setQueryListener(this);
        }
        else
        {
            // If the query is null or is canceled, we would simply check the
            // contact list content.
            filterQueryFinished(currentFilterQuery, !contactList.isEmpty());
        }
    }

    /**
     * Sets the unknown contact view to the main contact list window.
     *
     * @param isEnabled indicates if the unknown contact view should be enabled
     * or disabled.
     */
    public void enableUnknownContactView(final boolean isEnabled)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                sLog.debug("Enabling unknown contact view? " + isEnabled);
                if (container != null)
                    container.enableUnknownContactView(isEnabled);
            }
        });
    }

    /**
     * Sets the contact list, in which the search is performed.
     *
     * @param contactList the contact list in which the search is performed
     */
    public void setContactList(ContactList contactList)
    {
        this.contactList = contactList;
    }

    /**
     * Indicates that the given <tt>query</tt> has finished with failure, i.e.
     * no results for the filter were found.
     *
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQueryFailed(FilterQuery query)
    {
        // If the query has failed then we don't have results.
        if (currentFilterQuery.equals(query))
            filterQueryFinished(query, false);
    }

    /**
     * Indicates that the given <tt>query</tt> has finished with success, i.e.
     * the filter has returned results.
     *
     * @param query the <tt>FilterQuery</tt>, where this listener is registered
     */
    public void filterQuerySucceeded(FilterQuery query)
    {
        // If the query has succeeded then we have results.
        if (currentFilterQuery.equals(query))
            filterQueryFinished(query, true);
    }

    /**
     * Reloads text field UI defs.
     */
    public void loadSkin()
    {
        if(getUI() instanceof  SearchFieldUI)
            ((SearchFieldUI)getUI()).loadSkin();
    }

    /**
     * Returns the name of the L&F class that renders this component.
     *
     * @return the string "TreeUI"
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    public String getUIClassID()
    {
        return uiClassID;
    }

    /**
     * Performs all needed updates when a filter query has finished.
     *
     * @param query the query that has finished
     * @param hasResults indicates if the query has results
     */
    private void filterQueryFinished(FilterQuery query, boolean hasResults)
    {
        // If the unknown contact view was previously enabled, but we
        // have found matching contacts we enter the normal view.
        enableUnknownContactView(!hasResults);

        if (hasResults &&
            !DesktopUtilActivator.getConfigurationService().user()
                                         .getBoolean(ENTER_CALLS_NUMBER, false))
            // Select the first contact if 'enter' doesn't dial the searched
            // number, otherwise the first contact would be called when 'enter'
            // is pressed.
            contactList.selectFirstContact();

        query.setQueryListener(null);
    }

    /**
     * Returns the container of this SearchField
     * @return the container of this SearchField
     */
    public SearchFieldContainer getSearchFieldContainer()
    {
        return container;
    }

    /**
     * A SearchFieldContainer is the owner of a SearchField and provides access
     * to certain utility methods.
     */
    public interface SearchFieldContainer
    {
        /**
         * Enable the unknown contact view.
         * @param isEnabled
         */
        void enableUnknownContactView(boolean isEnabled);

        /**
         * Request focus in the currently displayed contact list
         */
        void requestFocusInContactList();

        /**
         * Provide the list of Telephony Providers to the SearchField so it
         * knows to display the call button
         * @return The list of TelephonyProviders
         */
        List<ProtocolProviderService> getTelephonyProviders();

        /**
         * Allow the search field to create a phone call
         * @param number Number to call
         * @param reformatting
         */
        void createCall(String number, Reformatting reformatting);

        /**
         * Sets the visibility of the dial pad.
         *
         * @param isDialPadVisible if true, makes the dial pad visible.
         */
        void setDialPadVisibility(boolean isDialPadVisible);
    }
}
