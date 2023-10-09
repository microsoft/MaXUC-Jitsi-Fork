/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui;

import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.*;

import org.jitsi.service.configuration.ScopedConfigurationService;

import org.jitsi.util.CustomAnnotations.*;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.ServiceType;
import net.java.sip.communicator.impl.gui.main.UrlServiceTools;
import net.java.sip.communicator.impl.gui.main.account.AccountRegWizardContainerImpl;
import net.java.sip.communicator.impl.gui.main.call.AddParticipantDialog;
import net.java.sip.communicator.impl.gui.main.call.CallManager;
import net.java.sip.communicator.impl.gui.main.call.DTMFHandler;
import net.java.sip.communicator.impl.gui.main.call.DTMFHandler.DTMFToneInfo;
import net.java.sip.communicator.impl.gui.main.callpark.CallParkWindow;
import net.java.sip.communicator.impl.gui.main.chat.AddChatParticipantsDialog;
import net.java.sip.communicator.impl.gui.main.chat.ChatPanel;
import net.java.sip.communicator.impl.gui.main.chat.ChatSession;
import net.java.sip.communicator.impl.gui.main.chat.ChatTransport;
import net.java.sip.communicator.impl.gui.main.chat.ChatWindowManager;
import net.java.sip.communicator.impl.gui.main.chat.EditGroupContactDialog;
import net.java.sip.communicator.impl.gui.main.chat.ViewGroupContactDialog;
import net.java.sip.communicator.impl.gui.main.chat.conference.ConferenceChatManager;
import net.java.sip.communicator.impl.gui.main.chat.history.HistoryWindowManager;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.MultiUserChatMenu;
import net.java.sip.communicator.impl.gui.main.chat.toolBars.MultiUserChatMenu.ChangeGroupNameDialog;
import net.java.sip.communicator.impl.gui.main.contactlist.ReducedMetaContactRightButtonMenu;
import net.java.sip.communicator.impl.gui.main.contactlist.TreeContactList;
import net.java.sip.communicator.impl.gui.utils.ContactWithNumberSelectDialog;
import net.java.sip.communicator.plugin.desktoputil.SIPCommSnakeButton;
import net.java.sip.communicator.plugin.desktoputil.WindowCacher;
import net.java.sip.communicator.plugin.desktoputil.WindowUtils;
import net.java.sip.communicator.service.browserpanel.BrowserPanelDisplayer;
import net.java.sip.communicator.service.browserpanel.BrowserPanelService;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.gui.Chat;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.UIContact;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.gui.UrlCreatorEnum;
import net.java.sip.communicator.service.gui.WizardContainer;
import net.java.sip.communicator.service.managecontact.ManageContactWindow;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.ContactLogger;
import net.java.sip.communicator.util.Logger;

/**
 * An implementation of the <tt>UIService</tt> that gives access to other
 * bundles to this particular swing ui implementation.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Dmitri Melnikov
 * @author Adam Netocny
 */
public class StandardUIServiceImpl extends AbstractUIServiceImpl
{
    /**
     * The <tt>Logger</tt> used by the <tt>UIServiceImpl</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(StandardUIServiceImpl.class);

    /**
     * The <tt>ContactLogger</tt> used by this class for logging contact
     * operations in more detail
     */
    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * Config key for whether the user has accepted the latest version of all
     * the EULAs they are required to (including extra EULAs).
     */
    private static final String LATEST_EULA_ACCEPTED =
            "net.java.sip.communicator.plugin.eula.LATEST_EULA_ACCEPTED";

    /**
     * Config key for whether the client is forced to upgrade to a newer version.
     */
    private static final String FORCE_UPDATE = "net.java.sip.communicator.plugin.update.FORCE_UPDATE";

    /**
     * Config key for whether the user's password has expired.
     */
    private static final String PASSWORD_EXPIRED = "net.java.sip.communicator.plugin.pw.PASSWORD_EXPIRED";

    /**
     * Config key for whether the user's CoS allows them to use MaX UC.
     */
    private static final String COS_ALLOWS_MAX_UC = "net.java.sip.communicator.impl.commportal.COS_ALLOWS_MAX_UC";

    private AccountRegWizardContainerImpl wizardContainer;

    protected static final List<Container> supportedContainers
        = new ArrayList<>();

