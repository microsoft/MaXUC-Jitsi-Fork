// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.gui;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.browserpanel.BrowserPanelService.UrlCreator;
import net.java.sip.communicator.service.commportal.*;
import net.java.sip.communicator.util.*;

/**
 * An enum for each option that we add to the tools menu.  Each menu option
 * opens a web page pointing at a certain URL
 */
public enum UrlCreatorEnum implements UrlCreator
{
    CONFERENCE()
    {
        @Override
        public String createUrl(String commpUrl, String sessionId)
        {
            try
            {
                String urlEncodedConfUrl = URLEncoder.encode(commpUrl, "UTF-8");
                String conferenceUrl =
                          commpUrl +
                          "/gadgets/conference/ConferenceWidget.html?custurl=" +
                          urlEncodedConfUrl +
                          "&id=conference0&env=vista";

                return conferenceUrl;
            }
            catch (UnsupportedEncodingException e)
            {
                logger.error(e);
                return null;
            }
        }

        @Override
        boolean isSupported(CPCos classOfService)
        {
            return classOfService.getSubscribedMashups().isConferenceAllowed();
        }
    },

    CALL_MANAGER("main.html")
    {
        @Override
        boolean isSupported(CPCos classOfService)
        {
            return classOfService.getIchAllowed() ||
                   classOfService.getBCMSubscribed();
        }
    },

    GROUPS("groups.html")
    {
        @Override
        boolean isSupported(CPCos classOfService)
        {
            // Groups are supported if
            // * The subscriber type is not an individual line
            //  (i.e. the subscriber is in a BG)
            // * The subscriber is a member of a group
            return classOfService.getGroupListLength() > 0 &&
                   !"IndividualLine".equals(classOfService.getSubscriberType());
        }
    },

    APPLICATIONS("applications.html")
    {
        @Override
        boolean isSupported(CPCos classOfService)
        {
            // Not dependent on the CoS
            return true;
        }
    },

    SETTINGS("settings.html")
    {
        @Override
        boolean isSupported(CPCos classOfService)
        {
            // Don't show if a meeting only user, as there are no relevant
            // settings on that page, unless either they are an individual line
            // or they have email login enabled:
            //  - Individual lines are allowed to change the name of their
            // account.
            //  - Users with email login can change their registered email
            // address.
            // Both of the above is done via this settings page.
            return ConfigurationUtils.isCallingOrImEnabled() ||
                   ConfigurationUtils.isEmailLoginEnabled() ||
                   "IndividualLine".equals(classOfService.getSubscriberType());
        }
    };

    private static final Logger logger = Logger.getLogger(UrlCreatorEnum.class);

    /**
     * The place in resources that contains the name of this menu option
     */
    private final String mNameRes;

    /**
     * The page to access under /session/line/ to reference.
     * If you don't want to use this to form the URL, override getUrl().
     */
    private final String mPage;

    /**
     * Constructs an option which doesn't by default return a URL.
     */
    UrlCreatorEnum()
    {
        this(null);
    }

    /**
     * Constructs an option which returns the provided page by default
     */
    UrlCreatorEnum(String page)
    {
        mPage = page;
        mNameRes = "plugin.toolsmenuoptions.menu." + this;
    }

    /**
     * Examine the CoS and subscriber info and return true if this menu item is
     * allowed
     *
     * @param classOfService The class of service retrieved from the server
     * @return true if the class of service allows this menu option
     */
    abstract boolean isSupported(CPCos classOfService);

    @Override
    public String createUrl(String url, String sessionId)
    {
        return mPage == null ?
                    null : url + "/session" + sessionId + "/line/" + mPage;
    }

    /**
     * @return the place in resources that contains the name of this menu option
     */
    public String getNameRes()
    {
        return mNameRes;
    }
}
