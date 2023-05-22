// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.*;
import javax.swing.*;

import org.jitsi.util.CustomAnnotations.*;

import net.java.sip.communicator.impl.gui.GuiActivator;
import net.java.sip.communicator.plugin.desktoputil.ErrorDialog;
import net.java.sip.communicator.plugin.desktoputil.SIPCommSnakeButton;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.service.websocketserver.WebSocketApiCrmService;
import net.java.sip.communicator.service.websocketserver.WebSocketApiMessageMap;
import net.java.sip.communicator.service.websocketserver.WebSocketApiRequestListener;
import net.java.sip.communicator.util.Logger;
import org.jitsi.service.resources.ResourceManagementService;

/**
 * Contains UI helper methods for the functions provided by the WebSocket API.
 */
public class WebSocketApiServicesTools
{
    private static final Logger sLog = Logger.getLogger(
            WebSocketApiServicesTools.class);

    /** Alpha value to use for CRM placeholder text. */
    private static final float CRM_PLACEHOLDER_TEXT_ALPHA = 0.5f;

    /**
     * Creates a CRM launch button with an action listener that will send a
     * WebSocket API request to launch CRM integration, and raise an error
     * prompt if the request fails.
     *
     * No button is created if the CRM service is not available, or if
     * the passed in number is bad (since all lookups would fail in that case).
     *
     * @param number The DN to launch CRM integration for.
     * @return The CRM launch button.
     */
    public SIPCommSnakeButton createCrmLaunchButton(@Nullable String number)
    {
        sLog.entry();
        WebSocketApiCrmService crmService =
                GuiActivator.getWebSocketApiCrmService();

        // Don't do any processing if:
        //     1. The CRM service is not available
        //     2. The CRM service is not ready
        //     3. The number passed in can't be parsed for CRM lookups - in this
        //      case all lookups will fail, so there is no point in creating
        //      a button
        if (crmService != null &&
            crmService.readyForCrmIntegration() &&
            crmService.canParseCrmAddress(number))
        {
            sLog.debug("Creating WebSocket CRM button for " +
                                 logHasher(number));

            String crmLaunchButtonText =
                    GuiActivator.getResources().getI18NString(
                            "service.websocketserver.WEBSOCKET_CRM_LAUNCH_ACTION_BUTTON_TEXT");

            final SIPCommSnakeButton crmLaunchButton = new SIPCommSnakeButton(
                    crmLaunchButtonText,
                    "service.gui.button.crm",
                    false,
                    true);

            crmLaunchButton.setFont(crmLaunchButton.getFont().deriveFont(
                    ScaleUtils.getMediumLargeFontSize()));

            crmLaunchButton.addActionListener(e -> {

                // Note - do not check if the CRM service is ready or available.
                // This should have been checked when first creating the
                // CRM button, and for smoother UX, we want to leave the
                // button clickable even if the CRM service has become
                // unavailable since.

                // Disable the button while the request is in progress.
                crmLaunchButton.setEnabled(false);

                sLog.debug("Initiating CRM launch action");
                crmService.openCrmContactPage(
                        number,
                        new CrmLaunchButtonListener(crmLaunchButton));
            });

            sLog.exit();
            return crmLaunchButton;
        }
        else
        {
            sLog.exit();
            return null ;
        }
    }

    /**
     * @return A label with a string indicating a CRM lookup is in progress, to
     * be used in the place of the CRM launch button during the lookup.
     *
     * No label is created if the CRM service is not ready for processing.
     */
    public JLabel createCrmSearchPlaceholderLabel()
    {
        return createLabelFromResource(
                "service.websocketserver.WEBSOCKET_CRM_DN_LOOKUP_IN_PROCESS_TEXT");
    }

    /**
     * @return A label with a string indicating a CRM lookup failed to find a
     * match.
     *
     * No label is created if the CRM service is not ready for processing.
     */
    public JLabel createNoCrmMatchLabel()
    {
        return createLabelFromResource(
                "service.websocketserver.WEBSOCKET_CRM_DN_LOOKUP_NO_MATCH");
    }

    /**
     * Creates a JLabel to display text in the place of the CRM lookup button.
     *
     * @param res The name of the string resource to display.
     * @return The JLabel containing the required text.
     */
    private JLabel createButtonReplacementText(String res)
    {
        String labelText = GuiActivator.getResources().getI18NString(res);

        JLabel label = new JLabel();
        label.setText(labelText);

        label.setFont(label.getFont().deriveFont(ScaleUtils.getMediumLargeFontSize()));

        // Set the appropriate text opacity.
        Color foregroundColour = label.getForeground();
        label.setForeground(
                new Color(foregroundColour.getRed(),
                          foregroundColour.getGreen(),
                          foregroundColour.getBlue(),
                          (int) (CRM_PLACEHOLDER_TEXT_ALPHA * 255)));

        return label;
    }

    /**
     * @return A JLabel with the given resource string.
     *
     * No label is created if the CRM service is not ready for processing.
     */
    private JLabel createLabelFromResource(String res)
    {
        sLog.entry();
        WebSocketApiCrmService crmService =
                GuiActivator.getWebSocketApiCrmService();

        // Don't do any processing if:
        //     1. The CRM service is not available
        //     2. The CRM service is not ready
        if (crmService != null &&
            crmService.readyForCrmIntegration())
        {
            sLog.debug("Creating WebSocket JLabel for resource " + res);

            sLog.exit();
            return createButtonReplacementText(
                    res);
        }
        else
        {
            sLog.exit();
            return null;
        }
    }

    /**
     * Listens for WebSocket API responses to CRM launch requests so it can
     * re-enable the CRM launch button once the request completes, and
     * potentially create an error prompt (if the request failed), but doesn't
     * do anything for successful requests - it is expected that the connected
     * application will step in in those cases.
     */
    private class CrmLaunchButtonListener implements WebSocketApiRequestListener
    {
        /** Stores the CRM launch button. */
        private SIPCommSnakeButton mCrmLaunchButton;

        CrmLaunchButtonListener(SIPCommSnakeButton crmLaunchButton)
        {
            mCrmLaunchButton = crmLaunchButton;
        }

        /**
         * @param success Whether the request was successful.
         * @param responseMessage The WebSocket API message map.
         */
        @Override
        public void responseReceived(boolean success,
                                     WebSocketApiMessageMap responseMessage)
        {
            if (success)
            {
                // Nothing to do - the application connected over the API will
                // take care of further processing.
                sLog.debug("CRM launch action successful");
            }
            else
            {
                requestTerminated();
            }

            mCrmLaunchButton.setEnabled(true);
        }

        /**
         * Raises an error prompt to the user to let them know something went
         * wrong with the request.
         */
        @Override
        public void requestTerminated()
        {
            sLog.debug("CRM launch action unsuccessful");

            // Show an error prompt to the user.
            ResourceManagementService res =
                    GuiActivator.getResources();

            String errorTitle = res.getI18NString(
                    "service.websocketserver.WEBSOCKET_CRM_LAUNCH_ACTION_FAILED_ERROR_TITLE");
            String errorMessage = res.getI18NString(
                    "service.websocketserver.WEBSOCKET_CRM_LAUNCH_ACTION_FAILED_ERROR_TEXT");

            new ErrorDialog(errorTitle, errorMessage).showDialog();

            // Re-enable the button.
            mCrmLaunchButton.setEnabled(true);
        }
    }
}
