/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChooseCallAccountDialog</tt> is the dialog shown when calling a
 * contact in order to let the user choose the account he'd prefer to use in
 * order to call this contact.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class ChooseCallAccountPopupMenu
    extends SIPCommPopupMenu
    implements Skinnable
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Phone number utils, used for format phone numbers for display in the UI.
     */
    private static final PhoneNumberUtilsService phoneNumberUtils =
                                             GuiActivator.getPhoneNumberUtils();

    /**
     * The invoker component.
     */
    private final JComponent invoker;

    /**
     * The call interface listener, which would be notified once the call
     * interface is created.
     */
    private CallInterfaceListener callInterfaceListener;

    /**
     * Creates this dialog.
     *
     * @param invoker the invoker of this pop up menu
     * @param contactToCall the contact to call
     * @param telephonyProviders a list of all possible telephony providers
     */
    public ChooseCallAccountPopupMenu(
        JComponent invoker,
        final String contactToCall,
        List<ProtocolProviderService> telephonyProviders)
    {
        this(invoker,
             contactToCall,
             telephonyProviders,
             OperationSetBasicTelephony.class,
             null);
    }

    /**
     * Creates this dialog.
     *
     * @param invoker the invoker of this pop up menu
     * @param contactToCall the contact to call
     * @param telephonyProviders a list of all possible telephony providers
     * @param opSetClass the operation set class indicating what operation
     * would be performed when a given item is selected from the menu
     * @param contactDisplayName optional - the user-visible name of the contact
     * to call.
     */
    public ChooseCallAccountPopupMenu(
        JComponent invoker,
        final String contactToCall,
        List<ProtocolProviderService> telephonyProviders,
        Class<? extends OperationSet> opSetClass,
        String contactDisplayName)
    {
        this.invoker = invoker;
        this.init(GuiActivator.getResources()
                    .getI18NString("service.gui.CALL_VIA"));

        for (ProtocolProviderService provider : telephonyProviders)
        {
            this.addTelephonyProviderItem(provider,
                                          contactToCall,
                                          opSetClass,
                                          contactDisplayName);
        }
    }

    /**
     * Creates this dialog by specifying a list of telephony contacts to choose
     * from.
     *
     * @param invoker the invoker of this pop up
     * @param telephonyObjects the list of telephony contacts to select through
     * @param opSetClass the operation class, which indicates what action would
     * be performed if an item is selected from the list
     * @param contact the contact associated with <tt>telephonyObjects</tt>
     * @param contactDisplayName the user-visible name of the contact to call.
     */
    public ChooseCallAccountPopupMenu(JComponent invoker,
                                      List<?> telephonyObjects,
                                      Class<? extends OperationSet> opSetClass,
                                      Contact contact,
                                      String contactDisplayName)
    {
        this.invoker = invoker;
        this.init(GuiActivator.getResources()
                    .getI18NString("service.gui.CHOOSE_CONTACT"));

        for (Object o : telephonyObjects)
        {
            if (o instanceof UIContactDetailImpl)
                this.addTelephonyContactItem(
                    (UIContactDetailImpl) o, opSetClass, contact, contactDisplayName);
            else if (o instanceof ChatTransport)
                this.addTelephonyChatTransportItem((ChatTransport) o,
                        opSetClass, contact, contactDisplayName);
        }
    }

    /**
     * Initializes and add some common components.
     *
     * @param infoString the string we'd like to show on the top of this
     * popup menu
     */
    private void init(String infoString)
    {
        // Need a border on non-mac clients
        if (!OSUtils.IS_MAC)
        {
            Border outside = BorderFactory.createLineBorder(Color.DARK_GRAY);
            Border inside = BorderFactory.createLineBorder(Color.WHITE, 2);
            Border border = BorderFactory.createCompoundBorder(outside, inside);
            setBorder(border);
        }

        setInvoker(invoker);

        this.add(createInfoLabel(infoString));

        this.addSeparator();

        this.setFocusable(true);
    }

    /**
     * Adds the given <tt>telephonyProvider</tt> to the list of available
     * telephony providers.
     *
     * @param telephonyProvider the provider to add.
     * @param contactString the contact to call when the provider is selected
     * @param opSetClass the operation set class indicating what action would
     * be performed when an item is selected
     * @param contactDisplayName the user-visible name of the contact to call.
     */
    private void addTelephonyProviderItem(
        final ProtocolProviderService telephonyProvider,
        final String contactString,
        final Class<? extends OperationSet> opSetClass,
        final String contactDisplayName)
    {
        final ProviderMenuItem providerItem
            = new ProviderMenuItem(telephonyProvider);

        providerItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                CallManager.createCall(
                        opSetClass,
                        providerItem.getProtocolProvider(),
                        null,
                        contactDisplayName,
                        contactString,
                        Reformatting.NEEDED);

                if (callInterfaceListener != null)
                    callInterfaceListener.callInterfaceStarted();

                ChooseCallAccountPopupMenu.this.setVisible(false);
            }
        });

        this.add(providerItem);
    }

    /**
     * Adds the given <tt>telephonyContact</tt> to the list of available
     * telephony contact.
     *
     * @param telephonyContact the telephony contact to add
     * @param opSetClass the operation set class, that indicates the action
     * that would be performed when an item is selected
     * @param contact the contact associated with the
     * <tt>telephonyContact</tt>, may be null if no contact has been
     * associated.
     * @param contactDisplayName optional - the user-visible display name of the
     * contact to call.
     */
    private void addTelephonyContactItem(
                                final UIContactDetailImpl telephonyContact,
                                final Class<? extends OperationSet> opSetClass,
                                final Contact contact,
                                final String contactDisplayName)
    {
        final ContactMenuItem contactItem = new ContactMenuItem(telephonyContact);

        contactItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                String number = telephonyContact.getAddress();

                List<ProtocolProviderService> providers;
                if (opSetClass.equals(OperationSetBasicTelephony.class))
                {
                    providers = CallManager.getAllTelephonyProviders(number);
                }
                else
                {
                    providers = AccountUtils.getAllProviders(opSetClass);
                }

                if (providers == null || providers.size() == 0)
                {
                    new ErrorDialog(null,
                        GuiActivator.getResources().getI18NString(
                            "service.gui.CALL_FAILED"),
                        GuiActivator.getResources().getI18NString(
                            "service.gui.NO_ONLINE_TELEPHONY_ACCOUNT"))
                        .showDialog();
                    return;
                }
                else if (providers.size() > 1)
                {
                    new ChooseCallAccountDialog(
                        telephonyContact.getAddress(),
                        contactDisplayName,
                        opSetClass,
                        providers,
                        Reformatting.NEEDED)
                    .setVisible(true);
                }
                else // providers size == 1
                {
                    CallManager.createCall(
                        opSetClass,
                        providers.get(0),
                        contact,
                        contactDisplayName,
                        telephonyContact.getAddress(),
                        Reformatting.NEEDED);
                }

                ChooseCallAccountPopupMenu.this.setVisible(false);
            }
        });

        String category = telephonyContact.getCategory();

        if (category != null && category.equals(ContactDetail.Category.Phone))
        {
            int index = findPhoneItemIndex();
            if (index < 0)
                add(contactItem);
            else
                insert(contactItem, findPhoneItemIndex());
        }
        else
        {
            Component lastComp = getComponent(getComponentCount() - 1);
            if (lastComp instanceof ContactMenuItem)
                category = ((ContactMenuItem) lastComp).getCategory();

            if (category != null
                && category.equals(ContactDetail.Category.Phone))
                addSeparator();

            add(contactItem);
        }
    }

    /**
     * Returns the index of a phone menu item.
     *
     * @return the index of a phone menu item
     */
    private int findPhoneItemIndex()
    {
        int index = -1;
        for (int i = getComponentCount() - 1; i > 1; i--)
        {
            Component c = getComponent(i);

            if (c instanceof ContactMenuItem)
            {
                String category = ((ContactMenuItem) c).getCategory();
                if (category == null
                    || !category.equals(ContactDetail.Category.Phone))
                continue;
            }
            else if (c instanceof JSeparator)
                index = i - 1;
            else
                return index;
        }

        return index;
    }

    /**
     * Adds the given <tt>ChatTransport</tt> to the list of available
     * telephony chat transports.
     *
     * @param telTransport the telephony chat transport to add
     * @param opSetClass the class of the operation set indicating the operation
     * to be executed in the item is selected
     * @param contact The contact to call.
     * @param contactDisplayName The user-visible display name of the contact to
     * call.
     */
    private void addTelephonyChatTransportItem(
        final ChatTransport telTransport,
        final Class<? extends OperationSet> opSetClass,
        final Contact contact,
        final String contactDisplayName)
    {
        final ChatTransportMenuItem transportItem
            = new ChatTransportMenuItem(telTransport);

        transportItem.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                CallManager.createCall(
                    opSetClass,
                    telTransport.getProtocolProvider(),
                    contact,
                    contactDisplayName,
                    telTransport.getName(),
                    Reformatting.NEEDED);

                ChooseCallAccountPopupMenu.this.setVisible(false);
            }
        });

        this.add(transportItem);
    }

    /**
     * Shows the dialog at the given location.
     *
     * @param x the x coordinate
     * @param y the y coordinate
     */
    public void showPopupMenu(int x, int y)
    {
        setLocation(x, y);
        setVisible(true);
    }

    /**
     * Shows this popup menu regarding to its invoker location.
     */
    public void showPopupMenu()
    {
        Point location = new Point(invoker.getX(),
            invoker.getY() + invoker.getHeight());

        SwingUtilities
            .convertPointToScreen(location, invoker.getParent());
        setLocation(location);
        setVisible(true);
    }

    /**
     * Creates the info label.
     *
     * @param infoString the string we'd like to show on the top of this
     * popup menu
     * @return the created info label
     */
    private Component createInfoLabel(String infoString)
    {
        return SIPCommMenuItem.createHeading(infoString);
    }

    @Override
    public void addSeparator()
    {
        add(SIPCommMenuItem.createSeparator());
    }

    /**
     * A custom menu item corresponding to a specific
     * <tt>ProtocolProviderService</tt>.
     */
    private class ProviderMenuItem
        extends SIPCommMenuItem
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private final ProtocolProviderService protocolProvider;

        public ProviderMenuItem(ProtocolProviderService protocolProvider)
        {
            this.protocolProvider = protocolProvider;
            this.setText(protocolProvider.getAccountID().getDisplayName());

            loadSkin();
        }

        public ProtocolProviderService getProtocolProvider()
        {
            return protocolProvider;
        }

        /**
         * Reloads protocol icon.
         */
        public void loadSkin()
        {
            BufferedImageFuture protocolIcon
                = protocolProvider.getProtocolIcon()
                    .getIcon(ProtocolIcon.ICON_SIZE_16x16);

            if (protocolIcon != null)
            {
                GuiActivator.getImageLoaderService().
                getIndexedProtocolIcon(protocolIcon,
                            protocolProvider)
                            .addToButton(this);
            }
        }
    }

    /**
     * A custom menu item corresponding to a specific protocol <tt>Contact</tt>.
     */
    private class ContactMenuItem
        extends SIPCommMenuItem
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private final UIContactDetailImpl uiContact;

        public ContactMenuItem(UIContactDetailImpl uiContact)
        {
            this.uiContact = uiContact;
            String itemName = uiContact.getDisplayName();

            // The display name may be returned in the format "+1234567890
            // (Type)". However, we need to format just the phone number part
            // of that nicely for display. Therefore, if we find a "(" in the
            // string we split it string into the 2 halves - the phone number
            // and the type.  We then format the phone number before rebuilding
            // the string to use as the item name.
            int typeIndex = itemName.lastIndexOf("(");
            String phoneNumber = itemName;
            String phoneType = "";

            if (typeIndex != -1)
            {
                phoneNumber = itemName.substring(0, typeIndex - 1);
                phoneType = " " + itemName.substring(typeIndex);
            }

            String formattedNumber =
                phoneNumberUtils.formatNumberForDisplay(phoneNumber);
            itemName = formattedNumber + phoneType;

            Iterator<String> labels = uiContact.getLabels();

            if (labels != null && labels.hasNext())
            {
                // Labels indicates what sort of number this detail is. We only
                // display one, so are only interested in the first
                String type = labels.next();

                // Using string concatenation is nasty, but matches what is done
                // in ContactPhoneNumberUtil.getAdditionalNumbers
                itemName += " (" + type + ")";
            }

            this.setText(itemName);
            loadSkin();
        }

        /**
         * Returns the category of the underlying contact detail.
         *
         * @return the category of the underlying contact detail
         */
        public String getCategory()
        {
            return uiContact.getCategory();
        }

        /**
         * Reloads contact icon.
         */
        public void loadSkin()
        {
            ImageIconFuture contactIcon = uiContact.getStatusIcon();

            if (contactIcon == null)
            {
                PresenceStatus status = uiContact.getPresenceStatus();

                BufferedImageFuture statusIcon = null;
                if (status != null)
                    statusIcon = Constants.getStatusIcon(status);

                if (statusIcon != null)
                    contactIcon = GuiActivator.getImageLoaderService().
                    getIndexedProtocolIcon(
                        statusIcon,
                        uiContact.getPreferredProtocolProvider(null));
            }

            if (contactIcon != null)
            {
                GuiActivator.getImageLoaderService()
                .getIndexedProtocolIcon(contactIcon.getImage(),uiContact.getPreferredProtocolProvider(null))
                .addToButton(this);
            }
        }
    }

    /**
     * A custom menu item corresponding to a specific <tt>ChatTransport</tt>.
     */
    private class ChatTransportMenuItem
        extends SIPCommMenuItem
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private final ChatTransport chatTransport;

        public ChatTransportMenuItem(ChatTransport chatTransport)
        {
            this.chatTransport = chatTransport;
            this.setText(chatTransport.getName());

            loadSkin();
        }

        /**
         * Reloads transport icon.
         */
        public void loadSkin()
        {
            PresenceStatus status = chatTransport.getStatus();
            ImageIconFuture statusIcon = status.getStatusIcon().getImageIcon();

            if (statusIcon != null)
            {
                statusIcon = GuiActivator.getImageLoaderService().
                    getIndexedProtocolIcon(
                        statusIcon.getImage(),
                        chatTransport.getProtocolProvider());
            }

            if (statusIcon != null)
            {
                statusIcon.addToButton(this);
            }
        }
    }

    /**
     * Reloads all menu items.
     */
    public void loadSkin()
    {
        Component[] components = getComponents();
        for(Component component : components)
        {
            if(component instanceof Skinnable)
            {
                Skinnable skinnableComponent = (Skinnable) component;
                skinnableComponent.loadSkin();
            }
        }
    }
}
