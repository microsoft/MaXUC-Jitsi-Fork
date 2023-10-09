// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.gui;

import java.io.*;
import java.net.*;

import net.java.sip.communicator.service.browserpanel.BrowserPanelService.UrlCreator;
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
        public String createUrl(String commpUrl, String redirectionAndTokenForUrl)
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

    },

    CALL_MANAGER("main.html")
    {
    },

    GROUPS("groups.html")
    {
    },

    APPLICATIONS("applications.html")
    {
    },

    SETTINGS("settings.html")
    {
    };

    private static final Logger logger = Logger.getLogger(UrlCreatorEnum.class);

    /**
     * The place in resources that contains the name of this menu option
     */
    private final String mNameRes;

    /**
     * The page to access under /line/ to reference.
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
     * Create the url via a short-lived token needed to direct the user to a
     * specific page on login to CommPortal.
     */
    @Override
    public String createUrl(String commpUrl, String redirectionAndTokenForUrl)
    {
        return mPage == null ?
                    null : (commpUrl + "/login?redirectTo=/line/" + mPage + redirectionAndTokenForUrl);
    }

    /**
     * @return the place in resources that contains the name of this menu option
     */
    public String getNameRes()
    {
        return mNameRes;
    }
}
