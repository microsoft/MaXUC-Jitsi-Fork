/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.mock;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseChatAddress;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.service.resources.*;

/**
 * @author Damian Minkov
 */
public class MockCallPeer
    extends AbstractCallPeer<MockCall, MockProvider>
{
    /**
     * The sip address of this peer
     */
    private String peerAddress = null;

    /**
     * The call peer belongs to.
     */
    private MockCall call;

    /**
     * A string uniquely identifying the peer.
     */
    private String peerID;

    public MockCallPeer(String address, MockCall owningCall)
    {
        this.peerAddress = address;
        this.call = owningCall;

        call.addCallPeer(this);

        //create the uid
        this.peerID = String.valueOf( System.currentTimeMillis())
                             + String.valueOf(hashCode());
    }

    /**
     * Returns a String locator for that peer.
     *
     * @return the peer's address or phone number.
     */
    public String getAddress()
    {
        return peerAddress;
    }

    /** Returns a loggable String locator for that peer. */
    public String getLoggableAddress()
    {
        return sanitiseChatAddress(peerAddress);
    }

    /**
     * Returns full URI of the address.
     *
     * @return full URI of the address
     */
    public String getURI()
    {
        return "mock:" + peerAddress;
    }

    /**
     * Returns a reference to the call that this peer belongs to.
     *
     * @return a reference to the call containing this peer.
     */
    public MockCall getCall()
    {
        return call;
    }

    /**
     * Returns a human readable name representing this peer.
     *
     * @return a String containing a name for that peer.
     */
    public String getDisplayName()
    {
        return peerAddress;
    }

    /**
     * The method returns an image representation of the call peer
     * (e.g.
     *
     * @return byte[] a byte array containing the image or null if no image
     *   is available.
     */
    public BufferedImageFuture getImage()
    {
        return null;
    }

    /**
     * Returns a unique identifier representing this peer.
     *
     * @return an identifier representing this call peer.
     */
    public String getPeerID()
    {
        return peerID;
    }

    /**
     * Returns the contact corresponding to this peer or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this peer or null
     * if no particular contact has been associated.
     */
    public Contact getContact()
    {
        /** @todo implement getContact() */
        return null;
    }

    /**
     * Returns the protocol provider that this peer belongs to.
     *
     * @return a reference to the ProtocolProviderService that this peer
     * belongs to.
     */
    public MockProvider getProtocolProvider()
    {
        return this.call.getProtocolProvider();
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in stream sound level
     * related information.
     *
     * @param listener the <tt>SoundLevelListener</tt> to add
     */
    public void addStreamSoundLevelListener(
        SoundLevelListener listener)
    {
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in stream sound level
     * related information.
     *
     * @param listener the <tt>SoundLevelListener</tt> to remove
     */
    public void removeStreamSoundLevelListener(
        SoundLevelListener listener)
    {
    }

    @Override
    public String getDiversionInfo()
    {
        return null;
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list
     * of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>SoundLevelListener</tt> to add
     */
    public void addConferenceMembersSoundLevelListener(
        ConferenceMembersSoundLevelListener listener)
    {
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the
     * list of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>SoundLevelListener</tt> to
     * remove
     */
    public void removeConferenceMembersSoundLevelListener(
        ConferenceMembersSoundLevelListener listener)
    {
    }
}