    static
    {
        supportedContainers.add(Container.CONTAINER_CONTACT_RIGHT_BUTTON_MENU);
        supportedContainers.add(Container.CONTAINER_GROUP_RIGHT_BUTTON_MENU);
        supportedContainers.add(Container.CONTAINER_TOOLS_MENU);
        supportedContainers.add(Container.CONTAINER_CHAT_TOOLS_MENU);
        supportedContainers.add(Container.CONTAINER_HELP_MENU);
        supportedContainers.add(Container.CONTAINER_CHAT_TOOL_BAR);
        supportedContainers.add(Container.CONTAINER_CALL_HISTORY);
        supportedContainers.add(Container.CONTAINER_MAIN_TABBED_PANE);
        supportedContainers.add(Container.CONTAINER_CHAT_HELP_MENU);
    }

    private final ChatWindowManager chatWindowManager
        = new ChatWindowManager();

    private final ConferenceChatManager conferenceChatManager
        = new ConferenceChatManager();

    private final HistoryWindowManager historyWindowManager
        = new HistoryWindowManager();

    /** The web displayer that actually displays the web window */
    private BrowserPanelDisplayer mDisplayer;

    /**
     * Initializes all frames and panels and shows the GUI.
     */
    @Override
    public void loadApplicationGui()
    {
        super.loadApplicationGui();

        this.wizardContainer = new AccountRegWizardContainerImpl(mainFrame);
    }

    /**
     * Implements <code>UIService.getSupportedContainers</code>. Returns the
     * list of supported containers by this implementation .
     *
     * @see UIService#getSupportedContainers()
     * @return an Iterator over all supported containers.
     */
    @Override
    public Iterator<Container> getSupportedContainers()
    {
        return Collections.unmodifiableList(supportedContainers).iterator();
    }

    @Override
    public void reloadContactList()
    {
        TreeContactList contactList = GuiActivator.getMainFrameContactList();

        if (contactList != null &&
            contactList.getCurrentFilter() != null)
        {
            logger.debug("Applying current filter again");
            contactList.applyFilter(contactList.getCurrentFilter());
        }
    }

    @Override
    public ChatPanel getChat(String smsNumber)
    {
        ChatPanel chatPanel = null;

        if (ConfigurationUtils.isSmsEnabled())
        {
            // If we can match the given SMS number to a MetaContact, get a chat
            // panel for that MetaContact, so that we will see that contact's
            // history and be able to SMS/IM them on all available numbers and
            // addresses.  Otherwise, just get an SMS chat to the given number.
            MetaContact metaContact = GuiActivator.getContactListService().
                                         findMetaContactForSmsNumber(smsNumber);
            String loggableSmsNumber = logHasher(smsNumber);
            if (metaContact == null)
            {

                contactLogger.debug("Opening SMS chat for " + loggableSmsNumber);
                chatPanel = chatWindowManager.getContactChat(
                                            smsNumber, true);
            }
            else
            {
                contactLogger.debug("Opening MetaContact chat for " +
                                     logHasher(metaContact.getDisplayName()) +
                                     " with SMS number " + loggableSmsNumber);
                chatPanel = chatWindowManager.getContactChat(
                                          metaContact, true);

                // Make sure we set the chat transport to the SMS number's chat
                // transport, as the user started a chat to that number.
                ChatSession chatSession = chatPanel.getChatSession();
                ChatTransport chatTransport =
                           chatSession.findChatTransportForSmsNumber(smsNumber);

                if (chatTransport != null)
                {
                    contactLogger.debug(
                                 "Setting SMS chat transport for " + loggableSmsNumber);
                    chatSession.setCurrentChatTransport(chatTransport);
                    chatPanel.setSelectedChatTransport(chatTransport);
                }
            }
        }
        else
        {
            logger.warn("Tried to start SMS chat with SMS disabled");
        }

        return chatPanel;
    }

    /**
     * Implements {@link UIService#getChat(Contact)}. If a chat for the given
     * contact exists already, returns it; otherwise, creates a new one.
     *
     * @param contact the contact that we'd like to retrieve a chat window for.
     * @return the <tt>Chat</tt> corresponding to the specified contact.
     * @see UIService#getChat(Contact)
     */
    @Override
    public ChatPanel getChat(Contact contact)
    {
        return this.getChat(contact, null);
    }

