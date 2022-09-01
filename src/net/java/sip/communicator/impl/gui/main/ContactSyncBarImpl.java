// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.*;
import javax.swing.plaf.*;

import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.util.*;

/**
 * Implements a status bar at the bottom of the main UI to show the
 * synchronization status with the contact server.
 */
public class ContactSyncBarImpl
    extends AbstractPluginComponent
    implements ContactSyncBarService,
               ActionListener
{
    /**
     * The <tt>Logger</tt> for this class
     */
    private static final Logger logger
                                = Logger.getLogger(ContactSyncBarImpl.class);

    // A 2/3 transparent black and white for the top border
    private Color borderDark = new Color(0.0f, 0.0f, 0.0f, 0.33f);
    private Color borderLight = new Color(1.0f, 1.0f, 1.0f, 0.33f);

    // Whether to display the sync panel
    private boolean showSyncPanel = false;

    // The contact sync panel
    private JPanel mContactSyncPanel;

    /**
     * The length of time in ms that we must have no notifications for before
     * clearing the sync panel
     */
    private static final int NOTIFICATION_TIMEOUT = 1000;

    /**
     * A timer used to determine if we are still sync'ing with contacts. Set to
     * 500ms, if we don't receive a new notification in this time then assume
     * we have sync'd successfully.
     */
    private final Timer syncTimer = new Timer(NOTIFICATION_TIMEOUT, this);

    /**
     * Contact sync component constructor
     */
    public ContactSyncBarImpl()
    {
        super(Container.CONTAINER_MAIN_WINDOW);

        // Register on the EDT thread to make sure it is always
        // added when the main UI is ready for it.
        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                Hashtable<String, String> containerFilter =
                        new Hashtable<>();
                containerFilter.put(Container.CONTAINER_ID,
                                    Container.CONTAINER_MAIN_WINDOW.getID());

                GuiActivator.bundleContext.registerService(
                                                PluginComponent.class.getName(),
                                                ContactSyncBarImpl.this,
                                                containerFilter);
            }
        });
    }

    @Override
    public String getName()
    {
        return getClass().getName();
    }

    @Override
    public Object getComponent()
    {
        ResourceManagementService resources = GuiActivator.getResources();

        // Create a panel to display the text
        mContactSyncPanel = new JPanel();
        ColorUIResource panelColor = new ColorUIResource(resources
                            .getColor("service.gui.CONTACT_STATUS_BAR_BACKGROUND"));
        mContactSyncPanel.setBackground(panelColor);

        // Get the text string from resources
        String syncString = resources.getI18NString("service.gui.SYNCING");

        // Create and add the label to display in the panel
        final JLabel syncTextField = new JLabel(syncString);
        syncTextField.setForeground(Color.BLACK);
        mContactSyncPanel.add(syncTextField);

        // Create a border for the panel
        mContactSyncPanel.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderDark),
                BorderFactory.createMatteBorder(1, 0, 0, 0, borderLight)));

        // Initialise as not visible
        mContactSyncPanel.setVisible(showSyncPanel);

        return mContactSyncPanel;
    }

    /**
     * Sets whether contact sync is complete and sets the visibility of the
     * component accordingly
     *
     * @param syncComplete
     */
    public void contactSyncComplete(boolean syncComplete)
    {
        showSyncPanel = !syncComplete;

        logger.debug("Should show contact sync panel: " + showSyncPanel);

        if (mContactSyncPanel != null)
        {
            // This needs to be run on the EDT so make sure it is!!
            if (SwingUtilities.isEventDispatchThread())
            {
                mContactSyncPanel.setVisible(showSyncPanel);
            }
            else
            {
                SwingUtilities.invokeLater(new Runnable()
                {
                    public void run()
                    {
                        if (mContactSyncPanel != null)
                        {
                            mContactSyncPanel.setVisible(showSyncPanel);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void fireContactEvent()
    {
        // Called whenever a contact event has occurred. Resets the timer so
        // the sync bar is shown
        if (syncTimer.isRunning())
        {
            // Reset the syncTimer as we have just received a new notification
            syncTimer.restart();
        }
        else
        {
            // Start the syncTimer to indicate that we are currently syncing
            syncTimer.start();

            // Fire a notification to indicate that we have started sync'ing
            contactSyncComplete(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        Object source = evt.getSource();

        if (syncTimer.equals(source))
        {
            // Stop the timer so it doesn't keep firing
            syncTimer.stop();

            // We haven't received a new event for some time so we can hide
            // the sync bar.
            contactSyncComplete(true);
        }
    }
}
