/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.datatransfer.*;
import java.awt.im.*;
import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.UIService.Reformatting;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.resources.*;

/**
 * A <tt>TransferHandler</tt> that handles dropping of <tt>UIContact</tt>s or
 * <tt>String</tt> addresses on a <tt>CallConference</tt>. Dropping such data on
 * the <tt>CallDialog</tt> will turn a one-to-one <tt>Call</tt> into a telephony
 * conference.
 *
 * @author Yana Stamcheva
 */
public class CallTransferHandler
    extends ExtendedTransferHandler
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The data flavor used when transferring <tt>UIContact</tt>s.
     */
    protected static final DataFlavor uiContactDataFlavor
        = new DataFlavor(UIContact.class, "UIContact");

    /**
     * The logger.
     */
    private static final Logger logger
        = Logger.getLogger(CallTransferHandler.class);

    /**
     * The <tt>CallConference</tt> into which the dropped callees are to be
     * invited.
     */
    private final CallConference callConference;

    /**
     * Initializes a new <tt>CallTransferHandler</tt> instance which is to
     * invite dropped callees to a telephony conference specified by a specific
     * <tt>Call</tt> which participates in it.
     *
     * @param call the <tt>Call</tt> which specifies the telephony conference to
     * which dropped callees are to be invited
     */
    public CallTransferHandler(Call call)
    {
        this(call.getConference());
    }

    /**
     * Initializes a new <tt>CallTransferHandler</tt> instance which is to
     * invite dropped callees to a specific <tt>CallConference</tt>.
     *
     * @param callConference the <tt>CallConference</tt> to which dropped
     * callees are to be invited
     */
    public CallTransferHandler(CallConference callConference)
    {
        this.callConference = callConference;
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
    public boolean canImport(JComponent comp, DataFlavor[] flavor)
    {
        for (DataFlavor f : flavor)
        {
            if (f.equals(DataFlavor.stringFlavor)
                    || f.equals(uiContactDataFlavor))
            {
                return (comp instanceof JPanel);
            }
        }
        return false;
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
        String callee = null;
        ProtocolProviderService provider = null;
        Reformatting reformattingNeeded = Reformatting.NEEDED;

        if (t.isDataFlavorSupported(uiContactDataFlavor))
        {
            Object o = null;
            reformattingNeeded = Reformatting.NEEDED;

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
                UIContact uiContact = ((ContactNode) o).getContactDescriptor();
                Iterator<UIContactDetail> contactDetails
                    = uiContact
                        .getContactDetailsForOperationSet(
                                OperationSetBasicTelephony.class)
                            .iterator();

                while (contactDetails.hasNext())
                {
                    UIContactDetail detail = contactDetails.next();
                    ProtocolProviderService detailProvider
                        = detail.getPreferredProtocolProvider(
                                OperationSetBasicTelephony.class);

                    if (detailProvider != null)
                    {
                        callee = detail.getAddress();
                        provider = detailProvider;
                        break;
                    }
                }

                if (callee == null)
                {
                    /*
                     * It turns out that the error message to be reported would
                     * like to display information about the account which could
                     * not add the dropped callee to the telephony conference.
                     * Unfortunately, a telephony conference may have multiple
                     * accounts involved. Anyway, choose the first account
                     * involved in the telephony conference.
                     */
                    ProtocolProviderService callProvider
                        = callConference.getCalls().get(0)
                                .getProtocolProvider();

                    ResourceManagementService resources
                        = GuiActivator.getResources();
                    AccountID accountID = callProvider.getAccountID();

                    new ErrorDialog(null,
                            resources.getI18NString("service.gui.ERROR"),
                            resources.getI18NString(
                                    "service.gui.CALL_NOT_SUPPORTING_PARTICIPANT",
                                    new String[]
                                            {
                                                accountID.getService(),
                                                accountID.getUserID(),
                                                uiContact.getDisplayName()
                                            }))
                        .showDialog();
                }
            }
        }
        else if (t.isDataFlavorSupported(DataFlavor.stringFlavor))
        {
            reformattingNeeded = Reformatting.NOT_NEEDED;
            InputContext inputContext = comp.getInputContext();

            if (inputContext != null)
                inputContext.endComposition();

            try
            {
                try (BufferedReader reader = new BufferedReader(
                        DataFlavor.stringFlavor.getReaderForText(t)))
                {
                    String line;
                    StringBuilder calleeBuilder = new StringBuilder();

                    while ((line = reader.readLine()) != null)
                        calleeBuilder.append(line);

                    callee = calleeBuilder.toString();
                    /*
                     * The value of the local variable provider will be null
                     * because we have a String only and hence we have no
                     * associated ProtocolProviderService.
                     * CallManager.inviteToConferenceCall will accept it.
                     */
                }
            }
            catch (UnsupportedFlavorException | IOException e)
            {
                logger.debug("Failed to drop string.", e);
            }
        }

        if (callee == null)
            return false;
        else
        {
            Map<ProtocolProviderService, List<String>> callees
                = new HashMap<>();

            callees.put(provider, Arrays.asList(callee));
            CallManager.inviteToConferenceCall(callees,
                                               reformattingNeeded,
                                               callConference);

            return true;
        }
    }
}
