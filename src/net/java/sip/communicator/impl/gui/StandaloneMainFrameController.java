// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui;

import java.net.URL;
import java.util.ResourceBundle;

import org.jitsi.util.StringUtils;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.imageloader.ImageLoaderService;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService;
import net.java.sip.communicator.service.protocol.globalstatus.GlobalStatusService.GlobalStatusChangeListener;
import net.java.sip.communicator.util.Logger;

/**
 * Controller for the StandaloneMainFrame FXML UI.
 */
public class StandaloneMainFrameController implements Initializable,
                                                      GlobalStatusChangeListener
{
    @FXML
    private Label displayNameLabel;

    @FXML
    private ImageView meetingIconImageView;

    @FXML
    private ImageView createMeetingIconImageView;

    @FXML
    private Label createMeetingButtonLabel;

    @FXML
    private Button createMeetingButton;

    @FXML
    private Label scheduleMeetingButtonLabel;

    @FXML
    private ImageView scheduleMeetingIconImageView;

    @FXML
    private Button joinMeetingButton;

    @FXML
    private Button upcomingMeetingButton;

    @FXML
    private Button recordedMeetingButton;

    @FXML
    private Label joinMeetingHintTextLabel;

    private static final Logger logger =
        Logger.getLogger(StandaloneMainFrameController.class);

    /**
     * The image loader service
     */
    private static final ImageLoaderService sImageLoaderService =
        GuiActivator.getImageLoaderService();

    /**
     * The conference service
     */
    private static final ConferenceService sConferenceService =
        GuiActivator.getConferenceService();

    /**
     * The global status service
     */
    private static final GlobalStatusService sGlobalStatusService =
        GuiActivator.getGlobalStatusService();

    @Override
    public void initialize(URL location, ResourceBundle resources)
    {
        // Register as a Global Status service change listener so that we know
        // if the user has gone into a conference or not.
        sGlobalStatusService.addStatusChangeListener(this);

        // Get the display name and set the display name in the UI
        String globalDisplayName = GuiActivator.getGlobalDisplayDetailsService()
            .getGlobalDisplayName();

        if(!StringUtils.isNullOrEmpty(globalDisplayName))
        {
            displayNameLabel.setText(globalDisplayName);
        }

        // Set up the meeting icon beside the display name
        Image meetingIcon =
            new Image(sImageLoaderService.getImageUri(
                ImageLoaderService.STANDALONE_MEETING_ICON));
        meetingIconImageView.setImage(meetingIcon);

        // Set up the create meeting icon on the create meeting button
        Image createMeetingIcon =
            new Image(sImageLoaderService.getImageUri(
                ImageLoaderService.STANDALONE_CREATE_MEETING_ICON));
        createMeetingIconImageView.setImage(createMeetingIcon);

        // Set up schedule meeting icon on the schedule meeting button
        Image scheduleMeetingIcon =
            new Image(sImageLoaderService.getImageUri(
                ImageLoaderService.STANDALONE_SCHEDULE_MEETING_ICON));
        scheduleMeetingIconImageView.setImage(scheduleMeetingIcon);

        // Set up the button strings in the UI
        createMeetingButtonLabel.setText(GuiActivator.getResources().
            getI18NString("service.gui.conf.STANDALONE_CREATE_CONFERENCE_BUTTON"));

        scheduleMeetingButtonLabel.setText(GuiActivator.getResources().
            getI18NString("service.gui.conf.STANDALONE_SCHEDULE_CONFERENCE_BUTTON"));

        joinMeetingButton.setText(GuiActivator.getResources().
            getI18NString("service.gui.conf.STANDALONE_JOIN_CONFERENCE_BUTTON"));

        upcomingMeetingButton.setText(GuiActivator.getResources().
            getI18NString("service.gui.conf.STANDALONE_UPCOMING_CONFERENCE_BUTTON"));

        recordedMeetingButton.setText(GuiActivator.getResources().
            getI18NString("service.gui.conf.STANDALONE_RECORDED_CONFERENCE_BUTTON"));

        // Set up the join meeting hint text string
        joinMeetingHintTextLabel.setText(GuiActivator.getResources().
            getI18NString("service.gui.conf.STANDALONE_JOIN_CONFERENCE_HINT_TEXT"));
    }

    @FXML
    protected void createMeetingButtonPressed(ActionEvent event)
    {
        logger.debug("Create Meeting button pressed");
        sConferenceService.createOrAdd(false);
    }

    @FXML
    protected void scheduleMeetingButtonPressed(ActionEvent event)
    {
        logger.debug("Schedule Meeting button pressed");
        sConferenceService.showScheduler();
    }

    @FXML
    protected void joinMeetingButtonPressed(ActionEvent event)
    {
        logger.debug("Join Meeting button pressed");
        sConferenceService.showJoinConferenceDialog();
    }

    @FXML
    protected void upcomingMeetingButtonPressed(ActionEvent event)
    {
        logger.debug("View Upcoming Meeting button pressed");
        sConferenceService.showScheduledConferences();
    }

    @FXML
    protected void recordedMeetingButtonPressed(ActionEvent event)
    {
        logger.debug("View Recorded Meeting button pressed");
        sConferenceService.showRecordedConferences();
    }

    @Override
    public void onStatusChanged()
    {
        logger.debug("Global status service changed to: " +
            sGlobalStatusService.getGlobalStatus());
        // If the user is in a conference, disable the join and create meeting
        // buttons
        if (sGlobalStatusService.isInConference())
        {
            logger.debug("User is in a meeting. Disabling the create and join "
                + "meeting buttons.");
            joinMeetingButton.setDisable(true);
            createMeetingButton.setDisable(true);
        }
        else
        {
            logger.debug("User is in a meeting. Enabling the create and join "
                + "meeting buttons.");
            joinMeetingButton.setDisable(false);
            createMeetingButton.setDisable(false);
        }
    }
}