    /**
     * Implements {@link UIService#getChat(Contact)}. If a chat for the given
     * contact exists already, returns it; otherwise, creates a new one.
     *
     * @param contact the contact that we'd like to retrieve a chat window for.
     * @param smsNumber the SMS number of the last message in the panel
     *                 (may be null).
     * @return the <tt>Chat</tt> corresponding to the specified contact.
     * @see UIService#getChat(Contact)
     */
    @Override
    public ChatPanel getChat(Contact contact, String smsNumber)
    {
        MetaContact metaContact
            = GuiActivator.getContactListService()
                .findMetaContactByContact(contact);

        if(metaContact == null)
            return null;

        ChatPanel chatPanel = chatWindowManager.getContactChat(
            metaContact,
            true);

        // If an SMS number has been specified, set the chat transport to the
        // SMS number's chat transport, as the user started a chat to that number.
        if ((smsNumber != null) && (chatPanel != null))
        {
            ChatSession chatSession = chatPanel.getChatSession();
            ChatTransport chatTransport =
                           chatSession.findChatTransportForSmsNumber(smsNumber);

            if (chatTransport != null)
            {
                contactLogger.debug("Setting SMS chat transport for " +
                                                    logHasher(smsNumber));
                chatSession.setCurrentChatTransport(chatTransport);
                chatPanel.setSelectedChatTransport(chatTransport);
            }
        }

        return chatPanel;
    }

    /**
     * Returns the <tt>Chat</tt> corresponding to the given <tt>ChatRoom</tt>.
     *
     * @param chatRoom the <tt>ChatRoom</tt> for which the searched chat is
     * about.
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>ChatRoom</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @return the <tt>Chat</tt> corresponding to the given <tt>ChatRoom</tt>.
     */
    @Override
    public ChatPanel getChat(ChatRoom chatRoom, boolean create)
    {
        return chatWindowManager.getMultiChat(chatRoom, create);
    }

    /**
     * Returns the selected <tt>Chat</tt>.
     *
     * @return the selected <tt>Chat</tt>.
     */
    @Override
    public ChatPanel getCurrentChat()
    {
        return chatWindowManager.getSelectedChat();
    }

    /**
     * Changes the phone number currently entered in the phone number field.
     *
     * @param phoneNumber the phone number to enter in the phone number field.
     */
    @Override
    public void setCurrentPhoneNumber(String phoneNumber)
    {
    }

    /**
     * Implements the <code>UIService.isContainerSupported</code> method.
     * Checks if the plugable container with the given Container is supported
     * by this implementation.
     *
     * @param containerID the id of the container that we're making the query
     * for.
     *
     * @return true if the container with the specified id is exported by the
     * implementation of the UI service and false otherwise.
     *
     * @see UIService#isContainerSupported(Container)
     */
    @Override
    public boolean isContainerSupported(Container containerID)
    {
        return supportedContainers.contains(containerID);
    }

    /**
     * Implements the <code>UIService.getAccountRegWizardContainer</code>
     * method. Returns the current implementation of the
     * <tt>AccountRegistrationWizardContainer</tt>.
     *
     * @see UIService#getAccountRegWizardContainer()
     *
     * @return a reference to the currently valid instance of
     * <tt>AccountRegistrationWizardContainer</tt>.
     */
    @Override
    public WizardContainer getAccountRegWizardContainer()
    {
        return this.wizardContainer;
    }

    /**
     * Returns the chat conference manager.
     *
     * @return the chat conference manager.
     */
    public ConferenceChatManager getConferenceChatManager()
    {
        return conferenceChatManager;
    }

    @Override
    public ChatWindowManager getChatWindowManager()
    {
        return chatWindowManager;
    }

    @Override
    public HistoryWindowManager getHistoryWindowManager()
    {
        return historyWindowManager;
    }

    @Override
    public void showChatHistory(Object descriptor)
    {
        historyWindowManager.displayHistoryWindowForChatDescriptor(descriptor, logger);
    }

    @Override
    public void showGroupChatHistory(ChatRoom room)
    {
        ChatPanel chatPanel = getMultiChatPanel(room);
        ChatSession chatSession = null;
        if (chatPanel != null)
        {
             chatSession = chatPanel.getChatSession();
        }
        if (chatSession != null)
        {
            historyWindowManager.displayHistoryWindowForChatDescriptor(chatSession.getDescriptor(), logger);
        }
    }

