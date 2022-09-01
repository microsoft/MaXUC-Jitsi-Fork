// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.*;

import com.drew.lang.annotations.Nullable;

import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.protocol.CallPeer;
import net.java.sip.communicator.util.Logger;

/**
 * Used to insert/update an appropriate CRM button into a given bit of UI. The
 * CRM button is provided either by Accession's own CRM service, or by the
 * CRM service provided over the WebSocket server (where the latter one takes
 * priority if both are available).
 */
public class CrmButtonSetter
{
    private static final Logger sLog =
            Logger.getLogger(CrmButtonSetter.class);

    /** Stores the Accession CRM button, or null if not applicable. */
    private SIPCommSnakeButton mAccessionCrmLaunchButton;

    /** Stores the WebSocket CRM button, or null if not applicable. */
    private SIPCommSnakeButton mWebSocketCrmLaunchButton;

    /**
     * Stores the WebSocket CRM in progress indicator or null if not applicable.
     */
    private JLabel mCrmLookupIndicator;

    /**
     * Stores the WebSocket CRM failed indicator or null if not applicable.
     */
    private JLabel mCrmLookupFailedIndicator;

    /** The JPanel that stores the CRM button (or lookup progress indicator). */
    private final JPanel mCrmButtonPanel;

    /**
     * Constructor.
     *
     * @param searchName The name to use in the CRM search (used for Accession
     *                   CRM searches).
     * @param searchNumber The number to use in the CRM search (used for both
     *                     types of CRM searches).
     * @param crmButtonPanel The UI panel to insert the button into.
     */
    public CrmButtonSetter(@Nullable String searchName,
                           @Nullable String searchNumber,
                           JPanel crmButtonPanel)
    {
        mCrmButtonPanel = crmButtonPanel;

        UIService uiService = DesktopUtilActivator.getUIService();

        // Try to get the relevant CRM buttons and labels - any of them could be
        // null so the rest of the code should be able to handle that.
        if (uiService != null)
        {
            mAccessionCrmLaunchButton = uiService.getAccessionCrmLaunchButton(
                    searchName,
                    searchNumber);

            mWebSocketCrmLaunchButton = uiService.getWebSocketCrmLaunchButton(
                    searchNumber);

            mCrmLookupIndicator = uiService.getCrmLookupInProgressIndicator();

            mCrmLookupFailedIndicator = uiService.getCrmLookupFailedIndicator();
        }
    }

    /**
     * Inserts the CRM button into the provided JPanel (or else does nothing if
     * no button is available).
     *
     * @param callPeer The peer associated with the CRM lookup.
     */
    public void createButton(CallPeer callPeer)
    {
        createButton(callPeer.isCrmLookupCompleted(),
                     callPeer.isCrmLookupSuccessful());
    }

    /**
     * Inserts the CRM button into the provided JPanel (or else does nothing if
     * no button is available). Used when there is no CallPeer available.
     *
     * @param crmLookupCompleted Whether a (WebSocket) CRM lookup has been
     *                           completed.
     * @param crmLookupSuccessful The outcome of the CRM request (used when the
     *                            request has completed).
     */
    public void createButton(boolean crmLookupCompleted,
                             boolean crmLookupSuccessful)
    {
        // If the WebSocket API provided CRM service is available, it will have
        // priority over the Accession CRM service.
        if (mWebSocketCrmLaunchButton != null)
        {
            sLog.debug("Use the WebSocket provided CRM button in the " +
                               "in-call window");

            if (crmLookupCompleted)
            {
                if (crmLookupSuccessful)
                {
                    sLog.debug("Adding WebSocket CRM launch button");
                    mCrmButtonPanel.add(mWebSocketCrmLaunchButton);
                }
                else if (mAccessionCrmLaunchButton != null)
                {
                    sLog.debug("Adding Accession CRM launch button");
                    mCrmButtonPanel.add(mAccessionCrmLaunchButton);
                }
                else
                {
                    sLog.debug("Adding CRM lookup failed indicator");
                    if (mCrmLookupFailedIndicator != null)
                    {
                        mCrmButtonPanel.add(mCrmLookupFailedIndicator);
                    }
                    else
                    {
                        // This should never happen.
                        sLog.warn("CRM lookup failed indicator " +
                                          "cannot be created");
                    }
                }
            }
            else
            {
                // Display a temporary string indicating that a CRM lookup is
                // in progress instead. This will be removed when the
                // request terminates.
                sLog.debug("Adding CRM in progress indicator");
                if (mCrmLookupIndicator != null)
                {
                    mCrmButtonPanel.add(mCrmLookupIndicator);
                }
                else
                {
                    // This should never happen.
                    sLog.warn("CRM lookup in progress indicator " +
                                      "cannot be created");
                }
            }
        }
        else if (mAccessionCrmLaunchButton != null)
        {
            // If the WebSocket CRM service is not available, display the
            // Accession CRM button straight away.
            sLog.debug("Use the Accession CRM button in the " +
                               "in-call window");
            mCrmButtonPanel.add(mAccessionCrmLaunchButton);
        }
    }

