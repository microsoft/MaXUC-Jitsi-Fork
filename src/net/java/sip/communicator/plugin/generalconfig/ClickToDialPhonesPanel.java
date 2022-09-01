// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Map.*;

import javax.swing.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.phonenumberutils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * Class used to manage "My Phones" - the phones pre-configured to use for Click
 * to Dial calls.
 *
 * In particular, it is a dialog that allows the user to add, remove or edit
 * entries in the "my phones" list as they wish.
 */
public class ClickToDialPhonesPanel extends TransparentPanel
{
    private static final int MAX_LABEL_WIDTH = 130;
    private static final long serialVersionUID = 0L;
    private static final Logger sLog = Logger.getLogger(ClickToDialPhonesPanel.class);

    /**
     * Place in config where selected my phone is stored
     */
    private static final String SELECTED_MY_PHONE_STUB =
        "net.java.sip.communicator.impl.protocol.commportal.ctd.preferredPhone";

    /**
     * Place in config where my phones are stored
     */
    private static final String MY_PHONES_CONFIG_STUB =
              "net.java.sip.communicator.impl.protocol.commportal.ctd.myphones";

    /**
     * The location of each my phone
     */
    private static final String[] MY_PHONE_CONFIG_LOCATIONS =
    {
        MY_PHONES_CONFIG_STUB + ".1",
        MY_PHONES_CONFIG_STUB + ".2",
        MY_PHONES_CONFIG_STUB + ".3",
        MY_PHONES_CONFIG_STUB + ".4",
        MY_PHONES_CONFIG_STUB + ".5"
    };

    private static final ConfigurationService sConfig =
                         GeneralConfigPluginActivator.getConfigurationService();
    private static final ResourceManagementService sResources =
                                    GeneralConfigPluginActivator.getResources();
    private static final PhoneNumberUtilsService sPhoneNumberService =
                             GeneralConfigPluginActivator.getPhoneNumberUtils();

    // Images used by the delete button
    private static final BufferedImageFuture DELETE_BUTTON =
                sResources.getBufferedImage("service.gui.buttons.CLOSE_TAB");
    private static final BufferedImageFuture DELETE_BUTTON_ROLLOVER =
        sResources.getBufferedImage("service.gui.buttons.CLOSE_TAB_ROLLOVER");
    private static final BufferedImageFuture DELETE_BUTTON_PRESSED =
        sResources.getBufferedImage("service.gui.buttons.CLOSE_TAB_PRESSED");

    private static final Color TEXT_COLOR =
        ConfigurationUtils.useNativeTheme() ? Color.BLACK :
            new Color(GeneralConfigPluginActivator.getResources().getColor(
                "service.gui.DARK_TEXT"));

    /**
     * The combo box that displays where the CTD call will be originated from
     */
    private JComboBox<MyPhone> mWhereToCtdFromComboBox;

    /**
     * The component that allows the user to manage their "my phones"
     */
    private JPanel mManageMyPhones;

    /**
     * The list of "my phones" that the my phones panel is currently showing
     */
    private List<MyPhone> mMyPhones;

    /**
     * The currently selected item
     */
    private String mSelectedCallItem;

    /**
     * True if VoIP is enabled in the CoS and by the user
     */
    private boolean mVoipEnabled;

    /**
     * True if CTD remote is allowed
     */
    private boolean mCtdRemoteEnabled;

    /**
     *  Last time setting was changed, or zero if first time through
     *  (if test will always succeed first time).
     *  Used to ensure logs are not unnecessarily duplicated.
     */
    private long settingTime;