    /**
     * Get the MetaContact corresponding to the chat.
     * The chat must correspond to a one on one conversation, otherwise this
     * method will return null.
     *
     * @param chat  The chat to get the MetaContact from
     * @return      The MetaContact corresponding to the chat or null in case
     *              it is a chat with more then one person.
     */
    @Override
    public MetaContact getChatContact(Chat chat)
    {
        MetaContact metaContact = null;
        ChatSession chatSession = ((ChatPanel) chat).getChatSession();

        if (chatSession != null)
        {
            Object contact = chatSession.getDescriptor();
            // If it is a one on one conversation this would be a MetaContact
            // If not, we are talking to more then one person and we return null
            if (contact instanceof MetaContact)
                metaContact = (MetaContact) contact;
        }

        return metaContact;
    }

    /**
     * Creates a new <tt>Call</tt> with a specific set of participants.
     * <p>
     * The current implementation provided by <tt>UIServiceImpl</tt> supports a
     * single participant at the time of this writing.
     * </p>
     *
     * @param participants an array of <tt>String</tt> values specifying the
     * participants to be included into the newly created <tt>Call</tt>
     */
    @Override
    public void createCall(Reformatting reformattingNeeded,
                           String... participants)
    {
        ScopedConfigurationService userConfig = sConfigService.user();
        boolean userInteractionAllowed = userConfig.getBoolean(LATEST_EULA_ACCEPTED, false)
            && !userConfig.getBoolean(PASSWORD_EXPIRED, false)
            && !userConfig.getBoolean(FORCE_UPDATE, false)
            && userConfig.getBoolean(COS_ALLOWS_MAX_UC, true);
        if (!userInteractionAllowed)
        {
            logger.info("User interaction is not allowed yet so don't create call");
            return;
        }

        int numParticipants = participants.length;
        if (numParticipants == 1)
        {
            CallManager.createCall(participants[0], reformattingNeeded);
        }
        else
        {
            logger.warn("Tried to create a call with " + numParticipants + " participants");
        }
    }

    @Override
    public void hangupCall(Call call)
    {
        CallManager.hangupCall(call);
    }

    @Override
    public void focusCall(Call call)
    {
        CallManager.focusCall(call);
    }

    /**
     * Starts a new <tt>Chat</tt> with a specific set of participants.
     * <p>
     * The current implementation provided by <tt>UIServiceImpl</tt> supports a
     * single participant at the time of this writing.
     * </p>
     *
     * @param participants an array of <tt>String</tt> values specifying the
     * participants to be included into the newly created <tt>Chat</tt>
     * @see UIService#startChat(String[])
     */
    @Override
    public void startChat(String[] participants)
    {
        int numParticipants = participants.length;
        if (numParticipants == 1)
        {
            String participant = participants[0];
            String loggableParticipant = logHasher(participant);
            if (participant.contains("@"))
            {
                contactLogger.debug("Starting IM chat to " + loggableParticipant);
                getChatWindowManager().startChat(participant);
            }
            else
            {
                contactLogger.debug("Starting SMS chat to " + loggableParticipant);
                getChatWindowManager().startSMSChat(participant);
            }
        }
        else
            logger.warn(
                "Tried to start a chat with " + numParticipants + " participants");
    }

    @Override
    public void startGroupChat(String chatRoomUid, boolean isClosed, String chatRoomSubject)
    {
        logger.debug("Creating new chat room: " + chatRoomSubject + ", " + chatRoomUid +
                     ", closed=" + isClosed);
        getChatWindowManager().startChatRoom(chatRoomUid, isClosed, chatRoomSubject);
    }

    /**
     * Returns a collection of all currently in progress calls.
     *
     * @return a collection of all currently in progress calls.
     */
    @Override
    public Collection<Call> getInProgressCalls()
    {
        return CallManager.getInProgressCalls();
    }

    /**
     * Returns a collection of all currently in progress video calls.
     *
     * @return a collection of all currently in progress video calls.
     */
    @Override
    public Collection<Call> getInProgressVideoCalls()
    {
        return CallManager.getInProgressVideoCalls();
    }

    @Override
    public void updateVideoButtonsEnabledState()
    {
        CallManager.updateVideoButtonsEnabledState();
    }

