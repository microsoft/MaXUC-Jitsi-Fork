/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.chat.history.HistoryWindowManager;
import net.java.sip.communicator.impl.gui.main.contactlist.TreeContactList;
import net.java.sip.communicator.plugin.desktoputil.SIPCommSnakeButton;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.contactsource.SourceContact;
import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.service.protocol.ChatRoom;
import net.java.sip.communicator.service.protocol.ChatRoomMember;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.SecurityAuthority;
import net.java.sip.communicator.util.account.LoginManager;

/**
 * The <tt>UIService</tt> offers generic access to the graphical user interface
 * for all modules that would like to interact with the user.
 * <p>
 * Through the <tt>UIService</tt> all modules can add their own components in
 * different menus, toolbars, etc. within the ui. Each <tt>UIService</tt>
 * implementation should export its supported "plugable" containers - a set of
 * <tt>Container</tt>s corresponding to different "places" in the application,
 * where a module can add a component.
 * <p>
 * The <tt>UIService</tt> provides also methods that would allow to other
 * modules to control the visibility, size and position of the main application
 * window. Some of these methods are: setVisible, minimize, maximize, resize,
 * move, etc.
 * <p>
 * A way to show different types of simple windows is provided to allow other
 * modules to show different simple messages, like warning or error messages. In
 * order to show a simple warning message, a module should invoke the
 * getPopupDialog method and then one of the showXXX methods, which corresponds
 * best to the required dialog.
 * <p>
 * Certain components within the GUI, like "AddContact" window for example,
 * could be also shown from outside the UI bundle. To make one of these
 * component exportable, the <tt>UIService</tt> implementation should attach to
 * it an <tt>WindowID</tt>. A window then could be shown, by invoking
 * <code>getExportedWindow(WindowID)</code> and then <code>show</code>. The
 * <tt>WindowID</tt> above should be obtained from
 * <code>getSupportedExportedWindows</code>.
 *
 * @author Yana Stamcheva
 * @author Dmitri Melnikov
 * @author Adam Netocny
 * @author Lyubomir Marinov
 */
public interface UIService
{
    /**
     * Enable/disable the 'waiting' cursor on the main client frame.
     * @param enable
     */
    void enableWaitingCursor(boolean enable);

    /**
     * Returns TRUE if the application is visible and FALSE otherwise. This
     * method is meant to be used by the systray service in order to detect the
     * visibility of the application.
     *
     * @return <code>true</code> if the application is visible and
     *         <code>false</code> otherwise.
     *
     * @see #setVisible(boolean)
     */
    boolean isVisible();

    /**
     * Shows or hides the main application window depending on the value of
     * parameter <code>visible</code>. Meant to be used by the systray when it
     * needs to show or hide the application.
     *
     * @param visible if <code>true</code>, shows the main application window;
     *            otherwise, hides the main application window.
     *
     * @see #isVisible()
     */
    void setVisible(boolean visible);

    /**
     * Returns the current location of the main application window. The returned
     * point is the top left corner of the window.
     *
     * @return The top left corner coordinates of the main application window.
     */
    Point getLocation();

    /**
     * Locates the main application window to the new x and y coordinates.
     *
     * @param x The new x coordinate.
     * @param y The new y coordinate.
     */
    void setLocation(int x, int y);

    /**
     * Returns the size of the main application window.
     *
     * @return the size of the main application window.
     */
    Dimension getSize();

    /**
     * Sets the size of the main application window.
     *
     * @param width The width of the window.
     * @param height The height of the window.
     */
    void setSize(int width, int height);

    /**
     * Minimizes the main application window.
     */
    void minimize();

    /**
     * Maximizes the main application window.
     */
    void maximize();

    /**
     * Restores the main application window.
     */
    void restore();

    /**
     * Resizes the main application window with the given width and height.
     *
     * @param width The new width.
     * @param height The new height.
     */
    void resize(int width, int height);

    /**
     * Moves the main application window to the given coordinates.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     */
    void move(int x, int y);

    /**
     * Brings the focus to the main application window.
     */
    void bringToFront();

    /**
     * Returns TRUE if the application could be exited by closing the main
     * application window, otherwise returns FALSE.
     *
     * @return Returns TRUE if the application could be exited by closing the
     *         main application window, otherwise returns FALSE
     */
    boolean getExitOnMainWindowClose();

