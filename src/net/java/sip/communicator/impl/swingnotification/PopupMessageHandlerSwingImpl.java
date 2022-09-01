/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.swingnotification;

import java.awt.*;
import java.awt.Window.Type;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.chat.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.service.systray.event.*;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Html2Text;
import net.java.sip.communicator.util.Logger;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * Implements <tt>PopupMessageHandler</tt> using Swing.
 *
 * @author Symphorien Wanko
 * @author Lubomir Marinov
 */
public class PopupMessageHandlerSwingImpl
    extends AbstractPopupMessageHandler
{
    /** logger for the <tt>PopupMessageHandlerSwingImpl</tt> class */
    private static final Logger logger
        = Logger.getLogger(PopupMessageHandlerSwingImpl.class);

    /**
     * The max width of these notifications.
     */
    private static final int POP_UP_MAX_WIDTH = 300;

    /**
     * Implements <tt>PopupMessageHandler#showPopupMessage()</tt>
     *
     * @param popupMessage the message we will show
     */
    public void showPopupMessage(final PopupMessage popupMessage)
    {
        logger.debug("Asked to show pop-up message");
        if(!SwingUtilities.isEventDispatchThread())
        {
            logger.debug("Not on EDT - re-scheduling pop-up on EDT");
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    logger.debug("Re-running show popup on EDT");
                    showPopupMessage(popupMessage);
                }
            });
            return;
        }

        final GraphicsConfiguration graphicsConf =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
            getDefaultScreenDevice().
            getDefaultConfiguration();

        final JFrame notificationWindow = new JFrame(graphicsConf);

        BufferedImageFuture logo = SwingNotificationActivator.getResources().getBufferedImage(
                          "service.gui.SIP_COMMUNICATOR_LOGO_64x64");
        notificationWindow.setUndecorated(true);
        logo.addToFrame(notificationWindow);

        if (!ConfigurationUtils.isAccessibilityMode())
        {
            notificationWindow.setType(Type.UTILITY);
        }

        final Timer popupTimer = new Timer(10000, new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                if (notificationWindow.isVisible())
                    new Thread(new PopupDiscarder(notificationWindow)).start();
            }
        });
        popupTimer.setRepeats(false);

        MouseAdapter adapter = new MouseAdapter()
        {
            @Override
            public void mouseEntered(MouseEvent e)
            {
                popupTimer.stop();
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                popupTimer.start();
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                Container container = notificationWindow.getContentPane();
                PopupNotificationPanel notif =
                    (PopupNotificationPanel) container.getComponent(0);
                firePopupMessageClicked(
                    new SystrayPopupMessageEvent(e, notif.getTag()));
                popupMessage.onPopupClicked();
                notificationWindow.dispose();
            }
        };

        notificationWindow.addMouseListener(adapter);
        JComponent content = popupMessage.getComponent();

        if (content == null)
        {
            content = createPopup(
                popupMessage.getMessageTitle(),
                popupMessage.getMessage(),
                popupMessage.getIcon(),
                popupMessage.getTag(),
                popupMessage.isError());
        }

        // Set the title of the popup even though it won't be displayed - this
        // allows accessibility software to read it.
        String popupMessageContent = Html2Text.extractText("<pre>" +
                                          popupMessage.getMessage() + "</pre>");
        notificationWindow.setTitle(popupMessage.getMessageTitle() + " : " + popupMessageContent);

        registerMouseListener(content, adapter);
        notificationWindow.add(content);
        notificationWindow.setAlwaysOnTop(true);
        notificationWindow.pack();

        if (ConfigurationUtils.isAccessibilityMode())
        {
            notificationWindow.setFocusable(true);
        }
        else
        {
            notificationWindow.setFocusableWindowState(false);
            notificationWindow.setFocusable(false);
            notificationWindow.setAutoRequestFocus(false);
        }

        new Thread(new PopupLauncher(notificationWindow, graphicsConf)).start();
        popupTimer.start();
        logger.debug("Started popup timer");
    }

    private void registerMouseListener(Component content, MouseAdapter adapter)
    {
        content.addMouseListener(adapter);
        if(content instanceof JComponent)
            for(Component c : ((JComponent) content).getComponents())
                registerMouseListener(c, adapter);
    }

    /**
     * Builds the popup component with given informations. Wraps the specified
     * <tt>message</tt> in HTML &lt;pre&gt; tags to ensure that text such as
     * full pathnames is displayed correctly after HTML is stripped from it.
     *
     * @param titleString message title
     * @param message message content
     * @param imageBytes message icon
     * @param tag
     * @param isError whether this pop-up is due to an error
     * @return
     */
    private JComponent createPopup( String titleString,
                                    String message,
                                    BufferedImageFuture imageBytes,
                                    Object tag,
                                    boolean isError)
    {
        JLabel msgIcon = null;
        if (imageBytes != null)
        {
            // If we're on Windows, we might have to adjust for scaling.
            int width = OSUtils.IS_WINDOWS ? ScaleUtils.scaleInt(45) : 45;
            int height = OSUtils.IS_WINDOWS ? ScaleUtils.scaleInt(45) : 45;

            ImageIconFuture imageIcon
                = imageBytes.getScaledRoundedIcon(width, height);

            msgIcon = imageIcon.addToLabel(new JLabel());
        }

        //Formats message to HTML
        String processedMessage = ChatHtmlUtils.formatMessage(message, "text/plain", null);
        //Replaces unicode codepoints with emojis where appropriate
        processedMessage = ChatHtmlUtils.performReplacementsOnString(processedMessage);

        JEditorPane msgContent = new JEditorPane("text/html", processedMessage);
        ScaleUtils.scaleFontAsDefault(msgContent);
        msgContent.setEditable(false);

        //Allows JEditorPane to use default font
        msgContent.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        msgContent.setOpaque(false);
        msgContent.setAlignmentX(JTextArea.LEFT_ALIGNMENT);
        if (isError)
        {
            msgContent.setForeground(Color.RED);
        }
        else
        {
            msgContent.setForeground(new Color(
                SwingNotificationActivator.getResources().getColor(
                    "service.gui.DARK_TEXT")));
        }

        int msgContentHeight
            = getPopupMessageAreaHeight(msgContent, message);
        msgContent.setPreferredSize(new Dimension(POP_UP_MAX_WIDTH,
                                                  msgContentHeight));

        TransparentPanel notificationBody = new TransparentPanel();
        notificationBody.setLayout(
            new BoxLayout(notificationBody, BoxLayout.Y_AXIS));
        notificationBody.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        notificationBody.add(msgContent);

        TransparentPanel notificationContent
            = new TransparentPanel();

        notificationContent.setLayout(new BorderLayout(5, 0));

        notificationContent.setBorder(
                BorderFactory.createEmptyBorder(0, 5, 5, 5));

        if(msgIcon != null)
            notificationContent.add(msgIcon, BorderLayout.WEST);
        notificationContent.add(notificationBody, BorderLayout.CENTER);

        PopupNotificationPanel notificationPanel =
              new PopupNotificationPanel(titleString, notificationContent, tag);

        AccessibilityUtils.setName(notificationPanel, message);

        return notificationPanel;
    }

    /**
     * Implements <tt>toString</tt> from <tt>PopupMessageHandler</tt>
     * @return a description of this handler
     */
    @Override
    public String toString()
    {
        String applicationName
            = SwingNotificationActivator.getResources()
                .getSettingsString("service.gui.APPLICATION_NAME");

        return SwingNotificationActivator.getResources()
                .getI18NString("impl.swingnotification.POPUP_MESSAGE_HANDLER",
                    new String[]{applicationName});
    }

    /**
     * provide animation to hide a popup. The animation could be described
     * as an "inverse" of the one made by <tt>PopupLauncher</tt>.
     */
    private static class PopupDiscarder
        implements Runnable
    {
        private final JFrame notificationWindow;

        PopupDiscarder(JFrame notificationWindow)
        {
            this.notificationWindow = notificationWindow;
        }

        public void run()
        {
            int currentY = notificationWindow.getY();
            int targetY  = notificationWindow.getY() +
                            notificationWindow.getHeight();
            int x = notificationWindow.getX();

            do
            {
                currentY += 2;
                notificationWindow.setLocation(x, currentY);
                try
                {
                    Thread.sleep(10);
                } catch (InterruptedException ex)
                {
                    logger.warn("exception while discarding" +
                        " popup notification window :", ex);
                }
            } while (targetY > currentY);
            notificationWindow.dispose();
        }
    }

    /**
     * provide animation to show a popup. The popup comes from the bottom of
     * screen and will stay in the bottom right corner.
     */
    private static class PopupLauncher
        implements Runnable
    {
        private final JFrame notificationWindow;

        private final int x;

        PopupLauncher(
                JFrame notificationWindow,
                GraphicsConfiguration graphicsConf)
        {
            this.notificationWindow = notificationWindow;

            final Rectangle rec = graphicsConf.getBounds();

            final Insets ins =
                Toolkit.getDefaultToolkit().getScreenInsets(graphicsConf);

            x = rec.width + rec.x -
                ins.right - notificationWindow.getWidth() - 1;

            notificationWindow.setLocation(x, rec.height);
            notificationWindow.setVisible(true);

            if (ConfigurationUtils.isAccessibilityMode())
            {
                notificationWindow.requestFocus();
            }
            else
            {
                notificationWindow.setAutoRequestFocus(false);
            }
        }

        public void run()
        {
            int currentY = notificationWindow.getY();
            int targetY  = notificationWindow.getY() -
                           notificationWindow.getHeight();

            // Windows/Macs may have a task bar/dock at the bottom of the
            // screen.  If so we want the message to appear above this:
            Dimension scrnSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle winSize = GraphicsEnvironment
                     .getLocalGraphicsEnvironment().getMaximumWindowBounds();
            // Need to also subtract the winSize.y as this could be non-zero
            // on Mac due to the menu bar
            int taskBarHeight = scrnSize.height - winSize.height - winSize.y;
            targetY -= taskBarHeight;

            do
            {
                currentY -= 2;
                notificationWindow.setLocation(x, currentY);
                try
                {
                    Thread.sleep(10);
                } catch (InterruptedException ex)
                {
                    logger.warn("exception while showing" +
                        " popup notification window :", ex);
                }
            } while (currentY > targetY);
        }
    }

    /**
     * Returns the appropriate popup message height, according to the currently
     * used font and the size of the message.
     *
     * @param c the component used to show the message
     * @param message the message
     * @return the appropriate popup message height
     */
    private int getPopupMessageAreaHeight(Component c, String message)
    {
        int numberOfRows = 0;

        for (String line : message.split("\\n"))
        {
            int stringWidth = ComponentUtils.getStringWidth(c, line);
            int numberOfRowsOfLine = (stringWidth / POP_UP_MAX_WIDTH) + 1;

            numberOfRows += numberOfRowsOfLine;
        }

        FontMetrics fontMetrics = c.getFontMetrics(c.getFont());

        return fontMetrics.getHeight()*Math.max(numberOfRows, 3)+5;
    }

    /**
     * Implements <tt>getPreferenceIndex</tt> from <tt>PopupMessageHandler</tt>.
     * This handler is able to show images, detect clicks, match a click to a
     * message, thus the preference index is 3.
     * @return a preference index
     */
    public int getPreferenceIndex()
    {
        return 3;
    }
}
