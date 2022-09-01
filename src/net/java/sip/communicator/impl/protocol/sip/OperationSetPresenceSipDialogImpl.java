/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import java.util.*;

import javax.sip.*;
import javax.sip.address.*;

import org.w3c.dom.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * SIP dialog presence implementation.  For dialog presence, the SIP phone
 * subscribes to dialog event packages for another line on CFS, and receives
 * NOTIFYs for every change of state, detailing call ID, remote caller ID, and
 * call status (e.g. ringing, active).
 */
public class OperationSetPresenceSipDialogImpl
    extends OperationSetPresenceSipImpl
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(OperationSetPresenceSipDialogImpl.class);

    private static final String DIALOG_XML      = "dialog-info+xml";
    private static final String DIALOG_PKG      = "dialog";

    // dialog-info elements and attributes
    private static final String DIALOG_INFO_ELEMENT = "dialog-info";
    private static final String DIALOG_ELEMENT      = "dialog";
    private static final String STATE_ELEMENT       = "state";
    private static final String CONFIRMED_STATUS    = "confirmed";
    private static final String EARLY_STATUS        = "early";
    private static final String NO_DIALOG_STATUS    = "nodialog";

    /**
     * Creates an instance of this operation set keeping a reference to the
     * specified parent <tt>provider</tt>.
     * @param provider the ProtocolProviderServiceSipImpl instance that
     * created us.
     * @param presenceEnabled if we are activated or if we don't have to
     * handle the presence informations for contacts
    // * @param forceP2PMode if we should start in the p2p mode directly
     * @param pollingPeriod the period between two poll for offline contacts
     * @param subscriptionExpiration the default subscription expiration value
     * to use
     */
    public OperationSetPresenceSipDialogImpl(
        ProtocolProviderServiceSipImpl provider,
        boolean presenceEnabled,
        int pollingPeriod,
        int subscriptionExpiration)
    {
        super(provider,
            presenceEnabled,
            false,
            pollingPeriod,
            subscriptionExpiration,
            DIALOG_PKG,
            DIALOG_XML);

        if (this.presenceEnabled)
        {
            this.notifier = new EventPackageNotifier(
                this.parentProvider,
                DIALOG_PKG,
                PRESENCE_DEFAULT_EXPIRE,
                DIALOG_XML,
                this.timer)
            {
                /**
                 * Creates a new <tt>PresenceNotificationSubscription</tt>
                 * instance.
                 * @param fromAddress our AOR
                 * @param eventId the event id to use.
                 */
                @Override
                protected Subscription createSubscription(
                    Address fromAddress, String eventId)
                {
                    return new PresenceNotifierSubscription(
                        fromAddress, eventId);
                }
            };
        }
        else
        {
            this.notifier = null;
        }
    }

    /**
     * Requests the provider to enter into a status corresponding to the
     * specified parameters.
     *
     * @param status the PresenceStatus as returned by
     *   getRequestableStatusSet
     * @param statusMsg the message that should be set as the reason to
     *   enter that status
     * @throws IllegalArgumentException if the status requested is not a
     *   valid PresenceStatus supported by this provider.
     * @throws IllegalStateException if the provider is not currently
     *   registered.
     */
    @Override
    public void publishPresenceStatus(PresenceStatus status,
                                      String statusMsg)
        throws IllegalArgumentException,
               IllegalStateException
    {
        PresenceStatus oldStatus = this.presenceStatus;
        this.presenceStatus = status;
        String oldMessage = this.statusMessage;
        this.statusMessage = statusMsg;

        // inform the listeners of these changes
        this.fireProviderStatusChangeEvent(oldStatus);
        this.fireProviderMsgStatusChangeEvent(oldMessage);
    }

    /**
     * Analyzes the incoming <tt>responseEvent</tt> and then forwards it to the
     * proper event handler.
     *
     * @param responseEvent the responseEvent that we received
     *            ProtocolProviderService.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    @Override
    public boolean processResponse(ResponseEvent responseEvent)
    {
        return false;
    }

    /**
     * Process a request from a distant contact
     *
     * @param requestEvent the <tt>RequestEvent</tt> containing the newly
     *            received request.
     * @return <tt>true</tt> if the specified event has been handled by this
     *         processor and shouldn't be offered to other processors registered
     *         for the same method; <tt>false</tt>, otherwise
     */
    @Override
    public boolean processRequest(RequestEvent requestEvent)
    {
        return false;
    }

     /**
      * Sets the contact's presence status using the dialog-info document provided.
      *
      * @param presenceDoc the dialog-info document to use
      */
     @Override
    protected void setPidfPresenceStatus(String presenceDoc)
     {
         Document doc = convertDocument(presenceDoc);

         if (doc == null)
         {
             logger.warn("failed to convert pidf document");
             return;
         }

         logger.debug("parsing:\n" + presenceDoc);

         // <dialog-info>
         NodeList dialogInfoList = doc.getElementsByTagNameNS(ANY_NS, DIALOG_INFO_ELEMENT);

         if (dialogInfoList.getLength() == 0)
         {
             logger.error("no dialog-info element in this document");
             return;
         }
         else if (dialogInfoList.getLength() > 1)
         {
             logger.warn("more than one dialog-info element in this document");
         }

         Node dialogInfoNode = dialogInfoList.item(0);

         if (dialogInfoNode.getNodeType() != Node.ELEMENT_NODE)
         {
             logger.error("the dialog-info node is not an element");
             return;
         }

         Element dialogInfo = (Element) dialogInfoNode;

         // Extract the contact ID from the entity attribute
         NamedNodeMap attr = dialogInfoNode.getAttributes();
         Node nodeAttr = attr.getNamedItem(ENTITY_ATTRIBUTE);
         String contactID = nodeAttr.getTextContent();
         logger.info("Got contact URI: " + contactID);

         // Look up this contact ID in our contact list
         // we use a vector here and not an unique contact to handle an
         // error case where many contacts are associated with a status
         // Vector<ContactSipImpl>
         List<Object[]> sipcontact = new Vector<>(1, 3);
         Contact tmpContact = resolveContactID(contactID);
         if (tmpContact == null)
         {
             logger.error("Failed to find contact: " + contactID);
             return;
         }
         sipcontact.add(new Object[] { tmpContact, new Float(0f) });

         // Now look for individual dialog elements within this dialog-info.
         // These contain information about individual calls.
         NodeList dialogList = dialogInfo.getElementsByTagName(DIALOG_ELEMENT);
         int totalCalls = dialogList.getLength();
         PresenceSubscriberCallState[] callStates =
             new PresenceSubscriberCallState[totalCalls];

         for (int ii = 0; ii < totalCalls; ii++)
         {
             Node dialogNode = dialogList.item(ii);

             if (dialogNode.getNodeType() != Node.ELEMENT_NODE)
             {
                 logger.error("the dialog node is not an element");
                 return;
             }

             Element dialog = (Element) dialogNode;
             callStates[ii] = new PresenceSubscriberCallState();

             NodeList remoteList = dialog.getElementsByTagName("remote");
             if (remoteList.getLength() == 0)
             {
                 logger.error("no remote element in this document");
                 callStates[ii].setStatus(NO_DIALOG_STATUS);
             }
             else
             {
                 if (remoteList.getLength() > 1)
                 {
                     logger.error("more than one remote element in this document");
                 }

                 Node remoteNode = remoteList.item(0);

                 if (remoteNode.getNodeType() != Node.ELEMENT_NODE)
                 {
                     logger.error("the remote node is not an element");
                     return;
                 }

                 Element remote = (Element) remoteNode;
                 NodeList identList = remote.getElementsByTagName("identity");

                 if (identList.getLength() == 0)
                 {
                     logger.error("no ident element in this document");
                     callStates[ii].setStatus(NO_DIALOG_STATUS);
                 }
                 else
                 {
                     if (identList.getLength() > 1)
                     {
                         logger.error("more than one ident element in this document");
                     }

                     Node identNode = identList.item(0);

                     if (identNode.getNodeType() != Node.ELEMENT_NODE)
                     {
                         logger.error("the ident node is not an element");
                         return;
                     }
                 }
             }

             // Extract the state from this dialog.  If there's no dialog, set
             // the state to the special "no dialog" state.  This means the contact
             // is not in a call.
             NodeList stateList = dialog.getElementsByTagName(STATE_ELEMENT);
             if (stateList.getLength() == 0)
             {
                 logger.error("no state element in this document");
                 callStates[ii].setStatus(NO_DIALOG_STATUS);
             }
             else
             {
                 if (stateList.getLength() > 1)
                 {
                     logger.error("more than one state element in this document");
                 }

                 Node stateNode = stateList.item(0);

                 if (stateNode.getNodeType() != Node.ELEMENT_NODE)
                 {
                     logger.error("the state node is not an element");
                     return;
                 }

                 String state = stateNode.getTextContent();

                 if ((state.equals(CONFIRMED_STATUS)) ||
                     (state.equals(EARLY_STATUS)))
                 {
                     callStates[ii].setStatus(state);
                 }
                 else
                 {
                     logger.error("Unknown status:" + state);
                     return;
                 }
             }
         }

         setPresence(sipcontact, callStates);
     }

     /**
      * Set the presence for a contact based on the combined states of all
      * calls.
      * @param sipcontact the contact to set presence for
      * @param callStates an array of call states e.g. early, confirmed
      */
     private void setPresence(List<Object[]> sipcontact,
                              PresenceSubscriberCallState[] callStates)
     {
         // Vector containing the list of status to set for each contact in
         // the presence document ordered by priority (highest first).
         // <SipContact, Float (priority), SipStatusEnum>
         List<Object[]> newPresenceStates = new Vector<>(3, 2);
         PresenceStatus personStatus = sipStatusEnum.getStatus(SipStatusEnum.ONLINE);

         // Loop through the call states until we have decided what presence
         // state should be displayed:
         // one or more incoming calls ringing     => 'ringing'
         // one or more active calls; none ringing => 'on the phone'
         // no calls                               => 'online'
         for (PresenceSubscriberCallState callState : callStates)
         {
             String status = callState.status;

             if (status.equals(EARLY_STATUS))
             {
                 personStatus = sipStatusEnum.getStatus(SipStatusEnum.RINGING);
                 break;
             }
             else if (status.equals(CONFIRMED_STATUS))
             {
                 personStatus = sipStatusEnum
                     .getStatus(SipStatusEnum.ON_THE_PHONE);
             }
             else if (!status.equals(NO_DIALOG_STATUS))
             {
                 logger.error("Unknown status:" + status);
                 return;
             }
         }

         newPresenceStates = setStatusForContacts(
             personStatus,
             sipcontact,
             newPresenceStates);

         // Now really set the new presence status for the listed contacts
         // newPresenceStates is ordered so priority order is respected
         for (Object[] tab : newPresenceStates)
         {
             ContactSipImpl contact = (ContactSipImpl) tab[0];
             PresenceStatus status = (PresenceStatus) tab[2];

             changePresenceStatusForContact(contact, status);
         }
     }

     @Override
    protected void registrationStateRegistered()
     {
         super.registrationStateRegistered();

         /*
         * If presence support is enabled and the keep-alive method
         * is REGISTER, we'll get RegistrationState.REGISTERED more
         * than one though we're already registered. If we're
         * receiving such subsequent REGISTERED, we don't have to do
         * anything because we've already set it up in response to
         * the first REGISTERED.
         */
         if (presenceEnabled)
         {
             forcePollAllContacts();
         }
     }

     /**
      * Object containing all the information returned by SIP dialog presence
      * for a call:
      * <li>call status - ringing / active</li>
      */
    private class PresenceSubscriberCallState
    {
        private String status;

        PresenceSubscriberCallState()
        {
        }

        void setStatus(String xiStatus)
        {
            this.status = xiStatus;
        }
    }
}
