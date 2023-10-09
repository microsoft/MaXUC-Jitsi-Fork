/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.contactlist;

import static net.java.sip.communicator.util.PrivacyUtils.REDACTED;

import java.text.*;
import java.text.Normalizer.*;
import java.util.*;
import java.util.regex.*;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.commportal.CPCos.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>SearchFilter</tt> is a <tt>ContactListFilter</tt> that filters the
 * contact list content by a filter string.
 *
 * @author Yana Stamcheva
 */
public class SearchFilter
    implements ContactListSearchFilter, ContactQueryListener
{
    private static final Logger sLog = Logger.getLogger(SearchFilter.class);

    /**
     * If we're running in QA mode, we can log details of what the user types
     * into the search box, otherwise we can't as that may contain PII.
     */
    private final boolean mQAMode;

    /**
     * The string, which we're searching.
     */
    protected String mFilterString;

    /**
     * The pattern to filter.
     */
    protected Pattern mFilterPattern;

    /**
     * The pattern to use for filtering for MetaContacts.
     */
    private Pattern mFilterPatternMetaContact;

    /**
     * The source contact list.
     */
    protected final TreeContactList mSourceContactList;

    /**
     * The <tt>MetaContactListSource</tt> to search in.
     */
    protected final MetaContactListSource mMclSource;

    /**
     * The configuration service
     */
    private final ConfigurationService sConfigService;

    /**
     * Whether the client is allowed to view call history or not not
     */
    private boolean mCallHistoryAllowed = true;

    /**
     * Callback to inform about CoS changes.
     */
    private CPCosGetterCallback mCosCallback;

    /**
     * Creates an instance of <tt>SearchFilter</tt>.
     * @param sourceContactList the ContactList that this filter is applied to
     */
    public SearchFilter(TreeContactList sourceContactList)
    {
        mSourceContactList = sourceContactList;
        mMclSource = sourceContactList.getMetaContactListSource();
        sConfigService = GuiActivator.getConfigurationService();
        mQAMode = sConfigService.global().getBoolean(
                                   "net.java.sip.communicator.QA_MODE", false);

        listenForVoipEnabled();
    }

    /**
     * This constructor is for unit tests only - it allows us to create basic
     * tests of a search filter to apply to a contact skeleton, without the need
     * for other mock services being initiated.
     */
    public SearchFilter()
    {
        mSourceContactList = null;
        mMclSource = null;
        sConfigService = null;
        mQAMode = false;
    }

    /**
     * Must be called to free all resources owned by the SearchFilter, when it
     * is no longer required.
     */
    public void cleanup()
    {
        GuiActivator.getCosService().unregisterCallback(mCosCallback);
    }

    @Override
    public Pattern getMetaContactPattern()
    {
        return mFilterPatternMetaContact;
    }

    /**
     * Applies this filter to the default contact source.
     * @param filterQuery the query that tracks this filter.
     */
    public void applyFilter(FilterQuery filterQuery)
    {
        // Only log what the user has typed into the search box if we're
        // running in QA mode, as it might contain PII.
        String filterStringToLog = mQAMode ? mFilterString : REDACTED;
        sLog.debug("Applying search filter " + filterStringToLog);

        Iterator<UIContactSource> filterSources =
            mSourceContactList.getContactSources().iterator();

        MetaContactQuery defaultQuery =
            mMclSource.queryMetaContactSource(mFilterPatternMetaContact);

        defaultQuery.addContactQueryListener(mSourceContactList);

        // First add the MetaContactListSource
        filterQuery.addContactQuery(defaultQuery);

        // If we have stopped filtering in the meantime we return here.
        if (filterQuery.isCanceled())
            return;

        // Then we apply the filter on all its contact sources.
        while (filterSources.hasNext())
        {
            final UIContactSource filterSource = filterSources.next();
            sLog.trace("Found filter source " + filterSource);

            // Don't search in call history sources if calling is disabled,
            // as there won't be any call history.
            if (!mCallHistoryAllowed
                && filterSource.getContactSourceService().getType()
                    == ContactSourceService.CALL_HISTORY_TYPE)
            {
                sLog.trace("Not searching history: mCallHistoryAllowed = " +
                                                          mCallHistoryAllowed);
                continue;
            }

            // If we have stopped filtering in the meantime we return here.
            if (filterQuery.isCanceled())
                return;

            sLog.debug("Applying filter to source " + filterSource + ", " +
                       filterSource.getContactSourceService().getDisplayName());
            ContactQuery query = applyFilter(filterSource);

            if (query.getStatus() == ContactQuery.QUERY_IN_PROGRESS)
                filterQuery.addContactQuery(query);
        }

        // Closes this filter to indicate that we finished adding queries to it.
        if (filterQuery.isRunning())
            filterQuery.close();
        else if (!mSourceContactList.isEmpty() &&
                 !GuiActivator.enterDialsNumber())
            // Select the first contact if 'enter' doesn't dial the searched
            // number, otherwise the first contact would be called when 'enter'
            // is pressed.
            mSourceContactList.selectFirstContact();
    }

    /**
     * Applies this filter to the given <tt>contactSource</tt>.
     *
     * @param contactSource the <tt>ExternalContactSource</tt> to apply the
     * filter to
     * @return the <tt>ContactQuery</tt> that tracks this filter
     */
    protected ContactQuery applyFilter(UIContactSource contactSource)
    {
        sLog.debug("applyFilter UIContactSource");

        ContactSourceService sourceService
            = contactSource.getContactSourceService();

        ContactQuery contactQuery;
        if (sourceService instanceof ExtendedContactSourceService)
        {
          // Only log what the user has typed into the search box if we're
          // running in QA mode, as it might contain PII.
          String filterPatternToLog = mQAMode ? mFilterPattern.toString() : REDACTED;
            sLog.debug("use extended contact service filter: " + filterPatternToLog);
            contactQuery
                = ((ExtendedContactSourceService) sourceService)
                    .queryContactSource(mFilterPattern);
        }
        else
        {
          // Only log what the user has typed into the search box if we're
          // running in QA mode, as it might contain PII.
          String filterStringToLog = mQAMode ? mFilterString : REDACTED;
            sLog.debug("use basic contact service filter: " + filterStringToLog);
            contactQuery = sourceService.queryContactSource(mFilterString);
        }

        // Add first available results.
        addMatching(contactQuery.getQueryResults());

        contactQuery.addContactQueryListener(this);

        return contactQuery;
    }

    /**
     * Indicates if the given <tt>uiGroup</tt> matches this filter.
     * @param uiContact the <tt>UIGroup</tt> to check
     * @return <tt>true</tt> if the given <tt>uiGroup</tt> matches the current
     * filter, <tt>false</tt> - otherwise
     */
    public boolean isMatching(UIContact uiContact)
    {
        sLog.debug("isMatching UIContact");

        Iterator<String> searchStrings = uiContact.getSearchStrings();

        if (searchStrings != null)
        {
            while (searchStrings.hasNext())
            {
                if (isMatching(searchStrings.next()))
                    return true;
            }
        }
        return false;
    }

    /**
     * For all groups we return false. If some of the child contacts of this
     * group matches this filter the group would be automatically added when
     * the contact is added in the list.
     * @param uiGroup the <tt>UIGroup</tt> to check
     * @return false
     */
    public boolean isMatching(UIGroup uiGroup)
    {
        sLog.debug("isMatching UIGroup");
        return false;
    }

    /**
     * Creates the <tt>SearchFilter</tt> by specifying the string used for
     * filtering.
     * @param filter the String used for filtering
     */
    public void setFilterString(String filter)
    {
        mFilterString = filter;

        // First escape all special characters from the given filter string.
        mFilterString = Normalizer.normalize(mFilterString, Form.NFD)
                        .replaceAll("'|\\p{InCombiningDiacriticalMarks}+", "");

        // Split the string along the spaces so that different names are split
        // these are then recombined so that we search for (e.g. John Doe) "Jo
        // D" it searches for "Jo*" AND "D*" so either order is recognised.
        //
        // We also allow searching for characters in the middle of a contact
        // name, because for some languages (e.g. Chinese) it doesn't make sense
        // to limit our search to the first character.
        String[] splitFilterString = mFilterString.split("\\s+");
        StringBuilder combinedFilterString = new StringBuilder();

        for (String filterSubstring : splitFilterString)
        {
            combinedFilterString.append("(?=.*")
                                .append(Pattern.quote(filterSubstring))
                                .append(")");
        }

        // Then create the patterns. Different for MetaContact.
        // By default, case-insensitive matching assumes that only characters
        // in the US-ASCII charset are being matched, that's why we use
        // the UNICODE_CASE flag to enable unicode case-insensitive matching.
        // Sun Bug ID: 6486934 "RegEx case_insensitive match is broken"

        mFilterPatternMetaContact
                = Pattern.compile(
                        combinedFilterString.toString(),
                        Pattern.MULTILINE
                            | Pattern.CASE_INSENSITIVE
                            | Pattern.UNICODE_CASE);

        mFilterPattern
                = Pattern.compile(
                        Pattern.quote(mFilterString),
                        Pattern.MULTILINE
                        | Pattern.CASE_INSENSITIVE
                        | Pattern.UNICODE_CASE);
    }

    /**
     * Checks if the given <tt>contact</tt> is matching the current filter.
     * A <tt>SourceContact</tt> would be matching the filter if its display
     * name is matching the search string.
     * @param contact the <tt>ContactListContactDescriptor</tt> to check
     * @return <tt>true</tt> to indicate that the given <tt>contact</tt> is
     * matching the current filter, otherwise returns <tt>false</tt>
     */
    private boolean isMatching(SourceContact contact)
    {
        sLog.debug("isMatching SourceContact");

        return isMatching(contact.getDisplayName());
    }

    /**
     * Indicates if the given string matches this filter.
     * @param text the text to check
     * @return <tt>true</tt> to indicate that the given <tt>text</tt> matches
     * this filter, <tt>false</tt> - otherwise
     */
    private boolean isMatching(String text)
    {
        String filterStringToLog = mQAMode ? text : REDACTED;
        sLog.debug("isMatching String: " + filterStringToLog);

        if (mFilterPattern != null)
        {
            boolean retValue = mFilterPattern.matcher(text).find();
            sLog.debug("matches?: " + retValue);
            return retValue;
        }

        return true;
    }

    /**
     * Adds the list of <tt>sourceContacts</tt> to the contact list.
     * @param sourceContacts the list of <tt>SourceContact</tt>s to add
     */
    protected void addMatching(List<SourceContact> sourceContacts)
    {
      // No need to hash SourceContact, as its toString() method does that.
        sLog.debug("addMatching from: " + sourceContacts);
        sLog.debug("number of contacts: " + sourceContacts.size());

        // Take a copy to prevent Concurrent Modification Exceptions
        List<SourceContact> contacts =
                new ArrayList<>(sourceContacts);

        for (SourceContact contact : contacts)
        {
            sLog.debug("maybe add " + contact);
            addSourceContact(contact);
        }
    }

    /**
     * Adds the given <tt>sourceContact</tt> to the contact list.
     * @param sourceContact the <tt>SourceContact</tt> to add
     */
    private void addSourceContact(SourceContact sourceContact)
    {
        sLog.debug("addSourceContact");

        ContactSourceService contactSource = sourceContact.getContactSource();

        UIContactSource sourceUI
            = mSourceContactList.getContactSource(contactSource);

        if (sourceUI != null
            // ExtendedContactSourceService and Message History
            // have already matched the SourceContact over the pattern
            && (contactSource instanceof ExtendedContactSourceService ||
                contactSource.getType() == ContactSourceService.MESSAGE_HISTORY_TYPE)
                || isMatching(sourceContact))
        {
            sLog.debug("about to add");
            boolean isSorted = (sourceContact.getIndex() > -1) ? true : false;

            mSourceContactList.addContact(
                sourceUI.createUIContact(sourceContact, mMclSource),
                sourceUI.getUIGroup(),
                isSorted,
                true);
            mSourceContactList.getCurrentFilterQuery().setSucceeded(true);
        }
        else
        {
            sLog.debug("about to remove");
            sourceUI.removeUIContact(sourceContact);
        }
    }

    /**
     * Start listening to see if the class of service allows VoIP or not. If not
     * the searches should not include call history results
     */
    private void listenForVoipEnabled()
    {
        mCosCallback = new CPCosGetterCallback()
        {
            @Override
            public void onCosReceived(CPCos cos)
            {
                SubscribedMashups mashups = cos.getSubscribedMashups();

                // We could use ConfigurationUtils.isVoIPEnabledInCoS() for
                // the VoIP enabled by CoS setting, but there is a race
                // condition between the CoS storeCosFieldsInConfig() method
                // saving this setting to the config file, and this callback
                // being called (where isVoIPEnabledInCoS() would then read
                // that same config file).
                boolean voipEnabled = mashups.isVoipAllowed() &&
                    ConfigurationUtils.isVoIPEnabledByUser();
                boolean nchAllowed = cos.getCallLogEnabled() &&
                                     mashups.isNetworkCallHistoryAllowed() &&
                                     ConfigurationUtils.isNetworkCallHistoryEnabled();

                mCallHistoryAllowed = voipEnabled || nchAllowed;

                sLog.info("CoS received, enable call history search based " +
                          "on voipEnabled = " + voipEnabled + ", or " +
                          "on nchAllowed = " + nchAllowed);
            }
        };
        GuiActivator.getCosService().getClassOfService(mCosCallback);
    }

    @Override
    public void contactReceived(ContactReceivedEvent event)
    {
        // Default behaviour is to pass to the contact list.
        mSourceContactList.contactReceived(event);
    }

    @Override
    public void queryStatusChanged(ContactQueryStatusEvent event)
    {
        // Default behaviour is to pass to the contact list.
        mSourceContactList.queryStatusChanged(event);
    }

    @Override
    public void contactRemoved(ContactRemovedEvent event)
    {
        // Default behaviour is to pass to the contact list.
        mSourceContactList.contactRemoved(event);
    }

    @Override
    public void contactChanged(ContactChangedEvent event)
    {
        // Default behaviour is to pass to the contact list.
        mSourceContactList.contactChanged(event);
    }
}
