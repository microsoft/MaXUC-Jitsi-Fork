/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.configforms;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;

import javax.swing.*;
import javax.swing.border.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import com.explodingpixels.macwidgets.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.conference.ConferenceService;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.GuiUtils;
import net.java.sip.communicator.util.Logger;

/**
 * The <tt>ConfigurationFrame</tt> is the dialog opened when the "Options" menu
 * is selected. It contains different basic configuration forms, like General,
 * Accounts, Notifications, etc. and also allows plugin configuration forms to
 * be added.
 *
 * @author Yana Stamcheva
 */
public class ConfigurationFrame
    extends SIPCommDialog
    implements  ConfigurationContainer,
                ServiceListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>ConfigurationFrame</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(ConfigurationFrame.class);

    private static final int BORDER_SIZE = 20;

    private final ConfigFormList configList;

    private final JPanel centerPanel
        = new TransparentPanel(new BorderLayout(5, 5));

    /**
     * A LazyConfigurationForm for the accounts list.
     */
    private final LazyConfigurationForm accountsForm
        = new LazyConfigurationForm(
              AccountsConfigurationPanel.class.getName(),
              getClass().getClassLoader(),
              "service.gui.icons.ACCOUNT_ICON",
              "service.gui.ACCOUNTS",
              10);

    /**
     * A LazyConfigurationForm for the UC Services configuration window.
     */
    private final LazyConfigurationForm ucServicesForm =
        new LazyConfigurationForm(
            "net.java.sip.communicator.impl.gui.main." +
                                "UcServicesConfigPanel",
            getClass().getClassLoader(),
            "plugin.ucservicesconfig.UC_SERVICES_ICON",
            "service.gui.UC_SERVICES",
            400);

    /**
     * A LazyConfigurationForm for the Conference configuration window.
     */
    private final LazyConfigurationForm conferenceForm =
        new LazyConfigurationForm(
            "net.java.sip.communicator.impl.gui.main." +
                         "ConferenceConfigurationPanel",
            getClass().getClassLoader(),
            "service.gui.conf.WEB_COLLAB_SETTING_ICON",
            "service.gui.conf.CONFERENCES",
            2);

    /**
     * A LazyConfigurationForm for the audio configuration window.
     */
    private LazyConfigurationForm audioForm;

    /**
     * A LazyConfigurationForm for the video configuration window.
     */
    private LazyConfigurationForm videoForm;

    /**
     * The ConfigurationPanel that is currently visible.
     * We track this so that we can notify it when it is dismissed.
     */
    private ConfigurationPanel currentConfigPanel;

    /**
     * The mainframe that creates this panel
     */
    private final AbstractMainFrame mainFrame;

    /**
     * Indicates if the account config form should be shown.
     */
    public static final String SHOW_ACCOUNT_CONFIG_PROPERTY
        = "net.java.sip.communicator.impl.gui.main."
            + "configforms.SHOW_ACCOUNT_CONFIG";

    /**
     * Indicates if the configuration window should be shown.
     */
    public static final String SHOW_OPTIONS_WINDOW_PROPERTY
        = "net.java.sip.communicator.impl.gui.main."
            + "configforms.SHOW_OPTIONS_WINDOW";

    /**
     * Indicates if IM functionality is enabled in the client.
     */
    private static final String IM_ENABLED_PROPERTY
                                     = "net.java.sip.communicator.im.IM_ENABLED";

    /**
     * Indicates if the audio config form should be shown.
     */
    protected static final String AUDIO_CONFIG_DISABLED_PROPERTY
        = "net.java.sip.communicator.impl.neomedia.AUDIO_CONFIG_DISABLED";

    /**
     * Indicates if the video config form should be shown. This is affected
     * by whether or not we are in no VOIP mode.
     */
    protected static final String VIDEO_CONFIG_DISABLED_PROPERTY
        = "net.java.sip.communicator.impl.neomedia.VIDEO_CONFIG_DISABLED";

    /**
     * Property that determines whether we display video UI elements or not.
     * This is controlled by whether or not the subscriber is allowed to
     * make video calls.
     */
    private static final String DISABLE_VIDEO_UI_PROPERTY =
        "net.java.sip.communicator.impl.gui.DISABLE_VIDEO_UI";
    /**
     * The java class name of the audio config form.
     */
    private static final String AUDIO_FORM_CLASS_NAME =
        "net.java.sip.communicator.impl.neomedia.AudioConfigurationPanel";

    /**
     * The java class name of the video config form.
     */
    private static final String VIDEO_FORM_CLASS_NAME =
        "net.java.sip.communicator.impl.neomedia.VideoConfigurationPanel";

    /**
     * True if the conference configuration form is currently visible, false
     * otherwise.
     */
    private boolean isConfFormVisible = false;

    /**
     * The configuration service.
     */
    private final ConfigurationService mConfigService;

    /**
     * The conference service.
     */
    private final ConferenceService mConferenceService;

    /**
     * Initializes a new <tt>ConfigurationFrame</tt> instance.
     *
     * @param mainFrame The main application window.
     */
    public ConfigurationFrame(AbstractMainFrame mainFrame)
    {
        super(mainFrame, false);

        logger.info("Create new ConfigurationFrame");

        this.mainFrame = mainFrame;
        this.configList = new ConfigFormList(this);

        mConfigService = GuiActivator.getConfigurationService();
        mConferenceService = GuiActivator.getConferenceService();

        JScrollPane configScrollList = new JScrollPane();

        configScrollList.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        configScrollList.setBorder(BorderFactory.createEmptyBorder());
        configScrollList.setOpaque(false);
        configScrollList.getViewport().setOpaque(false);
        configScrollList.getViewport().add(configList);

        this.setTitle(GuiActivator.getResources()
                .getI18NString("service.gui.SETTINGS"));
        this.setResizable(false);
        this.getContentPane().setLayout(new BorderLayout());

        TransparentPanel mainPanel
            = new TransparentPanel(new BorderLayout());

        centerPanel.setMinimumSize(new ScaledDimension(665, 100));

        // Set the maximum size. We don't care about height, so it can be very
        // large
        centerPanel.setMaximumSize(
            new ScaledDimension(665, 20000));
        setMinimumSize(new ScaledDimension(665, 300));

        JComponent topComponent = createTopComponent();
        topComponent.add(configScrollList);
        mainPanel.add(topComponent, BorderLayout.NORTH);

        centerPanel.setBorder(BorderFactory.createEmptyBorder(BORDER_SIZE,
                                                              BORDER_SIZE,
                                                              BORDER_SIZE,
                                                              BORDER_SIZE));

        // Put the centre panel (i.e. the options for whichever settings tab is selected) inside
        // a JScrollPane.  The scroll bar will only be shown when the centre panel is too large to
        // display on the user's screen (which only happens when the user has a low resolution).
        JScrollPane settingsScrollList = new JScrollPane(centerPanel);
        settingsScrollList.setBorder(BorderFactory.createEmptyBorder());
        settingsScrollList.setOpaque(false);
        settingsScrollList.getViewport().setOpaque(false);

        settingsScrollList.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mainPanel.add(settingsScrollList, BorderLayout.CENTER);

        this.getContentPane().add(mainPanel);

        GuiActivator.bundleContext.addServiceListener(this);

        // General configuration forms only.
        String osgiFilter = "("
            + ConfigurationForm.FORM_TYPE
            + "="+ConfigurationForm.GENERAL_TYPE+")";

        ServiceReference<?>[] confFormsRefs = null;
        try
        {
            confFormsRefs = GuiActivator.bundleContext
                .getServiceReferences(
                    ConfigurationForm.class.getName(),
                    osgiFilter);
        }
        catch (InvalidSyntaxException ex)
        {}

        if(confFormsRefs != null)
        {
            for (int i = 0; i < confFormsRefs.length; i++)
            {
                ConfigurationForm form
                    = (ConfigurationForm) GuiActivator.bundleContext
                        .getService(confFormsRefs[i]);

                this.addConfigurationForm(form);

                // If this form is a LazyConfigurationForm, check whether it is
                // the audio or video form.  If so, check whether it is
                // disabled in config.  If so, remove it.
                if (form instanceof LazyConfigurationForm)
                {
                    LazyConfigurationForm lazyForm = (LazyConfigurationForm) form;

                    if (AUDIO_FORM_CLASS_NAME.equals(lazyForm.getFormClassName()))
                    {
                        audioForm = lazyForm;

                        if (showAudioConfigForm())
                        {
                            logger.debug("Audio form enabled");
                        }
                        else
                        {
                            logger.info("Audio form disabled - removing it");
                            this.removeConfigurationForm(audioForm);
                        }
                    }

                    if (VIDEO_FORM_CLASS_NAME.equals(lazyForm.getFormClassName()))
                    {
                        videoForm = lazyForm;

                        if (showVideoConfigForm())
                        {
                            logger.debug("Video form enabled");
                        }
                        else
                        {
                            logger.info("Video form disabled - removing it");
                            this.removeConfigurationForm(videoForm);
                        }
                    }
                }
            }
        }

        // This call currently just adds the 'Accounts' form.
        // We're adding this here so it appears to the right of the 'General'
        // options, as users are more likely to need 'General' options than
        // 'Accounts'.
        this.addDefaultForms();

        addWindowListener(new WindowAdapter()
        {
            public void windowClosing(WindowEvent evt)
            {
                // When the options window is closed, we should notify the
                // active configuration panel that it is being dismissed.
                if (currentConfigPanel != null)
                {
                    currentConfigPanel.onDismissed();
                }
            }

            @Override
            public void windowActivated(WindowEvent arg0)
            {
                // When the options window is activated, we should refresh its
                // contents in case something has changed since it was last
                // active that means that what is displayed in this options
                // window also needs to be updated.
                if (currentConfigPanel != null)
                {
                    currentConfigPanel.onRefresh();
                    updateCentralPanelSize();
                }
            }

            @Override
            public void windowDeactivated(WindowEvent arg0)
            {
                logger.user("Options window lost focus");
                // When the options window is deactivated, we notify the
                // conference form, if there is one.  This is so that it can
                // set its pending config changes. We wait until the window is
                // deactivated to do this, otherwise the window would lose
                // focus when the config changes were sent.
                if (conferenceForm != null)
                {
                    conferenceForm.getForm().onDeactivated();
                }
            }
        });

        addComponentListener(new ComponentAdapter()
        {
            public void componentHidden(ComponentEvent e)
            {
                // When the options window is hidden (for example, by pressing
                // ESC), we should notify the active configuration panel that it
                // is being dismissed.
                if (currentConfigPanel != null)
                {
                    currentConfigPanel.onDismissed();
                }
            }
        });

        // Listen for changes to whether IM, audio config and video config are
        // enabled and add or remove the accounts, audio and video forms
        // accordingly.
        mConfigService.user().addPropertyChangeListener(new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent e)
            {
                String propertyName = e.getPropertyName();

                if (!propertyName.equals(IM_ENABLED_PROPERTY) &&
                    !propertyName.equals(AUDIO_CONFIG_DISABLED_PROPERTY) &&
                    !propertyName.equals(VIDEO_CONFIG_DISABLED_PROPERTY) &&
                    !propertyName.equals(DISABLE_VIDEO_UI_PROPERTY))
                    // Only interested in IM, AUDIO or VIDEO config changes
                    return;

                boolean newValue = Boolean.parseBoolean(e.getNewValue().toString());
                boolean oldValue = Boolean.parseBoolean(e.getOldValue().toString());

                if (newValue == oldValue)
                    return;

                logger.debug(propertyName + " has changed from " +
                                                  oldValue + " to " + newValue);

                switch (propertyName)
                {
                    case IM_ENABLED_PROPERTY:
                        if (newValue)
                        {
                            logger.info(
                                    "IM has been enabled - adding account form");
                            addDefaultForms();
                        } else
                        {
                            logger.info(
                                    "IM has been disabled - removing account form");
                            removeConfigurationForm(accountsForm);
                        }
                        break;
                    case AUDIO_CONFIG_DISABLED_PROPERTY:
                        boolean showAudioConfig = showAudioConfigForm();
                        boolean hasAudioConfig = hasConfigurationForm(audioForm);

                        if (showAudioConfig && !hasAudioConfig)
                        {
                            logger.info("Audio config form has been enabled.");
                            addConfigurationForm(audioForm);
                        } else if (!showAudioConfig && hasAudioConfig)
                        {
                            logger.info("Audio config form has been disabled.");
                            removeConfigurationForm(audioForm);
                        }
                        break;
                    case VIDEO_CONFIG_DISABLED_PROPERTY:
                    case DISABLE_VIDEO_UI_PROPERTY:
                        boolean showVideoConfig = showVideoConfigForm();
                        boolean hasVideoConfig = hasConfigurationForm(videoForm);

                        if (showVideoConfig && !hasVideoConfig)
                        {
                            logger.info("Video config form has been enabled.");
                            addConfigurationForm(videoForm);
                        } else if (!showVideoConfig && hasVideoConfig)
                        {
                            logger.info("Video config form has been disabled");
                            removeConfigurationForm(videoForm);
                        }
                        break;
                }
            }
        });

        // Add the UC Services config form, but let URLServicesTools control its
        // visibility.
        UrlServiceTools.getUrlServiceTools().registerMainConfigFrame(this);
    }

    /**
     * @return should we be showing the audio configuration form
     */
    private boolean showAudioConfigForm()
    {
        // This property is based on whether we can make VoIP calls or join
        // meetings.
        boolean audioEnabled = !mConfigService.user()
            .getBoolean(AUDIO_CONFIG_DISABLED_PROPERTY, false);

        logger.debug("Show audio config form:" +
                       " audio enabled: " + audioEnabled);

        return audioEnabled;
    }

    /**
     * @return should we be showing the video configuration form
     */
    private boolean showVideoConfigForm()
    {
        // This property is based on whether we can make VoIP calls or join
        // meetings.
        boolean videoEnabled =
            !mConfigService.user().getBoolean(VIDEO_CONFIG_DISABLED_PROPERTY, false);

        // This property is based on whether we use video codecs in VoIP calls.
        boolean videoCallsEnabled = !mConfigService.user()
            .getBoolean(DISABLE_VIDEO_UI_PROPERTY, false);

        boolean meetingEnabled = mConferenceService.isJoinEnabled();

        logger.debug("Show video config form: " +
            " video calls enabled: " + videoCallsEnabled +
            " video enabled: " + videoEnabled +
            " meeting enabled: " + meetingEnabled);

        return ((videoEnabled && videoCallsEnabled) || meetingEnabled);
    }

    /**
     * Creates the toolbar panel for this chat window, depending on the current
     * operating system.
     *
     * @return the created toolbar
     */
    private JComponent createTopComponent()
    {
        JComponent topComponent = null;

        if (OSUtils.IS_MAC && ConfigurationUtils.useNativeTheme())
        {
            UnifiedToolBar macToolbarPanel = new UnifiedToolBar();

            MacUtils.makeWindowLeopardStyle(getRootPane());

            macToolbarPanel.getComponent().setLayout(new BorderLayout());
            macToolbarPanel.disableBackgroundPainter();
            macToolbarPanel.installWindowDraggerOnWindow(this);
            centerPanel.setOpaque(true);
            centerPanel.setBackground(
                new Color(GuiActivator.getResources()
                    .getColor("service.gui.MAC_PANEL_BACKGROUND")));

            topComponent = macToolbarPanel.getComponent();
        }
        else
        {
            topComponent = new TransparentPanel(new BorderLayout());
            topComponent.setBorder(
                new EmptyBorder(BORDER_SIZE / 2, BORDER_SIZE, 0, 0));
        }

        return topComponent;
    }

    /**
     * Some configuration forms constructed from the ui implementation itself
     * are added here in the configuration dialog.
     */
    public void addDefaultForms()
    {
        if (ConfigurationUtils.isShowAccountConfig() &&
            ConfigurationUtils.isImEnabled() &&
            "Manual".equals(ConfigurationUtils.getImProvSource()))
        {
            addConfigurationForm(accountsForm);
        }
    }

    /**
     * Shows on the right the configuration form given by the given
     * <tt>ConfigFormDescriptor</tt>.
     *
     * @param configFormDescriptor the descriptor of the for we will be showing.
     */
    public void showFormContent(ConfigFormDescriptor configFormDescriptor)
    {
        logger.user("Options tab " + configFormDescriptor.getConfigFormTitle() + " selected");
        if (currentConfigPanel != null)
        {
            currentConfigPanel.onDismissed();
        }

        this.centerPanel.removeAll();

        currentConfigPanel = configFormDescriptor.getConfigFormPanel();

        currentConfigPanel.setOpaque(false);

        // When the configuration form is shown we should refresh its contents
        // in case something has changed since it was last shown that means
        // that what is displayed in this configuration form window also needs
        // to be updated.
        currentConfigPanel.onRefresh();

        centerPanel.add(currentConfigPanel, BorderLayout.CENTER);

        centerPanel.revalidate();
        updateCentralPanelSize();
    }

    /**
     * Implements <code>ApplicationWindow.show</code> method.
     *
     * @param isVisible specifies whether the frame is to be visible or not.
     */
    public void setVisible(final boolean isVisible)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setVisible(isVisible);
                }
            });
            return;
        }

        if (isVisible)
        {
            logger.debug("Setting conference settings form visibility");
            // As we're about to make the configuration frame visible, set the
            // visibility of the conference settings, as whether or not the
            // conference service is enabled may have changed since we last
            // opened the options window.
            setConfFormVisibility();

            if (configList.getSelectedIndex() < 0)
            {
                this.configList.setSelectedIndex(0);
            }

            if (currentConfigPanel != null)
            {
                // Refresh now, for example to check whether conference client
                // is installed and set conference panel appropriately.
                currentConfigPanel.onRefresh();
                updateCentralPanelSize();
            }
        }

        super.setVisible(isVisible);
        toFront();
    }

    @Override
    public void toFront()
    {
        super.setAlwaysOnTop(true);
        super.toFront();
        super.requestFocus();
        super.setAlwaysOnTop(false);
    }

    /**
     * Callback from URL Service Tools to show or hide the UC Services config
     * options.
     * @param visible <tt>true</tt> if the config options should be visible,
     * <tt>false</tt> otherwise.
     */
    public void setUcServicesPanelVisible(boolean visible)
    {
        if (visible)
        {
            addConfigurationForm(ucServicesForm);
        }
        else
        {
            removeConfigurationForm(ucServicesForm);
        }
    }

    /**
     * Makes the conference settings form visible if the user has AMeet in their
     * CoS and if the AMeet client is installed.
     */
    private synchronized void setConfFormVisibility()
    {
        if (mConferenceService.isFullServiceEnabled() ||
            (mConferenceService.isJoinEnabled() &&
                 mConferenceService.isConfAppInstalled()))
        {
            logger.debug("Conference service enabled");
            if (!isConfFormVisible)
            {
                logger.debug("Adding conference settings form");
                addConfigurationForm(conferenceForm);
                isConfFormVisible = true;
            }
        }
        else if (isConfFormVisible)
        {
            logger.debug("Conference service not enabled: Removing conference settings form");
            removeConfigurationForm(conferenceForm);
            isConfFormVisible = false;
        }
    }

    /**
     * Implements <tt>SIPCommFrame.close()</tt> method. Performs a click on
     * the close button.
     *
     * @param isEscaped specifies whether the close was triggered by pressing
     *            the escape key.
     */
    @Override
    protected void close(boolean isEscaped)
    {
        this.dispose();
    }

    /**
     * Handles registration of a new configuration form.
     * @param event the <tt>ServiceEvent</tt> that notified us
     */
    public void serviceChanged(ServiceEvent event)
    {
        if(!GuiActivator.isStarted)
            return;

        ServiceReference<?> serRef = event.getServiceReference();

        Object property = serRef.getProperty(ConfigurationForm.FORM_TYPE);

        if (property != ConfigurationForm.GENERAL_TYPE)
            return;

        Object sService
            = GuiActivator.bundleContext.getService(
                    event.getServiceReference());

        // we don't care if the source service is not a configuration form
        if (!(sService instanceof ConfigurationForm))
            return;

        ConfigurationForm configForm = (ConfigurationForm) sService;

        if (configForm.isAdvanced())
            return;

        switch (event.getType())
        {
        case ServiceEvent.REGISTERED:
            logger.info("Handling registration of a new Configuration Form.");
            this.addConfigurationForm(configForm);
            break;

        case ServiceEvent.UNREGISTERING:
            this.removeConfigurationForm(configForm);
            break;
        }
    }

    /**
     * Implements the <code>ConfigurationManager.addConfigurationForm</code>
     * method. Checks if the form contained in the <tt>ConfigurationForm</tt>
     * is an instance of java.awt.Component and if so adds the form in this
     * dialog, otherwise throws a ClassCastException.
     *
     * @param configForm the form we are adding
     *
     * @see ConfigFormList#addConfigForm(ConfigurationForm)
     */
    private void addConfigurationForm(final ConfigurationForm configForm)
    {
        logger.debug("Adding config form " + configForm.getTitle());

        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addConfigurationForm(configForm);
                }
            });
            return;
        }

        configList.addConfigForm(configForm);
    }

    /**
     * Checks whether the given configuration form has been added.
     *
     * @param configForm the form we are checking for.
     *
     * @see ConfigFormList#hasConfigForm(ConfigurationForm)
     * @return
     */
    private boolean hasConfigurationForm(ConfigurationForm configForm)
    {
        return configList.hasConfigForm(configForm);
    }

    /**
     * Implements <code>ConfigurationManager.removeConfigurationForm</code>
     * method. Removes the given <tt>ConfigurationForm</tt> from this dialog.
     *
     * @param configForm the form we are removing.
     *
     * @see ConfigFormList#removeConfigForm(ConfigurationForm)
     */
    private void removeConfigurationForm(final ConfigurationForm configForm)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeConfigurationForm(configForm);
                }
            });
            return;
        }

        configList.removeConfigForm(configForm);
    }

    public void setSelected(final ConfigurationForm configForm)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setSelected(configForm);
                }
            });
            return;
        }

        configList.setSelected(configForm);
    }

    public void setSelected(final String title)
    {
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setSelected(title);
                }
            });
            return;
        }

        configList.setSelected(title);
    }

    /**
     * Validates the currently selected configuration form. This method is meant
     * to be used by configuration forms the re-validate when a new component
     * has been added or size has changed.
     */
    public void validateCurrentForm()
    {
        centerPanel.revalidate();

        centerPanel.setPreferredSize(null);

        validate();

        // Set the height of the center panel to be equal to the height of the
        // currently contained panel + all borders.
        centerPanel.setPreferredSize(
            new Dimension(ScaleUtils.scaleInt(550), centerPanel.getHeight()));

        pack();

        getContentPane().repaint();

        updateCentralPanelSize();
    }

    /**
     * @return the main frame that this config frame was created for
     */
    public AbstractMainFrame getMainFrame()
    {
        return mainFrame;
    }

    /**
     * @return the config form list of this frame
     */
    public ConfigFormList getConfigList()
    {
        return configList;
    }

    @Override
    public void bringToFront(final String tab)
    {
        // Because toFront() method gives us no guarantee that our frame would
        // go on top we'll try to also first request the focus and set our
        // window always on top to put all the chances on our side.
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                if (mainFrame.getExtendedState() == Frame.ICONIFIED)
                {
                    mainFrame.setExtendedState(Frame.NORMAL);
                    mainFrame.setVisible(true);
                }

                setVisible(true);
                setAlwaysOnTop(true);
                toFront();
                requestFocus();
                setAlwaysOnTop(false);

                if (tab != null)
                {
                    setSelected(tab);
                }
            }
        });
    }

    /**
     * Set the height of the center panel to be equal to the height of the
     * currently contained panel + all borders.
     */
    private void updateCentralPanelSize()
    {
        // Reset the preferred size
        setPreferredSize(null);

        if ((currentConfigPanel != null) &&
            (centerPanel != null))
        {
            centerPanel.setPreferredSize(
                new Dimension(
                        ScaleUtils.scaleInt(550),
                        currentConfigPanel.getPreferredSize().height +
                                2*BORDER_SIZE));
        }

        pack();

        // We inherit our size based on the preferred size of the current config tab, so it might be
        // too big to display on the user's display.  We can't calculate the overall size of the
        // frame based on its component parts (and adjust the preferred size accordingly), because
        // of unknown size overheads in creating the frame.  It's simplest to call pack() and then
        // check whether the size is too big, and reduce it if so (thus introducing a scroll bar).
        // This means that we also need to remove the preferred size a few lines above to shrink the
        // frame's size down when switching to a smaller config tab that isn't too big to show in
        // its entirety.

        int screenHeight = GuiUtils.getUsableDisplayHeightForComponent(this);

        logger.debug("Config frame height is now " + getHeight() + ", screen height is " + screenHeight);

        if (getHeight() > screenHeight)
        {
            setPreferredSize(new Dimension(ScaleUtils.scaleInt(665), screenHeight));
            pack();
        }
    }
}
