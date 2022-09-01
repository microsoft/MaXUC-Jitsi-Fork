/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;

/**
 * <tt>MessageDeliveredEvent</tt>s confirm successful delivery of an instant
 * message.
 *
 * @author Emil Ivov
 */
public abstract class MessageFailedEvent extends MessageEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

     /**
      * Set when no other error code can describe the exception that occurred.
      */
     public static final int UNKNOWN_ERROR = 1;

     /**
      * Set when delivery fails due to a failure in network communications or
      * a transport error.
      */
     public static final int NETWORK_FAILURE = 2;

     /**
      * Set to indicate that delivery has failed because the provider was not
      * registered.
      */
     public static final int PROVIDER_NOT_REGISTERED = 3;

     /**
      * Set when delivery fails for implementation specific reasons.
      */
     public static final int INTERNAL_ERROR = 4;

     /**
      * Set when delivery fails because we're trying to send a message to a
      * contact that is currently offline and the server does not support
      * offline messages.
      */
     public static final int OFFLINE_MESSAGES_NOT_SUPPORTED = 5;

     /**
      * Set when delivery fails because we're trying to send a message to a
      * contact that is currently offline and the contact's offline message
      * queue is full.
      */
     public static final int OFFLINE_MESSAGE_QUEUE_FULL = 6;

     /**
      * An error code indicating the reason for the failure of this delivery.
      */
     private int errorCode = UNKNOWN_ERROR;

     /**
      * Contains a human readable message indicating the reason for the failure
      * or null if the reason is unknown.
      */
     private String reasonPhrase = null;

     /**
      * The ID of the message being corrected, or null if this is a new message
      * and not a correction.
      */
     private String correctedMessageUID = null;

     /**
      * The ID of the message that failed to send.
      */
     private String failedMessageUID = null;

     /**
      * Constructor.
      *
      * @param source the message
      * @param peerIdentifier the identifier (SMS number or IM address) that
      * this message was sent to.
      * @param correctedMessageUID The ID of the message being corrected.
      * @param failedMessageUID The ID of the message that failed to be delivered.
      * @param errorCode error code
      * @param timestamp the exact timestamp when it was determined that delivery
      * had failed.
      * @param reason a human readable message indicating the reason for the
      * failure or null if the reason is unknown.
      * @param eventType the type of message event that this instance represents
      * (one of the XXX_MESSAGE static fields).
      */
     public MessageFailedEvent(ImMessage source,
                               String peerIdentifier,
                               String correctedMessageUID,
                               String failedMessageUID,
                               int errorCode,
                               Date timestamp,
                               String reason,
                               int eventType)
     {
         super(source,
               peerIdentifier,
               timestamp,
               true,
               eventType);

         this.errorCode = errorCode;
         this.reasonPhrase = reason;

         setCorrectedMessageUID(correctedMessageUID);
         setFailedMessageUID(failedMessageUID);
     }

     /**
      * Returns an error code descibing the reason for the failure of the
      * message delivery.
      * @return an error code descibing the reason for the failure of the
      * message delivery.
      */
     public int getErrorCode()
     {
        return errorCode;
     }

    /**
     * Returns a human readable message indicating the reason for the failure
     * or null if the reason is unknown.
     *
     * @return a human readable message indicating the reason for the failure
     * or null if the reason is unknown.
     */
    public String getReason()
    {
        return reasonPhrase;
    }

    /**
     * Returns the correctedMessageUID The ID of the message being corrected,
     * or null if this is a new message and not a correction.
     *
     * @return the correctedMessageUID The ID of the message being corrected,
     * or null if this is a new message and not a correction.
     */
    public String getCorrectedMessageUID()
    {
        return correctedMessageUID;
    }

   /**
    * Sets the ID of the message being corrected to the passed ID.
    *
    * @param correctedMessageUID The ID of the message being corrected.
    */
   private void setCorrectedMessageUID(String correctedMessageUID)
   {
       this.correctedMessageUID = correctedMessageUID;
   }

   /**
    * Returns the ID of the message that failed to send.
    *
    * @return the ID of the message that failed to send.
    */
   public String getFailedMessageUID()
   {
       return failedMessageUID;
   }

   /**
    * Sets the ID of the message that failed to send.
    *
    * @param failedMessageUID the ID of the message that failed to send.
    */
   private void setFailedMessageUID(String failedMessageUID)
   {
       this.failedMessageUID = failedMessageUID;
   }

   @Override
   public String getErrorMessage()
   {
       return getReason();
   }

   @Override
   public String toString()
   {
       return super.toString() + ", errorCode = " + errorCode +
           ", reasonPhrase = " + reasonPhrase +
           ", correctedMessageUID = " + correctedMessageUID +
           ", failedMessageUID = " + failedMessageUID;
   }
}
