/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.header.*;

import javax.sip.*;
import javax.sip.header.*;
import javax.sip.message.*;

import net.java.sip.communicator.util.*;

/**
 * The class handles cloning a request that needs to be retransmitted.
 */
public class RequestCloner
{
    /**
     * Our class logger.
     */
    private static final Logger logger
        = Logger.getLogger(RequestCloner.class);

    /**
     * Clones a request so that we can retry transmission.
     *
     * @param request the request that needs to be cloned.
     * @param transactionCreator the JAIN SipProvider that we should use to
     * create the new transaction.
     *
     * @return a transaction containing a cloned request
     *
     * @throws SipException if we get an exception white creating the
     * new transaction
     * @throws InvalidArgumentException if we fail to create a new header
     */
    public static synchronized ClientTransaction cloneRequest(
                                    Request           request,
                                    SipProvider       transactionCreator)
        throws SipException,
               InvalidArgumentException

    {
        Request clonedRequest = (Request) request.clone();

        //remove the branch id so that we could use the request in a new
        //transaction
        removeBranchID(clonedRequest, new HeaderFactoryImpl());

        //rfc 3261 says that the cseq header should be augmented for the new
        //request. do it here so that the new dialog (created together with
        //the new client transaction) takes it into account.
        //Bug report - Fredrik Wickstrom
        incrementRequestSeqNo(clonedRequest);

        ClientTransaction retryTran =
            transactionCreator.getNewClientTransaction(clonedRequest);

        // We have previously incremented the request sequence number and we
        // want to make sure that the dialog (if it exists) has its local
        // sequence number also incremented.
        Dialog tranDialog = retryTran.getDialog();
        if (tranDialog != null && tranDialog.getLocalSeqNumber()
                != getRequestSeqNo(clonedRequest))
            tranDialog.incrementLocalSequenceNumber();

        logger.debug("Returning cloned request's transaction.");
        return retryTran;
    }

     /**
     * Removes all via headers from <tt>request</tt> and replaces them with a
     * new one, equal to the one that was top most.
     *
     * @param request the Request whose branchID we'd like to remove.
     * @param headerFactory the HeaderFactory used to create message headers.
     */
    public static void removeBranchID(Request request, HeaderFactory headerFactory)
    {
        ViaHeader viaHeader = (ViaHeader) request.getHeader(ViaHeader.NAME);

        request.removeHeader(ViaHeader.NAME);

        ViaHeader newViaHeader;
        try
        {
            newViaHeader = headerFactory.createViaHeader(
                                            viaHeader.getHost()
                                            , viaHeader.getPort()
                                            , viaHeader.getTransport()
                                            , null);
            request.setHeader(newViaHeader);
        }
        catch (Exception exc)
        {
            // we are using the host port and transport of an existing Via
            // header so it would be quite weird to get this exception.
            logger.debug("failed to reset a Via header");
        }
    }

    /**
     * Increments the given <tt>request</tt> sequence number.
     * @param request the <tt>Request</tt>, which sequence number we would like
     * to increment
     *
     * @throws InvalidArgumentException if we fail to increase the value of the
     * cSeq header.
     */
    public static void incrementRequestSeqNo(Request request)
        throws InvalidArgumentException
    {
        CSeqHeader cSeq = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
        cSeq.setSeqNumber(cSeq.getSeqNumber() + 1L);
    }

    /**
     * Updates the given <tt>request</tt> sequence number.
     * @param request the <tt>Request</tt>, which sequence number we would like
     * to increment
     * @param newCSeq the new CSeq to set.
     *
     * @throws InvalidArgumentException if we fail to increase the value of the
     * cSeq header.
     */
    public static void updateRequestSeqNo(Request request, long newCSeq)
        throws InvalidArgumentException
    {
        CSeqHeader cSeq = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
        cSeq.setSeqNumber(newCSeq);
    }

    /**
     * Returns the request sequence number.
     * @param request the <tt>Request</tt>, for which we're returning a sequence
     * number
     * @return the sequence number of the given request
     */
    public static long getRequestSeqNo(Request request)
    {
        CSeqHeader cSeq = (CSeqHeader) request.getHeader(CSeqHeader.NAME);
        return cSeq.getSeqNumber();
    }
}
