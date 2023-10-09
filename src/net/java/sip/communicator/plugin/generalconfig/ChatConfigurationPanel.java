/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.generalconfig.autoaway.*;
import net.java.sip.communicator.util.Logger;

/**
 * The general configuration form.
 */
public class ChatConfigurationPanel
    extends OptionListConfigurationPanel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by this <tt>ChatConfigurationPanel</tt> for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ChatConfigurationPanel.class);

    /**
     * Indicates the name of the saved personal contact store.
     */
    private static final String PERSONAL_CONTACT_STORE_KEY =
        "net.java.sip.communicator.PERSONAL_CONTACT_STORE";

    /**
     * Indicates if the Chat configuration panel should be disabled, i.e.
     * not visible to the user.
     */
    private static final String CHAT_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.messageconfig.DISABLED";

    /**
     * If true, we will block all chat requests from external contacts,
     * otherwise reject.
     */
    private static final String BLOCK_AUTH_EXT_CHAT_PROP =
           "net.java.sip.communicator.impl.protocol.contactauth.BLOCK_AUTH_EXT_CHAT";

    /**
     * If true (or not set) we will automatically update the status to be in
     * a meeting when Outlook tells us so.
     */
    private static final String USE_OUTLOOK_FOR_STATUS_PROP =
                      "net.java.sip.communicator.protocol.presence.USE_OUTLOOK";

    /**
     * Indicates if the AutoAway configuration panel should be disabled, i.e.
     * not visible to the user.
     */
    private static final String AUTO_AWAY_CONFIG_DISABLED_PROP
        =
        "net.java.sip.communicator.plugin.generalconfig.autoawayconfig.DISABLED";

    /**
     * The configuration service
     */
    private static final ConfigurationService configService =
        GeneralConfigPluginActivator.getConfigurationService();

    /**
     * Creates the general configuration panel.
     */
    public ChatConfigurationPanel()
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        final Component chatConfigPanel = createChatConfigPanel();
        final AutoAwayConfigurationPanel autoAwayConfigPanel =
                                               new AutoAwayConfigurationPanel();

        if (!configService.global().getBoolean(CHAT_CONFIG_DISABLED_PROP, false))
        {
            add(chatConfigPanel);
        }

        if (!configService.global().getBoolean(AUTO_AWAY_CONFIG_DISABLED_PROP, false))
        {
            add(autoAwayConfigPanel);
        }
    }

    /**
     * Creates the chat configuration panel.
     * @return the created panel
     */
    private Component createChatConfigPanel()
    {
        ConfigSectionPanel configPanel = new ConfigSectionPanel("service.gui.MESSAGE");

        configPanel.add(createAutoAuthCheckBox());

        if (OSUtils.IS_WINDOWS)
        {
            // Only enabled if Outlook is present - i.e. on Windows
            configPanel.add(createMeetingCheckbox());
        }

        return configPanel;
    }

    /**
     * Initializes the "Automatically accept chat requests" check box.
     * @return the created check box.
     */
    private Component createAutoAuthCheckBox()
    {
        final JCheckBox autoAuthCheckBox = new SIPCommCheckBox();
        autoAuthCheckBox.setText(Resources.getString("service.gui.BLOCK_AUTH_EXT_CHAT"));
        autoAuthCheckBox.setForeground(TEXT_COLOR);
        autoAuthCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        autoAuthCheckBox.setSelected(configService.user().getBoolean(
                                                   BLOCK_AUTH_EXT_CHAT_PROP, true));

        autoAuthCheckBox.addActionListener(event ->
            {
                logger.user("Auto authorization of chat requests checkbox toggled");

                SIPCommCheckBox eventSource = (SIPCommCheckBox) event.getSource();
                boolean value = eventSource.isSelected();

                logger.info("New value: " + value);
                configService.user().setProperty(BLOCK_AUTH_EXT_CHAT_PROP, value);
            }
        );

        // Listen for changes to the config that controls auto authorization
        // of chat requests to make sure that the setting of the checkbox
        // matches the config. This is because the config change may have
        // been made elsewhere (i.e. by the user ticking the box on the
        // authorization dialog to automatically accept all future chat
        // requests), so the value of this checkbox might otherwise get
        // out-of-date.
        configService.user().addPropertyChangeListener(BLOCK_AUTH_EXT_CHAT_PROP, event -> {
            logger.debug("Auto authorization of chat requests config updated");
            autoAuthCheckBox.setSelected(
                Boolean.parseBoolean(event.getNewValue().toString()));
        });

        return autoAuthCheckBox;
    }

    /**
     * Initializes the "update status from Outlook" check box.
     * @return the created check box.
     */
    private Component createMeetingCheckbox()
    {
        final JCheckBox outlookCheckBox = new SIPCommCheckBox();
        outlookCheckBox.setText(
                Resources.getString("service.gui.presence.IN_MEETING_SETTING"));
        outlookCheckBox.setForeground(TEXT_COLOR);
        outlookCheckBox.setAlignmentX(Component.LEFT_ALIGNMENT);
        outlookCheckBox.setSelected(configService.user().getBoolean(
                                            USE_OUTLOOK_FOR_STATUS_PROP, true));

        outlookCheckBox.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                logger.user("Outlook presence checkbox toggled");
                boolean currentValue = configService.user().getBoolean(
                                             USE_OUTLOOK_FOR_STATUS_PROP, true);

                logger.info("New value: " + !currentValue);
                configService.user().setProperty(USE_OUTLOOK_FOR_STATUS_PROP, !currentValue);
                outlookCheckBox.setSelected(!currentValue);
            }
        });

        // At present, Outlook Calendar integration requires Outlook contacts.
        // Therefore, this option only makes sense if Outlook Contacts are
        // enabled.
        configService.user().addPropertyChangeListener(PERSONAL_CONTACT_STORE_KEY,
                                                new PropertyChangeListener()
        {
            @Override
            public void propertyChange(PropertyChangeEvent evt)
            {
                String contactStore = configService.user().getString(PERSONAL_CONTACT_STORE_KEY);
                logger.debug("Contact source changed to " + contactStore);
                boolean visible = "CSProtocol".equals(contactStore);
                outlookCheckBox.setVisible(visible);
            }
        });

        boolean visible = "CSProtocol".equals(
                           configService.user().getString(PERSONAL_CONTACT_STORE_KEY));
        outlookCheckBox.setVisible(visible);

        return outlookCheckBox;
    }

    /**
     * Split a resource string on the parameterised element, so that e.g a
     * drop-down selector can be placed in the appropriate place between the
     * strings.<p>
     * E.g. to create a config option like <tt>Display [N] messages<tt>:<br>
     * <tt>resource.DISPLAY=Display {0} messages<br>
     *     String labels = getSplitResourcesString("resource.DISPLAY");<br>
     *     <br>
     *     parentPanel.add(new JLabel(labels[0]));<br>
     *     parentPanel.add(new DropDownSelector());<br>
     *     parentPanel.add(new JLabel(labels[1]));<br>
     * </tt>
     * Caller must handle either of the returned labels being null, e.g. by only
     * creating and adding the appropriate JLabel if the string is not null.
     *
     * @param resourceId ID of the required string in resources.properties
     * @return an array containing two elements, either or both of which may be
     * null.  The selector element should be placed between these strings.
     */
    public static String[] getSplitResourcesString(String resourceId)
    {
        // String.split(pattern) takes a regex - don't use special chars here.
        final String PARAMETERISED_TOKEN_SPLIT = "<<~~TOKEN~~>>";

        String out[];
        String unsplitText = Resources.getString(resourceId,
                                       new String[]{PARAMETERISED_TOKEN_SPLIT});

        if (StringUtils.isNullOrEmpty(unsplitText))
        {
            logger.warn("Couldn't get resource string for resource: " + resourceId);
            out = new String[]{null, null};
        }
        else if (!unsplitText.contains(PARAMETERISED_TOKEN_SPLIT))
        {
            logger.warn("Resource '" + resourceId + "' wasn't tokenized: " + unsplitText);
            out = new String[]{unsplitText, null};
        }
        else
        {
            // "a@b".split(@) -> [a, b] : nothing more to do
            // "a@".split(@) -> [a] (no second string)
            // "@b".split(@) -> ["", b] (first string empty)
            out = unsplitText.split(PARAMETERISED_TOKEN_SPLIT, 2);

            if (out.length < 2)
            {
                // "a@".split(@) -> [a] (no second string)
                out = new String[]{ out[0], null };
            }
            else if ("".equals(out[0]))
            {
               // "@b".split(@) -> ["", b] (first string empty)
                out[0] = null;
            }
        }

        return out;
    }
}