    public ClickToDialPhonesPanel()
    {
        setLayout(new GridBagLayout());

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = 0;
        labelConstraints.weightx = 0;

        GridBagConstraints panelConstraints = new GridBagConstraints();
        panelConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
        panelConstraints.gridx = 1;
        panelConstraints.gridy = 0;
        panelConstraints.weightx = 0;

        // 1: How to make calls
        final JLabel howToMakeCallLabel = new JLabel(
            sResources.getI18NString("impl.protocol.commportal.MAKE_CALLS_USING"));
        howToMakeCallLabel.setForeground(TEXT_COLOR);
        ScaleUtils.scaleFontAsDefault(howToMakeCallLabel);
        final Component howToMakeCallPanel = createHowToMakeCallComboBox();
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(2, 0, 2, 0);
        panelConstraints.insets = new Insets(2, 10, 2, 0);
        add(howToMakeCallLabel, labelConstraints);
        add(howToMakeCallPanel, panelConstraints);
        labelConstraints.gridy += 1;
        panelConstraints.gridy += 1;

        // 2: Manage my phones option
        String text = sResources.getI18NString("impl.protocol.commportal.MY_PHONES");
        final JLabel manageMyPhonesLabel = createLabel(text);

        mManageMyPhones = new TransparentPanel();
        mManageMyPhones.add(createManageMyPhonesPanel());
        mManageMyPhones.setVisible(mCtdRemoteEnabled);
        manageMyPhonesLabel.setVisible(mCtdRemoteEnabled);
        labelConstraints.anchor = GridBagConstraints.FIRST_LINE_END;
        labelConstraints.insets = new Insets(8, 0, 2, 0);
        panelConstraints.insets = new Insets(0, 0, 2, 0);
        add(manageMyPhonesLabel, labelConstraints);
        add(mManageMyPhones, panelConstraints);
        labelConstraints.gridy += 1;
        panelConstraints.gridy += 1;

        // 3: Number to use when making CTD calls
        final JLabel whereCtdCallFromLabel = new JLabel(
            sResources.getI18NString("impl.protocol.commportal.CALL_ME_ON"));
        whereCtdCallFromLabel.setForeground(TEXT_COLOR);
        ScaleUtils.scaleFontAsDefault(whereCtdCallFromLabel);
        final Component whereCtdCallFromPanel = createWhereCtdCallFromPanel();
        labelConstraints.anchor = GridBagConstraints.LINE_END;
        labelConstraints.insets = new Insets(2, 0, 2, 0);
        panelConstraints.insets = new Insets(2, 10, 2, 0);
        add(whereCtdCallFromLabel, labelConstraints);
        add(whereCtdCallFromPanel, panelConstraints);
        labelConstraints.gridy += 1;
        panelConstraints.gridy += 1;

        // Add an empty panel at the third column to expand into the available space
        GridBagConstraints fillConstraints = new GridBagConstraints();
        fillConstraints.gridheight = panelConstraints.gridy;
        fillConstraints.gridx = 2;
        fillConstraints.gridy = 0;
        fillConstraints.weightx = 1;
        add(new TransparentPanel(), fillConstraints);

        // Finally, add a listener to the class of service to make sure that the
        // config options only appear when the user is allowed to see them
        setVisible(false);
        GeneralConfigPluginActivator.getCosService()
                                    .getClassOfService(new CPCosGetterCallback()
        {
            @Override
            public void onCosReceived(CPCos classOfService)
            {
                boolean ctdAllowed =
                    classOfService.getClickToDialEnabled() &&
                    classOfService.getSubscribedMashups().isCtdAllowed();

                boolean ctdRemoteAllowed =
                    classOfService.getClickToDialRemoteEnabled();

                boolean voipEnabledByUser = ConfigurationUtils.isVoIPEnabledByUser();
                // We could use ConfigurationUtils.isVoIPEnabledInCoS() for
                // the VoIP enabled by CoS setting, but there is a race
                // condition between the CoS storeCosFieldsInConfig() method
                // saving this setting to the config file, and this callback
                // being called (where isVoIPEnabledInCoS() would then read
                // that same config file).
                boolean voipEnabledInCoS = classOfService.getSubscribedMashups().isVoipAllowed();
                boolean voipEnabled = voipEnabledByUser && voipEnabledInCoS;

                mVoipEnabled = voipEnabled;
                mCtdRemoteEnabled = ctdRemoteAllowed;

                sLog.info("Class of Service received" +
                          ": CTD allowed = " + ctdAllowed +
                          ", Remote CTD allowed = " + ctdRemoteAllowed +
                          ", VoIP enabled = " + voipEnabledInCoS);

                // Only show this entire panel if there is something for the user
                // to interact with.  I.e. can the user
                // * Choose how to make a call
                // * Choose where to make a CTD call from.
                boolean howToMakeCallsChoice   = ctdAllowed && voipEnabled;
                boolean whereToMakeCallsChoice = ctdAllowed && ctdRemoteAllowed;
                boolean makeCallConfigVisible = howToMakeCallsChoice ||
                                                whereToMakeCallsChoice;

                setVisible(makeCallConfigVisible);

                // If this panel is visible, set the visibility of its
                // subcomponents, based on what is allowed in the CoS.
                if (makeCallConfigVisible)
                {
                    // Needs to be able to make a choice how to make a call
                    howToMakeCallPanel.setVisible(howToMakeCallsChoice);
                    howToMakeCallLabel.setVisible(howToMakeCallsChoice);

                    // Needs to be able to make a choice where to make a call
                    mManageMyPhones.setVisible(whereToMakeCallsChoice);
                    manageMyPhonesLabel.setVisible(whereToMakeCallsChoice);
                    whereCtdCallFromPanel.setVisible(whereToMakeCallsChoice);
                    whereCtdCallFromLabel.setVisible(whereToMakeCallsChoice);
                }

                // Some items are disabled if the "make calls with" option is
                // VoIP. If VoIP is not allowed then this doesn't make sense.
                // Thus update the enabled state of each item.
                boolean enableCtdItems = !selectedItemIsVoip();

                mWhereToCtdFromComboBox.setEnabled(enableCtdItems);

                for (MyPhone myPhone : mMyPhones)
                {
                    myPhone.setEnabled(enableCtdItems);
                }

                // Re-layout the my phones window, as it may have changed
                mManageMyPhones.removeAll();
                mManageMyPhones.add(createManageMyPhonesPanel());

                Window window = SwingUtilities.getWindowAncestor(
                                                   ClickToDialPhonesPanel.this);
                if (window != null)
                    window.pack();
            }
        });
    }

