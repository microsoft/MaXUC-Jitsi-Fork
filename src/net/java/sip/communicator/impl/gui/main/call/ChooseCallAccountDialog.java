/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import com.explodingpixels.macwidgets.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.UIService.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.Logger;

/**
 * A dialog allowing the user to choose a specific account from a given
 * list.
 *
 * @author Yana Stamcheva
 */
public class ChooseCallAccountDialog
{
    private static final Logger logger = Logger.getLogger(ChooseCallAccountDialog.class);

    /**
     * Call label for display name.
     */
    private JLabel callLabelDisplayName;

    /**
     * The window handling received calls.
     */
    private final Window preCallWindow;

    /**
     * The last dialog that we might be displaying, could be null
     */
    private static ChooseCallAccountDialog lastDialog;

    /**
     * The contact address to be called after an account has been chosen.
     */
    private final String contactAddress;

    /**
     * The display name of the contact to be called after an account has been
     * chosen.
     */
    private final String contactDisplayName;

    /**
     * The operation set class that specifies the operation we're going to make.
     */
    private final Class<? extends OperationSet> opSetClass;

    /**
     * Non-null if a preferred provider exists.  In this case the dialog won't
     * show itself, and will instead act as though the user selected it
     */
    private final ProtocolProviderService preferredProvider;

    /**
     * The providers that exist to make a call
     */
    private final List<ProtocolProviderService> providers;

    /**
     * The provider that has been selected by the user
     */
    private ProtocolProviderService selectedProvider;

    /**
     * Whether the number needs reformatting (with E164 or ELC) before
     * dialing.
     */
    private final Reformatting mReformattingNeeded;

    /**
     * Creates an instance of <tt>ChooseCallAccountDialog</tt>.  Note that this
     * dialog will only be shown if there isn't a preferred provider for the
     * op set stored in config.  If there is a preferred provider, then the
     * dialog will act as if that provider was selected.
     *
     * @param contactAddress the contact address to be called after an account
     * has been chosen
     * @param displayName the display name of the contact to call (optional).
     * @param opSetClass the operation set class that specifies the operation
     * we're going to make
     * @param providers the list of providers to choose from
     * @param reformattingNeeded whether the number needs reformatting (with
     * E164 or ELC) before dialing.
     */
    public ChooseCallAccountDialog(String contactAddress,
                                   String displayName,
                                   Class<? extends OperationSet> opSetClass,
                                   List<ProtocolProviderService> providers,
                                   Reformatting reformattingNeeded)
    {
        preCallWindow = createPreCallWindow(
            GuiActivator.getResources().getI18NString(
                "impl.protocol.commportal.SELECT_CALLING_METHOD"),
            GuiActivator.getResources().getI18NString(
                "service.gui.CHOOSE_ACCOUNT"));

        logger.debug("Created pre call window " + preCallWindow);
        if (lastDialog != null)
        {
            lastDialog.setVisible(false);
        }

        lastDialog = this;

        this.contactAddress = contactAddress;
        this.contactDisplayName = displayName;
        this.opSetClass = opSetClass;
        this.providers = providers;
        this.mReformattingNeeded = reformattingNeeded;

        initComponents();

        String preferredProviderName = GuiActivator.getConfigurationService().
                  user().getString(opSetClass.getName() + ".preferredProvider");
        ProtocolProviderService foundPreferredProvider = null;
        logger.info("Looking for preferred provider with name " + preferredProviderName);

        for (ProtocolProviderService provider : providers)
        {
            String name = provider.getProtocolName();
            logger.debug("Looking for preferred provider at " + name);

            if (name.equals(preferredProviderName))
            {
                logger.info("Found preferred provider " + name);
                foundPreferredProvider = provider;
                break;
            }
        }

        // Act as if there weren't a preferred provider if it is not online.
        preferredProvider = foundPreferredProvider == null ?
                            null : foundPreferredProvider.isRegistered() ?
                            foundPreferredProvider : null;

        if (preferredProvider != null)
        {
            callButtonPressed();
            dispose();
        }
    }

