/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.branding;

import java.awt.*;
import java.awt.event.*;
import java.io.File;

import javax.swing.*;
import javax.swing.event.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;
import org.osgi.framework.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.plaf.*;
import net.java.sip.communicator.service.browserlauncher.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>AboutWindow</tt> is containing information about the application
 * name, version, license etc..
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 * @author Lyubomir Marinov
 */
public class AboutWindow
    extends JDialog
    implements  HyperlinkListener,
                ActionListener,
                ExportedWindow,
                Skinnable,
                BrandingService
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private static final Logger sLog = Logger.getLogger(AboutWindow.class);

    /**
     * The global/shared <code>AboutWindow</code> currently showing.
     */
    private static AboutWindow aboutWindow;

    /**
     * Class id key used in UIDefaults for the version label.
     */
    private static final String uiClassID =
        AboutWindow.class.getName() +  "$VersionTextFieldUI";

    /**
     * Adds the ui class to UIDefaults.
     */
    static
    {
        UIManager.getDefaults().put(uiClassID,
            SIPCommTextFieldUI.class.getName());
    }

    private final JTextField versionLabel;

    /**
     * Shows a <code>AboutWindow</code> creating it first if necessary. The
     * shown instance is shared in order to prevent displaying multiple
     * instances of one and the same <code>AboutWindow</code>.
     */
    public static void showAboutWindow()
    {
        if (aboutWindow == null)
        {
            aboutWindow = new AboutWindow(null);

            /*
             * When the global/shared AboutWindow closes, don't keep a reference
             * to it and let it be garbage-collected.
             */
            aboutWindow.addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosed(WindowEvent e)
                {
                    if (aboutWindow == e.getWindow())
                        aboutWindow = null;
                }
            });
        }

        WindowUtils.makeWindowVisible(aboutWindow, false);
    }

    private static final int DEFAULT_TEXT_INDENT
        = BrandingActivator.getResources()
            .getScaledSize("plugin.branding.ABOUT_TEXT_INDENT");

    /**
     * Creates an <tt>AboutWindow</tt> by specifying the parent frame owner.
     * @param owner the parent owner
     */
    public AboutWindow(Frame owner)
    {
        super(owner);

        ResourceManagementService resources = BrandingActivator.getResources();

        String applicationName
            = resources.getSettingsString("service.gui.APPLICATION_NAME");

        this.setTitle(
            resources.getI18NString("plugin.branding.ABOUT_WINDOW_TITLE",
                new String[]{applicationName}));

        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel mainPanel = new WindowBackground();
        mainPanel.setLayout(new BorderLayout());

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        int borderWidth = ScaleUtils.scaleInt(15);
        textPanel.setBorder(BorderFactory.createEmptyBorder(
            borderWidth, borderWidth, borderWidth, borderWidth));
        textPanel.setOpaque(false);

        JLabel titleLabel = null;
        if (isApplicationNameShown())
        {
            titleLabel = new JLabel(applicationName);
            titleLabel.setFont(titleLabel.getFont().deriveFont(
                Font.BOLD, ScaleUtils.getScaledFontSize(28f)));
            titleLabel.setForeground(Constants.TITLE_COLOR);
            titleLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        }

        // Force the use of the custom text field UI in order to fix an
        // incorrect rendering on Ubuntu.
        versionLabel
            = new JTextField(" "
                    + System.getProperty("sip-communicator.version"))
        {
            private static final long serialVersionUID = 0L;

            /**
             * Returns the name of the L&F class that renders this component.
             *
             * @return the string "TreeUI"
             * @see JComponent#getUIClassID
             * @see UIDefaults#getUI
             */
            public String getUIClassID()
            {
                return uiClassID;
            }
        };

        versionLabel.setBorder(null);
        versionLabel.setOpaque(false);
        versionLabel.setEditable(false);
        versionLabel.setFont(versionLabel.getFont().deriveFont(
            Font.BOLD, ScaleUtils.getScaledFontSize(18f)));
        versionLabel.setForeground(Constants.TITLE_COLOR);
        versionLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        versionLabel.setHorizontalAlignment(JTextField.RIGHT);

        JTextField javaHashLabel = getJTextFieldContainingBuildHash(
                BrandingActivator.javaCommitHash, "Java");

        JTextField electronHashLabel = getJTextFieldContainingBuildHash(
                BrandingActivator.electronCommitHash,"Electron");

        int logoAreaFontSize
            = resources.getSettingsInt("plugin.branding.ABOUT_LOGO_FONT_SIZE");

        // FIXME: the message exceeds the window length
        JTextArea logoArea =
            new JTextArea(resources.getI18NString(
                "plugin.branding.LOGO_MESSAGE"));
        logoArea.setFont(
            logoArea.getFont().deriveFont(
                Font.BOLD, ScaleUtils.getScaledFontSize(logoAreaFontSize)));
        logoArea.setForeground(Constants.TITLE_COLOR);
        logoArea.setOpaque(false);
        logoArea.setLineWrap(true);
        logoArea.setWrapStyleWord(true);
        logoArea.setEditable(false);
        logoArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        logoArea.setBorder(BorderFactory
            .createEmptyBorder(30, DEFAULT_TEXT_INDENT, 0, 0));

        // This contains the version number and if the commit hash is enabled,
        // the commit hash from which the build was made. We don't want real
        // end-users to see this, so at the time of writing, this is only
        // enabled in Dev brandings built from CI.
        JPanel versionArea = new JPanel();
        versionArea.setLayout(new BoxLayout(versionArea, BoxLayout.Y_AXIS));
        versionArea.setBorder(BorderFactory.createEmptyBorder(
                                            10, DEFAULT_TEXT_INDENT, 0, 0));
        versionArea.setOpaque(false);
        versionArea.add(versionLabel);
        if (BrandingActivator.getConfigurationService().global()
                .getBoolean("net.java.sip.communicator.GIT_HASH", false))
        {
            versionArea.add(javaHashLabel);
            versionArea.add(electronHashLabel);
        }
        versionArea.setAlignmentX(Component.RIGHT_ALIGNMENT);

        SplashScreenHTMLPane rightsArea = new SplashScreenHTMLPane();
        rightsArea.setContentType("text/html");

        rightsArea.appendToEnd(resources.getI18NString(
            "plugin.branding.COPYRIGHT",
            new String[]
            { Constants.TEXT_COLOR }));

        rightsArea
                .setBorder(BorderFactory
                    .createEmptyBorder(0, DEFAULT_TEXT_INDENT, 0, 0));
        rightsArea.setOpaque(false);
        rightsArea.setEditable(false);
        rightsArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        rightsArea.addHyperlinkListener(this);

        SplashScreenHTMLPane licenseArea = new SplashScreenHTMLPane();
        licenseArea.setContentType("text/html");
        licenseArea.appendToEnd(resources.
            getI18NString("plugin.branding.LICENSE",
            new String[]{Constants.TEXT_COLOR}));

        licenseArea.setBorder(
            BorderFactory.createEmptyBorder(
                resources.getSettingsInt("plugin.branding.ABOUT_PARAGRAPH_GAP"),
                DEFAULT_TEXT_INDENT,
                0, 0));
        licenseArea.setOpaque(false);
        licenseArea.setEditable(false);
        licenseArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
        licenseArea.addHyperlinkListener(this);

        if (titleLabel != null)
            textPanel.add(titleLabel);

        textPanel.add(versionArea);
        textPanel.add(logoArea);
        textPanel.add(rightsArea);
        textPanel.add(licenseArea);

        JButton okButton
            = new SIPCommBasicTextButton(
                resources.getI18NString("service.gui.OK"));

        this.getRootPane().setDefaultButton(okButton);

        okButton.setMnemonic(resources.getI18nMnemonic("service.gui.OK"));
        okButton.addActionListener(this);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.setOpaque(false);

        mainPanel.add(textPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        this.getContentPane().add(mainPanel);

        this.setResizable(false);
        this.pack();

        setLocationRelativeTo(getParent());

        this.getRootPane().getActionMap().put("close", new CloseAction());

        InputMap imap = this.getRootPane().getInputMap(
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");

        if(OSUtils.IS_MAC)
        {
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK),
                "close");
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
                "close");
        }

        WindowUtils.addWindow(this);
    }

    /**
     * Returns a JTextField containing either the Java or Electron commit hash.
     * It's only non-empty if we're dealing with a CI-built client whose branding
     * has net.java.sip.communicator.GIT_HASH=true.
     */
    private JTextField getJTextFieldContainingBuildHash(String commitHash, String codebase)
    {
        JTextField hashLabel;
        if (commitHash == null || commitHash.equals(""))
        {
            hashLabel = new JTextField("");
        }
        else
        {
            hashLabel = new JTextField(codebase + " commit: " + commitHash);
        }
        hashLabel.setBorder(null);
        hashLabel.setOpaque(false);
        hashLabel.setEditable(false);
        hashLabel.setFont(hashLabel.getFont().deriveFont(
                Font.ITALIC, ScaleUtils.getScaledFontSize(13f)));
        hashLabel.setForeground(Constants.TITLE_COLOR);
        hashLabel.setAlignmentX(Component.RIGHT_ALIGNMENT);
        hashLabel.setHorizontalAlignment(JTextField.RIGHT);
        return hashLabel;
    }

    /**
     * Reloads text field UI.
     */
    public void loadSkin()
    {
        if(versionLabel.getUI() instanceof Skinnable)
            ((Skinnable)versionLabel.getUI()).loadSkin();
    }

    /**
     * Constructs the window background in order to have a background image.
     */
    private static class WindowBackground
        extends JPanel
        implements Skinnable
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private Image bgImage = null;

        public WindowBackground()
        {
            loadSkin();
        }

        /**
         * Reloads resources for this component.
         */
        public void loadSkin()
        {
            bgImage = BrandingActivator.getResources()
                .getBufferedImage("plugin.branding.ABOUT_WINDOW_BACKGROUND")
                .resolve();

            this.setPreferredSize(new Dimension(bgImage.getWidth(this),
                bgImage.getHeight(this)));
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            g = g.create();
            try
            {
                AntialiasingManager.activateAntialiasing(g);

                int bgImageWidth = bgImage.getWidth(null);
                int bgImageHeight = bgImage.getHeight(null);
                boolean bgImageHasBeenDrawn = false;

                if ((bgImageWidth != -1) && (bgImageHeight != -1))
                {
                    int width = getWidth();
                    int height = getHeight();

                    if ((bgImageWidth < width) || (bgImageHeight < height))
                    {
                        g.drawImage(bgImage, 0, 0, width, height, null);
                        bgImageHasBeenDrawn = true;
                    }
                }

                if (!bgImageHasBeenDrawn)
                    g.drawImage(bgImage, 0, 0, null);
            }
            finally
            {
                g.dispose();
            }
        }
    }

    /**
     * Opens a browser when the link has been activated (clicked).
     * @param e the <tt>HyperlinkEvent</tt> that notified us
     */
    public void hyperlinkUpdate(HyperlinkEvent e)
    {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
        {
            String href = e.getDescription();
            if (href.equals("licenses"))
            {
                // The licences link displays a local file.
                // We have to try 2 places for the file, due to different
                // positioning for a local build, vs where the installer may
                // place the file.
                File htmlFile = new File("resources/licences", "licences.html");
                if (!htmlFile.exists())
                {
                    htmlFile = new File("licences.html");
                }
                // Surprisingly Java does not have a standard method to encode
                // URL paths, and although you might think that the
                // browser launcher would cope with this, it only does so on
                // Windows, not on Mac, so make sure that spaces in the path
                // are correctly encoded.
                String path = htmlFile.getAbsolutePath().replaceAll(" ", "%20");
                // Windows paths start with a drive letter, so need an "/"
                // before them, but Mac paths start with "/" for root already and
                // object to an extra / being included!
                href = "file://" + (path.charAt(0) == '/' ? "" : "/") + path;

                sLog.info("Substituting " + href + " for licences");
            }

            ServiceReference<?> serviceReference = BrandingActivator
                .getBundleContext().getServiceReference(
                    BrowserLauncherService.class.getName());

            if (serviceReference != null)
            {
                BrowserLauncherService browserLauncherService
                = (BrowserLauncherService) BrandingActivator
                .getBundleContext().getService(serviceReference);

                browserLauncherService.openURL(href);
            }
        }
    }

    /**
     * Indicates that the ok button has been pressed. Closes the window.
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        setVisible(false);
        dispose();
    }

    /**
     * Implements the <tt>ExportedWindow.getIdentifier()</tt> method.
     * @return the identifier of this exported window
     */
    public WindowID getIdentifier()
    {
        return ExportedWindow.ABOUT_WINDOW;
    }

    /**
     * This dialog could not be minimized.
     */
    public void minimize()
    {
    }

    /**
     * This dialog could not be maximized.
     */
    public void maximize()
    {
    }

    /**
     * Implements the <tt>ExportedWindow.bringToFront()</tt> method. Brings
     * this window to front.
     */
    public void bringToFront()
    {
        this.toFront();
    }

    @Override
    public void bringToBack()
    {
        toBack();
    }

    /**
     * The source of the window
     * @return the source of the window
     */
    public Object getSource()
    {
        return this;
    }

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    public void setParams(Object[] windowParams) {}

    /**
     * The action invoked when user presses Escape key.
     */
    private class CloseAction extends UIAction
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            setVisible(false);
            dispose();
        }
    }

    /**
     * Indicates if the application name should be shown.
     *
     * @return <tt>true</tt> if the application name should be shown,
     * <tt>false</tt> - otherwise
     */
    private boolean isApplicationNameShown()
    {
        String showApplicationNameProp
            = BrandingActivator.getResources().getSettingsString(
                "plugin.branding.IS_APPLICATION_NAME_SHOWN");

        if (showApplicationNameProp != null
            && showApplicationNameProp.length() > 0)
        {
            return Boolean.parseBoolean(showApplicationNameProp);
        }

        return true;
    }

    @Override
    public void repaintWindow()
    {
        repaint();
    }
}
