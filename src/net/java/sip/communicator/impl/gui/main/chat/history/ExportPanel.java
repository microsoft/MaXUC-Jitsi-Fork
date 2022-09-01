// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.chat.history;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.*;
import java.util.*;

import javax.swing.*;
import javax.swing.filechooser.*;

import org.apache.commons.io.*;
import org.jitsi.service.resources.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.gui.ChatRoomWrapper;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.account.*;

/**
 * The export panel allows users to export their full chat history to file
 */
public class ExportPanel
    extends TransparentPanel
    implements  ActionListener
{
    private static final long serialVersionUID = 0L;

    /**
     * Create local access to the logger
     */
    private static final Logger logger = Logger.getLogger(ExportPanel.class);

    /**
     * Create local access to the Resources Service for retrieving strings
     */
    private static final ResourceManagementService resources =
                                                    GuiActivator.getResources();

    /**
     * The MetaContactListService for finding display names.
     */
    private final MetaContactListService metaContactListService =
                                           GuiActivator.getContactListService();

    /**
     * I18n string for the text displayed on the export chat button
     */
    private static final String BUTTON_TEXT =
        resources.getI18NString("service.gui.chat.export.BUTTON_TEXT");

    /**
     * I18n string for the default export file name
     */
    private static final String FILE_NAME =
        resources.getI18NString("service.gui.chat.export.DEFAULT_FILE_NAME");

    /**
     * I18n string for the title of the overwrite confirmation window
     */
    private static final String FILE_ALREADY_EXISTS_TITLE =
        resources.getI18NString(
                           "service.gui.chat.export.FILE_ALREADY_EXISTS_TITLE");

    /**
     * I18n string for the message in the overwrite confirmation window
     */
    private static final String FILE_ALREADY_EXISTS_TEXT =
        resources.getI18NString(
                            "service.gui.chat.export.FILE_ALREADY_EXISTS_TEXT");

    /**
     * The History Service for the History Window
     */
    private MetaHistoryService historyService;

    /**
     * The History Contact for the History Window. Can be either an individual
     * or a group.
     */
    private Object historyContact;

    /**
     * The File Chooser used to set the location of the exported chat history
     * file. To stop files being accidentally overridden, the new file is
     * checked and a dialog is shown to confirm or try again.
     */
    private final JFileChooser fileChooser = new JFileChooser()
    {
        private static final long serialVersionUID = 0L;

        @Override
        public void approveSelection()
        {
            File file = getSelectedFile();
            if (file.exists())
            {
                // If the file already exists, check the user is happy to
                // overwrite it via a confirm dialog
                int result = JOptionPane.showConfirmDialog(
                    this,
                    FILE_ALREADY_EXISTS_TEXT,
                    FILE_ALREADY_EXISTS_TITLE,
                    JOptionPane.YES_NO_CANCEL_OPTION);

                switch (result)
                {
                    case JOptionPane.YES_OPTION:
                        // Happy to overwrite, continue
                        super.approveSelection();
                        break;
                    case JOptionPane.CANCEL_OPTION:
                        // Decided to stop the whole process
                        cancelSelection();
                        break;
                    default:
                        break;
                }
            }
            else
            {
                // If the file doesn't exists, then we have no issues
                super.approveSelection();
            }
        }
    };

    /**
     * The filter used by the File Chooser to only show text files
     */
    private static final FileNameExtensionFilter FILE_FILTER =
        new FileNameExtensionFilter(
            resources.getI18NString("service.gui.chat.export.TEXT_FILES"),
            "txt",
            "text");

    /**
     * Used to filter the correct messages from the chat history
     */
    private static final String[] HISTORY_FILTER =
        new String[]
        {
            MessageHistoryService.class.getName(),
            FileHistoryService.class.getName()
        };

    /**
     * The button identifier
     */
    private static final String BUTTON_NAME = "export";

    /**
     * Creates an instance of the ExportPanel, which displays a simple 'Export
     * Chat History' button
     *
     * @param historyContact the historyContact for the historyWindow
     */
    public ExportPanel(Object historyContact) {

        // Create a FlowLayout to allow the button to stretch the window
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        // Store the history contact and retrieve the history
        // service
        this.historyContact = historyContact;
        historyService = GuiActivator.getMetaHistoryService();

        // Create the Export Chat button
        SIPCommBasicTextButton exportButton =
                                        new SIPCommBasicTextButton(BUTTON_TEXT);

        exportButton.setName(BUTTON_NAME);
        exportButton.addActionListener(this);
        add(exportButton, BorderLayout.SOUTH);

        // Scale the font in the fileChooser
        ScaleUtils.scaleFontRecursively(fileChooser.getComponents());
        // Set our fileChooser to only search for text files, and have a default
        // filename
        fileChooser.setFileFilter(FILE_FILTER);
        fileChooser.setSelectedFile(new File(FILE_NAME + ".txt"));
    }

    /**
     * Trigger a File Chooser to open when the button is clicked
     */
    @Override
    public void actionPerformed(ActionEvent evt)
    {
        JButton button = (JButton) evt.getSource();
        String buttonName = button.getName();

        if (!buttonName.equalsIgnoreCase(BUTTON_NAME))
        {
            // Unknown button
            logger.error("Unknown button in export pane of chat history: " +
                                                                    buttonName);
            return;
        }

        // Show a save dialog and get the outcome. To avoid overwriting files we
        // use a custom handler which is created with the fileChooser above
        logger.debug("Opening Save File dialog");
        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            // If the file isn't a .txt, append .txt to it
            if (!FilenameUtils.getExtension(file.getName()).
                                                  equalsIgnoreCase("txt")) {
                logger.debug("Appending .txt to file name");
                file = new File(file.toString() + ".txt");
            }

            try
            {
                // Create access to the file to allow us to write to it
                logger.debug("Beginning chat export to: " + file.getName());
                PrintWriter writer = new PrintWriter(file, "UTF-8");

                exportChat(writer);

                writer.close();

                logger.debug("Chat exported successfully");
            }
            catch (FileNotFoundException e)
            {
                logger.error("Unable to find file provided by the file " +
                                              "chooser during chat export.", e);
            }
            catch (UnsupportedEncodingException e)
            {
                // UTF-8 should always be supported
                logger.error("UTF-8 not supported during chat export.", e);
            }
        }
        else
        {
            logger.debug("Cancelled chat export");
        }
    }

    /**
     * Export the chat history using the provided Print Writer
     * @param writer
     */
    private void exportChat(PrintWriter writer) {

        // First we get our list of messages from the History Service. This
        // logic is repeated all over the place and should really be tidied up
        Collection<Object> msgList = null;

        if (historyContact instanceof MetaContact)
        {
            logger.debug("Exporting history using MetaContact");
            msgList = historyService.findByEndDate(
                HISTORY_FILTER,
                (MetaContact) historyContact,
                new Date(System.currentTimeMillis()));
        }
        else if (historyContact instanceof ChatRoomWrapper)
        {
            ChatRoomWrapper chatRoomWrapper
                = (ChatRoomWrapper) historyContact;

            if (chatRoomWrapper.getChatRoom() == null)
                return;

            logger.debug("Exporting history using ChatRoom");
            msgList = historyService.findByEndDate(
                HISTORY_FILTER,
                chatRoomWrapper,
                new Date(System.currentTimeMillis()));
        }
        else if (historyContact instanceof String)
        {
            logger.debug("Exporting history using String history contact");
            msgList = historyService.findByEndDate(
                HISTORY_FILTER,
                (String) historyContact,
                new Date(System.currentTimeMillis()));
        }

        if ((msgList != null) && (msgList.size() > 0))
        {
            // If we've got some messages, loop through them to extract the
            // required details
            Iterator<Object> messageIterator = msgList.iterator();

            while (messageIterator.hasNext())
            {
                Object o = messageIterator.next();

                Date timestamp;
                StringBuilder message = new StringBuilder();

                // Build up the message string for either a message or a file
                if (o instanceof MessageEvent) {

                    MessageEvent evt = (MessageEvent) o;

                    // If the event shouldn't be displayed or if there was a
                    // sending error then move on
                    if (!evt.isDisplayed() ||
                        evt.getErrorMessage() != null)
                    {
                        continue;
                    }

                    // We only care about text based messages, not status
                    // messages
                    int eventType = evt.getEventType();

                    if (!(eventType == MessageEvent.CHAT_MESSAGE ||
                          eventType == MessageEvent.GROUP_MESSAGE ||
                          eventType == MessageEvent.SMS_MESSAGE))
                    {
                        continue;
                    }

                    timestamp = evt.getTimestamp();

                    // Create the message for export. It takes the form:
                    // <Contact> : <Content>
                    message.append(evt.getContactDisplayName());
                    message.append(": ");
                    message.append(evt.getSourceMessage().getContent().
                                                replaceAll("[\\t\\r\\n]", " "));
                }
                else if (o instanceof FileRecord)
                {
                    // If we're looking at a file we need to manually work out
                    // the contact responsible
                    FileRecord fileRecord = (FileRecord) o;

                    timestamp = fileRecord.getDate();

                    String contact;

                    if (fileRecord.isInbound())
                    {
                        contact = getDisplayNameForContact(
                                          fileRecord.getContact().getAddress());
                    }
                    else
                    {
                        contact = GuiActivator.
                              getGlobalDisplayDetailsService().
                              getGlobalDisplayName();
                    }

                    // Build up the message to display in the export
                    message.append(GuiActivator.getResources().getI18NString(
                        "service.gui.chat.export.FILE_TRANSFER",
                        new String[] {
                            contact,
                            fileRecord.getFile().getName(),
                            fileRecord.getStatusString()}));
                }
                else
                {
                    continue;
                }

                // Finally, localise the date, then write the whole line using
                // the provided file access
                String date = DateFormat.getDateTimeInstance().format(timestamp);
                message.insert(0, " ").insert(0, date);
                writer.println(message);
            }
        }
    }

    /**
     * Determine the display name for the given contact address
     *
     * @param address the address of the contact for which to look up the
     * display name
     * @return the display name for the given contact address
     */
    private String getDisplayNameForContact(String address)
    {
        String displayName = address;

        ProtocolProviderService imProvider = AccountUtils.getImProvider();
        if (imProvider != null)
        {
            MetaContact metaContact =
                metaContactListService.findMetaContactByContact(
                                address,
                                imProvider.getAccountID().getAccountUniqueID());

            if (metaContact != null)
            {
                displayName = metaContact.getDisplayName();
            }
        }

        return displayName;
    }
}

