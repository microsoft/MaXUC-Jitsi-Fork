// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;

import javax.swing.*;

import org.jitsi.service.resources.BufferedImageFuture;
import org.jitsi.service.resources.ImageIconFuture;
import org.jitsi.util.swing.TransparentPanel;

import org.jitsi.util.CustomAnnotations.*;

import net.java.sip.communicator.service.analytics.AnalyticsParameter;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.phonenumberutils.PhoneNumberUtilsService;
import net.java.sip.communicator.util.Logger;

/**
 * Creates UI elements common to dialogs displayed to the user when they
 * receive an incoming alert. The class itself isn't a dialog, as the type of
 * dialog that is created varies depending on whether we are running on Mac.
 */
public abstract class ReceivedAlertDialogCreator
    extends AlertDialogCreator implements ActionListener
{
    private static final Logger sLog =
        Logger.getLogger(ReceivedAlertDialogCreator.class);

    /**
     * Phone number utils, used for format phone numbers for display in the UI.
     */
    private static final PhoneNumberUtilsService sPhoneNumberUtils =
                                            DesktopUtilActivator.getPhoneNumberUtils();

    /**
     * The UI service.
     */
    protected static final UIService sUiService =
                                            DesktopUtilActivator.getUIService();

    /**
     * The MetaContact who caused us to receive this alert.
     */
    protected final MetaContact mMetaContact;

    /**
     * The component that shows the description
     */
    private JLabel mDescriptionLabel;

    /**
     * The component that shows the heading text
     */
    private JLabel mHeadingLabel;

    /** Used to create and refresh the CRM button for the peer. */
    private CrmButtonSetter mCrmButtonSetter;

    /** Stores the CRM button. */
    private JPanel mCrmButtonPanel;

    /** Font to use for CRM buttons. */
    private static final Font CRM_FONT = new JLabel().getFont().deriveFont(
            Font.PLAIN, ScaleUtils.getScaledFontSize(11f));

    /**
     * Creates a new ReceivedConference Dialog.
     *
     * @param resourceId The id used to find resources specific to this dialog.
     * @param metaContact The MetaContact who caused us to receive this alert.
     */
    public ReceivedAlertDialogCreator(String resourceId,
                                      MetaContact metaContact)
    {
        super(resourceId);
        sLog.info("Creating received alert dialog");
        mMetaContact = metaContact;
    }

    /**
     * Initiates the dialog.
     */
    protected void initComponents()
    {
        super.initComponents();

        // Prepare the panel that would contain the CRM integration button, if
        // appropriate (or else it will be empty).
        mCrmButtonPanel = new TransparentPanel(new BorderLayout(0, 0));

        getCenterPanel().add(mCrmButtonPanel);

        createCrmButton();
    }

    @Override
    protected JButton createOkButton()
    {
        String imageRes = "service.gui.button." + mResourceType + ".ACCEPT";
        JButton okButton = createSIPCommSnakeButton(
            "service.gui." + mResourceType + ".ACCEPT", imageRes, OK_BUTTON_NAME, this);
        okButton.setForeground(Color.WHITE);
        return okButton;
    }

    @Override
    protected TransparentPanel createCancelButton()
    {
        return new RejectWithImButton(mMetaContact,
                                      mResourceType,
                                      new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                sLog.user("Alert rejected from " + mMetaContact);
                cancelButtonPressed(((e.getModifiers() & InputEvent.BUTTON1_MASK) != 0) ?
                    AnalyticsParameter.VALUE_MOUSE :
                    AnalyticsParameter.VALUE_KEYBOARD);
                dispose();
            }
        });
    }

    @Override
    public void okButtonPressed(String answeredWithAnalyticParam)
    {
        JButton crmButton = mCrmButtonSetter.getCrmButtonIfVisible();

        sLog.user("Alert accepted from " + mMetaContact + " CRM button? " +
                          (crmButton != null));

        // If we're set to auto-launch CRM integration,
        // and the CRM button is visible, launch the integration.
        if (crmButton != null)
        {
            if (sUiService.isCrmAutoLaunchAlways() ||
                (sUiService.isCrmAutoLaunchExternal() &&
                 ((mMetaContact == null) || (mMetaContact.getBGContact() == null))))
            {
                sLog.debug("Opening CRM");
                crmButton.doClick();
            }
        }
    }

    @Override
    protected JComponent createDescriptionComponent()
    {
        JComponent component;
        JLabel description = createDescriptionLabel(getDescriptionText());
        String extra = getExtraText();
        if (extra != null)
        {
            JLabel extraLabel = new JLabel(extra);
            extraLabel.setFocusable(true);
            extraLabel.setFont(description.getFont());
            extraLabel.setForeground(description.getForeground());
            if (description == null)
            {
                component = extraLabel;
            }
            else
            {
                component = new TransparentPanel(new GridLayout(0,1));
                component.add(description);
                component.add(extraLabel);
            }
        }
        else
        {
            component = description;
        }

        return component;
    }

    /**
     * Creates the CRM button and inserts it into the mCrmButtonPanel, if applicable.
     */
    private void createCrmButton()
    {
        sLog.debug("Creating CRM lookup button");

        // The number to use in the lookup.
        String phoneNumber = getPhoneNumber();

        // Create the CRM button setter and try to create the CRM button.
        // If a button can be created, it will be inserted into the
        // crmButtonPanel.
        //
        // N.B. - this will always create a CRM button (barring any further
        // lower level restrictions).
        mCrmButtonSetter = new CrmButtonSetter(getDisplayName(),
                                               phoneNumber,
                                               false,
                                               mCrmButtonPanel);

        mCrmButtonSetter.createButton();

        // If a button was created, set its font.
        setCrmButtonFont();

        getCenterPanel().revalidate();
        getCenterPanel().repaint();
    }

    /**
     * Sets the font and color of the CRM button, if it has been created.
     */
    private void setCrmButtonFont()
    {
        if (mCrmButtonSetter.getButton() != null)
        {
            mCrmButtonSetter.getButton().setFont(CRM_FONT);
            mCrmButtonSetter.getButton().setForeground(Color.WHITE);
        }
    }

    @Override
    protected JLabel createDescriptionLabel(String text)
    {
        // Over-ridden so that we can hold a reference to the label, so
        // that it can be updated if the display name changes
        mDescriptionLabel = super.createDescriptionLabel(text);
        mDescriptionLabel.setFocusable(true);

        return mDescriptionLabel;
    }

    @Override
    protected JLabel createHeadingLabel(String headingText)
    {
        // Over-ridden so that we can hold a reference to the label, so
        // that it can be updated if the display name changes
        mHeadingLabel = super.createHeadingLabel(headingText);
        mHeadingLabel.setFocusable(true);

        return mHeadingLabel;
    }

    /**
     * Update the text in the description label.
     *
     * @param newText the new text to display
     */
    protected void updateDescriptionLabel(String newText)
    {
        if (mDescriptionLabel != null)
        {
            mDescriptionLabel.setText(newText);
        }
    }

    /**
     * Update the text in the heading label.
     *
     * @param newText the new text to display
     */
    protected void updateHeadingLabel(String newText)
    {
        if (mHeadingLabel != null)
        {
            mHeadingLabel.setText(newText);
        }
    }

    protected String getDisplayNumber()
    {
        return sPhoneNumberUtils.formatNumberForDisplay(getPhoneNumber());
    }

    @Override
    protected String getHeadingText()
    {
        String displayName = getDisplayName();
        return ((displayName != null) ? displayName : getDisplayNumber());
    }

    @Override
    protected ImageIconFuture getIconImage()
    {
        ImageIconFuture imageIcon;

        // Use the contact's avatar for the icon if we have one, otherwise use
        // the default user photo.
        BufferedImageFuture image = getContactImage();
        if (image != null && (image.resolve() != null))
        {
            imageIcon =
                ImageUtils.getScaledEllipticalIcon(image, ICON_SIZE, ICON_SIZE);
        }
        else
        {
            imageIcon = ImageUtils.getScaledEllipticalIcon(
                sImageLoaderService.getImage(ImageLoaderService.DEFAULT_USER_PHOTO),
                ICON_SIZE,
                ICON_SIZE);
        }

        return imageIcon;
    }

    /**
     * @return the display name of the contact who caused us to receive this
     * alert.
     */
    @Nullable
    protected abstract String getDisplayName();

    /**
     * @return the phone number of the contact who caused us to receive this
     * alert.
     */
    @Nullable
    protected abstract String getPhoneNumber();

    /**
     * @return the image of the contact who caused us to receive this alert.
     */
    protected abstract BufferedImageFuture getContactImage();
}
