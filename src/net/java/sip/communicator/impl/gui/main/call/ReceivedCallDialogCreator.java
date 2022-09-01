/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import com.drew.lang.annotations.Nullable;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.Logger;

/**
 * Creates dialogs displayed to the user when they receive an incoming call.
 * The class itself isn't a dialog, as the type of dialog that is created
 * varies depending on whether we are running on Mac.
 */
public class ReceivedCallDialogCreator extends ReceivedAlertDialogCreator
                                       implements ActionListener,
                                                  CallListener,
                                                  CallPeerListener
{
    private static final Logger sLog =
        Logger.getLogger(ReceivedCallDialogCreator.class);

    /**
     * The title of this dialog (only visible on Mac).
     */
    private static final String TITLE =
        resources.getI18NString("service.gui.INCOMING_CALL_TITLE");

    /**
     * The id used to find resources specific to this dialog.
     */
    private static final String RESOURCE_ID = "call";

    /**
     * The incoming call.
     */
    private final Call mIncomingCall;

    /**
     * The call peer from the incoming call.
     */
    private final CallPeer mCallPeer;

    /**
     * The display name to use for the person who is calling us.
     */
    private String mDisplayName;

    /**
     * The phone number that is calling us.
     */
    private final String mCallingNumber;

    /**
     * The description text to display in the dialog.
     */
    private String mDescriptionText;

    /**
     * The Basic Telephony OperationSet associated with the incoming call.
     */
    private final OperationSetBasicTelephony<?> mOpSetBasicTel;

    /**
     * Creates a <tt>ReceivedCallDialogCreator</tt> by specifying the
     * associated call.
     *
     * @param call The incoming call associated with this dialog.
     * @throws IllegalStateException if no call peers are present or no OperationSetBasicTelephony is found
     */
    public ReceivedCallDialogCreator(Call call)
    {
        super(TITLE, RESOURCE_ID, getMetaContact(call));

        // Add a call listener to the call so we can dismiss this dialog if the
        // call ends.
        mOpSetBasicTel = call.getProtocolProvider().getOperationSet(
                                              OperationSetBasicTelephony.class);
        if (mOpSetBasicTel == null)
        {
            // This shouldn't happen, but if we have no operation set, all we
            // can do is log and stop any more processing.
            String errorMessage = "No OperationSetBasicTelephony found";
            sLog.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        mOpSetBasicTel.addCallListener(this);
        mIncomingCall = call;

        // Save the details of the call peer to display in the UI.
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();
        if (callPeers.hasNext())
        {
            mCallPeer = callPeers.next();
            String address = mCallPeer.getAddress();
            mCallingNumber = (address == null) ? null : address.split("@")[0];

            String displayName = mCallPeer.getDisplayName();

            if (StringUtils.isNullOrEmpty(displayName, true) ||
                displayName.equalsIgnoreCase(mCallingNumber))
            {
                // No display name, so leave that null.  Places using mDisplayName
                // should provide a sensible default when it's null.
                // As the phone number is already displayed in the display
                // name, there's no need to include it in the description, so
                // just use standard "incoming call" text.
                mDescriptionText =
                    resources.getI18NString("service.gui.INCOMING_CALL_STATUS");
            }
            else
            {
                // We have a display name that is different from the phone
                // number so use it.
                mDisplayName = displayName;

                // As the phone number is not displayed as part of the display
                // name, display the nicely formatted number it in the
                // description text.
                mDescriptionText = resources.getI18NString(
                    "service.gui.call.CALL_FROM", new String[]{getDisplayNumber()});
            }

            // Listen for the display name changing - usually indicates that
            // we've found a name from LDAP or a CRM lookup.
            mCallPeer.addCallPeerListener(this);
        }
        else
        {
            /*
             * This might occur because of a race condition where the peers
             * from the call are removed just before this ReceivedCallDialogCreator
             * gets created.
             * All we can do is log and stop any more processing
             */
            String errorMessage = "No call peers found";
            sLog.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        sLog.info("Creating received call dialog for " +
                  logHasher(mDisplayName) + ": " + mCallingNumber);

        // We should never have more than one call peer, but check for it.  If
        // it does happen, log and carry on using the first call peer.
        int callPeerCount = mIncomingCall.getCallPeerCount();
        if (callPeerCount > 1)
        {
            sLog.warn("Multiple call peers found: " + callPeerCount);
        }

        // Set the status of the CRM lookup for the calling party so the CRM
        // button can be created appropriately.
        mCrmLookupCompleted = mCallPeer.isCrmLookupCompleted();
        mCrmLookupSuccessful = mCallPeer.isCrmLookupSuccessful();

        initComponents(true);

        // The super will have set an accessibility name, but actually at the time it
        // did we had not determined the correct description string, so we
        // set it again now that information is available
        AccessibilityUtils.setName(alertWindow, getDescriptionText());
    }

    /**
     * Returns the MetaContact associated with CallPeer associated with the
     * given Call, or null if no MetaContact exists.
     *
     * @param call the Call
     *
     * @return the MetaContact, or null if none exists.
     */
    private static MetaContact getMetaContact(Call call)
    {
        MetaContact metaContact = null;
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();
        if (callPeers.hasNext())
        {
            CallPeer peer = callPeers.next();
            metaContact = peer.getMetaContact();
        }

        return metaContact;
    }

    @Override
    public void okButtonPressed(boolean mouse)
    {
        sLog.info("Call accepted from " + logHasher(mDisplayName) + ": " + mCallingNumber);
        mIncomingCall.setUserPerceivedCallStartTime(System.currentTimeMillis());
        sendAnalyticsEvent(AnalyticsEventType.INCOMING_ANSWERED, mouse);
        CallManager.answerCall(mIncomingCall);
        super.okButtonPressed(mouse);
    }

    @Override
    public void cancelButtonPressed(boolean mouse)
    {
        sLog.info("Call rejected from " + logHasher(mDisplayName) + ": " + mCallingNumber);
        sendAnalyticsEvent(AnalyticsEventType.INCOMING_REJECT, mouse);
        CallManager.hangupCall(mIncomingCall);
    }

    @Override
    protected String getDescriptionText()
    {
        return mDescriptionText;
    }

    @Override @Nullable
    protected String getDisplayName()
    {
        return mDisplayName;
    }

    @Override
    protected String getPhoneNumber()
    {
        return mCallingNumber;
    }

    @Override
    protected BufferedImageFuture getContactImage()
    {
        BufferedImageFuture contactImage = null;

        // If we have a MetaContact, see if we have an avatar for them.
        if (mMetaContact != null)
        {
            contactImage = mMetaContact.getAvatar();
        }

        // If we didn't get an image from a MetaContact, try to find one for
        // the call peer.
        if (contactImage == null || (contactImage.resolve() == null))
        {
            contactImage = CallManager.getPeerImage(mCallPeer);
        }

        return contactImage;
    }

    @Override
    public void dispose()
    {
        try
        {
            if (mOpSetBasicTel != null)
            {
                mOpSetBasicTel.removeCallListener(this);
            }

            if (mCallPeer != null)
            {
                mCallPeer.removeCallPeerListener(this);
            }
        }
        finally
        {
            super.dispose();
        }
    }

    /**
     * Send an analytics event saying how the user interacted with the call
     * dialog.
     *
     * @param event the event to send to the analytics server
     * @param mouse True if the button was pressed with the mouse, false if by keyboard.
     */
    private void sendAnalyticsEvent(AnalyticsEventType event, boolean mouse)
    {
        int numberCalls = CallManager.getInProgressCalls().size();
        GuiActivator.getAnalyticsService().onEvent(event,
            AnalyticsParameter.NAME_NUM_CALLS_IN_PROGRESS,
            String.valueOf(numberCalls),
            AnalyticsParameter.NAME_DIALOG_ANSWERED_WITH,
            (mouse ? AnalyticsParameter.VALUE_MOUSE : AnalyticsParameter.VALUE_KEYBOARD));
    }

    /**
     * {@inheritDoc}
     *
     * When the <tt>Call</tt> depicted by this dialog is (remotely) ended,
     * close/dispose of this dialog.
     *
     * @param event a <tt>CallEvent</tt> which specifies the <tt>Call</tt> that
     * has ended
     */
    public void callEnded(CallEvent event)
    {
        if (event.getSourceCall().equals(mIncomingCall))
            dispose();
    }

    public void incomingCallReceived(CallEvent event) {}

    public void outgoingCallCreated(CallEvent event) {}

    @Override
    public void peerDisplayNameChanged(CallPeerChangeEvent evt)
    {
        Object newValue = evt.getNewValue();
        sLog.info("Call display name changed on " + mCallPeer);

        if (newValue instanceof String)
        {
            mDescriptionText = resources.getI18NString(
                   "service.gui.call.CALL_FROM", new String[]{getDisplayNumber()});
            mDisplayName = (String) newValue;

            SwingUtilities.invokeLater(() ->
            {
                updateDescriptionLabel(mDescriptionText);
                updateHeadingLabel(mDisplayName);

                mCrmLookupCompleted = mCallPeer.isCrmLookupCompleted();
                mCrmLookupSuccessful = mCallPeer.isCrmLookupSuccessful();
                updateCrmButton();
            });
        }
    }

    @Override
    public void peerAddressChanged(CallPeerChangeEvent evt) {}

    @Override
    public void peerStateChanged(CallPeerChangeEvent evt) {}

    @Override
    public void peerImageChanged(CallPeerChangeEvent evt) {}

    @Override
    public void peerTransportAddressChanged(CallPeerChangeEvent evt) {}
}
