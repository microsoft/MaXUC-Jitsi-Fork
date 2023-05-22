/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.service.protocol.OperationSetServerStoredContactInfo;
import net.java.sip.communicator.service.protocol.ServerStoredDetails.GenericDetail;
import net.java.sip.communicator.util.Logger;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredContactInfoJabberImpl
    implements OperationSetServerStoredContactInfo
{
    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(OperationSetServerStoredContactInfoJabberImpl.class);

    private InfoRetreiver infoRetreiver = null;

    /**
     * If we got several listeners for the same contact lets retrieve once
     * but deliver result to all.
     */
    private final Hashtable<String, List<DetailsResponseListener>>
        listenersForDetails =
            new Hashtable<>();

    protected OperationSetServerStoredContactInfoJabberImpl(
        InfoRetreiver infoRetreiver)
    {
        this.infoRetreiver = infoRetreiver;
    }

    /**
     * returns the user details from the specified class or its descendants
     * the class is one from the
     * net.java.sip.communicator.service.protocol.ServerStoredDetails
     * or implemented one in the operation set for the user info
     *
     * @param contact Contact
     * @param detailClass Class
     * @return Iterator
     */
    public <T extends GenericDetail> Iterator<T> getDetailsAndDescendants(
        Contact contact,
        Class<T> detailClass)
    {
        List<GenericDetail> details
            = infoRetreiver.getContactDetails(contact.getAddress());
        List<T> result = new LinkedList<>();

        if(details == null)
            return result.iterator();

        for (GenericDetail item : details)
            if(detailClass.isInstance(item))
            {
                @SuppressWarnings("unchecked")
                T t = (T) item;

                result.add(t);
            }

        return result.iterator();
    }

    /**
     * returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param contact Contact
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator<GenericDetail> getDetails(
        Contact contact,
        Class<? extends GenericDetail> detailClass)
    {
        List<GenericDetail> details
            = infoRetreiver.getContactDetails(contact.getAddress());
        List<GenericDetail> result = new LinkedList<>();

        if(details == null)
            return result.iterator();

        for (GenericDetail item : details)
            if(detailClass.equals(item.getClass()))
                result.add(item);

        return result.iterator();
    }

    /**
     * request the full info for the given uin
     * waits and return this details
     *
     * @param contact Contact
     * @return Iterator
     */
    public Iterator<GenericDetail> getAllDetailsForContact(Contact contact)
    {
        List<GenericDetail> details
            = infoRetreiver.getContactDetails(contact.getAddress());

        if(details == null)
            return new LinkedList<GenericDetail>().iterator();
        else
            return new LinkedList<>(details).iterator();
    }

    /**
     * Requests all details existing for the specified contact.
     * @param contact the specified contact
     * @return a java.util.Iterator over all details existing for the specified
     * contact.
     */
    public Iterator<GenericDetail> requestAllDetailsForContact(
        final Contact contact, DetailsResponseListener listener)
    {
        List<GenericDetail> res =
            infoRetreiver.getCachedContactDetails(contact.getAddress());

        if(res != null)
        {
            return res.iterator();
        }

        synchronized(listenersForDetails)
        {
            List<DetailsResponseListener> ls =
                listenersForDetails.get(contact.getAddress());

            boolean isFirst = false;
            if(ls == null)
            {
                ls = new ArrayList<>();
                isFirst = true;
                listenersForDetails.put(contact.getAddress(), ls);
            }

            if(!ls.contains(listener))
                ls.add(listener);

            // there is already scheduled retrieve, will deliver at listener.
            if(!isFirst)
                return null;
        }

        new Thread(new Runnable()
        {
            public void run()
            {
                List<GenericDetail> result =
                    infoRetreiver.retrieveDetails(contact.getAddress());

                List<DetailsResponseListener> listeners;

                synchronized(listenersForDetails)
                {
                    listeners =
                        listenersForDetails.remove(contact.getAddress());
                }

                if(listeners == null || result == null)
                    return;

                for(DetailsResponseListener l : listeners)
                {
                    try
                    {
                        l.detailsRetrieved(result.iterator());
                    }
                    catch(Throwable t)
                    {
                        logger.error(
                            "Error delivering for retrieved details", t);
                    }
                }
            }
        }, getClass().getName() + ".RetrieveDetails").start();

        // return null as there is no cache and we will try to retrieve
        return null;
    }
}
