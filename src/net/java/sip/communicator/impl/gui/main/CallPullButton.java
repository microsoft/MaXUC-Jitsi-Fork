// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.Timer;

import javax.swing.*;

import com.metaswitch.maxanalytics.event.CallKt;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.calljump.*;
import net.java.sip.communicator.service.calljump.CallJumpService.*;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * A clickable banner displayed at the bottom of the main frame when the user has a call available to pull.
 */
public class CallPullButton extends AbstractPluginComponent implements CallJumpComponent, CPCosGetterCallback
{
    private static final Logger sLog = Logger.getLogger(CallPullButton.class);

    private static final ResourceManagementService sResources = GuiActivator.getResources();
    private static final CallJumpService sCallJumpService = GuiActivator.getCallJumpService();
    private static final MetaContactListService sContactListService = GuiActivator.getContactListService();

    private static final int DIALOG_TIMEOUT = 3000;
    private static final int BUTTON_DISABLED_TIMEOUT = 1500;

    private static final Color DEFAULT_COLOR = new Color(sResources.getColor("service.gui.CALL_PULL_BUTTON"));
    private static final Color HOVER_COLOR = new Color(sResources.getColor("service.gui.CALL_PULL_BUTTON_HOVER"));
    private static final Color PRESSED_COLOR = new Color(sResources.getColor("service.gui.CALL_PULL_BUTTON_PRESSED"));
    private static final Color TEXT_COLOR = new Color(sResources.getColor("service.gui.CALL_PULL_BUTTON_TEXT"));

    private JPanel mComponent;
    private CallData mCallData;
    private CallPullDialog mDialog;

    private boolean mIsCallJumpAllowed = false;
    private boolean mIsCallAvailableToPull = false;

    private static final ErrorDialog sErrorDialog = new ErrorDialog(
            sResources.getI18NString("service.calljump.CALL_PULL_ERROR_DIALOG_TITLE"),
            sResources.getI18NString("service.calljump.CALL_PULL_ERROR_DIALOG_MESSAGE"));

    public CallPullButton()
    {
        super(Container.CONTAINER_MAIN_WINDOW);
    }

    @Override
    public void onCosReceived(CPCos classOfService)
    {
        mIsCallJumpAllowed = classOfService.getSubscribedMashups().isCallJumpAllowed();

        SwingUtilities.invokeLater(() -> mComponent.setVisible(mIsCallJumpAllowed && mIsCallAvailableToPull));
    }