    /**
     * Returns an exported window given by the <tt>WindowID</tt>. This could be
     * for example the "Add contact" window or any other window within the
     * application. The <tt>windowID</tt> should be one of the WINDOW_XXX
     * obtained by the <tt>getSupportedExportedWindows</tt> method.
     *
     * @param windowID One of the WINDOW_XXX WindowID-s.
     * @throws IllegalArgumentException if the specified <tt>windowID</tt> is
     *             not recognized by the implementation (note that
     *             implementations MUST properly handle all WINDOW_XXX ID-s.)
     * @return the window to be shown.
     */
    ExportedWindow getExportedWindow(WindowID windowID)
        throws IllegalArgumentException;

    /**
     * Returns an exported window given by the <tt>WindowID</tt>. This could be
     * for example the "Add contact" window or any other window within the
     * application. The <tt>windowID</tt> should be one of the WINDOW_XXX
     * obtained by the <tt>getSupportedExportedWindows</tt> method.
     *
     * @param windowID One of the WINDOW_XXX WindowID-s.
     * @param params The parameters to be passed to the returned exported
     *            window.
     * @throws IllegalArgumentException if the specified <tt>windowID</tt> is
     *             not recognized by the implementation (note that
     *             implementations MUST properly handle all WINDOW_XXX ID-s.)
     * @return the window to be shown.
     */
    ExportedWindow getExportedWindow(WindowID windowID, Object[] params)
        throws IllegalArgumentException;

    /**
     * Hides the exported window given by the parameter
     */
    void hideExportedWindow(WindowID windowID);

    /**
     * Returns a configurable popup dialog, that could be used to show either a
     * warning message, error message, information message, etc. or to prompt
     * user for simple one field input or to question the user.
     *
     * @return a <code>PopupDialog</code>.
     * @see PopupDialog
     */
    PopupDialog getPopupDialog();

    /**
     * Returns the <tt>Chat</tt> corresponding to the given SMS number.
     *
     * @param smsNumber the SMS number for which the searched chat is about.
     * @return the <tt>Chat</tt> corresponding to the given SMS number.
     */
    Chat getChat(String smsNumber);

    /**
     * Returns the <tt>Chat</tt> corresponding to the given <tt>Contact</tt>.
     *
     * @param contact the <tt>Contact</tt> for which the searched chat is about.
     * @return the <tt>Chat</tt> corresponding to the given <tt>Contact</tt>.
     */
    Chat getChat(Contact contact);

    /**
     * Returns the <tt>Chat</tt> corresponding to the given <tt>Contact</tt>.
     *
     * @param contact the <tt>Contact</tt> for which the searched chat is about.
     * @param smsNumber the SMS number of the last message in the panel
     *                  (may be null).
     * @return the <tt>Chat</tt> corresponding to the given <tt>Contact</tt>.
     */
    Chat getChat(Contact contact, String smsNumber);

    /**
     * Returns the <tt>Chat</tt> corresponding to the given <tt>ChatRoom</tt>.
     *
     * @param chatRoom the <tt>ChatRoom</tt> for which the searched chat is
     *            about.
     * @param create <tt>true</tt> to create a <tt>ChatPanel</tt> corresponding
     * to the specified <tt>ChatRoom</tt> if such <tt>ChatPanel</tt> does not
     * exist yet
     * @return the <tt>Chat</tt> corresponding to the given <tt>ChatRoom</tt>.
     */
    Chat getChat(ChatRoom chatRoom, boolean create);

    /**
     * Get the MetaContact corresponding to the chat.
     * The chat must correspond to a one on one conversation. If it is a
     * group chat an exception will be thrown.
     *
     * @param chat  The chat to get the MetaContact from
     * @return      The MetaContact corresponding to the chat.
     */
    MetaContact getChatContact(Chat chat);

    /**
     * Returns the selected <tt>Chat</tt>.
     *
     * @return the selected <tt>Chat</tt>.
     */
    Chat getCurrentChat();

    /**
     * Sets the phone number in the phone number field. This method is meant to
     * be used by plugins that are interested in operations with the currently
     * entered phone number.
     *
     * @param phoneNumber the phone number to enter.
     */
    void setCurrentPhoneNumber(String phoneNumber);