    private void initComponents()
    {
        final ResourceManagementService res = GuiActivator.getResources();
        TransparentPanel mainPanel = new TransparentPanel(new BorderLayout(0, 10));
        Border border;

        if (OSUtils.IS_MAC)
        {
            border = BorderFactory.createEmptyBorder();
        }
        else
        {
            border = BorderFactory.createLineBorder(Color.BLACK);
        }

        mainPanel.setBorder(border);

        // Title
        if (!OSUtils.IS_MAC)
        {
            String titleText = res.getI18NString(
                              "impl.protocol.commportal.SELECT_CALLING_METHOD");
            JLabel title = new JLabel(titleText);
            title.setHorizontalAlignment(SwingConstants.CENTER);
            title.setFont(title.getFont().deriveFont(Font.ITALIC));
            mainPanel.add(title, BorderLayout.NORTH);
        }

        // Button for each provider:
        TransparentPanel accountsPanel = new TransparentPanel();
        accountsPanel.setLayout(new BoxLayout(accountsPanel, BoxLayout.Y_AXIS));

        for (final ProtocolProviderService provider : providers)
        {
            logger.debug("Examining provider " + provider.getProtocolName());
            TransparentPanel wrapper = new TransparentPanel();
            String name = res.getI18NString("impl.protocol.commportal." +
                                                    provider.getProtocolName());
            if (name == null)
                name = provider.getProtocolName();

            JButton callButton = new SIPCommSnakeButton(name,
                                                        "plugin.cpprotocol.ctd.ok",
                                                        true);

            callButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    logger.user("Provider selected: " + provider.getProtocolName());

                    if (provider.isRegistered())
                    {
                        logger.debug("User selected a registered provider");
                        selectedProvider = provider;
                        callButtonPressed();
                    }
                    else
                    {
                        logger.debug("User selected unregistered provider");
                        CallManager.showNotOnlineWarning();
                    }

                    dispose();
                    lastDialog = null;
                }
            });

            callButton.setPreferredSize(new ScaledDimension(230,
                                             callButton.getPreferredSize().height));

            wrapper.add(callButton);
            accountsPanel.add(wrapper);
        }

        // Cancel button
        JButton cancelButton =
                   new SIPCommSnakeButton(res.getI18NString("service.gui.conf.CANCEL"),
                                          "plugin.cpprotocol.ctd.CANCEL",
                                          true);

        cancelButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                logger.user("Cancel button clicked in provider dialog");
                dispose();
            }
        });

        cancelButton.setPreferredSize(new ScaledDimension(230,
                                       cancelButton.getPreferredSize().height));
        TransparentPanel cancelWrapper = new TransparentPanel();
        cancelWrapper.add(cancelButton);

        mainPanel.add(accountsPanel, BorderLayout.CENTER);
        mainPanel.add(cancelWrapper, BorderLayout.SOUTH);

        preCallWindow.add(mainPanel);
        preCallWindow.pack();

        // Set the size of the displayer:
        Dimension preferredSize = preCallWindow.getPreferredSize();
        preCallWindow.setPreferredSize(
            new ScaledDimension(Math.max(230, preferredSize.width),
                                preferredSize.height));
    }

    public synchronized void setVisible(boolean isVisible)
    {
        // Only actually change the visibility if we don't have a preferred
        // provider
        if (preferredProvider == null)
        {
            UIService uiService = GuiActivator.getUIService();

            if (isVisible)
            {
                uiService.showStackableAlertWindow(preCallWindow);
            }
            else
            {
                dispose();
            }
        }
    }

    /**
     * @return the selected provider, or the preferred provider if there is one.
     */
    protected ProtocolProviderService getSelectedProvider()
    {
        // If we have a default provider, then use it.  Otherwise return the
        // selected provider
        return preferredProvider != null ? preferredProvider : selectedProvider;
    }

    /**
     * Calls through the selected account when the call button is pressed.
     */
    protected void callButtonPressed()
    {
        logger.user("Call button clicked");
        lastDialog = null;
        CallManager.createCall(opSetClass,
                               getSelectedProvider(),
                               null,
                               contactDisplayName,
                               contactAddress,
                               mReformattingNeeded);
    }

    /**
     * Creates this received call window.
     *
     * @param title the title of the created window
     * @param text the text to show
     * @return the created window
     */
    private Window createPreCallWindow( String title,
                                        String text)
    {
        Window receivedCallWindow = null;

        if (OSUtils.IS_MAC)
        {
            HudWindow window = new HudWindow();
            window.hideCloseButton();

            JDialog dialog = window.getJDialog();
            dialog.setUndecorated(true);
            dialog.setTitle(title);

            receivedCallWindow = window.getJDialog();

            callLabelDisplayName = HudWidgetFactory.createHudLabel("");
        }
        else
        {
            SIPCommFrame frame = new SIPCommFrame(false);

            // Get rid of the title bar and border but still allow the user to
            // drag the frame to reposition it on the screen.
            frame.setDraggableUndecorated();

            receivedCallWindow = frame;

            callLabelDisplayName = new JLabel();
        }

        if (text != null)
            callLabelDisplayName.setText(text);

        // Prevent the window from being blocked by modal dialogs - since we
        // are always-on-top, we don't want to appear over a modal dialog but
        // be unable to be moved away from it.
        receivedCallWindow.setModalExclusionType(
                                 Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

        // prevents dialog window to get unwanted key events and when going
        // on top on linux, it steals focus and if we are accidently
        // writing something and pressing enter a call get answered
        receivedCallWindow.setFocusableWindowState(false);

        receivedCallWindow.setAlwaysOnTop(true);

        return receivedCallWindow;
    }

    /**
     * Disposes this window.
     */
    public void dispose()
    {
        logger.info("Disposing window");
        preCallWindow.dispose();
    }
}
