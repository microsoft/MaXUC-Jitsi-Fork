/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.chat;

import java.awt.datatransfer.*;
import java.awt.im.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.contactsource.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * A TransferHandler that we use to handle copying, pasting and DnD operations
 * in our <tt>ChatPanel</tt>. The string handler is heavily inspired
 * by Sun's <tt>DefaultTransferHandler</tt> with the main difference being that
 * we only accept pasting of plain text. We do this in order to avoid HTML
 * support problems that appear when pasting formatted text into our editable
 * area.
 *
 * @author Emil Ivov
 * @author Yana Stamcheva
 */
public class ChatTransferHandler
    extends ExtendedTransferHandler
{
    private static final long serialVersionUID = 1L;

    /**
     * The data flavor used when transferring <tt>UIContact</tt>s.
     */
    protected static final DataFlavor uiContactDataFlavor
        = new DataFlavor(UIContact.class, "UIContact");

    /**
     * This class logger.
     */
    private static final Logger logger
        = Logger.getLogger(ChatTransferHandler.class);

    /**
     * The data flavor used when transferring <tt>File</tt>s under Linux.
     */
    private static DataFlavor uriListFlavor;
    static
    {
         try
         {
             uriListFlavor =
                 new DataFlavor("text/uri-list;class=java.lang.String");
         } catch (ClassNotFoundException e)
         {
            // can't happen
             logger.error("", e);
         }
    }

    /**
     * Data flavor used when transferring a URL copied from Internet Explorer
     */
    private static DataFlavor urlFlavor;
    static
    {
        try
        {
            urlFlavor =
                    new DataFlavor("application/x-java-url;class=java.net.URL");
        }
        catch (ClassNotFoundException e)
        {
            // Really shouldn't ever happen.
            logger.error("Class not found initialising url flavor", e);
        }
    }

    /**
     * The chat panel involved in the copy/paste/DnD operation.
     */
    private final ChatPanel chatPanel;

    /**
     * Constructs the <tt>ChatTransferHandler</tt> by specifying the
     * <tt>ChatPanel</tt> we're currently dealing with.
     *
     * @param chatPanel the <tt>ChatPanel</tt> we're currently dealing with
     */
    public ChatTransferHandler(ChatPanel chatPanel)
    {
        this.chatPanel = chatPanel;
    }

    /**
     * Indicates whether a component will accept an import of the given
     * set of data flavors prior to actually attempting to import it. We return
     * <tt>true</tt> to indicate that the transfer with at least one of the
     * given flavors would work and <tt>false</tt> to reject the transfer.
     * <p>
     * @param comp component
     * @param flavor the data formats available
     * @return  true if the data can be inserted into the component, false
     * otherwise
     * @throws NullPointerException if <code>support</code> is {@code null}
     */
    public boolean canImport(JComponent comp, DataFlavor flavor[])
    {
        for (DataFlavor f: flavor)
        {
            if (f.equals(uiContactDataFlavor) || f.equals(uriListFlavor))
                return true;
        }

        return super.canImport(comp, flavor);
    }

    /**
     * Handles transfers to the chat panel from the clip board or a
     * DND drop operation. The <tt>Transferable</tt> parameter contains the
     * data that needs to be imported.
     * <p>
     * @param comp  the component to receive the transfer;
     * @param t the data to import
     * @return  true if the data was inserted into the component and false
     * otherwise
     */
    public boolean importData(JComponent comp, Transferable t)
    {
        // getTransferDataFlavors() gets the supported flavors. The order should
        // be the preferred order.  I.e. the first flavor is the one which most
        // accurately describes the data in the transferable. T hus check each
        // flavor in turn
        for (DataFlavor flavor : t.getTransferDataFlavors())
        {
            logger.trace("Examining flavor " + flavor);

            // Files
            if (flavor.equals(DataFlavor.javaFileListFlavor))
            {
                if (handleFileFlavor(t))
                {
                    logger.debug("Handled file flavor");
                    return true;
                }
            }

            // list of URLs
            if (flavor.equals(uriListFlavor))
            {
                if (handleUrlListFlavor(t))
                {
                    logger.debug("Handled URL list flavor");
                    return true;
                }
            }

            // Contact
            if (flavor.equals(uiContactDataFlavor))
            {
                if (handleContactFlavor(t))
                {
                    logger.debug("Handled contact flavor");
                    return true;
                }
            }

            // String
            if (flavor.equals(DataFlavor.stringFlavor))
            {
                if (handleStringFlavor(comp, t))
                {
                    logger.debug("Handled string flavor");
                    return true;
                }
            }

            // URL
            if (flavor.equals(urlFlavor))
            {
                if (handleUrlFlavor(comp, t))
                {
                    logger.debug("Handled URL flavor");
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Handle a URL flavor data transfer
     *
     * @param comp The component to insert the URL to
     * @param t the data transfer object
     * @return true if we managed to handle this transfer
     */
    private boolean handleUrlFlavor(JComponent comp, Transferable t)
    {
        try
        {
            URL url = (URL) t.getTransferData(urlFlavor);
            ((JTextComponent)comp).replaceSelection(url.toString());

            return true;
        }
        catch (UnsupportedFlavorException e)
        {
            e.printStackTrace();
            logger.debug("Unsupported flavor getting URL", e);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            logger.debug("IO exception getting URL", e);
        }

        return false;
    }

    /**
     * Handle a string flavor data transfer
     *
     * @param comp The component to insert the string to
     * @param t the data transfer object
     * @return true if we managed to handle this transfer
     */
    private boolean handleStringFlavor(JComponent comp, Transferable t)
    {
        InputContext inputContext = comp.getInputContext();
        if (inputContext != null)
        {
            inputContext.endComposition();
        }
        try
        {
            BufferedReader reader = new BufferedReader(
                DataFlavor.stringFlavor.getReaderForText(t));

            StringBuilder buffToPaste = new StringBuilder();
            String line = reader.readLine();

            while (line != null)
            {
                buffToPaste.append(line);

                //read next line
                line = reader.readLine();
                if (line != null)
                    buffToPaste.append("\n");
            }

            ((JTextComponent)comp)
                .replaceSelection(buffToPaste.toString());
            return true;
        }
        catch (UnsupportedFlavorException | IOException ufe)
        {
            logger.debug("Failed to drop string.", ufe);
        }

        return false;
    }

    /**
     * Handle a contact flavor data transfer
     *
     * @param t the data transfer object
     * @return true if we managed to handle this transfer
     */
    private boolean handleContactFlavor(Transferable t)
    {
        Object o = null;

        try
        {
            o = t.getTransferData(uiContactDataFlavor);
        }
        catch (UnsupportedFlavorException | IOException e)
        {
            logger.debug("Failed to drop meta contact.", e);
        }

        if (o instanceof ContactNode)
        {
            UIContact uiContact
                = ((ContactNode) o).getContactDescriptor();

            // We only support drag&drop for MetaContacts for now.
            if (!(uiContact instanceof MetaUIContact))
                return false;

            ChatTransport currentChatTransport
                = chatPanel.getChatSession().getCurrentChatTransport();

            Iterator<Contact> contacts = ((MetaContact) uiContact
                .getDescriptor()).getContactsForProvider(
                    currentChatTransport.getProtocolProvider());

            String contact = null;
            if (contacts.hasNext())
                contact = contacts.next().getAddress();

            if (contact != null)
            {
                List<String> inviteList = new ArrayList<>();
                inviteList.add(contact);
                chatPanel.inviteContacts(currentChatTransport,
                                         inviteList, null, false, null);

                return true;
            }
            else
                new ErrorDialog(
                    null,
                    GuiActivator.getResources().getI18NString(
                        "service.gui.ERROR"),
                    GuiActivator.getResources().getI18NString(
                        "service.gui.CONTACT_NOT_SUPPORTING_CHAT_CONF",
                        new String[]{uiContact.getDisplayName()}))
                .showDialog();
        }

        return false;
    }

    /**
     * Handle a url list flavor data transfer
     *
     * @param t the data transfer object
     * @return true if we managed to handle this transfer
     */
    private boolean handleUrlListFlavor(Transferable t)
    {
        try
        {
            Object o = t.getTransferData(uriListFlavor);
            boolean dataProcessed = false;

            StringTokenizer tokens = new StringTokenizer((String)o);
            while (tokens.hasMoreTokens())
            {
                String urlString = tokens.nextToken();

                if (urlString.startsWith("http"))
                {
                    // URLs beginning with "http" are clearly not file transfers
                    continue;
                }

                URL url = new URL(urlString);
                File file = new File(
                    URLDecoder.decode(url.getFile(), "UTF-8"));
                chatPanel.sendFile(file);
                dataProcessed = true;
            }

            return dataProcessed;
        }
        catch (UnsupportedFlavorException | IOException e)
        {
            logger.debug("Failed to drop files.", e);
        }

        return false;
    }

    /**
     * Handle a file flavor data transfer
     *
     * @param t the data transfer object
     * @return true if we managed to handle this transfer
     */
    private boolean handleFileFlavor(Transferable t)
    {
        try
        {
            Object o = t.getTransferData(DataFlavor.javaFileListFlavor);

            if (o instanceof java.util.Collection)
            {
                @SuppressWarnings("unchecked")
                Collection<File> files = (Collection<File>) o;

                for (File file: files)
                    chatPanel.sendFile(file);

                // Otherwise fire files dropped event.
                return true;
            }
        }
        catch (UnsupportedFlavorException | IOException e)
        {
            logger.debug("Failed to drop files.", e);
        }

        return false;
    }
}