    /**
     * Returns a default implementation of the <tt>SecurityAuthority</tt>
     * interface that can be used by non-UI components that would like to launch
     * the registration process for a protocol provider. Initially this method
     * was meant for use by the systray bundle and the protocol URI handlers.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for which
     *            the authentication window is about.
     *
     * @return a default implementation of the <tt>SecurityAuthority</tt>
     *         interface that can be used by non-UI components that would like
     *         to launch the registration process for a protocol provider.
     */
    SecurityAuthority getDefaultSecurityAuthority(
            ProtocolProviderService protocolProvider);

    /**
     * Returns an iterator over a set of windowID-s. Each <tt>WindowID</tt>
     * points to a window in the current UI implementation. Each
     * <tt>WindowID</tt> in the set is one of the constants in the
     * <tt>ExportedWindow</tt> interface. The method is meant to be used by
     * bundles that would like to have access to some windows in the gui - for
     * example the "Add contact" window, the "Settings" window, the
     * "Chat window", etc.
     *
     * @return Iterator An iterator to a set containing WindowID-s representing
     *         all exported windows supported by the current UI implementation.
     */
    Iterator<WindowID> getSupportedExportedWindows();

    /**
     * Checks if a window with the given <tt>WindowID</tt> is contained in the
     * current UI implementation.
     *
     * @param windowID one of the <tt>WindowID</tt>-s, defined in the
     *            <tt>ExportedWindow</tt> interface.
     * @return <code>true</code> if the component with the given
     *         <tt>WindowID</tt> is contained in the current UI implementation,
     *         <code>false</code> otherwise.
     */
    boolean isExportedWindowSupported(WindowID windowID);

    /**
     * Returns the <tt>WizardContainer</tt> for the current UIService
     * implementation. The <tt>WizardContainer</tt> is meant to be implemented
     * by the UI service implementation in order to allow other modules to add
     * to the GUI <tt>AccountRegistrationWizard</tt> s. Each of these wizards is
     * made for a given protocol and should provide a sequence of user interface
     * forms through which the user could register a new account.
     *
     * @return Returns the <tt>AccountRegistrationWizardContainer</tt> for the
     *         current UIService implementation.
     */
    WizardContainer getAccountRegWizardContainer();

    /**
     * Returns an iterator over a set containing containerID-s pointing to
     * containers supported by the current UI implementation. Each containerID
     * in the set is one of the CONTAINER_XXX constants. The method is meant to
     * be used by plugins or bundles that would like to add components to the
     * user interface. Before adding any component they should use this method
     * to obtain all possible places, which could contain external components,
     * like different menus, toolbars, etc.
     *
     * @return Iterator An iterator to a set containing containerID-s
     *         representing all containers supported by the current UI
     *         implementation.
     */
    Iterator<Container> getSupportedContainers();

    /**
     * Checks if the container with the given <tt>Container</tt> is supported
     * from the current UI implementation.
     *
     * @param containerID One of the CONTAINER_XXX Container-s.
     * @return <code>true</code> if the container with the given
     *         <tt>Container</tt> is supported from the current UI
     *         implementation, <code>false</code> otherwise.
     */
    boolean isContainerSupported(Container containerID);

    /**
     * Determines whether the Mac OS X screen menu bar is being used by the UI
     * for its main menu instead of the Windows-like menu bars at the top of the
     * windows.
     * <p>
     * A common use of the returned indicator is for the purposes of
     * platform-sensitive UI since Mac OS X employs a single screen menu bar,
     * Windows and Linux/GTK+ use per-window menu bars and it is inconsistent on
     * Mac OS X to have the Window-like menu bars.
     * </p>
     *
     * @return <tt>true</tt> if the Mac OS X screen menu bar is being used by
     *         the UI for its main menu instead of the Windows-like menu bars at
     *         the top of the windows; otherwise, <tt>false</tt>
     */
    boolean useMacOSXScreenMenuBar();

    /**
     * Returns the <tt>ConfigurationContainer</tt> associated with this
     * <tt>UIService</tt>.
     *
     * @return the <tt>ConfigurationContainer</tt> associated with this
     * <tt>UIService</tt>
     */
    ConfigurationContainer getConfigurationContainer();

    /**
     * Returns the <tt>HistoryWindowManager</tt>.
     * @return the <tt>HistoryWindowManager</tt>
     */
    HistoryWindowManager getHistoryWindowManager();

    /**
     * Shows a chat history window for the passed chat session descriptor
     * @param descriptor chat session descriptor, e.g. smsNumber, MetaContact
     */
    void showChatHistory(Object descriptor);

    /**
     * Shows a chat history window for the passed chat room
     * @param room ChatRoom to show history for
     */
    void showGroupChatHistory(ChatRoom room);

