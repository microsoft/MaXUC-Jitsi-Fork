/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil.lookandfeel;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.ToolTipManager;
import javax.swing.plaf.ComponentUI;
import javax.swing.text.JTextComponent;

import org.jitsi.service.resources.BufferedImageFuture;

import net.java.sip.communicator.plugin.desktoputil.AntialiasingManager;
import net.java.sip.communicator.plugin.desktoputil.DesktopUtilActivator;
import net.java.sip.communicator.plugin.desktoputil.ScaleUtils;
import net.java.sip.communicator.plugin.desktoputil.lookandfeel.SearchField.SearchFieldContainer;
import net.java.sip.communicator.plugin.desktoputil.plaf.SIPCommTextFieldUI;
import net.java.sip.communicator.service.analytics.AnalyticsEventType;
import net.java.sip.communicator.service.gui.UIService.Reformatting;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.UtilActivator;
import net.java.sip.communicator.util.account.AccountUtils;
import net.java.sip.communicator.util.skin.Skinnable;

/**
 * The <tt>SearchTextFieldUI</tt> is the one responsible for the search field
 * look & feel. It draws a search icon inside the field and adjusts the bounds
 * of the editor rectangle according to it.
 *
 * @author Yana Stamcheva
 * @author Adam Netocny
 */
