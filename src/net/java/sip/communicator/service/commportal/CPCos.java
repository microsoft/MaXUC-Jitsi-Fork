// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

import java.util.*;

/**
 * A service allowing interaction with the Class of Service
 */
public interface CPCos
{
    /**
     * Returns true if the values in this CoS have been resolved with the
     * server, or false if they are taken from the CoS cache in config.
     *
     * @return the value of the "isResolved" field
     */
    boolean isResolved();

    /**
     * @return the value of the "IchServiceLevel" field
     */
    String getIchServiceLevel();

    /**
     * @return the value of the "SubscriberType" field
     */
    String getSubscriberType();

    /**
     * @return the value of the "BusinessGroupName" field
     */
    String getBGName();

    /**
     * @return the value of the "BCMSubscribed" field
     */
    boolean getBCMSubscribed();

    /**
     * @return the value of the "IchAllowed" flag
     */
    boolean getIchAllowed();

    /**
     * @return the value of the "MessagingAllowed" flag
     */
    boolean getMessagingAllowed();

    /**
     * @return the value of the "MWI" flag
     */
    boolean getNotifyMWIAvailable();

    /**
     * @return the value of the "FusionUIEnabled" flag
     */
    boolean getFusionUiEnabled();

    /**
     * @return the value of the "ClickToDialAllowed" flag
     */
    boolean getClickToDialEnabled();

    /**
     * @return the value of the "ClickToDialRemoteAllowed" flag
     */
    boolean getClickToDialRemoteEnabled();

    /**
     * @return the value of the "CallLogEnabled" flag
     */
    boolean getCallLogEnabled();

    /**
     * @return the value of the "FaxEnabled" flag
     */
    boolean getFaxEnabled();

    /**
     * @return the length of the group list option
     */
    int getGroupListLength();

    /**
     * @return the value of the "MinPasswordLength" setting.
     */
    int getMinPasswordLength();

    /**
     * @return the value of the "MaxPasswordLength" setting.
     */
    int getMaxPasswordLength();

    /**
     * @return the value of the "MaxPinLength" setting.
     */
    int getMaxPinLength();

    /**
     * @return the value of the "MinPinLength" setting.
     */
    int getMinPinLength();

    /**
     * @return the value of the "PasswordStrengthMinDigits" setting.
     */
    int getPasswordStrengthMinDigits();

    /**
     * @return the value of the "PasswordStrengthMinLetters" setting.
     */
    int getPasswordStrengthMinLetters();

    /**
     * @return the value of the "PasswordStrengthMinSpecChars" setting.
     */
    int getPasswordStrengthMinChars();

    /**
     * @return a <tt>Date</tt> representation of the "PasswordChangedDate" setting.
     */
    Date getPasswordChangedDate();

    /**
     * @return The length of time in days before a user's password expires.  A
     * value of <tt>0</tt> indicates that the password never expires.
     */
    int getMaximumPasswordAge();

    /**
     * @return True if password has expired.
     */
    boolean getIsPasswordAged();

    /**
     * @return an object that contains the information parsed from the CoS
     */
    SubscribedMashups getSubscribedMashups();

    interface SubscribedMashups
    {
        /**
         * @return true if the subscribed mashups allow IM
         */
        boolean isImAllowed();

        /**
         * @return true if the subscribed mashups allow SMS
         */
        boolean isSmsAllowed();

        /**
         * @return true if the subscribed mashups allow Click-to-Dial
         */
        boolean isCtdAllowed();

        /**
         * @return true if the subscribed mashups allow Voip calls
         */
        boolean isVoipAllowed();

        /**
         * @return false if the subscribed mashups indicate that the user has
         *         no phone service (i.e. the DN is just an arbitrary user ID).
         */
        boolean hasPhoneService();

        /**
         * @return true if conferencing is allowed
         */
        boolean isConferenceAllowed();

        /**
         * @return true if the subscribed mashups allow Network Call History
         */
        boolean isNetworkCallHistoryAllowed();

        /**
         * @return true if the subscribed mashups allow Group IM
         */
        boolean isGroupImAllowed();

        /**
         * @return true if the subscribed mashups allow Meeting integration
         */
        boolean isMeetingAllowed();

        /**
         * @return true if the subscribed mashups allow Call Jump
         */
        boolean isCallJumpAllowed();

        /**
         * @return true if the subscribed mashups prompt an IM migration
         */
        boolean isIMMigrating();

        boolean isAccessionDesktopAllowed();

        /**
         * @return The raw text for any related subscribed mashups parsed from the CoS.
         */
        String getRawText();
    }
}
