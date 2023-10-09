// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import javax.swing.*;

import org.jitsi.util.CustomAnnotations.*;

import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.util.Logger;

/**
 * Used to insert/update an appropriate CRM button into a given bit of UI.
 * The CRM button is provided by Accession's own CRM service.
 */
public class CrmButtonSetter
{
    private static final Logger sLog =
            Logger.getLogger(CrmButtonSetter.class);

    /** Stores the CRM button, or null if not applicable. */
    private SIPCommSnakeButton mCrmLaunchButton;

    /** The JPanel that stores the CRM button (or lookup progress indicator). */
    private final JPanel mCrmButtonPanel;

    /**
     * Constructor.
     *
     * @param searchName The name to use in the CRM search.
     * @param searchNumber The number to use in the CRM search.
     * @param crmButtonPanel The UI panel to insert the button into.
     */
    public CrmButtonSetter(@Nullable String searchName,
                           @Nullable String searchNumber,
                           JPanel crmButtonPanel)
    {
        mCrmButtonPanel = crmButtonPanel;

        UIService uiService = DesktopUtilActivator.getUIService();
        if (uiService != null)
        {
            mCrmLaunchButton = uiService.getCrmLaunchButton(
                    searchName,
                    searchNumber);
        }
    }

    /**
     * Inserts the CRM button into the provided JPanel (or else does nothing if
     * no button is available). Used when there is no CallPeer available.
     */
    public void createButton()
    {
        if (mCrmLaunchButton != null)
        {
            sLog.debug("Use the CRM button in the in-call window");
            mCrmButtonPanel.add(mCrmLaunchButton);
        }
    }

    /**
     * @return The CRM button that is currently displayed, or null if no button is shown.
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
     * @return The CRM button, if it is visible, else null.
     */
    JButton getCrmButtonIfVisible()
    {
        synchronized (mCrmButtonPanel.getTreeLock())
        {
            return (mCrmButtonPanel.getComponents().length > 0 &&
                    mCrmButtonPanel.getComponent(0).equals(mCrmLaunchButton)) ?
                    mCrmLaunchButton : null;
        }
    }

    /**
     * @return The height of the UI element inserted into the JPanel.
     */
    public double getButtonHeight()
    {
        synchronized (mCrmButtonPanel.getTreeLock())
        {
            return mCrmLaunchButton != null ?
                    mCrmLaunchButton.getExpectedSize().getHeight() : 0;
        }
    }
}
