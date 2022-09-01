/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.utils;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.account.*;

/**
 * The invite dialog is a widget that shows a list of contacts, from which the
 * user could pick in order to create a conference chat or call.
 *
 * @author Yana Stamcheva
 */
public class ContactInviteDialog
    extends SIPCommDialog
    implements ContactListContainer
{
    private static final long serialVersionUID = 0L;
    private static final Logger sLog =
        Logger.getLogger(ContactInviteDialog.class);

    /**
     * The information text area.
     */
    private final JTextArea mInfoTextArea;

    /**
     * The label containing the icon of this dialog.
     */
    private final JLabel mInfoIconLabel = new JLabel();

    /**
     * The 'positive' button that carries out any actions selected by the user
     * in the dialog.
     */
    protected final JButton mOkButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.OK"));

    /**
     * The 'negative' button that disposes the dialog and does any tidying up
     * but doesn't carry out any positive actions selected by the user in the
     * dialog.
     */
    protected final JButton mCancelButton = new JButton(
        GuiActivator.getResources().getI18NString("service.gui.CANCEL"));

    /**
     * The contact list.
     */
    protected TreeContactList mContactList;

    /**
     * The contact list's MetaContact list source.
     */
    protected MetaContactListSource mMetaContactListSource;

    /**
     * A set containing the protocol providers who were registered for
     * OperationSetPresence when the contact list was created.  This is used to
     * register and unregister contact presence change listeners to ensure we
     * always display up to date presence for contacts in the list.
     */
    private final Set<ProtocolProviderService> mPresenceProviders =
            new HashSet<>();

    /**
     * The search field.
     */
    protected SearchField mSearchField;

    /**
     * The northern panel in this dialog.
     */
    protected TransparentPanel mNorthPanel;

    /**
     * The panel that contains the list of contacts in this dialog.
     */
    protected TransparentPanel mListPanel;

    /**
     * The panel that contains all UI elements in this dialog.
     */
    protected TransparentPanel mMainPanel;

    /**
     * The panel at the bottom of this dialog.
     */
    protected TransparentPanel bottomPanel;

    /**
     * The panel containing the buttons in the bottomPanel of this dialog.
     */
    protected TransparentPanel buttonsPanel;

    /**
     * Whether double clicking on a contact is enabled
     */
    protected boolean mDoubleClickEnabled = true;

    /**
     * Keyboard Focus Manager.
     */
    KeyboardFocusManager mKeyManager;

    /**
     * Key dispatcher.
     */
    ContactListSearchKeyDispatcher mClKeyDispatcher;

    /**
     * Filter for list of contacts.
     */
    InviteContactListFilter mInviteFilter;

    /**
     * Constructs an <tt>OneChoiceInviteDialog</tt>.
     * @param parent the parent of this dialog, on which this dialog is
     *               centered and which, when closed, causes this dialog to
     *               be closed too
     * @param title the title to show on the top of this dialog
     */
    public ContactInviteDialog(Window parent, String title)
    {
        super(parent, false);

        setModal(true);

        setTitle(title);

        mMainPanel = new TransparentPanel(ScaleUtils.createBorderLayout(5, 0));

        mNorthPanel = new TransparentPanel(ScaleUtils.createBorderLayout(10, 0));
        mNorthPanel.setBorder(ScaleUtils.createEmptyBorder(5, 0, 5, 0));

        setPreferredSize(new ScaledDimension(300, 475));
        setMinimumSize(new ScaledDimension(300, 475));

        mMainPanel.setBorder(BorderFactory.createEmptyBorder());
        mInfoTextArea = createInfoArea();

        mNorthPanel.add(mInfoIconLabel, BorderLayout.WEST);
        mNorthPanel.add(mInfoTextArea, BorderLayout.CENTER);

        bottomPanel = new TransparentPanel(new BorderLayout());

        buttonsPanel = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT));
        Border insideBorder = ScaleUtils.createEmptyBorder(5, 0, 5, 0);
        MatteBorder outsideBorder =
            BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY);
        bottomPanel.setBorder(
            BorderFactory.createCompoundBorder(outsideBorder, insideBorder));

        if (OSUtils.IS_MAC)
        {
            buttonsPanel.add(mCancelButton);
            buttonsPanel.add(mOkButton);
        }
        else
        {
            buttonsPanel.add(mOkButton);
            buttonsPanel.add(mCancelButton);
        }

        mOkButton.setOpaque(false);
        mCancelButton.setOpaque(false);

        ScaleUtils.scaleFontAsDefault(mOkButton);
        ScaleUtils.scaleFontAsDefault(mCancelButton);

        bottomPanel.add(buttonsPanel, BorderLayout.EAST);

        this.getRootPane().setDefaultButton(mOkButton);
        mOkButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.OK"));
        mCancelButton.setMnemonic(
            GuiActivator.getResources().getI18nMnemonic("service.gui.CANCEL"));

        Component contactListComponent = createContactListComponent();

        initSearchField();

        mListPanel = new TransparentPanel(new BorderLayout());
        mListPanel.add(contactListComponent, BorderLayout.CENTER);
        mListPanel.add(mSearchField, BorderLayout.NORTH);

        mSearchField.setBorder(ScaleUtils.createEmptyBorder(0, 7, 0, 7));

        mMainPanel.add(mNorthPanel, BorderLayout.NORTH);
        mMainPanel.add(mListPanel, BorderLayout.CENTER);
        mMainPanel.add(bottomPanel, BorderLayout.SOUTH);

        getContentPane().add(mMainPanel);

        requestFocus();
        toFront();

        mKeyManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();

        mClKeyDispatcher = new ContactListSearchKeyDispatcher(mKeyManager,
                                                              mSearchField,
                                                              this);

        mClKeyDispatcher.setContactList(mContactList);

        mKeyManager.addKeyEventDispatcher(mClKeyDispatcher);
    }

    @Override
    public void dispose()
    {
        if (mKeyManager != null)
        {
            mKeyManager.removeKeyEventDispatcher(mClKeyDispatcher);
        }

        mKeyManager = null;
        mClKeyDispatcher = null;

        if (mInviteFilter != null)
        {
            mInviteFilter.cleanup();
        }

        synchronized (mPresenceProviders)
        {
            // Make sure we don't leak contact presence status listeners
            for (ProtocolProviderService presenceProvider : mPresenceProviders)
            {
                OperationSetPresence presenceOpSet =
                    presenceProvider.getOperationSet(OperationSetPresence.class);

                if (presenceOpSet != null)
                {
                    presenceOpSet.removeContactPresenceStatusListener(
                                                        mMetaContactListSource);
                }
            }
        }

        super.dispose();
    }

    /**
     * Initialises the search field in this contact list
     */
    protected void initSearchField()
    {
        mInviteFilter = new InviteContactListFilter(mContactList);

        mContactList.setCurrentFilter(mInviteFilter);

        mSearchField = new SearchField(null, mInviteFilter, false);
        mSearchField.setPreferredSize(new ScaledDimension(200, 30));
        mSearchField.setContactList(mContactList);
        mSearchField.addFocusListener(new FocusAdapter()
        {
            /**
             * Removes all other selections.
             * @param e the <tt>FocusEvent</tt> that notified us
             */
            public void focusGained(FocusEvent e)
            {
                mContactList.removeSelection();
            }
        });
    }

    /**
     * Returns an enumeration of the list of selected <tt>MetaContact</tt>s.
     * @return an enumeration of the list of selected <tt>MetaContact</tt>s
     */
    public UIContact getSelectedContact()
    {
        return mContactList.getSelectedContact();
    }

    /**
     * Returns an enumeration of the list of selected Strings.
     * @return an enumeration of the list of selected Strings
     */
    public String getSelectedString()
    {
        return mSearchField.getText();
    }

    /**
     * Sets the information text explaining how to use the containing form.
     * @param text the text
     */
    public void setInfoText(String text)
    {
        mInfoTextArea.setText(text);
        mInfoTextArea.setVisible(true);
    }

    /**
     * Return the phone number entered into the search field by the user.  This
     * will be null if the text entered by the user is not a phone number.
     *
     * @return The phone number entered into the search field
     */
    public String getPhoneNumber()
    {
        return mSearchField.getPhoneNumber();
    }

    /**
     * Sets the icon shown in the left top corner of this dialog.
     * @param icon the icon
     */
    public void setIcon(ImageIconFuture icon)
    {
        icon.addToLabel(mInfoIconLabel);
    }

    /**
     * Sets the text of the ok button.
     * @param text the text of the ok button
     */
    public void setOkButtonText(String text)
    {
        mOkButton.setText(text);
    }

    /**
     * Sets the topmost panel in this dialog. This allows subclasses to insert
     * an explanatory panel before the contact list if desired.
     *
     * @param panel the panel to set
     */
    public void setTopPanel(JPanel panel)
    {
        mNorthPanel.add(panel, BorderLayout.NORTH);
    }

    /**
     * Adds an <tt>ActionListener</tt> to the contained "Invite" button.
     * @param l the <tt>ActionListener</tt> to add
     */
    public void addOkButtonListener(ActionListener l)
    {
        mOkButton.addActionListener(l);
    }

    /**
     * Adds an <tt>ActionListener</tt> to the contained "Cancel" button.
     * @param l the <tt>ActionListener</tt> to add
     */
    public void addCancelButtonListener(ActionListener l)
    {
        mCancelButton.addActionListener(l);
    }

    /**
     * Closes this dialog by clicking on the "Cancel" button.
     * @param isEscaped indicates if this <tt>close</tt> is provoked by an
     * escape
     */
    protected void close(boolean isEscaped)
    {
        mCancelButton.doClick();
    }

    /**
     * Creates the an info text area.
     * @return the created <tt>JTextArea</tt>
     */
    private JTextArea createInfoArea()
    {
        JTextArea mInfoTextArea = new JTextArea();

        ScaleUtils.scaleFontAsDefault(mInfoTextArea);
        mInfoTextArea.setLineWrap(true);
        mInfoTextArea.setOpaque(false);
        mInfoTextArea.setWrapStyleWord(true);
        mInfoTextArea.setEditable(false);

        // Start invisible unless we have some text to display:
        mInfoTextArea.setVisible(false);
        mInfoTextArea.setBorder(ScaleUtils.createEmptyBorder(7, 0, 7, 0));

        return mInfoTextArea;
    }

    /**
     * Creates the contact list component.
     * @return the created contact list component
     */
    private Component createContactListComponent()
    {
        mContactList =
            GuiActivator.getUIService().createContactListComponent(this);

        // Add the contact list's contact list source as a contact presence
        // change listener so that the presence of contacts in the dialog will
        // update in real time.
        mMetaContactListSource = mContactList.getMetaContactListSource();

        synchronized (mPresenceProviders)
        {
            mPresenceProviders.addAll(AccountUtils.getOpSetRegisteredProviders(
                OperationSetPresence.class, null, null));
            for (ProtocolProviderService presenceProvider : mPresenceProviders)
            {
                OperationSetPresence presenceOpSet =
                    presenceProvider.getOperationSet(OperationSetPresence.class);

                if (presenceOpSet != null)
                {
                    presenceOpSet.addContactPresenceStatusListener(mMetaContactListSource);
                }
            }
        }

        mContactList.setContactButtonsVisible(false);
        mContactList.setRightButtonMenuEnabled(false);
        mContactList.addContactListListener(new ContactListListener()
        {
            public void groupSelected(ContactListEvent evt) {}

            public void groupClicked(ContactListEvent evt) {}

            public void contactSelected(ContactListEvent evt) {}

            public void contactClicked(ContactListEvent evt)
            {
                int clickCount = evt.getClickCount();

                if (clickCount > 1 && mDoubleClickEnabled)
                {
                    // Ensure that the clicked contact is selected before we
                    // trigger the OK button, as the click handler code may use
                    // the currently selected contact as an indicator of which
                    // contact was clicked.
                    UIContact contact = evt.getSourceContact();
                    sLog.user("Contact double-clicked: " + contact);
                    mContactList.setSelectedContact(contact, false);
                    mOkButton.doClick();
                }
            }
        });

        // By default we set the current filter to be the presence filter.
        JScrollPane contactListScrollPane = new JScrollPane();

        contactListScrollPane.setOpaque(false);
        contactListScrollPane.getViewport().setOpaque(false);
        contactListScrollPane.getViewport().add(mContactList.getComponent());
        contactListScrollPane.getViewport().setBorder(null);
        contactListScrollPane.setViewportBorder(null);
        Border outsideBorder = ScaleUtils.createEmptyBorder(7, 0, 0, 0);
        MatteBorder insideBorder = BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY);
        contactListScrollPane.setBorder(BorderFactory.createCompoundBorder(outsideBorder, insideBorder));

        return contactListScrollPane;
    }

    /**
     * Adds the given contact to this contact list.
     *
     * @param contact
     */
    protected void addContact(UIContact contact)
    {
        mContactList.addContact(contact, null, true, false);
    }

    /**
     * Called when the ENTER key was typed when this container was the focused
     * container. Performs the appropriate actions depending on the current
     * state of the contained contact list.
     * @param evt
     */
    public void enterKeyTyped(KeyEvent evt)
    {
        String phoneNumber = getPhoneNumber();

        if (mContactList.getSelectedContact() != null)
        {
            // A contact has been selected - press ok for it.
            mOkButton.doClick();
        }
        else if (phoneNumber == null ||
                 phoneNumber.length() == 0)
        {
            // User hasn't entered a phone number, so just select the first
            // matching contact
            mContactList.selectFirstContact();
        }
        else
        {
            // There is an entered number - dial it
            mOkButton.doClick();
        }
    }

    /**
     * Returns the text currently shown in the search field.
     * @return the text currently shown in the search field
     */
    public String getCurrentSearchText()
    {
        return mSearchField.getText();
    }

    /**
     * Clears the current text in the search field.
     */
    public void clearCurrentSearchText()
    {
        mSearchField.setText("");
    }

    /**
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when this
     * container was the focused container. Performs the appropriate actions
     * depending on the current state of the contained contact list.
     */
    public void ctrlEnterKeyTyped() {}
}
