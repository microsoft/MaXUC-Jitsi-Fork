// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.commportal;

/**
 * Service for dealing with the CoS.
 */
public interface ClassOfServiceService
{
    /**
     * Place in config where whether VoIP is allowed in the CoS is cached
     */
    String CONFIG_COS_VOIPALLOWED = "provisioning.cos.VOIPALLOWED";

     /**
      * Place in config where whether the user's DN is used for phone service in the CoS is cached
      */
     String CONFIG_COS_HAS_PHONE_SERVICE = "provisioning.cos.HAS_PHONE_SERVICE";

    /**
     * Place in config where whether CTD is allowed by the subscribed mashups is cached
     */
    String CONFIG_COS_CTD_ALLOWED = "provisioning.cos.CTD_ALLOWED";

    /**
     * Place in config where whether CTD is enabled in the CoS is cached
     */
    String CONFIG_COS_CTD_ENABLED = "provisioning.cos.CTD_ENABLED";

    /** Place in config where whether remote CTD is enabled in the CoS is cached */
    String CONFIG_COS_CTD_REMOTE_ENABLED = "provisioning.cos.CTD_REMOTE_ENABLED";

    /**
     * Place in config where whether group IM is allowed in the CoS is cached
     */
    String CONFIG_COS_GROUP_IM_ALLOWED = "provisioning.cos.GROUP_IM_ALLOWED";

    /**
     * Place in config where CoS subscriber type e.g. BusinessGroupLine,
     * IndividualLine, etc. is cached.
     */
    String CONFIG_COS_SUBSCRIBER_TYPE =
            "provisioning.cos.SUBSCRIBER_TYPE";

    /**
     * The number of groups the subscriber is a member of (will be 0 for
     * non-BG lines).
     */
    String CONFIG_COS_GROUP_LIST_LENGTH =
            "provisioning.cos.GROUP_LIST_LENGTH";

    /**
     * Place in config where whether webinars are enabled is cached
     */
    String CONFIG_WEBINARS_ENABLED =
        "net.java.sip.communicator.plugin.conference.impls.WEBINARS_ENABLED";

    /**
     * Place in config where whether fax is enabled is cached
     */
    String CONFIG_FAX_ENABLED = "provisioning.cos.FAX_ENABLED";

    /**
     * Place in config where EAS Business Group name is cached
     */
    String CONFIG_COS_EAS_BG =
         "provisioning.cos.EAS_BUSINESS_GROUP";

    /**
     * Place in config where the CTD auto answer is cached
     */
    String CONFIG_CTD_AUTO_ANSWER =
            "net.java.sip.communicator.impl.protocol.commportal.ctd.AUTO_ANSWER";

    /**
     * Place in config where the IM Enabled is cached
     */
    String CONFIG_IM_ENABLED =
            "net.java.sip.communicator.im.IM_ENABLED";

    /**
     * Place in config where the SMS Enabled is cached
     */
    String CONFIG_SMS_ENABLED =
            "net.java.sip.communicator.im.SMS_ENABLED";
    /**
     * Place in config where the IM Previously Enabled is cached
     */
    String CONFIG_IM_PREV_ENABLED =
            "net.java.sip.communicator.im.IM_PREV_ENABLED";

    /**
     * Request the class of service from CommPortal
     * <p/>
     * Note that this may return on either the same thread, or a different
     * thread depending on if the CoS is available to be returned or must
     * be requested first
     *
     * @param callback the callback for the request
     */
    void getClassOfService(CPCosGetterCallback callback);

    /**
     * Refresh the cached CoS
     */
    void refreshStoredCos();

    /**
     * Removes the given callback from the list of callbacks listening for CoS
     * updates.
     * <p/>
     *
     * @param callback the callback to be removed
     */
    void unregisterCallback(CPCosGetterCallback callback);
}
