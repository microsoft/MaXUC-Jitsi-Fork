/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.*;

import javax.swing.*;

import org.jitsi.service.configuration.*;
import org.jitsi.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;

/**
 * @author Yana Stamcheva
 * @author Lubomir Marinov
 */
public class SIPCommDialog
    extends JDialog
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>SIPCommDialog</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(SIPCommDialog.class);

    /**
     * The action map of this dialog.
     */
    private ActionMap amap;

    /**
     * The input map of this dialog.
     */
    private InputMap imap;

    /**
     * Indicates if the size and location of this dialog are stored after
     * closing.
     */
    private boolean isSaveSizeAndLocation = true;

    /**
     * The class name that should be used as the config property to save the
     * size and location of this dialog.  By default this is the class name of
     * this instance.
     */
    private String saveSizeAndLocationName = getClass().getName();

    /**
     * Creates an instance of <tt>SIPCommDialog</tt>.
     */
    public SIPCommDialog()
    {
        super();

        this.init();
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying the
     * <tt>Window</tt>owner of this dialog.
     * @param owner the owner of this dialog
     */
    public SIPCommDialog(Window owner)
    {
        super(determineOwner(owner));

        this.init();
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying explicitly
     * if the size and location properties are saved. By default size and
     * location are stored.
     * @param isSaveSizeAndLocation indicates whether to save the size and
     * location of this dialog
     */
    public SIPCommDialog(boolean isSaveSizeAndLocation)
    {
        this();

        this.isSaveSizeAndLocation = isSaveSizeAndLocation;
    }

    /**
     * Creates an instance of <tt>SIPCommDialog</tt> by specifying the owner
     * of this dialog and indicating whether to save the size and location
     * properties.
     * @param owner the owner of this dialog
     * @param isSaveSizeAndLocation indicates whether to save the size and
     * location of this dialog
     */
    public SIPCommDialog(Window owner, boolean isSaveSizeAndLocation)
    {
        this(owner);

        this.isSaveSizeAndLocation = isSaveSizeAndLocation;
    }

    /**
     * Initializes this dialog.
     */
    private void init()
    {
        // Check if we should use the native background.
        if (!ConfigurationUtils.useNativeTheme())
            this.setContentPane(new SIPCommFrame.MainContentPane());

        this.addWindowListener(new DialogWindowAdapter());

        WindowUtils.setWindowIcons(this);

        this.initInputMap();

        WindowUtils.addWindow(this);
    }

    private void initInputMap()
    {
        amap = this.getRootPane().getActionMap();

        amap.put("close", new CloseAction());

        imap = this.getRootPane().getInputMap(
                JComponent.WHEN_IN_FOCUSED_WINDOW);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");

        // put the defaults for macosx
        if(OSUtils.IS_MAC)
        {
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.META_DOWN_MASK),
                "close");
            imap.put(
                KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK),
                "close");
        }
    }

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
            if(isSaveSizeAndLocation)
                saveSizeAndLocation();

            close(true);
        }
    }

    /**
     * Adds a key - action pair for this frame.
     *
     * @param keyStroke the key combination
     * @param action the action which will be executed when user presses the
     * given key combination
     */
    protected void addKeyBinding(KeyStroke keyStroke, Action action)
    {
        String actionID = action.getClass().getName();

        amap.put(actionID, action);

        imap.put(keyStroke, actionID);
    }

    /**
     * Before closing the application window saves the current size and position
     * through the <tt>ConfigurationService</tt>.
     */
    public class DialogWindowAdapter extends WindowAdapter
    {
        /**
         * Invoked when this window is in the process of being closed.
         * @param e the <tt>WindowEvent</tt> that notified us
         */
        public void windowClosing(WindowEvent e)
        {
            if(isSaveSizeAndLocation)
                saveSizeAndLocation();

            close(false);
        }
    }

    /**
     * Sets the class name that should be used as the config property to save
     * the size and location of this dialog.
     *
     * @param name the class name
     */
    public void setSaveSizeAndLocationName(String name)
    {
        saveSizeAndLocationName = name;
    }

    /**
     * Saves the size and the location of this dialog through the
     * <tt>ConfigurationService</tt>.
     */
    public void saveSizeAndLocation()
    {
        SIPCommFrame.saveSizeAndLocation(this, saveSizeAndLocationName);
    }

    /**
     * Sets window size and position.
     */
    private void setSizeAndLocation()
    {
        ConfigurationService config = DesktopUtilActivator.getConfigurationService();
        String className = saveSizeAndLocationName.replaceAll("\\$", "_");

        int width = ScaleUtils.scaleInt(config.user().getInt(className + ".width", 0));
        int height = ScaleUtils.scaleInt(config.user().getInt(className + ".height", 0));

        String xString = config.user().getString(className + ".x");
        String yString = config.user().getString(className + ".y");

        // Only resize the window if it is resizable - otherwise, allow the
        // calling code to handle the layout
        if(width > 0 && height > 0 && isResizable())
            this.setSize(width, height);

        if(xString != null && yString != null)
        {
            int x = Integer.parseInt(xString);
            int y = Integer.parseInt(yString);
            if(ScreenInformation.
                isTitleOnScreen(new Rectangle(x, y, width, height))
                || config.user().getBoolean(
                    SIPCommFrame.PNAME_CALCULATED_POSITIONING, true))
            {
                this.setLocation(x, y);
            }
        }
        else
            this.setCenterLocation();
    }

    /**
     * Positions this window in the centre of the parent window, or the centre
     * of the screen if the parent window is minimized.
     */
    private void setCenterLocation()
    {
        Container parent = getParent();

        boolean isIconifiedFrame =
                (parent instanceof Frame) &&
                (((Frame) parent).getExtendedState() == Frame.ICONIFIED);

        if (isIconifiedFrame)
        {
            // Set location to centre of primary display.
            setLocationRelativeTo(null);
        }
        else
        {
            setLocationRelativeTo(parent);
        }
    }

    /**
     * Checks whether the current component will
     * exceeds the screen size and if it do will set a default size
     */
    private void ensureOnScreenLocationAndSize()
    {
        ConfigurationService config = DesktopUtilActivator.getConfigurationService();
        if(config.user() != null &&
           !config.user().getBoolean(SIPCommFrame.PNAME_CALCULATED_POSITIONING, true))
            return;

        int x = this.getX();
        int y = this.getY();

        int width = this.getWidth();
        int height = this.getHeight();

        Rectangle virtualBounds = ScreenInformation.getScreenBounds();

        // the default distance to the screen border
        final int borderDistance = 10;

        // in case any of the sizes exceeds the screen size
        // we set default one
        // get the left upper point of the window
        if (!(virtualBounds.contains(x, y)))
        {
            // top left exceeds screen bounds
            if (x < virtualBounds.x)
            {
                // window is too far to the left
                // move it to the right
                x = virtualBounds.x + borderDistance;
            } else if (x > virtualBounds.x)
            {
                // window is too far to the right
                // can only occour, when screen resolution is
                // changed or displayed are disconnected

                // move the window in the bounds to the very right
                x = virtualBounds.x + virtualBounds.width - width
                        - borderDistance;
                if (x < virtualBounds.x + borderDistance)
                {
                    x = virtualBounds.x + borderDistance;
                }
            }

            // top left exceeds screen bounds
            if (y < virtualBounds.y)
            {
                // window is too far to the top
                // move it to the bottom
                y = virtualBounds.y + borderDistance;
            } else if (y > virtualBounds.y)
            {
                // window is too far to the bottom
                // can only occour, when screen resolution is
                // changed or displayed are disconnected

                // move the window in the bounds to the very bottom
                y = virtualBounds.y + virtualBounds.height - height
                        - borderDistance;
                if (y < virtualBounds.y + borderDistance)
                {
                    y = virtualBounds.y + borderDistance;
                }
            }
            this.setLocation(x, y);
        }

        // check the lower right corder
        if (!(virtualBounds.contains(x, y, width, height)))
        {
            if (x + width > virtualBounds.x + virtualBounds.width)
            {
                // location of window is too far to the right, its right
                // border is out of bounds

                // calculate a new horizontal position
                // move the whole window to the left
                x = virtualBounds.x + virtualBounds.width - width
                        - borderDistance;
                if (x < virtualBounds.x + borderDistance)
                {
                    // window is already on left side, it is too wide.
                    x = virtualBounds.x + borderDistance;
                    // reduce the width, so it surely fits
                    width = virtualBounds.width - 2 * borderDistance;
                }
            }
            if (y + height > virtualBounds.y + virtualBounds.height)
            {
                // location of window is too far to the bottom, its bottom
                // border is out of bounds

                // calculate a new vertical position
                // move the whole window to the top
                y = virtualBounds.y + virtualBounds.height - height
                        - borderDistance;
                if (y < virtualBounds.y + borderDistance)
                {
                    // window is already on top, it is too high.
                    y = virtualBounds.y + borderDistance;
                    // reduce the width, so it surely fits
                    height = virtualBounds.height - 2 * borderDistance;
                }
            }
            this.setPreferredSize(new Dimension(width, height));
            this.setSize(width, height);
            this.setLocation(x, y);
        }
    }

    /**
     * If the owner specified for the dialog is null, this method sets the
     * owner to be the main window, unless it is is not visible.  This means
     * that the dialog will appear in front of the main window, rather than
     * defaulting to the centre of the primary monitor.
     *
     * @param owner The owner specified in the constructor
     * @return The Window that will be used as the owner
     */
    private static Window determineOwner(Window owner)
    {
        if (owner == null)
        {
            UIService uiService = DesktopUtilActivator.getUIService();

            if (uiService != null)
            {
                ExportedWindow mainFrame =
                    uiService.getExportedWindow(ExportedWindow.MAIN_WINDOW);

                if (mainFrame instanceof Window)
                {
                    logger.debug("Updating dialog owner to mainframe");
                    owner = (Window) mainFrame;
                }
            }
        }

        return owner;
    }

    /**
     * Overwrites the setVisible method in order to set the size and the
     * position of this window before showing it.
     * @param isVisible indicates if the dialog should be visible
     */
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            this.pack();

            // If the dialog is already visible, do not want to change its size
            // or location.
            if (!this.isVisible())
            {
                if (isSaveSizeAndLocation)
                {
                    this.setSizeAndLocation();
                }
                else
                {
                    this.setCenterLocation();
                }

                ensureOnScreenLocationAndSize();
            }

            JButton button = this.getRootPane().getDefaultButton();

            if (button != null)
                button.requestFocus();
        }
        super.setVisible(isVisible);
    }

    /**
     * Overwrites the dispose method in order to save the size and the position
     * of this window before closing it.
     */
    public void dispose()
    {
        if(isSaveSizeAndLocation)
            this.saveSizeAndLocation();

        super.dispose();
    }

    /**
     * All functions implemented in this method will be invoked when user
     * presses the Escape key.
     *
     * @param escaped <tt>true</tt> if this frame has been closed by pressing
     * the Esc key; otherwise, <tt>false</tt>
     */
    protected void close(boolean escaped)
    {
    }

    /**
     * Sets whether this <tt>SIPCommDialog</tt> ignores the presence of modal
     * dialogs onscreen.
     *
     * @param excludeModality <tt>true</tt> if this frame should ignore modal
     * dialogs; <tt>false</tt> otherwise.
     */
    public void setExcludeModality(boolean excludeModality)
    {
        if (excludeModality)
        {
            setModalExclusionType(
                                 Dialog.ModalExclusionType.APPLICATION_EXCLUDE);
        }
        else
        {
            setModalExclusionType(Dialog.ModalExclusionType.NO_EXCLUDE);
        }
    }
}