    @Override
    public JDialog createViewGroupContactDialog(Contact groupContact)
    {
        Window cachedWindow = WindowCacher.get(groupContact);
        JDialog dialog;

        if (!(cachedWindow instanceof JDialog))
        {
            // No cache, or cache is wrong object type (shouldn't happen but
            // better to be safe).
            // No need to hash Contact, as its toString() method does that.
            logger.debug("Creating new dialog for " + groupContact);
            dialog = new ViewGroupContactDialog(groupContact);
            WindowCacher.put(groupContact, dialog);
        }
        else
        {
            // We're already showing a dialog, continue to use that.  Even if it
            // is an edit contact window - we don't want to hide that and lose
            // any changes that the user has made.
            logger.debug("");
            dialog = (JDialog) cachedWindow;
            dialog.toFront();
        }

        return dialog;
    }

    @Override
    public JDialog createEditGroupContactDialog(Contact groupContact,
                                                String displayName,
                                                Set<MetaContact> contacts,
                                                boolean showViewOnCancel)
    {
        Window cachedWindow = WindowCacher.get(groupContact);
        JDialog dialog = null;

        // Note: (null instanceof Object) is false.
        if (cachedWindow instanceof EditGroupContactDialog)
        {
            // No need to hash Contact, as its toString() method does that.
            logger.debug("Updating edit contact dialog for " + groupContact);
            EditGroupContactDialog editDialog =
                                          (EditGroupContactDialog) cachedWindow;

            if (contacts != null)
            {
                // Add the new contacts to the edit dialog
                editDialog.addSelectedContacts(contacts);
            }

            // Save the current location.  Otherwise, when we set the dialog
            // visible again, it might change location.
            editDialog.saveSizeAndLocation();
            dialog = editDialog;
        }
        else if (cachedWindow instanceof ViewGroupContactDialog)
        {
            // We had a cached view contact dialog.  Replace it with an edit.
            // No need to hash Contact, as its toString() method does that.
            logger.debug("Replacing view contact dialog for " + groupContact);
            WindowCacher.remove(groupContact);
            cachedWindow.dispose();
        }

        if (dialog == null)
        {
            // Either we don't have a cached window, or the one we do have needs
            // to be recreated.  Do so now.
            // No need to hash Contact, as its toString() method does that.
            logger.debug("Creating new dialog for " + groupContact);
            dialog = new EditGroupContactDialog(groupContact,
                                                displayName,
                                                contacts,
                                                showViewOnCancel);
            WindowCacher.put(groupContact, dialog);
        }

        return dialog;
    }

    @Override
    public SIPCommSnakeButton getCrmLaunchButton(
            @Nullable final String searchName,
            @Nullable final String searchNumber)
    {
        SIPCommSnakeButton crmLaunchButton = null;

        UrlServiceTools ust = UrlServiceTools.getUrlServiceTools();
        if (ust.isServiceTypeEnabled(ServiceType.CRM))
        {
            logger.debug(
                "Creating CRM button for " + logHasher(searchName) +
                ": " + sanitisePeerId(searchNumber));
            crmLaunchButton = ust.createCrmLaunchButton(searchName, searchNumber);
        }

        return crmLaunchButton;
    }

    @Override
    public boolean isCrmAutoLaunchAlways()
    {
        return ServiceType.CRM.getAutoLaunchType().equals(
                                                ServiceType.AUTO_LAUNCH_ALWAYS);
    }

    @Override
    public boolean isCrmAutoLaunchExternal()
    {
        return ServiceType.CRM.getAutoLaunchType().equals(
                                              ServiceType.AUTO_LAUNCH_EXTERNAL);
    }

    @Override
    public JDialog createContactWithNumberSelectDialog(Window parent,
                                                       String title,
                                                       Consumer<UIContact> callback)
    {
        return new ContactWithNumberSelectDialog(parent, title, callback);
    }

    @Override
    public void showNewGroupChatWindow()
    {
        AddChatParticipantsDialog dialog = new AddChatParticipantsDialog(
                GuiActivator.getResources().getI18NString(
                        "service.gui.chat.CREATE_NEW_GROUP"),
                GuiActivator.getResources().getI18NString("service.gui.CREATE_GROUP_CHAT"),
                null,
                true,
                null);

        WindowUtils.makeWindowVisible(dialog, true);
    }

