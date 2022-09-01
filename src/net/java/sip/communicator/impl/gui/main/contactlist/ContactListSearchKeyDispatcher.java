/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.contactlist;

import java.awt.*;
import java.awt.event.*;

import javax.swing.text.*;

import net.java.sip.communicator.plugin.desktoputil.lookandfeel.*;
import net.java.sip.communicator.service.gui.*;

/**
 * The <tt>MainKeyDispatcher</tt> is added to pre-listen KeyEvents before
 * they're delivered to the current focus owner in order to introduce a
 * specific behavior for the <tt>SearchField</tt> on top of the contact
 * list.
 *
 * @author Yana Stamcheva
 */
public class ContactListSearchKeyDispatcher
    implements KeyEventDispatcher
{
    /**
     * The keyboard focus manager.
     */
    private KeyboardFocusManager mKeyManager;

    /**
     * The contact list on which this key dispatcher works.
     */
    private ContactList mContactList;

    /**
     * The search field of this key dispatcher.
     */
    private final SearchField mSearchField;

    /**
     * The container of the contact list.
     */
    private final ContactListContainer mContactListContainer;

    /**
     * Creates an instance of <tt>MainKeyDispatcher</tt>.
     * @param keyManager the parent <tt>KeyboardFocusManager</tt>
     */
    public ContactListSearchKeyDispatcher(KeyboardFocusManager keyManager,
                                          SearchField searchField,
                                          ContactListContainer container)
    {
        mKeyManager = keyManager;
        mSearchField = searchField;
        mContactListContainer = container;
    }

    /**
     * Sets the contact list.
     *
     * @param contactList the contact list to set
     */
    public void setContactList(ContactList contactList)
    {
        mContactList = contactList;
    }

    /**
     * Dispatches the given <tt>KeyEvent</tt>.
     * @param e the <tt>KeyEvent</tt> to dispatch
     * @return <tt>true</tt> if the KeyboardFocusManager should take no
     * further action with regard to the KeyEvent; <tt>false</tt>
     * otherwise
     */
    public boolean dispatchKeyEvent(KeyEvent e)
    {
        boolean handled;

        // Setup some variables that are used in multiple conditions.
        Component focusOwner = mKeyManager.getFocusOwner();
        Component permanentFocusOwner = mKeyManager.getPermanentFocusOwner();
        Component contactListComponent = mContactList.getComponent();
        int eventType = e.getID();
        int keyCode = e.getKeyCode();
        char keyChar = e.getKeyChar();

        // IF:
        //   - This window is not the focus window
        //   OR
        //   - The focus owner is a JTextComponent other than the search field
        //   OR
        //   - The event type is not PRESSED AND not TYPED
        if ((!mContactListContainer.isFocused()) ||
            (focusOwner == null) ||
            ((focusOwner instanceof JTextComponent) &&
                (!focusOwner.equals(mSearchField))) ||
            ((eventType != KeyEvent.KEY_PRESSED) &&
                (eventType != KeyEvent.KEY_TYPED)))
        {
            // We have nothing to do here.  Allow the key event to be
            // handled elsewhere.
            handled = false;
        }
        // IF:
        //   - The key combination pressed was 'Ctrl + Enter' or 'Cmd + Enter'
        //
        // By checking for the 'Enter' keycode (as opposed to keychar), this
        // condition will only be met during key PRESSED events, not key TYPED
        // events.
        else if ((keyCode == KeyEvent.VK_ENTER) &&
                 (e.isControlDown() || e.isMetaDown()))
        {
            mContactListContainer.ctrlEnterKeyTyped();

            // Allow the key event to also be handled elsewhere.
            handled = false;
        }
        // IF:
        //   - The key pressed was 'Enter'
        //
        // Tried to make this with key bindings first, but has a problem
        // with enter key binding. When the popup menu containing call
        // contacts was opened the default keyboard manager was prioritizing
        // the window ENTER key, which will open a chat and we wanted that
        // the enter starts a call with the selected contact from the menu.
        // This is why we need to do it here and to check if the
        // permanent focus owner is equal to the focus owner, which is not
        // the case when a popup menu is opened.
        //
        // By checking for the 'Enter' keycode (as opposed to keychar), this
        // condition will only be met during key PRESSED events, not key TYPED
        // events.
        else if ((keyCode == KeyEvent.VK_ENTER) &&
                 focusOwner.equals(permanentFocusOwner))
        {
            mContactListContainer.enterKeyTyped(e);
            handled = true;
        }
        // IF:
        //   - The key pressed was one of 'Up', 'Down', 'Page Up', 'Page Down'
        //   AND
        //   - The search field has the focus
        else if (((keyCode == KeyEvent.VK_UP) ||
                  (keyCode == KeyEvent.VK_DOWN) ||
                  (keyCode == KeyEvent.VK_PAGE_UP) ||
                  (keyCode == KeyEvent.VK_PAGE_DOWN)) &&
                 mSearchField.isFocusOwner())
        {
            mContactList.selectFirstContact();
            contactListComponent.requestFocus();

            // Allow the key event to also be handled elsewhere.
            handled = false;
        }
        // IF:
        //   - The contact list is the focus owner
        //   AND
        //   - The user pressed 'escape'.
        else if (contactListComponent.isFocusOwner() &&
                 (keyCode == KeyEvent.VK_ESCAPE))
        {
            // Remove all current selections.
            mContactList.removeSelection();

            if (mSearchField.getText() != null)
            {
                mSearchField.requestFocus();
            }

            // Allow the key event to also be handled elsewhere.
            handled = false;
        }
        // IF:
        //   - The key pressed was one of 'Enter', 'Delete', 'Backspace', 'Tab',
        //     'Space'
        //   OR
        //   - The key pressed was not a unicode character
        //   OR
        //   - The key pressed was '+' or '-' AND a contact group is selected
        else if ((keyCode == KeyEvent.VK_ENTER) ||
                 (keyCode == KeyEvent.VK_DELETE) ||
                 (keyCode == KeyEvent.VK_BACK_SPACE) ||
                 (keyCode == KeyEvent.VK_TAB) ||
                 (keyCode == KeyEvent.VK_SPACE) ||
                 (keyChar == KeyEvent.VK_ENTER) ||
                 (keyChar == KeyEvent.VK_DELETE) ||
                 (keyChar == KeyEvent.VK_BACK_SPACE) ||
                 (keyChar == KeyEvent.VK_TAB) ||
                 (keyChar == '\t') ||
                 (keyChar == KeyEvent.CHAR_UNDEFINED) ||
                 (((keyChar == '+') || (keyChar == '-')) &&
                  (mContactList.getSelectedGroup() != null)))
        {
            // We have nothing to do here.  Allow the key event to be
            // handled elsewhere.
            handled = false;
        }
        // IF:
        //   - The key pressed was 'Space'
        //   AND
        //   - A contact is selected
        //   AND
        //   - The focus owner is not the search field
        else if (keyChar == KeyEvent.VK_SPACE &&
                 !focusOwner.equals(mSearchField) &&
                 mContactList.getSelectedContact() != null)
        {
            // This event is handled elsewhere so mark it as such
            handled = true;
        }
        // IF:
        //   - The focus owner is equal to the permanent focus owner
        //   AND
        //   - The focus owner is not the search field
        //   AND
        //   - The focus owner is the contact list OR The app is running in
        //     single window mode
        else if (focusOwner.equals(permanentFocusOwner) &&
                 !focusOwner.equals(mSearchField) &&
                 ((focusOwner.equals(contactListComponent))))
        {
            // Request the focus in the search field if a letter is typed.
            mSearchField.requestFocusInWindow();

            // We re-dispatch the event to search field.
            mKeyManager.redispatchEvent(mSearchField, e);

            // We don't want to dispatch further this event.
            handled = true;
        }
        else
        {
            // We have nothing to do here.  Allow the key event to be
            // handled elsewhere.
            handled = false;
        }

        return handled;
    }
}
