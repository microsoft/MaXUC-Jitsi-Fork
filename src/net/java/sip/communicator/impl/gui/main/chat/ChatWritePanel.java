/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import static java.lang.Character.toUpperCase;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.undo.*;

import org.jitsi.service.resources.*;
import org.jitsi.util.*;

import com.google.common.annotations.VisibleForTesting;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.chat.conference.*;
import net.java.sip.communicator.impl.gui.main.chat.menus.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.OperationSetTypingNotifications.TypingState;
import net.java.sip.communicator.util.AccessibilityUtils;
import net.java.sip.communicator.util.ConfigurationUtils;
import net.java.sip.communicator.util.Logger;
import net.java.sip.communicator.util.account.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The <tt>ChatWritePanel</tt> is the panel, where user writes her messages.
 * It is located at the bottom of the split in the <tt>ChatPanel</tt> and it
 * contains an editor, where user writes the text.
 *
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 * @author Adam Netocny
 */
public class ChatWritePanel
    extends TransparentPanel
    implements  ActionListener,
                KeyListener,
                MouseListener,
                UndoableEditListener,
                DocumentListener,
                Skinnable
{
    private static final long serialVersionUID = 0L;

    /**
     * The <tt>Logger</tt> used by the <tt>ChatWritePanel</tt> class and its
     * instances for logging output.
     */
    private static final Logger sLog = Logger.getLogger(ChatWritePanel.class);

    private static final String SEND_MESSAGE_COMMAND = "service.gui.SEND_MESSAGE_COMMAND";

    /**
     * The <tt>PropertyChangeListener</tt> which listens for updates to the send
     * message command (i.e. send with Enter or Ctrl-Enter) and updates the send
     * command and the corresponding tooltip in the <tt>ChatWritePanel</tt>.
     */
    private final PropertyChangeListener mSendCommandListener
        = new PropertyChangeListener()
    {
        @Override
        public void propertyChange(PropertyChangeEvent e)
        {
            sLog.debug("ChatWritePanel send command changed");

            setSendCommand();
            mEditorPane.setToolTipText(resources.getI18NString(mToolTipResource));
        }
    };

    private final HintPane mEditorPane = new HintPane();

    // Flag for ignoring undoable edits.  Used for when we're manipulating the user's entered text
    // and we don't want the we make changes to count as undoable.
    private boolean ignoreUndoableEdits = false;

    private final UndoManager mUndo = new UndoManager();

    private final ChatPanel mChatPanel;

    private final int TIME_IN_MS_UNTIL_PAUSED_TYPING = 5000;
    @VisibleForTesting final Timer mPausedTypingTimer = new Timer(TIME_IN_MS_UNTIL_PAUSED_TYPING, this);

    private TypingState mTypingState = TypingState.NOT_TYPING;

    private final WritePanelRightButtonMenu mRightButtonMenu;

    private final SIPCommScrollPane mScrollPane = new SIPCommScrollPane();

    private ChatTransportSelectorBox mTransportSelectorBox;

    private final Container mCenterPanel;

    private final AttributeSet mDefaultAttributes;

    private IconSelectorBox mIconSelectorBox;

    private JLabel mSmsLabel;

    private JCheckBoxMenuItem mSmsMenuItem;

    private JLabel mSmsCharCountLabel;

    private JLabel mSmsNumberLabel;

    private int mSmsNumberCount = 1;

    private int mSmsCharCount = 160;

    private boolean mSmsMode = false;

    private int mEmojiCount = 0;

    ResourceManagementService resources = GuiActivator.getResources();

    /**
     * The resource string used for the chat tooltip
     */
    private String mToolTipResource;

    private AccessibleChatBuffer mAccessibleChatBuffer;

    /**
     * Creates an instance of <tt>ChatWritePanel</tt>.
     *
     * @param chatPanel The parent <tt>ChatPanel</tt>.
     */
    public ChatWritePanel(ChatPanel chatPanel)
    {
        super(new BorderLayout());

        GuiActivator.getConfigurationService().user().addPropertyChangeListener(
            SEND_MESSAGE_COMMAND, mSendCommandListener);

        setSendCommand();
        mChatPanel = chatPanel;
        mCenterPanel = createCenter();
        add(mCenterPanel, BorderLayout.CENTER);
        mRightButtonMenu = new WritePanelRightButtonMenu(mChatPanel.getChatContainer());

        AccessibilityUtils.setDescription(this, resources.
                getI18NString("service.gui.chat.CHAT_WRITE_PANEL_DESCRIPTION"));

        if (ConfigurationUtils.isAccessibilityMode())
        {
            // DefaultEditorKit used in Accessibility Mode does not have attributes
            mDefaultAttributes = null;
        }
        else
        {
            // Save default attributes so we can reset them as necessary to ensure
            // emojis and text are displayed correctly
            StyledEditorKit editorKit = (StyledEditorKit) mEditorPane.getEditorKit();
            mDefaultAttributes = editorKit.getInputAttributes().copyAttributes();
        }

        addPasteKeyBinding();
    }

    /**
     * Sets the tooltip for the send/new line command and initialises the key
     * strokes for each command.
     */
    private void setSendCommand()
    {
        ActionMap actionMap = mEditorPane.getActionMap();
        actionMap.put("send", new SendMessageAction());
        actionMap.put("newLine", new NewLineAction());

        InputMap im = mEditorPane.getInputMap();

        String messageCommand = ConfigurationUtils.getSendMessageCommand();

        if ("enter".equalsIgnoreCase(messageCommand))
        {
            // The send message command is enter, so set the tooltip
            // accordingly
            mToolTipResource = "service.gui.ENTER_MESSAGE_TEXT";

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "send");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.CTRL_DOWN_MASK), "newLine");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.SHIFT_DOWN_MASK), "newLine");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.ALT_DOWN_MASK), "newLine");
        }
        else
        {
            mToolTipResource = "service.gui.ALT_ENTER_MESSAGE_TEXT";

            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.CTRL_DOWN_MASK), "send");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "newLine");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.SHIFT_DOWN_MASK), "newLine");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,
                KeyEvent.ALT_DOWN_MASK), "newLine");
        }
    }

    /**
     * Ensures that using the CTRL+V paste shortcut will also result in the
     * pasted text being refreshed to display emojis correctly
     */
    private void addPasteKeyBinding()
    {
        ActionMap actionMap = mEditorPane.getActionMap();
        ChatWindow window = (ChatWindow) mChatPanel.getConversationContainerWindow();

        actionMap.put("paste-shortcut", window.new PasteAction());

        InputMap im = mEditorPane.getInputMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK),
               "paste-shortcut");
    }

    /**
     * Creates the center panel.
     *
     * @return the created center panel
     */
    private Container createCenter()
    {
        JPanel centerPanel = new JPanel(new GridBagLayout());

        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 3));

        GridBagConstraints constraints = new GridBagConstraints();

        initSmsLabel(centerPanel);
        if (!ConfigurationUtils.isAccessibilityMode())
        {
            initIconSelectorBox(centerPanel);
        }
        initTextArea(centerPanel);

        mSmsCharCountLabel = new JLabel(String.valueOf(mSmsCharCount));
        mSmsCharCountLabel.setForeground(Color.GRAY);
        mSmsCharCountLabel.setVisible(false);
        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 2, 0, 2);
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        centerPanel.add(mSmsCharCountLabel, constraints);

        mSmsNumberLabel = new JLabel(String.valueOf(mSmsNumberCount))
        {
            private static final long serialVersionUID = 0L;

            @Override
            public void paintComponent(Graphics g)
            {
                AntialiasingManager.activateAntialiasing(g);
                g.setColor(getBackground());
                g.fillOval(0, 0, getWidth(), getHeight());

                super.paintComponent(g);
            }
        };
        mSmsNumberLabel.setHorizontalAlignment(JLabel.CENTER);
        mSmsNumberLabel.setPreferredSize(new Dimension(18, 18));
        mSmsNumberLabel.setMinimumSize(new Dimension(18, 18));
        mSmsNumberLabel.setForeground(Color.WHITE);
        mSmsNumberLabel.setBackground(Color.GRAY);
        mSmsNumberLabel.setVisible(false);
        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 4;
        constraints.gridy = 0;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 2, 0, 2);
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        centerPanel.add(mSmsNumberLabel, constraints);

        return centerPanel;
    }

    /**
     * Creates the icon selector box
     *
     * @return the created icon selector box
     */
    private void initIconSelectorBox(final JPanel centerPanel)
    {
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.NORTHEAST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 3;
        constraints.gridy = 0;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 2, 0, 2);
        constraints.gridheight = 1;
        constraints.gridwidth = 1;

        mIconSelectorBox = new IconSelectorBox();

        mIconSelectorBox.setName("smiley");
        mIconSelectorBox.setToolTipText(resources
            .getI18NString("service.gui.INSERT_SMILEY"));

        SIPCommMenuBar smileyMenuBar = new SIPCommMenuBar();
        smileyMenuBar.setOpaque(false);
        smileyMenuBar.setLayout(new GridLayout(1, 1));
        smileyMenuBar.add(mIconSelectorBox);
        centerPanel.add(smileyMenuBar, constraints, 0);
    }

    /**
     * Initializes the sms menu.
     *
     * @param centerPanel the parent panel
     */
    private void initSmsLabel(final JPanel centerPanel)
    {
        GridBagConstraints constraints = new GridBagConstraints();

        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.gridheight = 1;
        constraints.weightx = 0f;
        constraints.weighty = 0f;
        constraints.insets = new Insets(0, 3, 0, 0);

        final ImageIconFuture smsIcon = resources
        .getImage("service.gui.icons.SEND_SMS");

        final ImageIconFuture selectedIcon = resources
            .getImage("service.gui.icons.SEND_SMS_SELECTED");

        mSmsLabel = smsIcon.addToLabel(new JLabel());

        // We hide the sms label until we know if the chat supports sms.
        mSmsLabel.setVisible(false);

        mSmsMenuItem = new JCheckBoxMenuItem(resources
            .getI18NString("service.gui.VIA_SMS"));

        mSmsMenuItem.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                mSmsMode = mSmsMenuItem.isSelected();

                Color bgColor;
                if (mSmsMode)
                {
                    selectedIcon.addToLabel(mSmsLabel);
                    bgColor = new Color(resources
                        .getColor("service.gui.LIGHT_HIGHLIGHT"));
                }
                else
                {
                    smsIcon.addToLabel(mSmsLabel);
                    bgColor = Color.WHITE;
                }

                centerPanel.setBackground(bgColor);
                mEditorPane.setBackground(bgColor);

                mSmsLabel.repaint();
            }
        });

        mSmsLabel.addMouseListener(new MouseAdapter()
        {
            public void mousePressed(MouseEvent mouseevent)
            {
                Point location = new Point(mSmsLabel.getX(),
                    mSmsLabel.getY() + mSmsLabel.getHeight());

                SwingUtilities.convertPointToScreen(location, mSmsLabel);

                JPopupMenu smsPopupMenu = new JPopupMenu();
                smsPopupMenu.setFocusable(true);
                smsPopupMenu.setInvoker(ChatWritePanel.this);
                smsPopupMenu.add(mSmsMenuItem);
                smsPopupMenu.setLocation(location.x, location.y);
                smsPopupMenu.setVisible(true);
            }
        });

        centerPanel.add(mSmsLabel, constraints);
    }

    private void initTextArea(JPanel centerPanel)
    {
        GridBagConstraints constraints = new GridBagConstraints();
        mEditorPane.setForeground(Color.BLACK);

        if (!ConfigurationUtils.isAccessibilityMode())
        {
            mEditorPane.setContentType("text/html");
            mEditorPane.setEditorKit(new SIPCommHTMLEditorKit(this)
            {
                private static final long serialVersionUID = 0L;

                @Override
                public Cursor getDefaultCursor()
                {
                    // Make sure we use the text cursor
                    return Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
                }
            });
        }
        else
        {
            mEditorPane.setContentType("text/plain");
        }

        mEditorPane.putClientProperty(
            JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        mEditorPane.setCaretPosition(0);

        mEditorPane.getDocument().addUndoableEditListener(this);
        mEditorPane.getDocument().addDocumentListener(this);
        mEditorPane.addKeyListener(this);
        mEditorPane.addMouseListener(this);
        mEditorPane.setTransferHandler(new ChatTransferHandler(mChatPanel));
        mEditorPane.setToolTipText(resources.getI18NString(mToolTipResource));
        ScaleUtils.scaleFontAsDefault(mEditorPane);

        mScrollPane.setHorizontalScrollBarPolicy(
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        mScrollPane.setVerticalScrollBarPolicy(
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        mScrollPane.setOpaque(false);
        mScrollPane.getViewport().setOpaque(false);
        mScrollPane.setBorder(null);

        mScrollPane.setViewportView(mEditorPane);

        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 1f;
        constraints.weighty = 1f;
        constraints.gridheight = 1;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0, 0, 0, 0);
        centerPanel.add(mScrollPane, constraints);
    }

    /**
     * Runs clean-up for associated resources which need explicit disposal (e.g.
     * listeners keeping this instance alive because they were added to the
     * model which operationally outlives this instance).
     */
    public void dispose()
    {
        /*
         * Stop the Timers because they're implicitly globally referenced and
         * thus don't let them retain this instance.
         */
        mPausedTypingTimer.stop();
        mPausedTypingTimer.removeActionListener(this);
        updateTypingState(TypingState.NOT_TYPING);
        mScrollPane.dispose();
        GuiActivator.getConfigurationService().user().removePropertyChangeListener(
            SEND_MESSAGE_COMMAND, mSendCommandListener);
    }

    /**
     * Get the icon selector box.
     *
     * @return the icon selector box
     */
    public IconSelectorBox getIconSelectorBox()
    {
        return mIconSelectorBox;
    }

    /**
     * Returns the editor panel, contained in this <tt>ChatWritePanel</tt>.
     *
     * @return The editor panel, contained in this <tt>ChatWritePanel</tt>.
     */
    public JEditorPane getEditorPane()
    {
        return mEditorPane;
    }

    /**
     * Enables/disables the sms mode.
     *
     * @param selected <tt>true</tt> to enable sms mode, <tt>false</tt> -
     * otherwise
     */
    public void setSmsSelected(boolean selected)
    {
        mSmsMenuItem.setSelected(selected);
    }

    /**
     * Returns <tt>true</tt> if the sms mode is enabled, otherwise returns
     * <tt>false</tt>.
     * @return <tt>true</tt> if the sms mode is enabled, otherwise returns
     * <tt>false</tt>
     */
    public boolean isSmsSelected()
    {
        return mSmsMode;
    }

    /**
     * The <tt>SendMessageAction</tt> is an <tt>AbstractAction</tt> that
     * sends the text that is currently in the write message area.
     */
    private class SendMessageAction
        extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            // mChatPanel.stopTypingNotifications();
            sLog.user("Chat message sent");
            mChatPanel.sendButtonDoClick();
        }
    }

    /**
     * The <tt>NewLineAction</tt> is an <tt>AbstractAction</tt> that types
     * an enter in the write message area.
     */
    private class NewLineAction
        extends AbstractAction
    {
        private static final long serialVersionUID = 0L;

        public void actionPerformed(ActionEvent e)
        {
            if (ConfigurationUtils.isAccessibilityMode())
            {
                // Cannot add a new line when in accessibility mode
                return;
            }

            int caretPosition = mEditorPane.getCaretPosition();
            HTMLDocument doc = (HTMLDocument) mEditorPane.getDocument();

            try
            {
                doc.insertString(caretPosition, "\n", null);
            }
            catch (BadLocationException e1)
            {
                sLog.error("Could not insert <br> to the document.", e1);
            }

            mEditorPane.setCaretPosition(caretPosition + 1);
        }
    }

    /**
     * Handles the <tt>UndoableEditEvent</tt>, by adding the content edit to
     * the <tt>UndoManager</tt>.
     *
     * @param e The <tt>UndoableEditEvent</tt>.
     */
    public void undoableEditHappened(UndoableEditEvent e)
    {
        if (!ignoreUndoableEdits)
        {
            mUndo.addEdit(e.getEdit());
        }
    }

    /**
     * Implements the undo operation.
     */
    private void undo()
    {
        try
        {
            mUndo.undo();
        }
        catch (CannotUndoException e)
        {
            sLog.error("Unable to undo.", e);
        }
    }

    /**
     * Implements the redo operation.
     */
    private void redo()
    {
        try
        {
            mUndo.redo();
        }
        catch (CannotRedoException e)
        {
            sLog.error("Unable to redo.", e);
        }
    }

    /**
     * Sends typing notifications when user types.
     *
     * @param e the event.
     */
    public void keyTyped(KeyEvent e)
    {
        if (!StringUtils.isNullOrEmpty(getText()))
        {
            updateTypingState(TypingState.TYPING);
        }
        else
        {
            // If there's no text, the user isn't typing.
            updateTypingState(TypingState.NOT_TYPING);
        }
    }

    /**
     * When CTRL+Z is pressed invokes the <code>ChatWritePanel.undo()</code>
     * method, when CTRL+R is pressed invokes the
     * <code>ChatWritePanel.redo()</code> method.
     *
     * @param e the <tt>KeyEvent</tt> that notified us
     */
    public void keyPressed(KeyEvent e)
    {
        final int KEY_FOR_PREVIOUS = toUpperCase(resources.getI18NString("service.gui.chat.CHAT_WRITE_PANEL_PREV_KEY").charAt(0));
        final int KEY_FOR_NEXT = toUpperCase(resources.getI18NString("service.gui.chat.CHAT_WRITE_PANEL_NEXT_KEY").charAt(0));

        if (e.isControlDown() && (e.getKeyCode() == KeyEvent.VK_Z))
        {
            if (mUndo.canUndo())
                undo();
        }
        else if (e.isControlDown() && (e.getKeyCode() == KeyEvent.VK_R))
        {
            if (mUndo.canRedo())
                redo();
        }
        else if (e.getKeyCode() == KeyEvent.VK_UP)
        {
            // Only enters editing mode if the write panel is empty in
            // order not to lose the current message contents, if any.
            if (mChatPanel.getLastSentMessageUID() != null
                    && mChatPanel.isWriteAreaEmpty())
            {
                mChatPanel.startLastMessageCorrection();
                e.consume();
            }
        }
        else if (e.getKeyCode() == KeyEvent.VK_DOWN)
        {
            if (mChatPanel.isMessageCorrectionActive())
            {
                Document doc = mEditorPane.getDocument();
                if (mEditorPane.getCaretPosition() == doc.getLength())
                {
                    mChatPanel.stopMessageCorrection();
                }
            }
        }
        else if (e.isControlDown() && (e.getKeyCode() == KEY_FOR_PREVIOUS))
        {
            if (mAccessibleChatBuffer == null)
            {
                mAccessibleChatBuffer = mChatPanel.getAccessibleChatBuffer();
            }

            if (mAccessibleChatBuffer != null)
            {
                mAccessibleChatBuffer.getPreviousMessage();
            }
        }
        else if (e.isControlDown() && (e.getKeyCode() == KEY_FOR_NEXT))
        {
            if (mAccessibleChatBuffer == null)
            {
                mAccessibleChatBuffer = mChatPanel.getAccessibleChatBuffer();
            }

            if (mAccessibleChatBuffer != null)
            {
                mAccessibleChatBuffer.getNextMessage();
            }
        }
        else
        {
            refreshInputAttributes(); //Refresh the input attributes whenever text is typed
        }
    }

    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT)
        {
            moveCaretPastEmoji(true);
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT)
        {
            moveCaretPastEmoji(false);
        }
    }

    /**
     * This method deals with the fact that the underlying text representation
     * of emojis is at least 2 chars wide. It stops the caret being placed in
     * the centre of an emoji by checking if the current character has icon
     * attributes (i.e. is an emoji character) and if the previous character is
     * part of a different element (text or another emoji). If the caret is in
     * the middle of an emoji, it is moved accordingly.
     *
     * @param movingLeft: true if caret has just moved left, false if right
     */
    private synchronized void moveCaretPastEmoji(boolean movingLeft)
    {
        if (ConfigurationUtils.isAccessibilityMode())
        {
            // Sending Emojis is not supported in Accessibility Mode
            return;
        }

        HTMLDocument doc = (HTMLDocument) mEditorPane.getDocument();
        Element currentCaretElement =
            doc.getCharacterElement(mEditorPane.getCaretPosition());
        AttributeSet currentAttributes = currentCaretElement.getAttributes();

        if (isIcon(currentAttributes))
        {
            Element previousCaretElement =
                doc.getCharacterElement(mEditorPane.getCaretPosition()-1);
            AttributeSet previousAttributes = previousCaretElement.getAttributes();

            if (!isAcrossElementBoundary(currentAttributes,previousAttributes))
            {
                //Caret is in the middle of an emoji
                if (movingLeft) //Caret has just moved left, so need to skip to start of this emoji
                {
                    mEditorPane.setCaretPosition(currentCaretElement.getStartOffset());
                }
                else //Caret has just moved right, so need to skip to end of this emoji
                {
                    mEditorPane.setCaretPosition(currentCaretElement.getEndOffset());
                }
            }
        }
    }

    private boolean isIcon(AttributeSet attrs)
    {
        return attrs.containsAttribute("$ename", "icon");
    }

    private boolean isAcrossElementBoundary(AttributeSet currentAttrs, AttributeSet prevAttrs)
    {
        return !(currentAttrs.equals(prevAttrs));
    }

    /**
     * Resets the Editor Kit's input attributes to the default to ensure text
     * is input correctly (for a StyledEditorKit)
     */
    public synchronized void refreshInputAttributes() {
        if (ConfigurationUtils.isAccessibilityMode())
        {
            // Attributes not supported in Accessibility Mode
            return;
        }

        StyledEditorKit editorKit = (StyledEditorKit) mEditorPane.getEditorKit();
        AttributeSet currentAttributes = editorKit.getInputAttributes();
        editorKit.getInputAttributes().removeAttributes(currentAttributes);
        editorKit.getInputAttributes().addAttributes(mDefaultAttributes);
    }

    /**
     * Refreshes the text currently in mEditorPane to ensure emoji display correctly
     */
    public void refreshCurrentText() throws BadLocationException
    {
        String plainText = this.getText();
        refreshText(plainText, true);
    }

    /**
     * Helper function to determine if the list of displayable message parts is just a single
     * block of text
     */
    private boolean isSingleTextPart(List<DisplayablePartOfMessage> messageParts)
    {
        return ((messageParts.size() == 1) && (TextPartOfMessage.class.isInstance(messageParts.get(0))));
    }

    /**
     * Runs over a plaintext String to set all emoji attributes correctly in
     * the editor, ensuring they are displayed as images. Used when emoji are
     * inserted without using the iconSelectorBox (e.g. editing a previous
     * message, copy & paste)
     *
     * @param currentText: String of plaintext to refresh
     * @param saveCaretPosition: true if we wish to return the caret to its
     * original position after the refresh, false if it can be moved to the end
     * of the text
     * @throws BadLocationException
     */
    public void refreshText(String currentText, boolean saveCaretPosition) throws BadLocationException
    {
        if (ConfigurationUtils.isAccessibilityMode())
        {
            // Sending Emojis is not supported in Accessibility Mode
            return;
        }

        int caretPosition = 0;
        if (saveCaretPosition)
        {
            caretPosition = mEditorPane.getCaretPosition();
        }

        HTMLDocument doc = (HTMLDocument) mEditorPane.getDocument();
        //Split the plaintext String into text and image portions
        List<DisplayablePartOfMessage> replacedText = mIconSelectorBox.replaceText(new TextPartOfMessage(currentText));

        // refreshText involves removing the existing text, and reinserting texts and pictures once
        // any emoji unicode characters are converted to the corresponding pictures.  We don't want
        // 'remove everything' and 'reinsert refreshed text' actions to be recorded by the
        // UndoManager (else a user who pastes text and then presses undo will see all text removed
        // etc.) so we flag for the UndoManager to ignore these actions.
        //
        // However, we can't do that if refreshing the text actually changes something, else the
        // UndoManager will have an inaccurate view of what text there is and if the user presses
        // undo there could be asserts thrown from the UndoManager.  Currently the only way
        // refreshText can change the text is via insertion of pictures, so we check for any
        // pictures and if there are any we have to continue recording UndoableEdits and live with
        // funny undo behaviour.  There's also a problem when the UndoManager doesn't see the
        // refresh of any text that contains the newline character.  So we restrict this fix to
        // single blocks of text with no pictures or newlines.
        if (isSingleTextPart(replacedText) &&
            (!StringUtils.containsNewLine(((TextPartOfMessage) replacedText.get(0)).getText())))
        {
            sLog.debug("No new line or picture in refreshed text - ignore undoable edits");
            ignoreUndoableEdits = true;
        }

        Element currentElement =
            doc.getCharacterElement(mEditorPane.getCaretPosition());

        SimpleAttributeSet currentAttributes = new SimpleAttributeSet(currentElement.getAttributes());
        SimpleAttributeSet picAttributes = new SimpleAttributeSet(currentAttributes);

        //Clear the editor
        mEditorPane.getDocument().remove(0, mEditorPane.getDocument().getLength());

        for (DisplayablePartOfMessage messagePart : replacedText)
        {
            if (TextPartOfMessage.class.isInstance(messagePart))
            {
                //Text should be reinserted normally into the editor
                doc.insertString(mEditorPane.getCaretPosition(),((TextPartOfMessage) messagePart).getText(), currentAttributes);
            }
            else
            {
                //Pictures may not have the correct attributes set
                PicturePartOfMessage ppm = (PicturePartOfMessage) messagePart;
                ImageIconFuture imageFuture = resources.getImageFromPath(ppm.getImagePath());

                //Add the attributes relating to the emoji's image file to picAttributes
                // mEmojiCount should not be accessed between assigning it to an
                // emoji and incrementing it
                synchronized(this)
                {
                    picAttributes.addAttribute("iconID", mEmojiCount); //makes emoji elements unique
                    mEmojiCount++;
                }

                StyleConstants.setIcon(picAttributes, imageFuture.resolve());

                //Insert emoji codepoint into editor with attributes that will
                // render it as the correct image
                doc.insertString(mEditorPane.getCaretPosition(), ppm.getAlt(), picAttributes);
            }
        }

        if (saveCaretPosition)
        {
            //Return caret to original position
            mEditorPane.setCaretPosition(caretPosition);
            //Return caret to the position it held before this method was called
        }

        // Finished refreshing text, make sure we stop ignoring UndoableEdits.
        ignoreUndoableEdits = false;
    }

    /**
     * Performs actions when typing timer has expired.
     *
     * @param e the <tt>ActionEvent</tt> that notified us
     */
    public void actionPerformed(ActionEvent e)
    {
        // Before sending the typing notification, check whether the last
        // message we received was from a different chat transport than the
        // one the chat session is currently set to use and, if so, reset to
        // sending notifications to all registered resources for this session.
        mChatPanel.checkChatTransport();

        if (mPausedTypingTimer.equals(e.getSource()))
        {
            updateTypingState(TypingState.PAUSED);
        }
    }

    /**
     * Updates our current typing chat state, and sends a typing notification if it's new.
     */
    public void updateTypingState(TypingState typingState)
    {
        if (typingState == TypingState.TYPING)
        {
            // Note that if the timer hasn't been started yet, restart() is equivalent to start().
            mPausedTypingTimer.restart();
        }
        else
        {
            mPausedTypingTimer.stop();
        }

        if (typingState != mTypingState)
        {
            sLog.info("Changing typing state from " + mTypingState + " to " + typingState);
            mTypingState = typingState;

            // Don't send a chat state update if we're configured not to, unless it's to move into the
            // NOT_TYPING state - in case the user has disabled this config whilst in the TYPING/PAUSED
            // state, which we don't want to be stuck in.
            if ((ConfigurationUtils.isSendTypingNotifications() || mTypingState == TypingState.NOT_TYPING) &&
                (mChatPanel.getChatSession() != null) &&
                (mChatPanel.getChatSession().getCurrentChatTransport() != null))
            {
                mChatPanel.getChatSession().getCurrentChatTransport().sendTypingNotification(mTypingState);
            }
        }
    }

    /**
     * Opens the <tt>WritePanelRightButtonMenu</tt> when user clicks with the
     * right mouse button on the editor area.
     *
     * @param e the <tt>MouseEvent</tt> that notified us
     */
    @Override
    public void mouseClicked(MouseEvent e)
    {
        if ((e.getModifiers() & InputEvent.BUTTON3_MASK) != 0
            || (e.isControlDown() && !e.isMetaDown()))
        {
            Point p = e.getPoint();
            SwingUtilities.convertPointToScreen(p, e.getComponent());

            //SPELLCHECK
            List <JMenuItem> contributedMenuEntries = new ArrayList<>();

            for (JMenuItem item : contributedMenuEntries)
            {
                mRightButtonMenu.add(item);
            }

            JPopupMenu rightMenu
                = mRightButtonMenu.makeMenu(contributedMenuEntries);
            rightMenu.setInvoker(mEditorPane);
            rightMenu.setLocation(p.x, p.y);
            rightMenu.setVisible(true);
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    /**
     * Returns the <tt>WritePanelRightButtonMenu</tt> opened in this panel.
     * Used by the <tt>ChatWindow</tt>, when the ESC button is pressed, to
     * check if there is an open menu, which should be closed.
     *
     * @return the <tt>WritePanelRightButtonMenu</tt> opened in this panel
     */
    public WritePanelRightButtonMenu getRightButtonMenu()
    {
        return mRightButtonMenu;
    }

    /**
     * Returns the write area text as an html text.
     *
     * @return the write area text as an html text.
     */
    public String getTextAsHtml()
    {
        String msgText = mEditorPane.getText();
        String formattedString = msgText.replaceAll(
            "<html>|<head>|<body>|</html>|</head>|</body>", "");

        formattedString = extractFormattedText(formattedString);
        // Returned string is formatted with newlines, etc. so we need to get
        // rid of them before checking for the ending <br/>.
        formattedString = formattedString.trim();

        if (formattedString.endsWith("<BR/>"))
            formattedString = formattedString
            .substring(0, formattedString.lastIndexOf("<BR/>"));

        // Finally, get rid of any doubly defined newline characters
        formattedString = formattedString.replaceAll("<br>\n", "\n");

        return formattedString;
    }

    /**
     * Returns the write area text as a plain text without any formatting.
     *
     * @return the write area text as a plain text without any formatting.
     */
    public String getText()
    {
        try
        {
            Document doc = mEditorPane.getDocument();
            return doc.getText(0, doc.getLength());
        }
        catch (BadLocationException e)
        {
            sLog.error("Could not obtain write area text.", e);
        }

        return null;
    }

    /**
     * Clears write message area.
     */
    public void clearWriteArea()
    {
        try
        {
            Document doc = mEditorPane.getDocument();
            if (doc != null)
            {
                doc.remove(0, doc.getLength());
            }
        }
        catch (BadLocationException e)
        {
            sLog.error("Failed to obtain write panel document content.", e);
        }
    }

    /**
     * Appends the given text into the contained HTML document, but to be
     * to be displayed as the supplied imageIcon.  This method is used to
     * insert pictures selects from the menu.
     * @param text the text to append.
     * @param imageIcon the icon the text will be replaced with
     */
    public synchronized void appendTextAsImage(String text, ImageIcon imageIcon)
    {
        if (ConfigurationUtils.isAccessibilityMode())
        {
            // Cannot add pictures when in accessibility mode
            return;
        }

        HTMLDocument doc = (HTMLDocument) mEditorPane.getDocument();

        Element currentElement =
            doc.getCharacterElement(mEditorPane.getCaretPosition());

        try
        {
            // Get a copy of the current attributes, and a new set with the image added
            SimpleAttributeSet currentAttributes = new SimpleAttributeSet(currentElement.getAttributes());
            SimpleAttributeSet picAttributes = new SimpleAttributeSet(currentAttributes);
            StyledEditorKit editorKit = (StyledEditorKit) mEditorPane.getEditorKit();

            picAttributes.addAttribute("iconID", mEmojiCount); //makes emoji elements unique
            mEmojiCount++;

            StyleConstants.setIcon(picAttributes, imageIcon);

            doc.insertString(mEditorPane.getCaretPosition(), text, picAttributes);

            // Resets the attributes to the way they were before the image
            // icon was added, so that the user can keep typing
            editorKit.getInputAttributes().removeAttributes(picAttributes);
            editorKit.getInputAttributes().addAttributes(currentAttributes);
        }
        catch (BadLocationException e)
        {
            sLog.error("Insert in the HTMLDocument failed.", e);
        }
    }

    /**
     * Return all html paragraph content separated by <BR/> tags.
     *
     * @param msgText the html text.
     * @return the string containing only paragraph content.
     */
    private String extractFormattedText(String msgText)
    {
        String resultString = msgText.replaceAll("<p\\b[^>]*>", "");

        return resultString.replaceAll("<\\/p>", "<BR/>");
    }

    /**
     * Initializes the send via label and selector box.
     *
     * @return the chat transport selector box
     */
    private Component createChatTransportSelectorBox()
    {
        // Initialize the "send via" selector box and adds it to the send panel.
        if (mTransportSelectorBox == null)
        {
            sLog.debug("mTransportSelectorBox is null - creating new instance");
            mTransportSelectorBox = new ChatTransportSelectorBox(
                mChatPanel,
                mChatPanel.getChatSession(),
                mChatPanel.getChatSession().getCurrentChatTransport());
        }

        updateEditorPaneState();

        return mTransportSelectorBox;
    }

    /**
     *
     * @param isVisible
     */
    public void setTransportSelectorBoxVisible(boolean isVisible)
    {
        if (isVisible)
        {
            if (mTransportSelectorBox == null)
            {
                createChatTransportSelectorBox();

                if (mTransportSelectorBox.getMenu().isEnabled())
                {
                    GridBagConstraints constraints = new GridBagConstraints();
                    constraints.anchor = GridBagConstraints.NORTHEAST;
                    constraints.fill = GridBagConstraints.NONE;
                    constraints.gridx = 0;
                    constraints.gridy = 0;
                    constraints.weightx = 0f;
                    constraints.weighty = 0f;
                    constraints.gridheight = 1;
                    constraints.gridwidth = 1;

                    mCenterPanel.add(mTransportSelectorBox, constraints, 0);
                }
            }
            else
            {
                mTransportSelectorBox.setVisible(true);
                mCenterPanel.repaint();
            }
        }
        else if (mTransportSelectorBox != null)
        {
            mTransportSelectorBox.setVisible(false);
            mCenterPanel.repaint();
        }
        updateHintTextForTransport(mChatPanel.getChatSession().getCurrentChatTransport());
    }

    /**
     * Selects the given chat transport in the send via box.
     *
     * @param chatTransport the chat transport to be selected
     */
    public void setSelectedChatTransport(final ChatTransport chatTransport)
    {
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setSelectedChatTransport(chatTransport);
                }
            });
            return;
        }

        if (mTransportSelectorBox != null)
        {
            mTransportSelectorBox.setSelected(chatTransport);
        }

        updateTransportImage();
    }

    /**
     * Updates the hint text to that for the given chat transport
     *
     * @param chatTransport the transport whose help text should be used.
     */
    public void updateHintTextForTransport(ChatTransport chatTransport)
    {
        String hintText = "";
        ProtocolProviderService imProvider = AccountUtils.getImProvider();
        boolean imRegistered = (imProvider != null) && imProvider.isRegistered();

        if (!imRegistered)
        {
            hintText = resources.getI18NString("service.gui.SEND_MESSAGE_NO_SERVICE");
        }
        else if (chatTransport == null)
        {
            hintText = resources.getI18NString("service.gui.SEND_MESSAGE_NO_ADDRESS");
        }
        else if (chatTransport instanceof MetaContactChatTransport)
        {
            hintText = resources.getI18NString("service.gui.SEND_MESSAGE");
        }
        else if (chatTransport instanceof SMSChatTransport)
        {
            String smsNumber = (String) chatTransport.getDescriptor();
            // TODO: DUIR-5256 This is not correctly localized - the sms number should be a
            // replaceable parameter in the resource string, not concatenated to it
            hintText =
                resources.getI18NString("service.gui.SEND_SMS_TO") + " " + smsNumber;
        }
        else if (chatTransport instanceof ConferenceChatTransport)
        {
            hintText = resources.getI18NString("service.gui.SEND_GROUP_CHAT_TO");
        }

        sLog.debug("Setting hint text to '" + hintText + "' for " +
                    chatTransport + ", imProvider registered? " + imRegistered);
        mEditorPane.setHintText(hintText);

        // We must repaint here, otherwise the new hint text will not actually
        // be displayed.
        mEditorPane.repaint();
    }

    /**
     * Adds the given chatTransport to the given send via selector box.
     *
     * @param chatTransport the transport to add
     */
    public void addChatTransport(final ChatTransport chatTransport)
    {
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    addChatTransport(chatTransport);
                }
            });
            return;
        }

        sLog.debug("Adding chat transport: " + chatTransport);

        if (mTransportSelectorBox != null)
        {
            mTransportSelectorBox.addChatTransport(chatTransport);
        }

        updateEditorPaneState();
    }

    /**
     * Updates the status of the given chat transport in the send via selector
     * box and notifies the user for the status change.
     * @param chatTransport the <tt>chatTransport</tt> to update
     */
    public void updateChatTransportStatus(final ChatTransport chatTransport)
    {
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    updateChatTransportStatus(chatTransport);
                }
            });
            return;
        }

        if (mTransportSelectorBox != null)
            mTransportSelectorBox.updateTransportStatus(chatTransport);

        updateTransportImage();
    }

    /**
     * Opens the selector box containing the protocol contact icons.
     * This is the menu, where user could select the protocol specific
     * contact to communicate through.
     */
    public void openChatTransportSelectorBox()
    {
        mTransportSelectorBox.getMenu().doClick();
    }

    /**
     * Removes the given chat status state from the send via selector box.
     *
     * @param chatTransport the transport to remove
     */
    public void removeChatTransport(final ChatTransport chatTransport)
    {
        // We need to be sure that the following code is executed in the event
        // dispatch thread.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    removeChatTransport(chatTransport);
                }
            });
            return;
        }

        sLog.debug("Removing chat transport: " + chatTransport);

        if (mTransportSelectorBox != null)
        {
            mTransportSelectorBox.removeChatTransport(chatTransport);
        }

        updateEditorPaneState();
    }

    /**
     * Updates the visibility, enabled state, icon and hint text of the editor
     * pane and its transport selector box.
     */
    protected void updateEditorPaneState()
    {
        if (mTransportSelectorBox != null)
        {
            SIPCommMenu menu = mTransportSelectorBox.getMenu();
            int itemCount = menu.getItemCount();

            // The transport selector box is only visible if SMS is enabled, as
            // the user isn't allowed to choose between different IM transports.
            boolean isTransportSelectorVisible = ConfigurationUtils.isSmsEnabled();
            sLog.debug(
                "Is mTransportSelectorBox visible? " + isTransportSelectorVisible);
            mTransportSelectorBox.setVisible(isTransportSelectorVisible);

            // Only enable the editor pane if the IM account is online and
            // there is at least one chat transport that we can send a message
            // to.
            ProtocolProviderService imProvider = AccountUtils.getImProvider();
            boolean imRegistered = (imProvider != null) && imProvider.isRegistered();
            if (itemCount > 0 && imRegistered)
            {
                sLog.debug("Enabling editor pane. ItemCount = " + itemCount);
                getEditorPane().setEnabled(true);
                if (getIconSelectorBox() != null)
                {
                    getIconSelectorBox().setEnabled(true);
                }
                updateHintTextForTransport((ChatTransport) menu.getSelectedObject());
            }
            else
            {
                sLog.debug("Disabling editor pane. ItemCount = " + itemCount +
                                    ", imProvider registered? " + imRegistered);
                getEditorPane().setEnabled(false);
                if (getIconSelectorBox() != null)
                {
                    getIconSelectorBox().setEnabled(false);
                }
                updateHintTextForTransport(null);
            }
        }

        updateTransportImage();
    }

    /**
     * Show the sms menu.
     * @param isVisible <tt>true</tt> to show the sms menu, <tt>false</tt> -
     * otherwise
     */
    public void setSmsLabelVisible(boolean isVisible)
    {
        // Re-init sms count properties.
        mSmsCharCount = 160;
        mSmsNumberCount = 1;

        mSmsLabel.setVisible(isVisible);
        mSmsCharCountLabel.setVisible(isVisible);
        mSmsNumberLabel.setVisible(isVisible);

        mCenterPanel.repaint();
    }

    /**
     * Reloads menu.
     */
    public void loadSkin()
    {
        getRightButtonMenu().loadSkin();
    }

    public void changedUpdate(DocumentEvent documentevent) {}

    /**
     * Updates write panel size and adjusts sms properties if the sms menu
     * is visible.
     *
     * @param event the <tt>DocumentEvent</tt> that notified us
     */
    public void insertUpdate(DocumentEvent event)
    {
        // If we're in sms mode count the chars typed.
        if (mSmsLabel.isVisible())
        {
            if (mSmsCharCount == 0)
            {
                mSmsCharCount = 159;
                mSmsNumberCount ++;
            }
            else
                mSmsCharCount--;

            mSmsCharCountLabel.setText(String.valueOf(mSmsCharCount));
            mSmsNumberLabel.setText(String.valueOf(mSmsNumberCount));
        }
    }

    /**
     * Updates write panel size and adjusts sms properties if the sms menu
     * is visible.
     *
     * @param event the <tt>DocumentEvent</tt> that notified us
     */
    public void removeUpdate(DocumentEvent event)
    {
        // If we're in sms mode count the chars typed.
        if (mSmsLabel.isVisible())
        {
            if (mSmsCharCount == 160 && mSmsNumberCount > 1)
            {
                mSmsCharCount = 0;
                mSmsNumberCount --;
            }
            else
                mSmsCharCount++;

            mSmsCharCountLabel.setText(String.valueOf(mSmsCharCount));
            mSmsNumberLabel.setText(String.valueOf(mSmsNumberCount));
        }
    }

    /**
     * Sets the background of the write area to the specified color.
     *
     * @param color The color to set the background to.
     */
    public void setEditorPaneBackground(Color color)
    {
        mCenterPanel.setBackground(color);
        mEditorPane.setBackground(color);
    }

    /**
     * Update the <tt>transportImage</tt> to have the current status image.
     */
    private void updateTransportImage()
    {
        ChatContainer chatWindow = mChatPanel.getChatContainer();
        if (chatWindow != null)
        {
            chatWindow.updateContainer(mChatPanel);
        }
    }

    /**
     * An editor pane that allows a hint to be set in grey on the background.
     * The hint disappears when the user starts typing in the pane.
     */
    private static class HintPane extends JEditorPane
    {
        private static final long serialVersionUID = 0L;

        private String hintText = "";

        @Override
        public void paint(Graphics g)
        {
            super.paint(g);
            if (this.getDocument().getLength() == 0)
            {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(Color.LIGHT_GRAY);

                // There is no easy way of determining where to draw text so it
                // lines up with the typed text. Therefore we have some fudge
                // factors for displaying at 100%, 125% and 150% magnification.
                int yOffset = 16;
                if (OSUtils.IS_WINDOWS)
                {
                    if (getFont().getSize() > 15)
                    {
                        yOffset = 23;
                    }
                    else if (getFont().getSize() > 12)
                    {
                        yOffset = 20;
                    }
                }
                g2.drawString(hintText, 4, yOffset);
            }
        }

        public void setHintText(String text)
        {
            hintText = text;
        }
    }
}