    /**
     * Finds a <tt>ChatPanel</tt> associated with the provided <tt>ChatRoom</tt>.
     * If the <tt>ChatRoom</tt> is <tt>null</tt>, returns null
     *
     * @param chatRoom {@link ChatRoom} The chat room to look for a chat panel.
     * @return {@link ChatPanel}
     */
    private ChatPanel getMultiChatPanel(ChatRoom chatRoom)
    {
        if (chatRoom == null)
        {
            logger.error("Cannot get a multi chat panel for a null chat room.");
            return null;
        }

        logger.debug("Looking for chat panel for existing chat room");
        ChatPanel chatPanel = getChatWindowManager().getMultiChat(chatRoom, true);

        return chatPanel;
    }

    @Override
    public void showUpdateGroupChatSubjectWindow(ChatRoom chatRoom)
    {
        ChatPanel chatPanel = getMultiChatPanel(chatRoom);

        if (chatPanel != null)
        {
            ChangeGroupNameDialog changeGroupNameDialog = new MultiUserChatMenu(chatPanel)
                                                                .getChangeGroupNameDialog();
            WindowUtils.makeWindowVisible(changeGroupNameDialog, true);
        }
    }

    @Override
    public void showAddChatParticipantsDialog(ChatRoom chatRoom)
    {
        ChatPanel chatPanel = getMultiChatPanel(chatRoom);

        if (chatPanel != null)
        {
            new AddChatParticipantsDialog(chatPanel, false, chatRoom.getMetaContactMembers())
                .setVisible(true);
        }
    }

    @Override
    public void showLeaveDialog(ChatRoom chatRoom)
    {
        ChatPanel chatPanel = getMultiChatPanel(chatRoom);

        if (chatPanel != null)
        {
            new MultiUserChatMenu(chatPanel).attemptLeaveGroupChat();
        }
    }

    @Override
    public void showRemoveDialog(MetaContact metaContact, ChatRoomMember member)
    {
        new ReducedMetaContactRightButtonMenu(metaContact, member).attemptRemoveFromGroupChat();
    }

    @Override
    public void createMainFrame()
    {
        this.mainFrame = new MainFrame();
    }

    @Override
    public void showAddUserWindow(SourceContact contact)
    {
        ManageContactWindow addContactWindow =
                GuiActivator.getAddContactWindow(contact);

        if (addContactWindow != null)
        {
            addContactWindow.setVisible(true);
            addContactWindow.focusOnWindow();
        }
    }

    @Override
    public void showEditUserWindow(MetaContact contact)
    {
        ManageContactWindow editContactWindow =
            GuiActivator.getEditContactWindow(contact);

        if (editContactWindow != null)
        {
            editContactWindow.setVisible(true);
            editContactWindow.focusOnWindow();
        }
    }

    @Override
    public void showCallParkWindow() {
        CallParkWindow.showCallParkWindow();
    }

    @Override
    public void showAddCallPeerWindow(Call call) {
        AddParticipantDialog addParticipantDialog = new AddParticipantDialog(call);
        addParticipantDialog.setVisible(true);
    }

    @Override
    public void showTransferCallWindow(CallPeer peer) {
        CallManager.openCallTransferDialog(peer);
    }

    @Override
    public void showBrowserService(UrlCreatorEnum mOption)
    {
        BrowserPanelService browserService = GuiActivator.getBrowserPanelService();
        mDisplayer = browserService.getBrowserPanelDisplayer(mOption, mOption.getNameRes());
        mDisplayer.setVisible(true);
    }

    @Override
    public boolean isLocalVideoEnabled(Call call)
    {
        return CallManager.isLocalVideoEnabled(call);
    }

    @Override
    public void enableLocalVideo(Call call, boolean enable)
    {
        CallManager.enableLocalVideo(call, enable);
    }

    @Override
    public void playDTMF(Call call, String toneValue) throws InterruptedException
    {
        DTMFHandler dtmfHandler = new DTMFHandler();
        DTMFToneInfo info = dtmfHandler.getDTMFToneInfo(toneValue);
        dtmfHandler.startSendingDtmfTone(call, info);
        // TODO REFRESH-CALL-UI: Consider if this is the best option - the Java UI code
        // listens to mouse down and up events to start and stop the dialtone, but we
        // probably don't want to do this over WISPA (we could end up with a dialtone that
        // doesn't stop until WISPA reconnects...)
        Thread.sleep(100);
        dtmfHandler.stopSendingDtmfTone(call);
    }
}