    /**
     * Create a label that will wrap onto multiple lines if necessary.
     *
     * @param text The text to show in the label
     * @return the created label
     */
    private JLabel createLabel(String text)
    {
        JLabel label = new JLabel(text);
        ScaleUtils.scaleFontAsDefault(label);

        if (ComponentUtils.getStringWidth(label, text) >
                                           ScaleUtils.scaleInt(MAX_LABEL_WIDTH))
        {
            // Label is too large to display on one line - wrap it in HTML tags
            // so that it auto-wraps
            label.setText("<html><p align=\"right\">" + text + "</p></html>");
            label.setMaximumSize(new ScaledDimension(MAX_LABEL_WIDTH, 150));
            label.setPreferredSize(new ScaledDimension(MAX_LABEL_WIDTH, 150));
            label.setVerticalAlignment(SwingConstants.TOP);
            label.setVerticalTextPosition(SwingConstants.TOP);
            label.setHorizontalAlignment(SwingConstants.RIGHT);
            label.setAlignmentX(Component.RIGHT_ALIGNMENT);
            label.setForeground(TEXT_COLOR);
        }

        return label;
    }

    /**
     * @return the "my phones" panel
     */
    private JPanel createManageMyPhonesPanel()
    {
        TransparentPanel myPhonesPanel = new TransparentPanel();
        myPhonesPanel.setLayout(new GridLayout(0, 1));
        myPhonesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Create the components for each my phone
        mMyPhones = fetchMyPhones();
        boolean shouldBeEnabled = true;
        boolean isVoip = selectedItemIsVoip();

        for (int i = 0; i < mMyPhones.size(); i++)
        {
            MyPhone myPhone = mMyPhones.get(i);
            MyPhone nextMyPhone = (i + 1) < mMyPhones.size() ? mMyPhones.get(i + 1) : null;

            TransparentPanel myPhonePanel = myPhone.getUI(nextMyPhone);

            myPhone.setEnabled(shouldBeEnabled && !isVoip);
            myPhone.setTransparent(!shouldBeEnabled || (isVoip && myPhone.isEmpty()));

            if (myPhone.isEmpty())
                // This phone is empty - the UI for all subsequent phones should
                // be disabled.
                shouldBeEnabled = false;

            myPhonesPanel.add(myPhonePanel);
        }

        return myPhonesPanel;
    }

