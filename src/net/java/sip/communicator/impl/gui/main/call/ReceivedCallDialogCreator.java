/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import static org.jitsi.util.Hasher.logHasher;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import com.google.common.annotations.VisibleForTesting;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import org.jitsi.util.CustomAnnotations.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.analytics.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.ServiceUtils;

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
     * The extra description text to display in the dialog, may contain MLHG info.
     */
    private final String mExtraText;

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
        super(RESOURCE_ID, getMetaContact(call));

        // Add a call listener to the call so that we can dismiss this dialog if the
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

            String extraDisplayValue = null;
            String analyticsValue = null;

            // Get any associated diversion information
            final NameAndNumber diversionValue = extractDiversionValue(mCallPeer.getDiversionInfo());

            if (diversionValue.name != null)
            {
                extraDisplayValue = diversionValue.name;
                analyticsValue = AnalyticsParameter.VALUE_SIGNALLED;
            }
            else if (diversionValue.number != null)
            {
                // First we see if we can get a contact name for this number
                MetaContactListService metaContactListService = ServiceUtils.getService(
                        GuiActivator.bundleContext,
                        MetaContactListService.class);

                if (metaContactListService != null)
                {
                    List<MetaContact> metaContactList = metaContactListService
                            .findMetaContactByNumber(diversionValue.number);

                    if (metaContactList.size() > 0)
                    {
                        // If there are multiple contacts, then we simply use the first.
                        // Other parts of the code try and do better, but that is
                        // false accuracy here.
                        extraDisplayValue = metaContactList.get(0).getDisplayName();
                        analyticsValue = AnalyticsParameter.VALUE_CONTACT_MATCH;
                    }
                }

                if (extraDisplayValue == null)
                {
                    // We didn't find a matching contact, so just format the existing number
                    extraDisplayValue = DesktopUtilActivator.getPhoneNumberUtils().formatNumberForDisplay(diversionValue.number);
                    analyticsValue = AnalyticsParameter.VALUE_NO_MATCH;
                }
            }

            if (analyticsValue != null)
            {
                GuiActivator.getAnalyticsService().onEvent(AnalyticsEventType.INCOMING_DIVERTED_CALL,
                                                           AnalyticsParameter.PARAM_DIVERSION_NAME,
                                                           analyticsValue);
            }

            // If we have a value to display, then use it to set the extra text field
            mExtraText = extraDisplayValue == null ? null :
                resources.getI18NString("service.gui.call.CALL_VIA", new String[]{extraDisplayValue});

            // We have already set the accessibility name in the superclass.
            // Fortunately if we have extra information we can set that as the description.
            if (extraDisplayValue != null)
            {
                AccessibilityUtils.setDescription(alertWindow, mExtraText);
            }
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
                  logHasher(mDisplayName) + ": " + logHasher(mCallingNumber));

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

    @VisibleForTesting
    public static class NameAndNumber
    {
        public final String name;
        public final String number;

        public NameAndNumber(String name, String number)
        {
            this.name = name;
            this.number = number;
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            NameAndNumber that = (NameAndNumber) o;
            return Objects.equals(name, that.name) && Objects.equals(number, that.number);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(name, number);
        }
    }

    // Parse the value from the diversion header, to get the text that we should
    // use in the UI.
    // Effectively a port of the Android code SIPURIParser, without fixing any
    // of the bugs there!
    // Specifically, I believe this is a valid diversion header
    //   Diversion: <sip:userA>;count=23
    // Note the sip address is an alphabetic name which does not contain an @.
    // This code specifically handles the absence of the @ by keeping
    // numeric digits from the remainder of the string, and thus ends up
    // using the digits from the count at the end of the header.
    @VisibleForTesting
    public static NameAndNumber extractDiversionValue(String diversion)
    {
        String name = null;
        String number = null;
        if (diversion != null)
        {
            // We expect various parts in the diversion info
            // - an optional friendly name
            // - an address
            // - optional parameters (not specifically accounted for in the Android code we have ported)
            String uri = diversion;
            if (diversion.startsWith("\""))
            {
                // A friendly name is present.
                // Get the display name from the URI and replace escaped characters.
                name = diversion.substring(1, diversion.lastIndexOf('"'))
                        .replaceAll("\\\\([\\\\|\"])", "$1");

                // I'm not sure if there is another bug here - the parameters
                // may include string values, and if so it's possible that the
                // < or > characters could appear in those strings.
                uri = diversion.substring(diversion.lastIndexOf('<'), diversion.lastIndexOf('>'));
            }

            // I believe the absence of an @ handling is bugged;
            // - a sip address without an @ may be alphabetic, not numeric
            // - even if numeric, we should not include digits that appear after the end of the sip url
            int numberEndIndex = uri.indexOf('@');
            // Note unlike the Android code, we don't do % decoding here,
            // that uses an Android only method, and I see no evidence that
            // the data will be encoded using that method anyway.
            number = uri.substring(uri.indexOf(':') + 1, numberEndIndex < 0 ? uri.length() : numberEndIndex);

            // Strip out invalid chars.
            number = number.replaceAll("[^0-9#*+]+", "");
        }

        return new NameAndNumber(name, number);
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
    public void okButtonPressed(String answeredWithAnalyticParam)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> okButtonPressed(answeredWithAnalyticParam));
            return;
        }

        sLog.info("Call accepted from " + logHasher(mDisplayName) + ": " + logHasher(mCallingNumber));
        mIncomingCall.setUserPerceivedCallStartTime(System.currentTimeMillis());
        sendAnalyticsEvent(AnalyticsEventType.INCOMING_ANSWERED, answeredWithAnalyticParam);
        CallManager.answerCall(mIncomingCall);
        super.okButtonPressed(answeredWithAnalyticParam);
    }

    @Override
    public void cancelButtonPressed(String answeredWithAnalyticParam)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(() -> cancelButtonPressed(answeredWithAnalyticParam));
            return;
        }

        sLog.info("Call rejected from " + logHasher(mDisplayName) + ": " + logHasher(mCallingNumber));
        sendAnalyticsEvent(AnalyticsEventType.INCOMING_REJECT, answeredWithAnalyticParam);
        CallManager.hangupCall(mIncomingCall);
    }

    @Override
    protected String getDescriptionText()
    {
        return mDescriptionText;
    }

    @Override
    protected String getExtraText()
    {
        return mExtraText;
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
     * @param answeredWithAnalyticParam - value to use in analytics for the
     *        AnalyticsParameter.NAME_DIALOG_ANSWERED_WITH param.
     */
    private void sendAnalyticsEvent(AnalyticsEventType event, String answeredWithAnalyticParam)
    {
        int numberCalls = CallManager.getInProgressCalls().size();
        GuiActivator.getAnalyticsService().onEvent(event,
            AnalyticsParameter.NAME_NUM_CALLS_IN_PROGRESS,
            String.valueOf(numberCalls),
            AnalyticsParameter.NAME_DIALOG_ANSWERED_WITH,
            answeredWithAnalyticParam);
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