    /**
     * Updates the CRM button. Removes anything that could have been inserted
     * into the JPanel earlier (a CRM button or the CRM lookup in progress
     * indicator string) and inserts the most appropriate UI element
     * (or nothing if no UI element is appropriate).
     *
     * @param callPeer The peer associated with the CRM lookup.
     */
    public void updateButton(CallPeer callPeer)
    {
        updateButton(callPeer.isCrmLookupCompleted(),
                     callPeer.isCrmLookupSuccessful());
    }

    /**
     * Updates the CRM button. Removes anything that could have been inserted
     * into the JPanel earlier (a CRM button or the CRM lookup in progress
     * indicator string) and inserts the most appropriate UI element
     * (or nothing if no UI element is appropriate).
     *
     * @param crmLookupCompleted Whether a (WebSocket) CRM lookup has been
     *                           completed.
     * @param crmLookupSuccessful The outcome of the CRM request (used when the
     *                            request has completed).
     */
    public void updateButton(boolean crmLookupCompleted,
                             boolean crmLookupSuccessful)
    {
        // Only make updates if the WebSocket CRM service is used, since the
        // Accession CRM button does not get dynamically updated.
        if (mWebSocketCrmLaunchButton != null)
        {
            if (crmLookupCompleted)
            {
                sLog.debug("Removing CRM in progress indicator");
                mCrmButtonPanel.removeAll();

                // Insert the appropriate button, if either type of CRM service
                // is enabled. In the case of a WebSocket API CRM lookup
                // failure, provide Accession's own CRM service button instead
                // (if enabled).
                if (crmLookupSuccessful)
                {
                    sLog.debug("Adding WebSocket CRM launch button");
                    mCrmButtonPanel.add(mWebSocketCrmLaunchButton);
                }
                else if (mAccessionCrmLaunchButton != null)
                {
                    sLog.debug("Adding Accession CRM lookup button");
                    mCrmButtonPanel.add(mAccessionCrmLaunchButton);
                }
                else
                {
                    sLog.debug("Adding CRM lookup failed indicator");
                    if (mCrmLookupFailedIndicator != null)
                    {
                        mCrmButtonPanel.add(mCrmLookupFailedIndicator);
                    }
                    else
                    {
                        // This should never happen.
                        sLog.warn("CRM lookup failed indicator " +
                                          "cannot be created");
                    }
                }
            }
        }
    }

    /**
     * @return The CRM button that is currently displayed, or null if no button
     * is shown (or the WebSocket CRM lookup in progress indicator string is
     * shown).
     */
    public JButton getButton()
    {
        // The mCrmButtonPanel should only ever contain a single element, so
        // only bother checking that.
        synchronized (mCrmButtonPanel.getTreeLock())
        {
            if (mCrmButtonPanel.getComponents().length > 0 &&
                    mCrmButtonPanel.getComponent(0) instanceof JButton)
            {
                return (JButton) mCrmButtonPanel.getComponents()[0];
            }
            else
            {
                return null;
            }
        }
    }

    /**
     * @return The Accession CRM button, if it is visible, else null.
     */
    JButton getAccessionCrmButtonIfVisible()
    {
        synchronized (mCrmButtonPanel.getTreeLock())
        {
            return (mCrmButtonPanel.getComponents().length > 0 &&
                    mCrmButtonPanel.getComponent(0).equals(
                            mAccessionCrmLaunchButton)) ?
                    mAccessionCrmLaunchButton : null;
        }
    }

    /**
     * @return The height of the UI element inserted into the JPanel. Returns
     * the (WebSocket) CRM button height when either the (WebSocket) CRM lookup
     * in progress indicator is shown, or when a (WebSocket) CRM lookup failed
     * and no button is shown. This is for consistency in the height of elements
     * that size themselves based on this method.
     */
    public double getButtonHeight()
    {
        synchronized (mCrmButtonPanel.getTreeLock())
        {
            return mWebSocketCrmLaunchButton != null ?
                    mWebSocketCrmLaunchButton.getExpectedSize().getHeight() :
                    mAccessionCrmLaunchButton != null ?
                            mAccessionCrmLaunchButton.getExpectedSize().getHeight() : 0;
        }
    }
}
