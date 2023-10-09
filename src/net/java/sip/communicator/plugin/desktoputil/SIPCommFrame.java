/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import net.java.sip.communicator.service.keybindings.*;
import net.java.sip.communicator.util.Logger;

/**
 * A custom frame that remembers its size and location and could have a
 * semi-transparent background.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
public class SIPCommFrame
    extends JFrame
    implements Observer
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * Property that disables the automatic resizing and positioning when a
     * window's top edge is outside the visible area of the screen.
     * <p>
     * <tt>true</tt> use automatic repositioning (default)<br/>
     * <tt>false</tt> rely on the window manager to place the window
     */
    static final String PNAME_CALCULATED_POSITIONING
        = "net.sip.communicator.util.swing.USE_CALCULATED_POSITIONING";

    /**
     * Key identifier for closing the frame
     */
    private static final String CLOSE_KEY = "close";

    /**
     * The <tt>Logger</tt> used by the <tt>SIPCommFrame</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(SIPCommFrame.class);

    /**
     * The action map of this dialog.
     */
    private ActionMap amap;

    /**
     * The input map of this dialog.
     */
    private InputMap imap;

    /**
     * The key bindings set.
     */
    private KeybindingSet bindings = null;

    /**
     * Indicates if the size of this dialog is stored after closing.
     */
    private boolean isSaveSize = true;

    /**
     * Indicates if the location of this dialog is stored after closing.
     */
    private boolean isSaveLocation = true;

    /**
     * The default name that should be used as the config property to save the
     * size and location of this frame.  This is the class name of this
     * instance.
     */
    private final String saveSizeAndLocationName = getClass().getName();

    /**
     * The point where the mouse is clicking on the screen.  This is used to
     * tell if the user is clicking on/dragging this frame when it is set to be
     * undecorated but draggable (see <tt>setDraggableUndecorated()</tt>).
     */
    private Point mouseClickPoint = new Point();

    /**
     * Creates an instance of <tt>SIPCommFrame</tt> by specifying explicitly
     * if the size property and location property are saved. By default size and
     * location are stored.
     * @param isSaveSizeAndLocation indicates whether to save the size of this dialog
     * @param bgStartOverrideColor if not null, this Color will be used as the
     * start Color for this dialog, instead of the Color saved in config.
     * @param bgEndOverrideColor if not null, this Color will be used as the
     * end Color for this dialog, instead of the Color saved in config.
     * @param useBgOverrideColor whether to use the bgOverrideColor. If false,
     * the native background will be used.
     */
    public SIPCommFrame(boolean isSaveSizeAndLocation,
                        Color bgStartOverrideColor,
                        Color bgEndOverrideColor,
                        boolean useBgOverrideColor)
    {
        this.isSaveSize = isSaveSizeAndLocation;
        this.isSaveLocation = isSaveSizeAndLocation;

        if (useBgOverrideColor)
        {
            setContentPane(
                new MainContentPane(bgStartOverrideColor, bgEndOverrideColor));
        }

        init();

        addWindowListener(new FrameWindowAdapter());

        JRootPane rootPane = getRootPane();
        amap = rootPane.getActionMap();
        amap.put(CLOSE_KEY, new CloseAction());

        imap = rootPane.getInputMap(
            JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), CLOSE_KEY);

        // put the defaults for macosx
        if (OSUtils.IS_MAC)
        {
            imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                InputEvent.META_DOWN_MASK), CLOSE_KEY);
            imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_W,
                InputEvent.CTRL_DOWN_MASK), CLOSE_KEY);
        }

        WindowUtils.addWindow(this);
    }

    public SIPCommFrame(boolean isSaveSizeAndLocation,
        Color bgStartOverrideColor,
        Color bgEndOverrideColor)
    {
        this(isSaveSizeAndLocation, bgStartOverrideColor, bgEndOverrideColor, true);
    }

    public SIPCommFrame(Color bgStartOverrideColor,
                        Color bgEndOverrideColor,
                        boolean useBgOverrideColor)
    {
        this(true, bgStartOverrideColor, bgEndOverrideColor, useBgOverrideColor);
    }

    /**
     * Initialize default values.
     */
    private void init()
    {
        WindowUtils.setWindowIcons(this);
    }

    /**
     * Creates an instance of <tt>SIPCommFrame</tt> by specifying explicitly
     * if the size and location properties are saved. By default size and
     * location are stored.
     * @param isSaveSizeAndLocation indicates whether to save the size and
     * location of this dialog
     */
    public SIPCommFrame(boolean isSaveSizeAndLocation)
    {
        this(isSaveSizeAndLocation, null, null);
    }

    /**
     * Creates a <tt>SIPCommFrame</tt>.
     */
    public SIPCommFrame()
    {
        this(true);
    }

    /**
     * The action invoked when user presses Esc, Ctrl-W or Cmd-W key combination.
     */
    private class CloseAction
        extends UIAction
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            if (isSaveSize || isSaveLocation)
            {
                saveSizeAndLocation();
            }

            logger.user("Call window closed using ESC, CTRL-W or CMD-W");

            close(true);
        }
    }

    /**
     * Sets the input map to utilize a given category of keybindings. The frame
     * is updated to reflect the new bindings when they change. This replaces
     * any previous bindings that have been added.
     *
     * @param category set of keybindings to be utilized
     */
    protected void setKeybindingInput(KeybindingSet.Category category)
    {
        // Removes old binding set
        if (bindings != null)
        {
            bindings.deleteObserver(this);
            resetInputMap();
        }

        // Adds new bindings to input map
        bindings
            = DesktopUtilActivator.getKeybindingsService().getBindings(category);

        if (bindings != null)
        {
            for (Map.Entry<KeyStroke, String> key2action
                    : bindings.getBindings().entrySet())
                imap.put(key2action.getKey(), key2action.getValue());

            bindings.addObserver(this);
        }
    }

    /**
     * Bindings the string representation for a keybinding to the action that
     * will be executed.
     *
     * @param binding string representation of action used by input map
     * @param action the action which will be executed when user presses the
     *            given key combination
     */
    protected void addKeybindingAction(String binding, Action action)
    {
        amap.put(binding, action);
    }

    private static class FrameWindowAdapter
        extends WindowAdapter
    {
        @Override
        public void windowClosing(WindowEvent e)
        {
            ((SIPCommFrame) e.getWindow()).windowClosing(e);
        }
    }

    /**
     * Invoked when this window is in the process of being closed. The close
     * operation can be overridden at this point.
     * @param e the <tt>WindowEvent</tt> that notified us
     */
    protected void windowClosing(WindowEvent e)
    {
        /*
         * Before closing the application window save the current size and
         * position through the ConfigurationService.
         */
        if (isSaveSize || isSaveLocation)
        {
            saveSizeAndLocation();
        }

        logger.info("Call window closing");

        close(false);
    }

    /**
     * Saves the size and the location of this frame through the
     * <tt>ConfigurationService</tt>.
     */
    protected void saveSizeAndLocation()
    {
        saveGeometry(this, true, true, saveSizeAndLocationName);
    }

    /**
     * Saves the location of this frame via the <tt>ConfigurationService</tt>.
     */
    protected void saveLocation()
    {
        saveGeometry(this, false, true, saveSizeAndLocationName);
    }

    /**
     * Saves the size and location of a specific <tt>Component</tt> through
     * the <tt>ConfigurationService</tt>.
     *
     * @param component the <tt>Component</tt> which is to have its size and
     * location saved through the <tt>ConfigurationService</tt>
     * @param saveClassName the name to use as the config property to save the
     * size and location of this component. Normally, this is the class name of
     * the component.
     */
    public static void saveSizeAndLocation(Component component,
                                           String saveClassName)
    {
        saveGeometry(component, true, true, saveClassName);
    }

    /**
     * Saves the size and or location of a specific <tt>Component</tt> through
     * the <tt>ConfigurationService</tt>.
     *
     * @param component the <tt>Component</tt> which is to have its size and
     * location saved through the <tt>ConfigurationService</tt>
     * @param saveSize whether to save the size of the window.
     * @param saveLocation whether to save the location of the window.
     * @param saveClassName the name to use as the config property to save the
     * size and location of this component. Normally, this is the class name of
     * the component.
     */
    private static void saveGeometry(Component component,
                                    boolean saveSize,
                                    boolean saveLocation,
                                    String saveClassName)
    {
        final int x = component.getX();
        final int y = component.getY();

        // When saving off sizes, unscale the size so if the user changes the
        // screen resolution, we show a proportioned application
        final int width  = ScaleUtils.unscaleInt(component.getWidth());
        final int height = ScaleUtils.unscaleInt(component.getHeight());

        //Do nothing if all parameters are 0, or size and location are both
        // false.
        boolean skip = (saveSize && width == 0 && height == 0) ||
                       (saveLocation && x == 0 && y == 0) ||
                       (!saveSize && !saveLocation);

        if (!skip)
        {
            // If no class name has been specified, just default to using the
            // class name of the component that has been passed in.
            saveClassName = (saveClassName == null) ?
                component.getClass().getName() : saveClassName;

            Map<String, Object> props = new HashMap<>();
            String className = saveClassName.replaceAll("\\$", "_");

            if (saveSize)
            {
                props.put(className + ".width", width);
                props.put(className + ".height", height);
            }

            if (saveLocation)
            {
                props.put(className + ".x", x);
                props.put(className + ".y", y);
            }

            DesktopUtilActivator.getConfigurationService().user().setProperties(props);
        }
    }

    /**
     * Sets window size and position.  Does nothing if this is not configured to
     * save the size.
     */
    public void setSizeAndLocation()
    {
        String widthString = null;
        String heightString = null;
        String xString = null;
        String yString = null;

        ConfigurationService configService =
            DesktopUtilActivator.getConfigurationService();
        String className = saveSizeAndLocationName.replaceAll("\\$", "_");
        if (configService.user() != null)
        {
            widthString = configService.user().getString(className + ".width");
            heightString = configService.user().getString(className + ".height");
            xString = configService.user().getString(className + ".x");
            yString = configService.user().getString(className + ".y");
        }

        int width = 0;
        int height = 0;

        if (isSaveSize && widthString != null && heightString != null)
        {
            // When retrieving the frame sizes, we need to scale them back up as
            // they are stored unscaled
            width = ScaleUtils.scaleInt(Integer.parseInt(widthString));
            height = ScaleUtils.scaleInt(Integer.parseInt(heightString));

            if (width > 0 && height > 0)
            {
                Dimension screenSize =
                    Toolkit.getDefaultToolkit().getScreenSize();
                if (width <= screenSize.width && height <= screenSize.height)
                    this.setSize(width, height);
            }
        }

        int x = 0;
        int y = 0;

        if (isSaveLocation && xString != null && yString != null)
        {
            x = Integer.parseInt(xString);
            y = Integer.parseInt(yString);

            if(ScreenInformation.
                isTitleOnScreen(new Rectangle(x, y, width, height))
                || configService.user().getBoolean(
                    SIPCommFrame.PNAME_CALCULATED_POSITIONING, false))
            {
                this.setLocation(x, y);
            }
            else
            {
                this.setCenterLocation();
            }
        }
        else
        {
            this.setCenterLocation();
        }
    }

    /**
     * Positions this window in the center of the screen.
     */
    private void setCenterLocation()
    {
        setLocationRelativeTo(null);
    }

    /**
     * Checks whether the current component will exceeds the screen size and if
     * it do will set a default size
     */
    public void ensureOnScreenLocationAndSize()
    {
        ConfigurationService config = DesktopUtilActivator.getConfigurationService();
        if((config.user() == null) ||
           !config.user().getBoolean(SIPCommFrame.PNAME_CALCULATED_POSITIONING, true))
            return;

        // (0,0) is the top left of the primary screen
        // x increases as you go right
        // y increases as you go down
        Rectangle screen = ScreenInformation.getScreenBounds();
        int border = 10;

        // Check the top left hand corner
        Point topLeftDialog = getLocation();
        Point topLeftScreen = screen.getLocation();
        boolean topLeftChanged = false;
        Point newTopLeft = new Point(topLeftDialog);

        if (topLeftDialog.x < topLeftScreen.x)
        {
            // Dialog is too far to the left
            newTopLeft.x = topLeftScreen.x + border;
            topLeftChanged = true;
        }
        if (topLeftDialog.y < topLeftScreen.y)
        {
            // Dialog is too high
            newTopLeft.y = topLeftScreen.y + border;
            topLeftChanged = true;
        }

        if (topLeftChanged)
            setLocation(newTopLeft);

        // Check the bottom right hand corner
        Point bottomRightDialog = new Point(topLeftDialog.x + getWidth(),
                                            topLeftDialog.y + getHeight());
        Point bottomRightScreen =
                   new Point(topLeftScreen.x + (int)screen.getWidth(),
                             topLeftScreen.y + (int)screen.getHeight());
        boolean bottomRightChanged = false;
        Dimension amountToMoveBottomRight = new Dimension(0, 0);

        if (bottomRightDialog.x > bottomRightScreen.x)
        {
            // Dialog is too far to the right
            bottomRightChanged = true;
            amountToMoveBottomRight.width = bottomRightScreen.x -
                                            bottomRightDialog.x -
                                            border;
        }
        if (bottomRightDialog.y > bottomRightScreen.y)
        {
            // Dialog is too low
            bottomRightChanged = true;
            amountToMoveBottomRight.height = bottomRightScreen.y -
                                             bottomRightDialog.y -
                                             border;
        }

        if (bottomRightChanged)
        {
            // Bottom right has changed thus we either need to move the dialog,
            // or resize it (if we have already moved it)
            if (topLeftChanged)
            {
                Dimension size = getSize();
                setSize(size.width + amountToMoveBottomRight.width,
                        size.height + amountToMoveBottomRight.height);
            }
            else
            {
                setLocation(topLeftDialog.x + amountToMoveBottomRight.width,
                            topLeftDialog.y + amountToMoveBottomRight.height);
            }
        }
    }

    /**
     * Overwrites the setVisible method in order to set the size and the
     * position of this window before showing it.
     * @param isVisible indicates if this frame should be visible
     */
    @Override
    public void setVisible(boolean isVisible)
    {
        setVisible(isVisible, true);
    }

    /**
     * Overwrites the setVisible method in order to set the size and the
     * position of this window before showing it.
     * @param isVisible indicates if this window will be made visible or will
     * be hidden
     * @param repositionWindow indicates whether the window should be moved
     */
    public void setVisible(boolean isVisible, boolean repositionWindow)
    {
        if (isVisible && repositionWindow)
        {
            this.setSizeAndLocation();
            this.ensureOnScreenLocationAndSize();
        }
        super.setVisible(isVisible);
    }

    /**
     * {@inheritDoc}
     *
     * Overwrites the super's <tt>dispose</tt> method in order to save the size
     * and the position of this <tt>Window</tt> before closing it.
     */
    @Override
    public void dispose()
    {
        if (isSaveSize || isSaveLocation)
            saveSizeAndLocation();

        /*
         * The KeybindingsService will outlive us so don't let us retain our
         * memory.
         */
        if (bindings != null)
            bindings.deleteObserver(this);

        super.dispose();
    }

    private void resetInputMap()
    {
        imap.clear();
    }

    /**
     * Listens for changes in binding sets so they can be reflected in the input
     * map.
     * @param obs the <tt>KeybindingSet</tt> from which to update
     */
    public void update(Observable obs, Object arg)
    {
        if (obs instanceof KeybindingSet)
        {
            KeybindingSet changedBindings = (KeybindingSet) obs;

            resetInputMap();
            for (Map.Entry<KeyStroke, String> key2action : changedBindings
                .getBindings().entrySet())
            {
                imap.put(key2action.getKey(), key2action.getValue());
            }
        }
    }

    /**
     * The main content pane.
     */
    public static class MainContentPane
        extends JPanel
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private boolean isColorBgEnabled;

        private boolean isImageBgEnabled;

        private Color bgStartColor;

        private Color bgEndColor;

        private Color bgStartOverrideColor = null;

        private Color bgEndOverrideColor = null;

        private BufferedImage bgImage = null;

        private TexturePaint texture = null;

        /**
         * Creates an instance of <tt>MainContentPane</tt>.
         */
        public MainContentPane()
        {
            this(null, null);
        }

        /**
         * Creates an instance of <tt>MainContentPane</tt>.
         *
         * @param bgStartOverrideColor if not null, this Color will be used as
         * the start Color for this pane, instead of the Color saved in config.
         * @param bgEndOverrideColor if not null, this Color will be used as
         * the end Color for this pane, instead of the Color saved in config.
         */
        public MainContentPane(Color bgStartOverrideColor, Color bgEndOverrideColor)
        {
            super(new BorderLayout());

            this.bgStartOverrideColor = bgStartOverrideColor;
            this.bgEndOverrideColor = bgEndOverrideColor;

            initColors();
            initStyles();
        }

        /**
         * Validates this container and all of its subcomponents.
         * <p>
         * The <code>validate</code> method is used to cause a container
         * to lay out its subcomponents again. It should be invoked when
         * this container's subcomponents are modified (added to or
         * removed from the container, or layout-related information
         * changed) after the container has been displayed.
         *
         * <p>If this {@code Container} is not valid, this method invokes
         * the {@code validateTree} method and marks this {@code Container}
         * as valid. Otherwise, no action is performed.
         *
         * @see #add(java.awt.Component)
         * @see Component#invalidate
         * @see javax.swing.JComponent#revalidate()
         * @see #validateTree
         */
        @Override
        public void validate()
        {
            initStyles();
            super.validate();
        }

        /**
         * Repaints this component.
         */
        @Override
        public void repaint()
        {
            initColors();
            super.repaint();
        }

        /**
         * Initialize color values.
         */
        private void initColors()
        {
            ResourceManagementService resources =
                DesktopUtilActivator.getResources();

            isColorBgEnabled =
                    Boolean.valueOf(resources.getSettingsString(
                            "impl.gui.IS_WINDOW_COLOR_BACKGROUND_ENABLED"));

            if (isColorBgEnabled)
            {
                if (bgStartOverrideColor == null)
                {
                    bgStartColor = new Color(
                        resources.getColor("service.gui.LIGHT_BACKGROUND"));
                }
                else
                {
                    bgStartColor = bgStartOverrideColor;
                }

                if (bgEndOverrideColor == null)
                {
                    bgEndColor = new Color(
                        resources.getColor("service.gui.LIGHT_BACKGROUND"));
                }
                else
                {
                    bgEndColor = bgEndOverrideColor;
                }
            }
            else
            {
                bgStartColor = null;
                bgEndColor = null;
            }

            isImageBgEnabled =
                    Boolean.valueOf(resources.getSettingsString(
                            "impl.gui.IS_WINDOW_IMAGE_BACKGROUND_ENABLED"));

            if (isImageBgEnabled)
            {
                final URL bgImagePath
                    = resources.getImageURL("service.gui.WINDOW_TITLE_BAR_BG");

                bgImage = ImageUtils.getBufferedImage(bgImagePath);

                final Rectangle rect =
                    new Rectangle(0, 0, bgImage.getWidth(),
                                    bgImage.getHeight());

                texture = new TexturePaint(bgImage, rect);
            }
        }

        /**
         * Initialize style values.
         */
        private void initStyles()
        {
            ResourceManagementService resources =
                DesktopUtilActivator.getResources();

            int borderSize =
                resources
                    .getSettingsInt("impl.gui.MAIN_WINDOW_BORDER_SIZE");
            this.setBorder(BorderFactory.createEmptyBorder(borderSize,
                borderSize, borderSize, borderSize));
        }

        /**
         * Paints this content pane.
         * @param g the <tt>Graphics</tt> object used for painting
         */
        @Override
        public void paintComponent(Graphics g)
        {
            super.paintComponent(g);

            // If the custom color or image window background is not enabled we
            // have nothing to do here.
            if (isColorBgEnabled || isImageBgEnabled)
            {
                g = g.create();
                try
                {
                    internalPaintComponent(g);
                }
                finally
                {
                    g.dispose();
                }
            }
        }

        /**
         * Provides a custom paint if the color or image background properties
         * are enabled.
         * @param g the <tt>Graphics</tt> object used for painting
         */
        private void internalPaintComponent(Graphics g)
        {
            AntialiasingManager.activateAntialiasing(g);

            Graphics2D g2 = (Graphics2D) g;
            int width = getWidth();
            int height = getHeight();

            if (isColorBgEnabled)
            {
                GradientPaint bgGradientColor =
                    new GradientPaint(width / 2, 0, bgStartColor, width / 2, 80,
                        bgEndColor);

                g2.setPaint(bgGradientColor);
                g2.fillRect(0, 0, width, 80);

                g2.setColor(bgEndColor);
                g2.fillRect(0, 78, width, height);
            }

            if (isImageBgEnabled)
            {
                if (bgImage != null && texture != null)
                {
                    g2.setPaint(texture);

                    g2.fillRect(0, 0, this.getWidth(), bgImage.getHeight());
                }
            }
        }
    }

    /**
     * Notifies this instance that it has been requested to close. The default
     * <tt>SIPCommFrame</tt> implementation does nothing.
     *
     * @param escape <tt>true</tt> if the request to close this instance is in
     * response of a press on the Escape key; otherwise, <tt>false</tt>
     */
    protected void close(boolean escape)
    {
    }

    /**
     * Ordinarily, undecorated frames cannot be dragged around the screen.
     * This method disables decorations for this frame but still allows it to
     * be dragged to a different position on the screen.
     */
    public void setDraggableUndecorated()
    {
        setUndecorated(true);

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent evt)
            {
                mouseClickPoint = evt.getPoint();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override
            public void mouseDragged(MouseEvent evt)
            {
                Point locationPoint = evt.getLocationOnScreen();
                setLocation(
                    locationPoint.x - mouseClickPoint.x,
                    locationPoint.y - mouseClickPoint.y);
            }
        });
    }
}