    /**
     * Returns the create account window.
     *
     * @return the create account window
     */
    CreateAccountWindow getCreateAccountWindow();

    /**
     * Adds the given <tt>WindowListener</tt> listening for events triggered
     * by the main UIService component. This is normally the main application
     * window component, the one containing the contact list. This listener
     * would also receive events when this window is shown or hidden.
     * @param l the <tt>WindowListener</tt> to add
     */
    void addWindowListener(WindowListener l);

    /**
     * Repaints and revalidates the whole UI. This method is meant to be used
     * to runtime apply a skin and refresh automatically the user interface.
     */
    void repaintUI();

    /**
     * Creates a new <tt>Call</tt> with a specific set of participants.
     *
     * @param reformatNeeded whether the numbers need to be reformatted
     * @param participants an array of <tt>String</tt> values specifying the
     * participants to be included into the newly created <tt>Call</tt>
     */
    void createCall(Reformatting reformatNeeded,
                    String... participants);

    /**
     * Hangs up the given call
     *
     * @param call the call to hang up
     */
    void hangupCall(Call call);

    /**
     * Focus the window of a given call if exists
     *
     * @param call the call to focus
     */
    void focusCall(Call call);

    /**
     * Starts a new <tt>Chat</tt> with a specific set of participants.
     *
     * @param participants an array of <tt>String</tt> values specifying the
     * participants to be included into the newly created <tt>Chat</tt>
     */
    void startChat(String[] participants);

    /**
     * Opens a chat room. If a chat room already exists with the given chat
     * room UID then that is opened, otherwise a history view of the given chat
     * room UID is shown
     *
     * @param chatRoomUid the chat room uid to open
     * @param isClosed whether the user has previously left the chat room
     * @param chatRoomSubject the last known subject of this chat room
     */
    void startGroupChat(String chatRoomUid,
                        boolean isClosed,
                        String chatRoomSubject);

    /**
     * Creates a contact list component.
     *
     * @param clContainer the parent contact list container
     * @return the created <tt>ContactList</tt>
     */
    TreeContactList createContactListComponent(
            ContactListContainer clContainer);

    /**
     * Returns a collection of all currently in progress calls.
     *
     * @return a collection of all currently in progress calls.
     */
    Collection<Call> getInProgressCalls();

    /**
     * Returns a collection of all currently in progress video calls.
     *
     * @return a collection of all currently in progress video calls.
     */
    Collection<Call> getInProgressVideoCalls();

    /**
     * Updates the enabled state of the video buttons in all in progress call
     * frames. If any video devices are available, the buttons will be
     * enabled, otherwise they will be disabled.
     */
    void updateVideoButtonsEnabledState();

    /**
     * Alerts that an event has occurred (e.g. a message has been received or
     * a call has been missed) in the window specified by the parameter, using
     * a platform-dependent visual clue such as flashing it in the task bar on
     * Windows and Linux.
     *
     * @param window The window to alert.
     */
    void alertWindow(Window window);

    /**
     * Returns the LoginManager.
     *
     * @return the LoginManager
     */
    LoginManager getLoginManager();

    /**
     * Reloads the current contact list.  Usually called when there is reason to
     * believe that the current list is showing out of date information.
     */
    void reloadContactList();

    /**
     * Returns the index of the given protocol provider service within the UI
     * so as to provide consistency across the app
     *
     * @param pps the protocol provider service to look up
     * @return the index of the protocol provider service in the UI
     */
    int getProviderUIIndex(ProtocolProviderService pps);

    /**
     * Get the default font for the app
     *
     * @return the default font for the app
     */
    Font getDefaultFont();

    /**
     * Create a dialog that can be used to view the members of a group contact.
     * Note that this does not show the new dialog.
     *
     * Must be called on the EDT!
     * @param groupContact the group contact
     */
    JDialog createViewGroupContactDialog(Contact groupContact);

    /**
     * Create a dialog that can be used to create a new group contact or edit
     * an existing group contact. Note that this does not show the new dialog.
     *
     * Must be called on the EDT!
     * @param groupContact the group contact to edit, if null a new group
     * contact will be created.
     * @param defaultDisplayName the displayName to pre-populate the name field
     * with - if null, the field will use the current display name of the
     * group contact provided.  If the group contact is null, the field will be
     * left blank.
     * @param preSelectedContacts a list of contacts to preselect in the dialog
     * (may be null)
     * @param showViewOnCancel if true, display the 'view contact' dialog for
     * the edited group contact when the edit dialog is cancelled.
     */
    JDialog createEditGroupContactDialog(Contact groupContact,
                                         String defaultDisplayName,
                                         Set<MetaContact> preSelectedContacts,
                                         boolean showViewOnCancel);