    /**
     * @return the combo box which controls where to make CTD calls from
     */
    private Component createWhereCtdCallFromPanel()
    {
        // Get the "my phones" to display
        mWhereToCtdFromComboBox = new JComboBox<>();
        ScaleUtils.scaleFontAsDefault(mWhereToCtdFromComboBox);
        setWhereToCtdFromItems();

        mWhereToCtdFromComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent ev)
            {
                MyPhone selectedItem =
                              (MyPhone)mWhereToCtdFromComboBox.getSelectedItem();

                // Prevent duplicate logging for one combo box change
                if (System.currentTimeMillis() - settingTime > 1000)
                {
                    settingTime = System.currentTimeMillis();
                    sLog.user("Preferred phone changed to: " + selectedItem);
                }

                // Store in config the config location of the preferred phone
                sConfig.user().setProperty(SELECTED_MY_PHONE_STUB + ".value",
                                    selectedItem.getValueConfigLocation());
                sConfig.user().setProperty(SELECTED_MY_PHONE_STUB + ".name",
                                    selectedItem.getNameConfigLocation());
            }
        });

        // Disable the combo box if "SIP" is enabled.
        boolean enabled = !selectedItemIsVoip();
        mWhereToCtdFromComboBox.setEnabled(enabled);

        return mWhereToCtdFromComboBox;
    }

    /**
     * @return the combo box which controls where to make calls from
     */
    private Component createHowToMakeCallComboBox()
    {
        final String makeCallConfig =
              OperationSetBasicTelephony.class.getName() + ".preferredProvider";

        // Create the model that displays the choice
        final Map<String, String> displayValuesMap = new LinkedHashMap<>();
        displayValuesMap.put(sResources.getI18NString("impl.protocol.commportal.SIP"), "SIP");
        displayValuesMap.put(sResources.getI18NString("impl.protocol.commportal.CTD"), "CTD_CommPortal");
        displayValuesMap.put(sResources.getI18NString("impl.protocol.commportal.ALWAYS_ASK"), "ask");

        ComboBoxModel<String> makeCallsWithComboBoxModel =
                new DefaultComboBoxModel<>(displayValuesMap.keySet().toArray(new String[0]));
        final JComboBox<String> makeCallsWithComboBox = new JComboBox<>();
        ScaleUtils.scaleFontAsDefault(makeCallsWithComboBox);
        makeCallsWithComboBox.setModel(makeCallsWithComboBoxModel);

        // Set the selected item:
        String configValue = sConfig.user().getString(makeCallConfig, "SIP");
        mSelectedCallItem = "SIP";
        sLog.info("Getting how to make call, config value is " + configValue);

        for (Entry<String, String> entry : displayValuesMap.entrySet())
        {
            if (entry.getValue().equals(configValue))
            {
                mSelectedCallItem = entry.getKey();
                sLog.info("Selected value for how to make call " + mSelectedCallItem);
                break;
            }
        }

        makeCallsWithComboBox.setSelectedItem(mSelectedCallItem);

        // Add item listener
        makeCallsWithComboBox.addItemListener(new ItemListener()
        {
            public void itemStateChanged(ItemEvent ev)
            {
                String selectedItem = (String)
                                        makeCallsWithComboBox.getSelectedItem();

                // Update the config with the value that the user selected
                if (System.currentTimeMillis() - settingTime > 5000)
                {
                    settingTime = System.currentTimeMillis();
                    sLog.user("Make call with changed to: " + selectedItem);
                }
                sConfig.user().setProperty(makeCallConfig,
                                    displayValuesMap.get(selectedItem));

                // And disable all the CTD items if "SIP" were selected
                boolean enableCtdItems =
                              !"SIP".equals(displayValuesMap.get(selectedItem));
                mWhereToCtdFromComboBox.setEnabled(enableCtdItems);

                mSelectedCallItem = selectedItem;

                mManageMyPhones.removeAll();
                mManageMyPhones.add(createManageMyPhonesPanel());
                SwingUtilities.getWindowAncestor(ClickToDialPhonesPanel.this).pack();
            }
        });

        return makeCallsWithComboBox;
    }

    /**
     * Creates the combo box model for the "where to make CTD calls from" item
     */
    private void setWhereToCtdFromItems()
    {
        // The choice should be all the phones with value, and an extra option
        // for the user to be always asked.
        List<MyPhone> myPhones = fetchMyPhonesWithValue();

        // Add an empty phone for always ask.
        String name = sResources.getI18NString("impl.protocol.commportal.ALWAYS_ASK");
        myPhones.add(new MyPhone(name, null));
        MyPhone selectedMyPhone = ClickToDialPhonesPanel.getSelectedMyPhone(myPhones);

        ComboBoxModel<MyPhone> whereToCtdFromModel =
                new DefaultComboBoxModel<>(myPhones.toArray(new MyPhone[0]));
        whereToCtdFromModel.setSelectedItem(selectedMyPhone);
        mWhereToCtdFromComboBox.setModel(whereToCtdFromModel);
    }

    /**
     * @return the list of my phones stored in config
     */
    private List<MyPhone> fetchMyPhones()
    {
        sanitiseMyPhones();
        List<MyPhone> myPhones = new ArrayList<>();

        // Add an entry for the account phone
        final String valueLocation =
                  "net.java.sip.communicator.plugin.provisioning.auth.USERNAME";
        final String nameLocation = valueLocation + ".name";

        String name = sResources.getI18NString("impl.protocol.commportal.MY_PHONE");
        sConfig.user().setProperty(nameLocation, name);

        myPhones.add(new MyPhone(name, valueLocation, sConfig.global())
        {
            @Override
            public String getValueConfigLocation()
            {
                return valueLocation;
            }

            @Override
            public String getNameConfigLocation()
            {
                return nameLocation;
            }
        });

        // Add all the other entries that have been stored.
        for (String property : MY_PHONE_CONFIG_LOCATIONS)
        {
            myPhones.add(new MyPhone(property));
        }

        sLog.debug("Got " + myPhones.size() + " My Phones");
        return myPhones;
    }

    /**
     * @return the list of my phones in config which actually contain some data
     */
    private List<MyPhone> fetchMyPhonesWithValue()
    {
        List<MyPhone> myPhones = fetchMyPhones();
        List<MyPhone> myPhonesWithValue = new ArrayList<>();

        for (MyPhone myPhone : myPhones)
        {
            if (myPhone.getValue() != null &&
                myPhone.getValue().length() > 0 &&
                sPhoneNumberService.isValidNumber(myPhone.getValue()))
            {
                // Only add phones with a valid value
                myPhonesWithValue.add(myPhone);
            }
        }

        sLog.debug("Got " + myPhonesWithValue.size() + " MyPhones with valid values");
        return myPhonesWithValue;
    }

    /**
     * @param myPhones the list of current phones to examine
     * @return the phone which is currently selected in config.
     */
    private static MyPhone getSelectedMyPhone(List<MyPhone> myPhones)
    {
        String preferredPhone = sConfig.user().getString(SELECTED_MY_PHONE_STUB + ".value");
        MyPhone selectedPhone = null;
        sLog.info("Get selected my phone, config value is " + preferredPhone);

        for (MyPhone myPhone : myPhones)
        {
            if (myPhone.mValue == null)
            {
                // This is the "always ask" value which is the default if we
                // don't find the value which matches it
                selectedPhone = myPhone;
            }
            else if (myPhone.getValueConfigLocation() != null &&
                     myPhone.getValueConfigLocation().equals(preferredPhone))
            {
                // The config locations match
                selectedPhone = myPhone;
                break;
            }
        }

        sLog.debug("Found selected phone " + selectedPhone);
        return selectedPhone;
    }

    /**
     * Clears the list of empty phones, moving the non-empty phones to the
     * top.
     *
     * This is required because users might have phones 1,2,3 and then delete
     * phone 1.  In this case, we want to display phone 2 first, and phone 3
     * second.  I.e. we want to move phone 2 to 1 and 3 to 2.
     */
    private void sanitiseMyPhones()
    {
        List<MyPhone> myPhones = new ArrayList<>();

        for (String property : MY_PHONE_CONFIG_LOCATIONS)
        {
            myPhones.add(new MyPhone(property));
        }

        // Now sanitise the list so that the phones appear in order:
        List<MyPhone> emptyMyPhones = new ArrayList<>();
        for (MyPhone myPhone : myPhones)
        {
            if ((myPhone.getName()  == null || myPhone.getName().trim().length()  == 0) &&
                (myPhone.getValue() == null || myPhone.getValue().trim().length() == 0))
            {
                // This phone has no value thus can be got rid of.
                emptyMyPhones.add(myPhone);
            }
            else if (!emptyMyPhones.isEmpty())
            {
                // There is an empty phone - move this (non-empty) phone into it
                MyPhone emptyMyPhone = emptyMyPhones.get(0);
                emptyMyPhone.setName(myPhone.getName());
                emptyMyPhone.setValue(myPhone.getValue());

                // Clear out the my phone and mark it as empty:
                myPhone.setName(null);
                myPhone.setValue(null);

                emptyMyPhones.remove(emptyMyPhone);
                emptyMyPhones.add(myPhone);
            }
        }
    }

    /**
     * @return true if the currently selected "make calls with" item is VoIP
     */
    private boolean selectedItemIsVoip()
    {
        return mVoipEnabled &&
               sResources.getI18NString("impl.protocol.commportal.SIP")
                         .equals(mSelectedCallItem);
    }

    /**
     * Class representing one of the stored "My Phones"
     */
    private class MyPhone
    {
        /**
         * The location of this particular phone
         */
        private final String mConfigLocation;

        /**
         * True if this phone is editable
         */
        private final boolean mEditable;

        /**
         * The value of this my phone object (i.e. the phone number)
         */
        private String mValue;

        /**
         * The name of this my phone object
         */
        private String mName;

        // Elements of the UI created for this phone
        private TransparentPanel mUiContainer;
        private MyPhoneTextField mNameField;
        private MyPhoneTextField mValueField;
        private JButton mDeleteButton;
        private JPanel mDeleteFiller;

        /**
         * Create a my phone from a single config location.  This location is a
         * stub which points to the name and value of the phone
         *
         * @param configLocation the location of the name and value
         */
        public MyPhone(String configLocation)
        {
            mConfigLocation = configLocation;
            mEditable = true;
            mValue = sConfig.user().getString(mConfigLocation + ".value");
            mName = sConfig.user().getString(mConfigLocation + ".name");
        }

        /**
         * Create a my phone from a particular name and value
         *
         * @param name the name of the phone
         * @param valueLocation the location where the value can be found
         */
        public MyPhone(String name, String valueLocation)
        {
            this(name, valueLocation, sConfig.user());
        }

        /**
         * Create a my phone from a particular name and value
         *
         * @param name the name of the phone
         * @param valueLocation the location where the value can be found
         * @param cfg the config version to use to get the value
         */
        public MyPhone(String name, String valueLocation, ScopedConfigurationService cfg)
        {
            mConfigLocation = valueLocation;
            mEditable = false;
            mValue = valueLocation == null ? null : cfg.getString(valueLocation);
            mName = name;
        }

        /**
         * @param nextMyPhone The next phone that will be displayed.  This is
         *                    required so that an empty phone can make the next
         *                    my phone field enabled when it is edited to be not
         *                    empty.
         * @return the UI for this phone
         */
        public TransparentPanel getUI(final MyPhone nextMyPhone)
        {
            if (mUiContainer != null)
            {
                // Make sure that the UI is showing the right values:
                if (!mNameField.getText().equals(mName))
                    mNameField.setText(mName);
                if (!mValueField.getText().equals(mValue))
                    mValueField.setText(mValue);

                return mUiContainer;
            }

            mUiContainer = new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));

            // Name field
            mNameField = new MyPhoneTextField(
                sResources.getI18NString("impl.protocol.commportal.ENTER_NAME"));
            mNameField.setText(mName);
            ScaleUtils.scaleFontAsDefault(mNameField);
            mNameField.setPreferredSize(new Dimension(ScaleUtils.scaleInt(120),
                mNameField.getPreferredSize().height));

            // Value field
            mValueField = new MyPhoneTextField(
                sResources.getI18NString(
                    "plugin.contactdetails.guidance.PHONE_NUMBER"));
            mValueField.setText(mValue);
            ScaleUtils.scaleFontAsDefault(mValueField);
            mValueField.setPreferredSize(new Dimension(ScaleUtils.scaleInt(180),
                mValueField.getPreferredSize().height));
            if (!sPhoneNumberService.isValidNumber(mValue))
                mValueField.setForeground(Color.RED);

            // Delete button
            mDeleteButton = new SIPCommButton(null,
                                              DELETE_BUTTON,
                                              DELETE_BUTTON_ROLLOVER,
                                              DELETE_BUTTON_PRESSED);

            // Add some filler with exactly the same size as the delete button.
            // If the delete button is hidden, this should be shown, otherwise
            // the display goes wrong
            Dimension size = new Dimension(DELETE_BUTTON.resolve().getWidth(null),
                                           DELETE_BUTTON.resolve().getHeight(null));
            mDeleteFiller = new TransparentPanel();
            mDeleteFiller.setPreferredSize(size);

            // Add a key listener to update the values
            KeyAdapter listener = new KeyAdapter()
            {
                @Override
                public void keyReleased(KeyEvent e)
                {
                    boolean wasEmpty = isEmpty();

                    setName(mNameField.getText());
                    setValue(mValueField.getText());

                    if (wasEmpty)
                    {
                        // This phone was empty thus make the next one enabled
                        // (if we have one)
                        if (nextMyPhone != null)
                        {
                            nextMyPhone.setEnabled(true);
                            nextMyPhone.setTransparent(false);
                        }

                        mDeleteButton.setVisible(true);
                        mDeleteFiller.setVisible(false);
                    }
                    else if (isEmpty())
                    {
                        // The phone is now empty, we need to recreate the UI in
                        // order to hide various components.
                        mDeleteButton.setVisible(false);
                        mDeleteFiller.setVisible(true);
                        mManageMyPhones.removeAll();
                        mManageMyPhones.add(createManageMyPhonesPanel());
                    }

                    // Items have changed, so need to update the ctd items:
                    setWhereToCtdFromItems();
                }
            };
            mNameField.addKeyListener(listener);
            mValueField.addKeyListener(listener);

            // Add the action listener for the delete button
            mDeleteButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    sLog.user("Phone deleted: " + MyPhone.this);
                    setName(null);
                    setValue(null);

                    mManageMyPhones.removeAll();
                    mManageMyPhones.add(createManageMyPhonesPanel());

                    // Items have changed, so need to update the ctd items:
                    setWhereToCtdFromItems();
                    mDeleteButton.setVisible(false);
                    mDeleteFiller.setVisible(true);
                }
            });

            // Add a focus listener to the number field, so that we can display
            // an error if the user enters a bad number
            mValueField.addFocusListener(new FocusListener()
            {
                @Override
                public void focusLost(FocusEvent e)
                {
                    if (!sPhoneNumberService.isValidNumber(mValue))
                    {
                        mValueField.setForeground(Color.RED);
                    }
                }

                @Override
                public void focusGained(FocusEvent e)
                {
                    mValueField.setForeground(Color.BLACK);
                }
            });

            mUiContainer.add(mNameField);
            mUiContainer.add(mValueField);
            mUiContainer.add(mDeleteButton);
            mUiContainer.add(mDeleteFiller);

            if (!mEditable)
            {
                mNameField.setEditable(false);
                mValueField.setEditable(false);
                mDeleteButton.setVisible(false);
            }
            else
            {
                // Enable the fields only if "SIP" isn't the top level make calls
                // option.
                boolean enabled = !selectedItemIsVoip();
                mNameField.setEnabled(enabled);
                mValueField.setEnabled(enabled);
                mDeleteButton.setEnabled(enabled);
            }

            if (isEmpty())
            {
                // Can't delete an already deleted phone.
                mDeleteButton.setVisible(false);
            }

            mDeleteFiller.setVisible(!mDeleteButton.isVisible());

            return mUiContainer;
        }

        /**
         * Enable or disable the UI that has been created for this my phone.
         *
         * @param enabled true to enable the UI
         */
        public void setEnabled(boolean enabled)
        {
            if (mUiContainer != null && mEditable)
            {
                for (Component comp : mUiContainer.getComponents())
                    comp.setEnabled(enabled);

                boolean showDelete = enabled && !isEmpty();
                mDeleteButton.setVisible(showDelete);
                mDeleteFiller.setVisible(!showDelete);
            }
        }

        /**
         * Enables or disables transparency for the name and value fields.
         * @param transparent Whether or not this field should be transparent
         */
        public void setTransparent(boolean transparent)
        {
            mNameField.setTransparent(transparent);
            mValueField.setTransparent(transparent);
        }

        /**
         * @return true if this phone doesn't contain any data
         */
        public boolean isEmpty()
        {
            return (mValue == null || mValue.trim().length() == 0) &&
                   (mName  == null || mName.trim().length() == 0);
        }

        @Override
        public String toString()
        {
            // Over-ride to string so that we display the value of this object
            return mValue == null ? mName :
                   mName == null ? mValue :
                   (mName + " (" + mValue + ")");
        }

        /**
         * @return the name of this phone
         */
        public String getName()
        {
            return mName;
        }

        /**
         * @return the value of this phone
         */
        public String getValue()
        {
            return mValue;
        }

        /**
         * @param name the new name of this phone
         */
        public void setName(String name)
        {
            mName = name;
            sConfig.user().setProperty(mConfigLocation + ".name", name);
        }

        /**
         * @param value the new value of this phone
         */
        public void setValue(String value)
        {
            mValue = value;
            sConfig.user().setProperty(mConfigLocation + ".value", value);
        }

        /**
         * @return the location where the value of this phone number is saved
         */
        public String getValueConfigLocation()
        {
            return mConfigLocation + ".value";
        }

        /**
         * @return the location where the name of this phone number is saved
         */
        public String getNameConfigLocation()
        {
            return mConfigLocation + ".name";
        }
    }

    /**
     * Extends the SIPCommTextField to add a feature that makes disabled fields
     * semi-transparent, in which case the hint text is not shown.
     */
    private static class MyPhoneTextField extends SIPCommTextField
    {
        private static final long serialVersionUID = 0L;

        private static final int ALPHA = 32;

        private boolean mIsTransparent = false;

        public MyPhoneTextField(String hintText)
        {
            super(hintText);

            // Make this component non-opaque on Windows to prevent undesirable
            // painting artifacts from being drawn. This is a workaround for a
            // common issue seen when drawing Swing components with transparent
            // background on Windows.
            setOpaque(OSUtils.IS_MAC);
        }

        /**
         * Enables or disables the field transparency. When a field is
         * transparent, its background color is changed to a light gray
         * with low opacity and the hint text is cleared.
         * @param transparent Whether or not this field should be transparent
         */
        public void setTransparent(boolean transparent)
        {
            if (transparent != mIsTransparent)
            {
                mIsTransparent = transparent;

                if (transparent)
                {
                    setBackground(new Color(Color.LIGHT_GRAY.getRed(),
                                            Color.LIGHT_GRAY.getGreen(),
                                            Color.LIGHT_GRAY.getBlue(),
                                            ALPHA));
                    clearHintText();
                }
                else
                {
                    setBackground(Color.WHITE);
                    if (getText() == null || getText().trim().length() == 0)
                    {
                        setToHintText();
                    }
                }
            }
        }

        @Override
        protected void setToHintText()
        {
            // Prevent the hint text from being set if this field is transparent.
            if (!mIsTransparent)
            {
                super.setToHintText();
            }
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            // This is a workaround for a common issue seen when drawing Swing
            // components with transparent background on Windows.
            if (OSUtils.IS_WINDOWS)
            {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }

            super.paintComponent(g);
        }
    }
}
