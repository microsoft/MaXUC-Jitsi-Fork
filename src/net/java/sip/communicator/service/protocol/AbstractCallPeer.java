/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.protocol;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatAddress;

import java.util.*;

import org.jitsi.service.protocol.event.*;
import org.jitsi.util.event.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Provides a default implementation for most of the <tt>CallPeer</tt> methods
 * with the purpose of only leaving custom protocol development to clients using
 * the PhoneUI service.
 *
 * @param <T> the call extension class like for example <tt>CallSipImpl</tt>
 * or <tt>CallJabberImpl</tt>
 * @param <U> the provider extension class like for example
 * <tt>ProtocolProviderServiceSipImpl</tt> or
 * <tt>ProtocolProviderServiceJabberImpl</tt>
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Yana Stamcheva
 */
public abstract class AbstractCallPeer<T extends Call,
                                       U extends ProtocolProviderService>
    extends PropertyChangeNotifier
    implements CallPeer
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(AbstractCallPeer.class);

    /**
     * The constant which describes an empty set of <tt>ConferenceMember</tt>s
     * (and which can be used to reduce allocations).
     */
    public static final ConferenceMember[] NO_CONFERENCE_MEMBERS
        = new ConferenceMember[0];

    /**
     * The time this call started at.
     */
    private long callDurationStartTime = CALL_DURATION_START_TIME_UNKNOWN;

    /**
     * The list of <tt>CallPeerConferenceListener</tt>s interested in and to be
     * notified about changes in conference-related information such as this
     * peer acting or not acting as a conference focus and conference membership
     * details.
     */
    protected final List<CallPeerConferenceListener>
        callPeerConferenceListeners
            = new ArrayList<>();

    /**
     * All the CallPeer listeners registered with this CallPeer.
     */
    protected final List<CallPeerListener> callPeerListeners
                            = new ArrayList<>();

    /**
     * All the CallPeerSecurityListener-s registered with this CallPeer.
     */
    protected final List<CallPeerSecurityListener>
        callPeerSecurityListeners
            = new ArrayList<>();

    /**
     * The indicator which determines whether this peer is acting as a
     * conference focus and thus may provide information about
     * <tt>ConferenceMember</tt> such as {@link #getConferenceMembers()} and
     * {@link #getConferenceMemberCount()}.
     */
    private boolean conferenceFocus;

    /**
     * The list of <tt>ConferenceMember</tt>s currently known to and managed in
     * a conference by this <tt>CallPeer</tt>. It is implemented as a
     * copy-on-write storage in order to optimize the implementation of
     * {@link #getConferenceMembers()} which is used more often than
     * {@link #addConferenceMember(ConferenceMember)} and
     * {@link #removeConferenceMember(ConferenceMember)}.
     */
    private List<ConferenceMember> conferenceMembers;

    /**
     * The <tt>Object</tt> which synchronizes the access to
     * {@link #conferenceMembers} and {@link #unmodifiableConferenceMembers}.
     */
    private final Object conferenceMembersSyncRoot = new Object();

    /**
     * The flag that determines whether our audio stream to this call peer is
     * currently muted.
     */
    private boolean isMute = false;

    /**
     * The last fired security event.
     */
    private CallPeerSecurityStatusEvent lastSecurityEvent;

    /**
     * The state of the call peer.
     */
    private CallPeerState state = CallPeerState.UNKNOWN;

    /**
     * An unmodifiable view of {@link #conferenceMembers}. The list of
     * <tt>ConferenceMember</tt>s participating in the conference managed by
     * this instance is implemented as a copy-on-write storage in order to
     * optimize the implementation of {@link #getConferenceMembers()} which is
     * used more often than {@link #addConferenceMember(ConferenceMember)} and
     * {@link #removeConferenceMember(ConferenceMember)}.
     */
    private List<ConferenceMember> unmodifiableConferenceMembers;

    /**
     * Whether to display the CRM looked-up display name for this peer (when
     * performing CRM lookups).
     */
    protected boolean mCanUseCrmLookupDisplayName;

    /**
     * Initializes a new <tt>AbstractCallPeer</tt> instance.
     */
    protected AbstractCallPeer()
    {
        conferenceMembers = Collections.emptyList();
        unmodifiableConferenceMembers
            = Collections.unmodifiableList(conferenceMembers);
    }

    /**
     * Implements
     * <tt>CallPeer#addCallPeerConferenceListener(
     * CallPeerConferenceListener)</tt>. In the fashion of the addition of the
     * other listeners, does not throw an exception on attempting to add a
     * <tt>null</tt> listeners and just ignores the call.
     *
     * @param listener the <tt>CallPeerConferenceListener</tt> to add
     */
    public void addCallPeerConferenceListener(
        CallPeerConferenceListener listener)
    {
        if (listener != null)
            synchronized (callPeerConferenceListeners)
            {
                if (!callPeerConferenceListeners.contains(listener))
                    callPeerConferenceListeners.add(listener);
            }
    }

    /**
     * Registers the <tt>listener</tt> to the list of listeners that would be
     * receiving CallPeerEvents.
     *
     * @param listener a listener instance to register with this peer.
     */
    public void addCallPeerListener(CallPeerListener listener)
    {
        if (listener == null)
            return;

        logger.debug("addCallPeerListener: " + listener + " to " + sanitiseChatAddress(this.toString()));

        synchronized(callPeerListeners)
        {
            if (!callPeerListeners.contains(listener))
                callPeerListeners.add(listener);
        }
    }

    /**
     * Registers the <tt>listener</tt> to the list of listeners that would be
     * receiving CallPeerSecurityEvents.
     *
     * @param listener a listener instance to register with this peer.
     */
    public void addCallPeerSecurityListener(
        CallPeerSecurityListener listener)
    {
        if (listener == null)
            return;
        synchronized(callPeerSecurityListeners)
        {
            if (!callPeerSecurityListeners.contains(listener))
                callPeerSecurityListeners.add(listener);
        }
    }

    /**
     * Adds a specific <tt>ConferenceMember</tt> to the list of
     * <tt>ConferenceMember</tt>s reported by this peer through
     * {@link #getConferenceMembers()} and {@link #getConferenceMemberCount()}
     * and fires
     * <tt>CallPeerConferenceEvent#CONFERENCE_MEMBER_ADDED</tt> to
     * the currently registered <tt>CallPeerConferenceListener</tt>s.
     *
     * @param conferenceMember a <tt>ConferenceMember</tt> to be added to the
     * list of <tt>ConferenceMember</tt> reported by this peer. If the specified
     * <tt>ConferenceMember</tt> is already contained in the list, it is not
     * added again and no event is fired.
     */
    public void addConferenceMember(ConferenceMember conferenceMember)
    {
        if (conferenceMember == null)
            throw new NullPointerException("conferenceMember");
        else
        {
            synchronized (conferenceMembersSyncRoot)
            {
                if (conferenceMembers.contains(conferenceMember))
                    return;
                else
                {
                    List<ConferenceMember> newConferenceMembers
                        = new ArrayList<>(conferenceMembers);

                    if (newConferenceMembers.add(conferenceMember))
                    {
                        conferenceMembers = newConferenceMembers;
                        unmodifiableConferenceMembers
                            = Collections.unmodifiableList(conferenceMembers);
                    }
                    else
                        return;
                }
            }

            fireCallPeerConferenceEvent(
                    new CallPeerConferenceEvent(
                            this,
                            CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED,
                            conferenceMember));
        }
    }

    /**
     * Finds the first <tt>ConferenceMember</tt> whose <tt>audioSsrc</tt> is
     * equals to a specific value. The method is meant for very frequent use so
     * it iterates over the <tt>List</tt> of <tt>ConferenceMember</tt>s without
     * creating an <tt>Iterator</tt>.
     *
     * @param ssrc the SSRC identifier of the audio RTP streams transmitted by
     * the <tt>ConferenceMember</tt> that we are looking for.
     * @return the first <tt>ConferenceMember</tt> whose <tt>audioSsrc</tt> is
     * equal to <tt>ssrc</tt> or <tt>null</tt> if no such
     * <tt>ConferenceMember</tt> was found
     */
    protected ConferenceMember findConferenceMember(long ssrc)
    {
        List<ConferenceMember> members = getConferenceMembers();

        for (ConferenceMember member : members)
        {
            if (member.getAudioSsrc() == ssrc)
                return member;
        }
        return null;
    }

    /**
     * Constructs a <tt>CallPeerChangeEvent</tt> using this call peer as source,
     * setting it to be of type <tt>eventType</tt> and the corresponding
     * <tt>oldValue</tt> and <tt>newValue</tt>,
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     */
    protected void fireCallPeerChangeEvent(String eventType,
                                                  Object oldValue,
                                                  Object newValue)
    {
        this.fireCallPeerChangeEvent( eventType, oldValue, newValue, null);
    }

    /**
     * Constructs a <tt>CallPeerChangeEvent</tt> using this call peer as source,
     * setting it to be of type <tt>eventType</tt> and the corresponding
     * <tt>oldValue</tt> and <tt>newValue</tt>.
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     * @param reason a string that could be set to contain a human readable
     * explanation for the transition (particularly handy when moving into a
     * FAILED state).
     */
    protected void fireCallPeerChangeEvent(String eventType,
                                                  Object oldValue,
                                                  Object newValue,
                                                  String reason)
    {
        this.fireCallPeerChangeEvent(eventType, oldValue, newValue, reason, -1);
    }

    /**
     * Constructs a <tt>CallPeerChangeEvent</tt> using this call peer as source,
     * setting it to be of type <tt>eventType</tt> and the corresponding
     * <tt>oldValue</tt> and <tt>newValue</tt>.
     *
     * @param eventType the type of the event to create and dispatch.
     * @param oldValue the value of the source property before it changed.
     * @param newValue the current value of the source property.
     * @param reason a string that could be set to contain a human readable
     * explanation for the transition (particularly handy when moving into a
     * FAILED state).
     * @param reasonCode the reason code for the reason of this event.
     */
    protected void fireCallPeerChangeEvent(String eventType,
                                                  Object oldValue,
                                                  Object newValue,
                                                  String reason,
                                                  int reasonCode)
    {
        CallPeerChangeEvent evt = new CallPeerChangeEvent(
            this, eventType, oldValue, newValue, reason, reasonCode);

        reason = (reason == null) ? "" : reason;

        logger.debug("Dispatching a CallPeerChangeEvent event to "
                 + callPeerListeners.size()
                 + " listeners. event is: " + sanitiseChatAddress(evt.toString()) +
                 " Reason: " + reason);

        Iterator<CallPeerListener> listeners = null;
        synchronized (callPeerListeners)
        {
            listeners = new ArrayList<>(
                    callPeerListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallPeerListener listener = listeners.next();
            logger.debug("Fire the event on " + listener +
                         " (" + sanitiseChatAddress(this.toString()) + ")");

            // catch any possible errors, so we are sure we dispatch events
            // to all listeners
            try
            {
                switch (eventType)
                {
                    case CallPeerChangeEvent
                            .CALL_PEER_ADDRESS_CHANGE:
                        listener.peerAddressChanged(evt);
                        break;
                    case CallPeerChangeEvent
                            .CALL_PEER_DISPLAY_NAME_CHANGE:
                        listener.peerDisplayNameChanged(evt);
                        break;
                    case CallPeerChangeEvent
                            .CALL_PEER_IMAGE_CHANGE:
                        listener.peerImageChanged(evt);
                        break;
                    case CallPeerChangeEvent
                            .CALL_PEER_STATE_CHANGE:
                        listener.peerStateChanged(evt);
                        break;
                }
            }
            catch(Throwable t)
            {
                logger.error("Error dispatching event of type"
                        + eventType + " in " + listener, t);
            }
        }
    }

    /**
     * Fires a specific <tt>CallPeerConferenceEvent</tt> to the
     * <tt>CallPeerConferenceListener</tt>s interested in changes in the
     * conference-related information provided by this peer.
     *
     * @param conferenceEvent a <tt>CallPeerConferenceEvent</tt> to be fired and
     * carrying the event data
     */
    protected void fireCallPeerConferenceEvent(
        CallPeerConferenceEvent conferenceEvent)
    {
        CallPeerConferenceListener[] listeners;

        synchronized (callPeerConferenceListeners)
        {
            listeners
                = callPeerConferenceListeners
                    .toArray(
                        new CallPeerConferenceListener[
                                callPeerConferenceListeners.size()]);
        }

        int eventID = conferenceEvent.getEventID();

        String eventIDString;

        switch (eventID)
        {
        case CallPeerConferenceEvent.CONFERENCE_FOCUS_CHANGED:
            eventIDString = "CONFERENCE_FOCUS_CHANGED";
            break;
        case CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED:
            eventIDString = "CONFERENCE_MEMBER_ADDED";
            break;
        case CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED:
            eventIDString = "CONFERENCE_MEMBER_REMOVED";
            break;
        default:
            eventIDString = "UNKNOWN";
            break;
        }
        logger
            .debug(
                "Firing CallPeerConferenceEvent with ID "
                    + eventIDString
                    + " to "
                    + listeners.length
                    + " listeners");

        for (CallPeerConferenceListener listener : listeners)
            switch (eventID)
            {
            case CallPeerConferenceEvent.CONFERENCE_FOCUS_CHANGED:
                listener.conferenceFocusChanged(conferenceEvent);
                break;
            case CallPeerConferenceEvent.CONFERENCE_MEMBER_ADDED:
                listener.conferenceMemberAdded(conferenceEvent);
                break;
            case CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED:
                listener.conferenceMemberRemoved(conferenceEvent);
                break;
            }
    }

    /**
     * Constructs a <tt>CallPeerSecurityStatusEvent</tt> using this call peer as
     * source, setting it to be of type <tt>eventType</tt> and the corresponding
     * <tt>oldValue</tt> and <tt>newValue</tt>.
     *
     * @param messageType the type of the message
     * @param i18nMessage message
     * @param severity severity level
     */
    protected void fireCallPeerSecurityMessageEvent(
        String messageType,
        String i18nMessage,
        int severity)
    {
        CallPeerSecurityMessageEvent evt
            = new CallPeerSecurityMessageEvent(   this,
                                                        messageType,
                                                        i18nMessage,
                                                        severity);

        logger.debug("Dispatching a CallPeerSecurityFailedEvent event to "
                 + callPeerSecurityListeners.size()
                 +" listeners. event is: " + evt.toString());

        Iterator<CallPeerSecurityListener> listeners = null;
        synchronized (callPeerSecurityListeners)
        {
            listeners = new ArrayList<>(
                    callPeerSecurityListeners).iterator();
        }

        while (listeners.hasNext())
        {
            CallPeerSecurityListener listener = listeners.next();

            listener.securityMessageRecieved(evt);
        }
    }

    /**
     * Constructs a <tt>CallPeerSecurityStatusEvent</tt> using this call peer as
     * source, setting it to be of type <tt>eventType</tt> and the corresponding
     * <tt>oldValue</tt> and <tt>newValue</tt>.
     *
     * @param evt the event object with details to pass on to the consumers
     */
    protected void fireCallPeerSecurityNegotiationStartedEvent(
        CallPeerSecurityNegotiationStartedEvent evt)
    {
        lastSecurityEvent = evt;

        logger.debug("Dispatching a CallPeerSecurityStatusEvent event to "
                 + callPeerSecurityListeners.size()
                 +" listeners. event is: " + evt.toString());

        List<CallPeerSecurityListener> listeners = null;
        synchronized (callPeerSecurityListeners)
        {
            listeners = new ArrayList<>(
                    callPeerSecurityListeners);
        }

        for(CallPeerSecurityListener listener : listeners)
        {
            listener.securityNegotiationStarted(evt);
        }
    }

    /**
     * Constructs a <tt>CallPeerSecurityStatusEvent</tt> using this call peer as
     * source, setting it to be of type <tt>eventType</tt> and the corresponding
     * <tt>oldValue</tt> and <tt>newValue</tt>.
     *
     * @param evt the event object with details to pass on to the consumers
     */
    protected void fireCallPeerSecurityOffEvent(CallPeerSecurityOffEvent evt)
    {
        lastSecurityEvent = evt;

        logger.debug(
            "Dispatching a CallPeerSecurityAuthenticationEvent event to "
                     + callPeerSecurityListeners.size()
                     +" listeners. event is: " + evt.toString());

        List<CallPeerSecurityListener> listeners = null;
        synchronized (callPeerSecurityListeners)
        {
            listeners = new ArrayList<>(
                    callPeerSecurityListeners);
        }

        for(CallPeerSecurityListener listener : listeners)
        {
            listener.securityOff(evt);
        }
    }

    /**
     * Constructs a <tt>CallPeerSecurityStatusEvent</tt> using this call peer as
     * source, setting it to be of type <tt>eventType</tt> and the corresponding
     * <tt>oldValue</tt> and <tt>newValue</tt>.
     *
     * @param evt the event object with details to pass on to the consumers
     */
    protected void fireCallPeerSecurityOnEvent(CallPeerSecurityOnEvent evt)
    {
        lastSecurityEvent = evt;

        logger.debug("Dispatching a CallPeerSecurityStatusEvent event to "
                 + callPeerSecurityListeners.size()
                 +" listeners. event is: " + evt.toString());

        List<CallPeerSecurityListener> listeners = null;
        synchronized (callPeerSecurityListeners)
        {
            listeners = new ArrayList<>(
                    callPeerSecurityListeners);
        }

        for(CallPeerSecurityListener listener : listeners)
        {
            listener.securityOn(evt);
        }
    }

    /**
     * Constructs a <tt>CallPeerSecurityStatusEvent</tt> using this call peer as
     * source, setting it to be of type <tt>eventType</tt> and the corresponding
     * <tt>oldValue</tt> and <tt>newValue</tt>.
     *
     * @param evt the event object with details to pass on to the consumers
     */
    protected void fireCallPeerSecurityTimeoutEvent(
        CallPeerSecurityTimeoutEvent evt)
    {
        lastSecurityEvent = evt;

        logger.debug("Dispatching a CallPeerSecurityStatusEvent event to "
                     + callPeerSecurityListeners.size()
                     +" listeners. event is: " + evt.toString());

        List<CallPeerSecurityListener> listeners = null;
        synchronized (callPeerSecurityListeners)
        {
            listeners = new ArrayList<>(
                    callPeerSecurityListeners);
        }

        for(CallPeerSecurityListener listener : listeners)
        {
            listener.securityTimeout(evt);
        }
    }

    /**
     * Returns a reference to the call that this peer belongs to.
     *
     * @return a reference to the call containing this peer.
     */
    public abstract T getCall();

    /**
     * {@inheritDoc}
     */
    public int getConferenceMemberCount()
    {
        synchronized (conferenceMembersSyncRoot)
        {
            return isConferenceFocus() ? getConferenceMembers().size() : 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<ConferenceMember> getConferenceMembers()
    {
        synchronized (conferenceMembersSyncRoot)
        {
            return unmodifiableConferenceMembers;
        }
    }

    /**
     * Returns the currently used security settings of this <tt>CallPeer</tt>.
     *
     * @return the <tt>CallPeerSecurityStatusEvent</tt> that contains the
     * current security settings.
     */
    public CallPeerSecurityStatusEvent getCurrentSecuritySettings()
    {
        return lastSecurityEvent;
    }

    /**
     * Returns the protocol provider that this peer belongs to.
     *
     * @return a reference to the ProtocolProviderService that this peer
     * belongs to.
     */
    public abstract U getProtocolProvider();

    /**
     * Returns an object representing the current state of that peer.
     *
     * @return a CallPeerState instance representing the peer's state.
     */
    public CallPeerState getState()
    {
        return state;
    }

    /**
     * Returns the MetaContact corresponding to this peer or null if no
     * particular MetaContact has been associated.
     *
     * @return the <tt>MetaContact</tt> corresponding to this peer or null
     * if no particular MetaContact has been associated.
     */
    public MetaContact getMetaContact()
    {
        return null;
    }

    /**
     * Sets the MetaContact corresponding to this peer.
     *
     * @param metaContact the <tt>MetaContact</tt> to associate with this peer.
     */
    public void setMetaContact(MetaContact metaContact)
    {
        // Do nothing. Specific implementations can override this behaviour.
    }

    /**
     * Sets the display name corresponding to this peer.
     *
     * @param displayName the display name for this peer
     */
    public void setDisplayName(String displayName)
    {
        // Do nothing. Specific implementations can override this behaviour.
    }

    /**
     * Determines whether this call peer is currently a conference focus.
     *
     * @return <tt>true</tt> if this peer is a conference focus and
     * <tt>false</tt> otherwise.
     */
    public boolean isConferenceFocus()
    {
        return conferenceFocus;
    }

    /**
     * Determines whether the audio stream (if any) being sent to this peer is
     * mute.
     * <p>
     * The default implementation returns <tt>false</tt>.
     * </p>
     *
     * @return <tt>true</tt> if an audio stream is being sent to this peer and
     * it is currently mute; <tt>false</tt>, otherwise
     */
    public boolean isMute()
    {
        return isMute;
    }

    /**
     * Implements
     * <tt>CallPeer#removeCallPeerConferenceListener(
     * CallPeerConferenceListener)</tt>.
     *
     * @param listener the <tt>CallPeerConferenceListener</tt> to remove
     */
    public void removeCallPeerConferenceListener(
        CallPeerConferenceListener listener)
    {
        if (listener != null)
            synchronized (callPeerConferenceListeners)
            {
                callPeerConferenceListeners.remove(listener);
            }
    }

    /**
     * Unregisters the specified listener.
     *
     * @param listener the listener to unregister.
     */
    public void removeCallPeerListener(CallPeerListener listener)
    {
        if (listener == null)
            return;
        synchronized(callPeerListeners)
        {
            logger.debug("removeCallPeerListener: " + listener + " from " +
                         sanitiseChatAddress(this.toString()));
            callPeerListeners.remove(listener);
        }
    }

    /**
     * Unregisters the specified listener.
     *
     * @param listener the listener to unregister.
     */
    public void removeCallPeerSecurityListener(
        CallPeerSecurityListener listener)
    {
        if (listener == null)
            return;
        synchronized(callPeerSecurityListeners)
        {
            callPeerSecurityListeners.remove(listener);
        }
    }

    /**
     * Removes a specific <tt>ConferenceMember</tt> from the list of
     * <tt>ConferenceMember</tt>s reported by this peer through
     * {@link #getConferenceMembers()} and {@link #getConferenceMemberCount()}
     * if it is contained and fires
     * <tt>CallPeerConferenceEvent#CONFERENCE_MEMBER_REMOVED</tt> to
     * the currently registered <tt>CallPeerConferenceListener</tt>s.
     *
     * @param conferenceMember a <tt>ConferenceMember</tt> to be removed from
     * the list of <tt>ConferenceMember</tt> reported by this peer. If the
     * specified <tt>ConferenceMember</tt> is no contained in the list, no event
     * is fired.
     */
    public void removeConferenceMember(ConferenceMember conferenceMember)
    {
        if (conferenceMember != null)
        {
            synchronized (conferenceMembersSyncRoot)
            {
                if (conferenceMembers.contains(conferenceMember))
                {
                    List<ConferenceMember> newConferenceMembers
                        = new ArrayList<>(conferenceMembers);

                    if (newConferenceMembers.remove(conferenceMember))
                    {
                        conferenceMembers = newConferenceMembers;
                        unmodifiableConferenceMembers
                            = Collections.unmodifiableList(conferenceMembers);
                    }
                    else
                        return;
                }
                else
                    return;
            }

            fireCallPeerConferenceEvent(
                    new CallPeerConferenceEvent(
                            this,
                            CallPeerConferenceEvent.CONFERENCE_MEMBER_REMOVED,
                            conferenceMember));
        }
    }

    /**
     * Specifies whether this peer is a conference focus.
     *
     * @param conferenceFocus <tt>true</tt> if this peer is to become a
     * conference focus and <tt>false</tt> otherwise.
     */
    public void setConferenceFocus(boolean conferenceFocus)
    {
        if (this.conferenceFocus != conferenceFocus)
        {
            this.conferenceFocus = conferenceFocus;

            fireCallPeerConferenceEvent(
                    new CallPeerConferenceEvent(
                            this,
                            CallPeerConferenceEvent.CONFERENCE_FOCUS_CHANGED));
        }
    }

    /**
     * Sets the mute property for this call peer.
     *
     * @param newMuteValue the new value of the mute property for this call peer
     */
    public void setMute(boolean newMuteValue)
    {
        this.isMute = newMuteValue;
        firePropertyChange(MUTE_PROPERTY_NAME, isMute, newMuteValue);
    }

    /**
     * Causes this CallPeer to enter the specified state. The method also sets
     * the currentStateStartDate field and fires a CallPeerChangeEvent.
     *
     * @param newState the state this call peer should enter.
     */
    public void setState(CallPeerState newState)
    {
        setState(newState, null);
    }

    /**
     * Causes this CallPeer to enter the specified state. The method also sets
     * the currentStateStartDate field and fires a CallPeerChangeEvent.
     *
     * @param newState the state this call peer should enter.
     * @param reason a string that could be set to contain a human readable
     * explanation for the transition (particularly handy when moving into a
     * FAILED state).
     */
    public void setState(CallPeerState newState, String reason)
    {
        setState(newState, reason, -1);
    }

    /**
     * Causes this CallPeer to enter the specified state. The method also sets
     * the currentStateStartDate field and fires a CallPeerChangeEvent.
     *
     * @param newState the state this call peer should enter.
     * @param reason a string that could be set to contain a human readable
     * explanation for the transition (particularly handy when moving into a
     * FAILED state).
     * @param reasonCode the code for the reason of the state change.
     */
    public void setState(CallPeerState newState, String reason, int reasonCode)
    {
        CallPeerState oldState = getState();

        if(oldState == newState)
            return;

        this.state = newState;

        if (CallPeerState.CONNECTED.equals(newState)
            && !CallPeerState.isOnHold(oldState))
        {
            callDurationStartTime = System.currentTimeMillis();
            logger.debug("Setting start time to " + callDurationStartTime);
        }

        fireCallPeerChangeEvent(
                CallPeerChangeEvent.CALL_PEER_STATE_CHANGE,
                oldState,
                newState,
                reason,
                reasonCode);
    }

    /**
     * Returns a string representation of the peer in the form of
     * <br/>
     * Display Name &lt;address&gt;;status=CallPeerStatus
     *
     * @return a string representation of the peer and its state.
     */
    @Override
    public String toString()
    {
        return "AbstractCallPeer:" + hashCode() + "<" + getLoggableAddress() + ", status=" + getState().getStateString() + ">";
    }
}