    /**
     * Constructs a dialog which allows the user to choose a contact or enter
     * a number to be passed to a callback which consumes a UIContact.
     */
    JDialog createContactWithNumberSelectDialog(
            Window parent,
            String title,
            Consumer<UIContact> callback);

    /** Invokes the window for creating a new group chat. */
    void showNewGroupChatWindow();

    /** Invokes the window for changing a group chat's subject. */
    void showUpdateGroupChatSubjectWindow(ChatRoom chatRoom);

    /** Invokes the window for adding participants to a group chat. */
    void showAddChatParticipantsDialog(ChatRoom chatRoom);

    /** Invokes the window for leaving a group chat. */
    void showLeaveDialog(ChatRoom chatRoom);

    /** Invokes the window for removing the given participant from a group chat. */
    void showRemoveDialog(MetaContact metaContact, ChatRoomMember member);

    /**
     * Shows the given window stacked beneath any other stackable alert windows
     * that are currently being displayed.
     * @param window
     */
    void showStackableAlertWindow(Window window);

    /**
     * Gets a button that will launch an CRM search when clicked.
     *
     * @param searchName The name on which to search.
     * @param searchNumber The number on which to search.
     *
     * @return the button.
     */
    SIPCommSnakeButton getCrmLaunchButton(
            final String searchName,
            final String searchNumber,
            final boolean madeFromCall);

    /**
     * @return true if CRM is configured to always auto-launch, false
     * otherwise.
     */
    boolean isCrmAutoLaunchAlways();

    /**
     * @return true if CRM is configured to auto-launch for external numbers,
     * false otherwise.
     */
    boolean isCrmAutoLaunchExternal();

    /**
     * Opens the add contact window, possibly receiving a meta contact should it
     * be an action from adding a non-contact.
     *
     * @param contact - the <tt>SourceContact</tt> to populate the dialog
     * @implNote The {@link TreeContactList#showAddContactDialog} method works
     * in a similar way, except it sets the selected account for the contact,
     * which is not necessary here.
     */
    void showAddUserWindow(SourceContact contact);

    // Opens the edit user window receiving a MetaContact
    void showEditUserWindow(MetaContact metaContact);

    /** Opens the callpark window */
    void showCallParkWindow();

    /** Opens the add call peer window */
    void showAddCallPeerWindow(Call call);

    /** Opens the transfer call window */
    void showTransferCallWindow(CallPeer peer);

    /**
     * Indicates if local video  is currently enabled for the given<tt>call</tt>.
     *
     * @param call the <tt>Call</tt>, for which we would to check if local
     * video is currently enabled
     * @return <tt>true</tt> if local video is currently enabled for the
     * given <tt>call</tt>, <tt>false</tt> otherwise
     */
    boolean isLocalVideoEnabled(Call call);

    /**
     * Enables/disables local video for a specific <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> to enable/disable to local video for
     * @param enable <tt>true</tt> to enable the local video; otherwise,
     * <tt>false</tt>
     */
    void enableLocalVideo(Call call, boolean enable);

    /** Play DTMF in call
     * @throws InterruptedException*/
    void playDTMF(Call call, String toneValue) throws InterruptedException;

    /** Opens a service in the browser
     * @param mOption - the Option that should be open in the browser
     * */
    void showBrowserService(UrlCreatorEnum mOption);

    void showBugReport();

    /**
     * A callback that can be passed into showSelectMultiIMContactsDialog to be
     * executed when the list of contacts has been selected.
     */
    interface ContactSelectorCallback
    {
        /**
         * Method that is executed when the list of contacts has been selected
         * in a selectMultiIMContactsDialog.
         *
         * @param contacts the contacts that have been selected in the dialog
         */
        void onContactsReceived(Set<MetaContact> contacts);
    }

    /**
     * Enum to help work out if reformatting is needed.
     */
    enum Reformatting
    {
        /**
         * Reformatting is not needed
         */
        NOT_NEEDED,

        /**
         * REFORMATTING TO E164 or ELC is needed
         */
        NEEDED
    }
}