public class SearchFieldUI
    extends SIPCommTextFieldUI
    implements Skinnable
{
    /**
     * The logger.
     */
    public static final Logger logger = Logger.getLogger(SearchFieldUI.class);

    /**
     * The default icon of the dialpad button when the dialpad is not visible.
     */
    private BufferedImageFuture dialPadIcon;

    /**
     * The roll over icon of the dialpad button when the dialpad is not visible.
     */
    private BufferedImageFuture dialPadRolloverIcon;

    /**
     * The pressed icon of the dialpad button.
     */
    private BufferedImageFuture dialPadPressedIcon;

    /**
     * The default icon of the dialpad button when the dialpad is visible.
     */
    private BufferedImageFuture dialPadOnIcon;

    /**
     * The roll over icon of the dialpad button when the dialpad is visible.
     */
    private BufferedImageFuture dialPadOnRolloverIcon;

    /**
     * The default icon of the call button.
     */
    private BufferedImageFuture callIcon;

    /**
     * The roll over icon of the call button.
     */
    private BufferedImageFuture callRolloverIcon;

    /**
     * The pressed icon of the call button.
     */
    private BufferedImageFuture callPressedIcon;

    /**
     * The default icon of the chat button.
     */
    private BufferedImageFuture chatIcon;

    /**
     * The roll over icon of the chat button.
     */
    private BufferedImageFuture chatRolloverIcon;

    /**
     * The pressed icon of the chat button.
     */
    private BufferedImageFuture chatPressedIcon;

    /**
     * The separator icon shown between the call and chat icons and the close.
     */
    private BufferedImageFuture separatorIcon;

    /**
     * The image drawn on the left hand side of the search field background when
     * no text has been entered
     */
    private BufferedImageFuture backgroundLeftImage;

    /**
     * The image drawn on the right hand side of the search field background
     * when no text has been entered
     */
    private BufferedImageFuture backgroundRightImage;

    /**
     * The image drawn repeatedly in the middle of the search field background
     * when no text has been entered
     */
    private BufferedImageFuture backgroundMiddleImage;

    /**
     * The image drawn on the left hand side of the search field background
     * when some text has been entered
     */
    private BufferedImageFuture backgroundLeftImageActive;

    /**
     * The image drawn on the right hand side of the search field background
     * when some text has been entered
     */
    private BufferedImageFuture backgroundRightImageActive;

    /**
     * The image drawn repeatedly in the middle of the search field background
     * when some text has been entered
     */
    private BufferedImageFuture backgroundMiddleImageActive;

    /**
     * Indicates if the mouse is currently over the dial pad button.
     */
    private boolean isDialPadMouseOver = false;

    /**
     * Indicates if the mouse is currently pressing the dial pad button.
     */
    private boolean isDialPadMousePressed = false;

    /**
     * Indicates if the dial pad is visible.
     */
    private boolean isDialPadVisible = false;

    /**
     * Indicates if the mouse is currently over the call button.
     */
    private boolean isCallMouseOver = false;

    /**
     * Indicates if the mouse is currently pressing the call button.
     */
    private boolean isCallMousePressed = false;

    /**
     * Indicates if the mouse is currently over the chat button.
     */
    private boolean isChatMouseOver = false;

    /**
     * Indicates if the mouse is currently pressing the chat button.
     */
    private boolean isChatMousePressed = false;

    /**
     * The dial pad button tool tip string.
     */
    private final String dialPadString
        = DesktopUtilActivator.getResources().getI18NString("service.gui.SHOW_HIDE_DIALPAD");

    /**
     * The call button tool tip string.
     */
    private final String callString
        = DesktopUtilActivator.getResources().getI18NString("service.gui.CALL");

    /**
     * The chat button tool tip string.
     */
    private final String chatString
        = DesktopUtilActivator.getResources().getI18NString("service.gui.CHAT");

    /**
     * Indicates if the call icon is currently visible.
     */
    private boolean isCallIconVisible = false;

    /**
     * Indicates if the call icon is currently visible.
     */
    private boolean isChatIconVisible = false;

    /**
     * Indicates if the call button is enabled in this search field.
     */
    private boolean buttonsEnabled = true;

    /**
     * Creates a <tt>SIPCommTextFieldUI</tt>.
     */
    public SearchFieldUI()
    {
        loadSkin();
    }

    /**
     * Enables/disables the call button in the search field.
     *
     * @param isEnabled indicates if the call button is enabled
     */
    public void setButtonsEnabled(boolean isEnabled)
    {
        buttonsEnabled = isEnabled;
    }

    /**
     * Implements parent paintSafely method and enables antialiasing.
     * @param g the <tt>Graphics</tt> object that notified us
     */
    protected void paintSafely(Graphics g)
    {
        customPaintBackground(g);
        super.paintSafely(g);
    }

    /**
     * Paints the background of the associated component.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    protected void customPaintBackground(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g2);

            SearchField searchField = (SearchField)this.getComponent();
            boolean searchFieldActive = searchField.isFocusOwner();

            // Get the right images for the background
            Image leftImage = (searchFieldActive ?
                                backgroundLeftImageActive : backgroundLeftImage).resolve();
            Image rightImage = (searchFieldActive ?
                              backgroundRightImageActive : backgroundRightImage).resolve();
            Image middleImage = (searchFieldActive ?
                            backgroundMiddleImageActive : backgroundMiddleImage).resolve();

            // And start to draw the background.  Note that it is drawn
            // slightly outside the rectangle so that the UI elements align
            // without the surround
            int dx = 0;
            int dy = ScaleUtils.scaleInt(-3);

            // Draw the left image:
            g2.drawImage(leftImage, dx, dy, null);
            dx += leftImage.getWidth(null);

            // Tile the central image:
            while (dx < searchField.getWidth() - rightImage.getWidth(null))
            {
                g2.drawImage(middleImage, dx, dy, null);
                dx += middleImage.getWidth(null);
            }

            // And draw the right image
            g2.drawImage(rightImage, dx, dy, null);

            String phoneNumber = searchField.getPhoneNumber();

            if (buttonsEnabled)
            {
                // Paint dial pad button.
                Rectangle dialPadRect = getDialPadButtonRect();
                Image icon = getDialPadIcon().resolve();
                dx = dialPadRect.x;
                dy = dialPadRect.y;
                g2.drawImage(icon, dx, dy, null);

                SearchFieldContainer container =
                    searchField.getSearchFieldContainer();

                if (phoneNumber != null
                    && phoneNumber.length() > 0
                    && container.getTelephonyProviders().size() > 0)
                {
                    // Paint call button.
                    Rectangle callRect = getCallButtonRect();
                    dx = callRect.x;
                    dy = callRect.y;

                    if (isCallMousePressed)
                        g2.drawImage(callPressedIcon.resolve(), dx, dy, null);
                    else if (isCallMouseOver)
                        g2.drawImage(callRolloverIcon.resolve(), dx, dy, null);
                    else
                        g2.drawImage(callIcon.resolve(), dx, dy, null);

                    g2.drawImage(separatorIcon.resolve(),
                                 dx + callRect.width + ScaleUtils.scaleInt(3),
                                 dy + (callRect.height
                                     - separatorIcon.resolve().getHeight(null))/2,
                                 null);

                    isCallIconVisible = true;
                }
                else
                    isCallIconVisible = false;

                if (phoneNumber != null
                    && phoneNumber.length() > 0
                    && ConfigurationUtils.isSmsEnabled()
                    && AccountUtils.getImProvider() != null)
                {
                    // Paint chat button.
                    Rectangle chatRect = getChatButtonRect();
                    dx = chatRect.x;
                    dy = chatRect.y;

                    if (isChatMousePressed)
                        g2.drawImage(chatPressedIcon.resolve(), dx, dy, null);
                    else if (isChatMouseOver)
                        g2.drawImage(chatRolloverIcon.resolve(), dx, dy, null);
                    else
                        g2.drawImage(chatIcon.resolve(), dx, dy, null);

                    if (!isCallIconVisible)
                    {
                        g2.drawImage(separatorIcon.resolve(),
                                     dx + chatRect.width + ScaleUtils.scaleInt(3),
                                     dy + (chatRect.height
                                         - separatorIcon.resolve().getHeight(null))/2,
                                     null);
                    }

                    isChatIconVisible = true;
                }
                else
                {
                    isChatIconVisible = false;
                }
            }

            drawDeleteIcon(g2, searchField);
        }
        finally
        {
            g2.dispose();
        }
    }

    /**
     * Gets the correct icon for the dial pad based on its current state
     */
    private BufferedImageFuture getDialPadIcon()
    {
        BufferedImageFuture icon;

        if (isDialPadMousePressed)
        {
            icon = dialPadPressedIcon;
        }
        else if (isDialPadMouseOver)
        {
            if (isDialPadVisible)
            {
                icon = dialPadOnRolloverIcon;
            }
            else
            {
                icon = dialPadRolloverIcon;
            }
        }
        else
        {
            if (isDialPadVisible)
            {
                icon = dialPadOnIcon;
            }
            else
            {
                icon = dialPadIcon;
            }
        }

        return icon;
    }

    @Override
    protected Rectangle getDeleteButtonRect()
    {
        // Over-ride the get delete button rectangle so that we can position
        // the delete button as we wish.
        JTextComponent c = getComponent();

        if (c == null)
        {
            intervalLogNullObject("getDeleteButtonRect");
            return null;
        }

        Rectangle rect = c.getBounds();

        int deleteButtonWidth = deleteButton.getWidth();
        int dx =
            rect.width - deleteButtonWidth - BUTTON_GAP - ScaleUtils.scaleInt(5);
        int deleteButtonHeight = deleteButton.getHeight();
        int dy = (rect.height - deleteButtonHeight) / 2;

        return new Rectangle(dx,
                             dy,
                             deleteButtonWidth,
                             deleteButtonHeight);
    }

    /**
     * If we are in the case of disabled delete button, we simply call the
     * parent implementation of this method, otherwise we recalculate the editor
     * rectangle in order to leave place for the delete button.
     * @return the visible editor rectangle
     */
    protected Rectangle getVisibleEditorRect()
    {
        Rectangle rect = super.getVisibleEditorRect();

        // Fixes NullPointerException if the rectangle is null for some reason.
        if (rect == null)
        {
            intervalLogNullObject("getVisibleEditorRect");
            return null;
        }

        if ((rect.width > 0) && (rect.height > 0))
        {
            rect.x += dialPadRolloverIcon.resolve().getWidth(null) +
                      backgroundLeftImage.resolve().getWidth(null) -
                      ScaleUtils.scaleInt(16);
            rect.width -= (dialPadRolloverIcon.resolve().getWidth(null) +
                                      backgroundRightImage.resolve().getWidth(null));

            rect.width += ScaleUtils.scaleInt(6);

            if (isCallIconVisible)
                rect.width -= (callRolloverIcon.resolve().getWidth(null) +
                    ScaleUtils.scaleInt(14));
            else
                rect.width -= ScaleUtils.scaleInt(8);

            if (isChatIconVisible)
                rect.width -= (chatRolloverIcon.resolve().getWidth(null) +
                    ScaleUtils.scaleInt(14));
            else
                rect.width -= ScaleUtils.scaleInt(8);

            return rect;
        }
        return null;
    }

    /**
     * Updates the call or chat button when the mouse was clicked.
     * @param e the <tt>MouseEvent</tt> that notified us of the click
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        super.mouseClicked(e);
        updateButtons(e);
    }

    /**
     * Updates the call or chat button when the mouse is enters the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseEntered(MouseEvent e)
    {
        super.mouseEntered(e);
        updateButtons(e);
    }

    /**
     * Updates the call or chat button when the mouse exits the component area.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseExited(MouseEvent e)
    {
        super.mouseExited(e);
        updateButtons(e);
    }

    @Override
    public void mousePressed(MouseEvent e)
    {
        super.mousePressed(e);
        updateButtons(e);
    }

    @Override
    public void mouseReleased(MouseEvent e)
    {
        super.mouseReleased(e);
        updateButtons(e);
    }

    /**
     * Updates the delete icon when the mouse is dragged over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseDragged(MouseEvent e)
    {
        super.mouseDragged(e);
        updateButtons(e);
    }

    /**
     * Updates the delete icon when the mouse is moved over.
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseMoved(MouseEvent e)
    {
        super.mouseMoved(e);
        updateButtons(e);
    }

    /**
     * Updates the call, chat and dial pad buttons, if they are enabled.
     *
     * @param evt the mouse event that has prompted us to update the buttons.
     */
    private void updateButtons(MouseEvent evt)
    {
        if(buttonsEnabled)
        {
            updateDialPadIcon(evt);
            updateCallIcon(evt);
            updateChatIcon(evt);
        }
    }

    /**
     * Updates the dial pad icon.
     *
     * @param evt the mouse event that has prompted us to update the dial pad
     * icon.
     */
    private void updateDialPadIcon(MouseEvent evt)
    {
        int x = evt.getX();
        int y = evt.getY();
        JTextComponent c = getComponent();

        Rectangle dialPadButtonRect = getDialPadButtonRect();

        if (dialPadButtonRect.contains(x, y))
        {
            String searchText = c.getText();

            if (searchText == null)
                return;

            // Show a tool tip over the dial pad button.
            c.setToolTipText(dialPadString);
            ToolTipManager.sharedInstance().mouseEntered(
                new MouseEvent(c, 0, x, y,
                        x, y, // X-Y of the mouse for the tool tip
                        0, false));

            if (evt.getID() == MouseEvent.MOUSE_PRESSED)
            {
                isDialPadMouseOver = false;
                isDialPadMousePressed = true;
            }
            else
            {
                isDialPadMouseOver = true;
                isDialPadMousePressed = false;
            }

            // Update the default cursor.
            c.setCursor(Cursor.getDefaultCursor());

            // Toggle the dial pad visibility when the dial pad button is clicked.
            if (evt.getID() == MouseEvent.MOUSE_CLICKED)
            {
                isDialPadVisible = !isDialPadVisible;
                logger.user("Dial pad visibility toggled to: " +
                                                              isDialPadVisible);

                SearchFieldContainer container =
                       ((SearchField) c).getSearchFieldContainer();
                container.setDialPadVisibility(isDialPadVisible);
            }
        }
        else
        {
            // Remove the dial pad button tool tip when the mouse exits the
            // dial pad button area.
            c.setToolTipText("");
            ToolTipManager.sharedInstance().mouseExited(
                new MouseEvent(c, 0, x, y,
                        x, y, // X-Y of the mouse for the tool tip
                        0, false));

            isDialPadMouseOver = false;
            isDialPadMousePressed = false;
        }

        c.repaint();
    }

    /**
     * Updates the call icon, changes the cursor and deletes the content of
     * the associated text component when the mouse is pressed over the call
     * icon.
     *
     * @param evt the mouse event that has prompted us to update the call
     * icon.
     */
    private void updateCallIcon(MouseEvent evt)
    {
        int x = evt.getX();
        int y = evt.getY();
        JTextComponent c = getComponent();

        Rectangle callButtonRect = getCallButtonRect();

        if (isCallIconVisible && callButtonRect.contains(x, y))
        {
            String searchText = c.getText();

            if (searchText == null)
                return;

            // Show a tool tip over the call button.
            c.setToolTipText(callString + " " + searchText);
            ToolTipManager.sharedInstance().mouseEntered(
                new MouseEvent(c, 0, x, y,
                        x, y, // X-Y of the mouse for the tool tip
                        0, false));

            if (evt.getID() == MouseEvent.MOUSE_PRESSED)
            {
                isCallMouseOver = false;
                isCallMousePressed = true;
            }
            else
            {
                isCallMouseOver = true;
                isCallMousePressed = false;
            }

            // Update the default cursor.
            c.setCursor(Cursor.getDefaultCursor());

            // Perform call action when the call button is clicked.
            if (evt.getID() == MouseEvent.MOUSE_CLICKED)
            {
                // Get the validated phone number from the field, rather than
                // the full string.
                SearchField searchField = (SearchField)c;
                String phoneNumber = searchField.getPhoneNumber();
                searchField.setText(null);

                // Send analytics event for making the call
                DesktopUtilActivator.getAnalyticsService().onEvent(AnalyticsEventType.OUTBOUND_CALL,
                                                           "Calling from",
                                                           "Search field");

                searchField.getSearchFieldContainer().createCall(phoneNumber, Reformatting.NOT_NEEDED);
            }
        }
        else
        {
            // Remove the call button tool tip when the mouse exits the call
            // button area.
            c.setToolTipText("");
            ToolTipManager.sharedInstance().mouseExited(
                new MouseEvent(c, 0, x, y,
                        x, y, // X-Y of the mouse for the tool tip
                        0, false));

            isCallMouseOver = false;
            isCallMousePressed = false;
        }

        c.repaint();
    }

    /**
     * Updates the chat icon, changes the cursor and deletes the content of
     * the associated text component when the mouse is pressed over the chat
     * icon.
     *
     * @param evt the mouse event that has prompted us to update the chat
     * icon.
     */
    private void updateChatIcon(MouseEvent evt)
    {
        int x = evt.getX();
        int y = evt.getY();
        JTextComponent c = getComponent();

        Rectangle chatButtonRect = getChatButtonRect();

        if (isChatIconVisible && chatButtonRect.contains(x, y))
        {
            String searchText = c.getText();

            if (searchText == null)
                return;

            // Show a tool tip over the chat button.
            c.setToolTipText(chatString + " " + searchText);
            ToolTipManager.sharedInstance().mouseEntered(
                new MouseEvent(c, 0, x, y,
                        x, y, // X-Y of the mouse for the tool tip
                        0, false));

            if (evt.getID() == MouseEvent.MOUSE_PRESSED)
            {
                isChatMouseOver = false;
                isChatMousePressed = true;
            }
            else
            {
                isChatMouseOver = true;
                isChatMousePressed = false;
            }

            // Update the default cursor.
            c.setCursor(Cursor.getDefaultCursor());

            // Perform chat action when the chat button is clicked.
            if (evt.getID() == MouseEvent.MOUSE_CLICKED)
            {
                // Get the validated phone number from the field, rather than
                // the full string.
                SearchField searchField = (SearchField)c;
                String smsNumber = searchField.getSmsNumber();
                String formattedSmsNumber = DesktopUtilActivator.
                     getPhoneNumberUtils().formatNumberToLocalOrE164(smsNumber);
                searchField.setText(null);

                // Send analytics event for making the chat
                DesktopUtilActivator.getAnalyticsService().onEvent(AnalyticsEventType.USER_CHAT_FROM_SEARCHBAR);

                DesktopUtilActivator.getUIService().startChat(new String[]{formattedSmsNumber});
            }
        }
        else
        {
            // Remove the chat button tool tip when the mouse exits the chat
            // button area.
            c.setToolTipText("");
            ToolTipManager.sharedInstance().mouseExited(
                new MouseEvent(c, 0, x, y,
                        x, y, // X-Y of the mouse for the tool tip
                        0, false));

            isChatMouseOver = false;
            isChatMousePressed = false;
        }

        c.repaint();
    }

    /**
     * Calculates the dial pad button rectangle.
     *
     * @return the dial pad button rectangle
     */
    protected Rectangle getDialPadButtonRect()
    {
        SearchField searchField = (SearchField)this.getComponent();

        int dx = ScaleUtils.scaleInt(9);
        int dy = (searchField.getY() + searchField.getHeight()) / 2
                                        - dialPadRolloverIcon.resolve().getHeight(null)/2;

        return new Rectangle(   dx,
                                dy,
                                dialPadRolloverIcon.resolve().getWidth(null),
                                dialPadRolloverIcon.resolve().getHeight(null));
    }

    /**
     * Calculates the call button rectangle.
     *
     * @return the call button rectangle
     */
    protected Rectangle getCallButtonRect()
    {
        Component c = getComponent();
        Rectangle rect = c.getBounds();

        int dx = getDeleteButtonRect().x -
            callRolloverIcon.resolve().getWidth(null) - ScaleUtils.scaleInt(8);
        int dy = (rect.y + rect.height - callRolloverIcon.resolve().getHeight(null)) / 2;

        return new Rectangle(   dx,
                                dy,
                                callRolloverIcon.resolve().getWidth(null),
                                callRolloverIcon.resolve().getHeight(null));
    }

    /**
     * Calculates the chat button rectangle.
     *
     * @return the chat button rectangle
     */
    protected Rectangle getChatButtonRect()
    {
        Component c = getComponent();
        Rectangle rect = c.getBounds();

        int callButtonWidth =
                        isCallIconVisible ? callRolloverIcon.resolve().getWidth(null) : 0;

        int dx = getDeleteButtonRect().x - callButtonWidth -
                 chatRolloverIcon.resolve().getWidth(null) - ScaleUtils.scaleInt(8);
        int dy = (rect.y + rect.height - chatRolloverIcon.resolve().getHeight(null)) / 2;

        return new Rectangle(   dx,
                                dy,
                                chatRolloverIcon.resolve().getWidth(null),
                                chatRolloverIcon.resolve().getHeight(null));
    }

    /**
     * Reloads UI icons.
     */
    public void loadSkin()
    {
        super.loadSkin();

        if (buttonsEnabled)
        {
            dialPadIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.CONTACT_LIST_DIAL_BUTTON");

            dialPadRolloverIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.CONTACT_LIST_DIAL_BUTTON_ROLLOVER");

            dialPadPressedIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.CONTACT_LIST_DIAL_BUTTON_PRESSED");

            dialPadOnIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.CONTACT_LIST_DIAL_BUTTON_ON");

            dialPadOnRolloverIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.CONTACT_LIST_DIAL_BUTTON_ON_ROLLOVER");

            callIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.SEARCH_CALL_ICON");

            callRolloverIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.SEARCH_CALL_ROLLOVER_ICON");

            callPressedIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.SEARCH_CALL_PRESSED_ICON");

            separatorIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.icons.SEARCH_SEPARATOR");

            chatIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.SEARCH_CHAT_ICON");

            chatRolloverIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.SEARCH_CHAT_ROLLOVER_ICON");

            chatPressedIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.buttons.SEARCH_CHAT_PRESSED_ICON");

            separatorIcon = UtilActivator.getResources()
                .getBufferedImage("service.gui.icons.SEARCH_SEPARATOR");
        }

        backgroundLeftImage = UtilActivator.getResources()
            .getBufferedImage("service.gui.search.INACTIVE_LEFT");
        backgroundRightImage = UtilActivator.getResources()
            .getBufferedImage("service.gui.search.INACTIVE_RIGHT");
        backgroundMiddleImage = UtilActivator.getResources()
            .getBufferedImage("service.gui.search.INACTIVE_MIDDLE");
        backgroundLeftImageActive = UtilActivator.getResources()
            .getBufferedImage("service.gui.search.ACTIVE_LEFT");
        backgroundRightImageActive = UtilActivator.getResources()
            .getBufferedImage("service.gui.search.ACTIVE_RIGHT");
        backgroundMiddleImageActive = UtilActivator.getResources()
            .getBufferedImage("service.gui.search.ACTIVE_MIDDLE");
    }

    /**
     * Creates a UI for a SearchFieldUI.
     *
     * @param c the text field
     * @return the UI
     */
    public static ComponentUI createUI(JComponent c)
    {
        return new SearchFieldUI();
    }
}