    @Override
    public void setEnabled(boolean enabled, CallData callData)
    {
        if ((mDialog != null) && (!enabled || !mCallData.equals(callData)))
        {
            SwingUtilities.invokeLater(() ->
            {
                sLog.debug("Changing call pull dialog message since call no longer available to pull");
                mDialog.setMessage(sResources.getI18NString("service.calljump.CALL_PULL_DIALOG_CALL_UNAVAILABLE"));
                mDialog.setOkButtonEnabled(false);
            });

            new Timer().schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    if (mDialog != null)
                    {
                        sLog.debug("Closing call pull dialog since call no longer available to pull");
                        SwingUtilities.invokeLater(() -> mDialog.close());
                    }
                }
            }, DIALOG_TIMEOUT);
        }

        mCallData = callData;
        mIsCallAvailableToPull = enabled;

        SwingUtilities.invokeLater(() ->
        {
            sLog.debug("Setting button visibility to " + (mIsCallAvailableToPull && mIsCallJumpAllowed));
            mComponent.setVisible(mIsCallJumpAllowed && mIsCallAvailableToPull);
        });
    }

    @Override
    public void onDataError(CallJumpResult errorStatus)
    {
        // An error occurred attempting to pull the call - show the user a dialog
        SwingUtilities.invokeLater(() ->
        {
            sErrorDialog.showDialog();
        });
    }

    @Override
    public String getName()
    {
        return null;
    }

    @Override
    public Object getComponent()
    {
        if (mComponent == null)
        {
            mComponent = new JPanel();
            mComponent.setBackground(DEFAULT_COLOR);

            JLabel label = new JLabel(sResources.getI18NString("service.calljump.CALL_PULL_BANNER"));
            label.setForeground(TEXT_COLOR);
            label.setFocusable(true);
            label.addKeyListener(new KeyAdapter() {
                @Override
                public void keyTyped(KeyEvent e)
                {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER)
                    {
                        onClick();
                    }
                }
            });
            AccessibilityUtils.setNameAndDescription(label, label.getText(),
                                                     sResources.getI18NString("service.gui.accessibility.CALL_PULL_BANNER"));

            mComponent.add(label);
            mComponent.setVisible(false);

            mComponent.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            mComponent.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseEntered(MouseEvent e)
                {
                    mComponent.setBackground(HOVER_COLOR);
                }

                @Override
                public void mouseExited(MouseEvent e)
                {
                    mComponent.setBackground(DEFAULT_COLOR);
                }

                @Override
                public void mousePressed(MouseEvent e)
                {
                    mComponent.setBackground(PRESSED_COLOR);
                }

                @Override
                public void mouseReleased(MouseEvent e)
                {
                    mComponent.setBackground(HOVER_COLOR);
                }

                @Override
                public void mouseClicked(MouseEvent e)
                {
                    onClick();
                }
            });

            sCallJumpService.registerForCallPullUpdates(this);
            GuiActivator.getCosService().getClassOfService(this);
        }

        return mComponent;
    }

    public void openDialog(CallData callData){
        this.getComponent();
        this.setEnabled(true, callData);
        this.onClick();
    }

    /**
     * Called when the user clicks this button.
     */
    private void onClick()
    {
        sLog.user("'Call Available to Pull' pop-up clicked");
        GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.CALL_PULLED);
        GuiActivator.getInsightService().logEvent(CallKt.EVENT_CALL_PULL);

        if (mCallData != null)
        {
            mDialog = new CallPullDialog();
            int returnValue = mDialog.showDialog();
            mDialog = null;

            if (returnValue == MessageDialog.OK_RETURN_CODE)
            {
                mComponent.setEnabled(false);
                sCallJumpService.pull(mCallData, this);

                new Timer().schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                       mComponent.setEnabled(true);
                    }
                }, BUTTON_DISABLED_TIMEOUT);
            }
        }
    }

    /**
     * A dialog presented to the user to provide further call details and prompting the user to confirm that they
     * wish to continue with the call pull. If the call becomes unavailable to pull while the dialog is showing,
     * it is updated to reflect this.
     */
    private class CallPullDialog extends MessageDialog
    {
        private CallPullDialog()
        {
            super(null,
                  sResources.getI18NString("service.calljump.CALL_PULL_DIALOG_TITLE"),
                  null,
                  sResources.getI18NString("service.calljump.CALL_PULL_DIALOG_BUTTON"),
                  new ImageID("service.gui.icons.PULL_CALL"),
                  false,
                  true);

            // This dialog should be the same size and at the same location as the call dialog
            setSaveSizeAndLocationName(CallFrame.class.getName());

            setMinimumSize(CallFrame.DEFAULT_AUDIO_SIZE);
            setSize(CallFrame.DEFAULT_AUDIO_SIZE);

            String displayName;
            String callerNumber = mCallData.getCallerNumber();

            if (callerNumber != null)
            {
                List<MetaContact> metaContacts = sContactListService.findMetaContactByNumber(callerNumber);

                switch (metaContacts.size())
                {
                    case 0:
                        displayName = callerNumber;
                        break;
                    case 1:
                        displayName = metaContacts.get(0).getDisplayName();
                        break;
                    case 2:
                        displayName = sResources.getI18NString("impl.protocol.sip.X_OR_1_OTHER",
                                                               new String[]{metaContacts.get(0).getDisplayName()});
                        break;
                    default:
                        displayName = sResources.getI18NString("impl.protocol.sip.X_OR_N_OTHERS",
                                                               new String[]{metaContacts.get(0).getDisplayName(),
                                                                            String.valueOf(metaContacts.size() - 1)});
                        break;
                }
            }
            else
            {
                displayName = sResources.getI18NString("service.gui.ANONYMOUS");
            }

            setMessage(sResources.getI18NString("service.calljump.CALL_PULL_DIALOG_MESSAGE", new String[]{displayName}));
        }

        /**
         * Closes the dialog. Has the same effect as the user clicking the Cancel button.
         */
        private void close()
        {
            close(false);
        }
    }
}
