/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.metahistory;

import java.util.*;

import org.osgi.framework.*;

import net.java.sip.communicator.service.callhistory.*;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.filehistory.*;
import net.java.sip.communicator.service.gui.ChatRoomWrapper;
import net.java.sip.communicator.service.metahistory.*;
import net.java.sip.communicator.service.msghistory.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The Meta History Service is wrapper around the other known
 * history services. Query them all at once, sort the result and return all
 * merged records in one collection.
 *
 * @author Damian Minkov
 */
public class MetaHistoryServiceImpl
    implements  MetaHistoryService,
                ServiceListener
{
    /**
     * The logger for this class.
     */
    private static final Logger sLog =
        Logger.getLogger(MetaHistoryServiceImpl.class);

    /**
     * The BundleContext that we got from the OSGI bus.
     */
    private BundleContext bundleContext = null;

    /**
     * Caching of the used services
     */
    private Hashtable<String, Object> services = new Hashtable<>();

    /**
     * Returns all the records before the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoomWrapper.
     * @param endDate Date the date of the last record to return
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByEndDate(String[] services,
            Object descriptor, Date endDate)
        throws RuntimeException
    {
        TreeSet<Object> result = new TreeSet<>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findByEndDate((MetaContact)descriptor, endDate));
                }
                else if(descriptor instanceof String)
                {
                    result.addAll(
                        mhs.findByEndDate((String)descriptor, endDate));
                }
                else if(descriptor instanceof ChatRoomWrapper)
                {
                    result.addAll(
                        mhs.findByEndDate((ChatRoomWrapper)descriptor, endDate));
                }
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findByEndDate(
                        (MetaContact)descriptor, endDate));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                result.addAll(chs.findByEndDate(endDate));
            }
        }

        return result;
    }

    /**
     * Returns all the records between the given dates
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoomWrapper.
     * @param startDate Date the date of the first record to return
     * @param endDate Date the date of the last record to return
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByPeriod(String[] services,
            Object descriptor, Date startDate, Date endDate)
        throws RuntimeException
    {
        TreeSet<Object> result = new TreeSet<>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findByPeriod(
                            (MetaContact)descriptor, startDate, endDate));
                }
                else if(descriptor instanceof String)
                {
                    result.addAll(
                        mhs.findByPeriod(
                            (String)descriptor, startDate, endDate));
                }
                else if(descriptor instanceof ChatRoomWrapper)
                {
                    result.addAll(
                        mhs.findByPeriod(
                            (ChatRoomWrapper) descriptor, startDate, endDate));
                }
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findByPeriod(
                        (MetaContact)descriptor, startDate, endDate));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                result.addAll(chs.findByPeriod(startDate, endDate));
            }
        }

        return result;
    }

    /**
     * Returns all the records having the given keyword.
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoomWrapper.
     * @param keyword keyword
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findByKeyword(String[] services,
            Object descriptor, String keyword)
        throws RuntimeException
    {
        TreeSet<Object> result = new TreeSet<>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if (serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs = (MessageHistoryService)serv;

                if (descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findByKeyword((MetaContact)descriptor, keyword));
                }
                else if (descriptor instanceof String)
                {
                    result.addAll(
                        mhs.findByKeyword((String)descriptor, keyword));
                }
                else if (descriptor instanceof ChatRoomWrapper)
                {
                    result.addAll(
                        mhs.findByKeyword((ChatRoomWrapper)descriptor, keyword));
                }
            }
            else if (serv instanceof FileHistoryService
                     && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findByKeyword(
                        (MetaContact)descriptor, keyword));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;

                // this will get all call records
                Collection<CallRecord> cs = chs.findByEndDate(new Date());

                Iterator<CallRecord> iter = cs.iterator();
                while (iter.hasNext())
                {
                    CallRecord callRecord = iter.next();

                    if (matchCallPeer(callRecord.getPeerRecords(), keyword))
                    {
                        result.add(callRecord);
                    }
                }
            }
        }

        return result;
    }

    /**
     * Returns the supplied number of recent records.
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoomWrapper.
     * @param count messages count
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findLast(String[] services,
            Object descriptor, int count)
        throws RuntimeException
    {
        TreeSet<Object> result = new TreeSet<>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs =
                    (MessageHistoryService)serv;

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findLast(
                            (MetaContact)descriptor,
                            count));
                }
                else if(descriptor instanceof String)
                {
                    result.addAll(
                        mhs.findLast(
                            (String)descriptor,
                            count));
                }
                else if(descriptor instanceof ChatRoomWrapper)
                {
                    result.addAll(
                        mhs.findLast(
                            (ChatRoomWrapper)descriptor,
                            count));
                }
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findLast(
                        (MetaContact)descriptor,
                        count));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;
                result.addAll(chs.findLast(count));
            }
        }

        LinkedList<Object> resultAsList = new LinkedList<>(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    /**
     * Returns the supplied number of recent records after the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param date messages after date
     * @param count messages count
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findFirstMessagesAfter(String[] services,
            Object descriptor, Date date, int count)
        throws RuntimeException
    {
        TreeSet<Object> result = new TreeSet<>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if (serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs = (MessageHistoryService)serv;

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findFirstMessagesAfter(
                            (MetaContact)descriptor,
                            date,
                            count));
                }
                else if(descriptor instanceof String)
                {
                    result.addAll(
                        mhs.findFirstMessagesAfter(
                            (String)descriptor,
                            date,
                            count));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findFirstMessagesAfter(
                            (ChatRoom)descriptor,
                            date,
                            count));
                }
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findFirstRecordsAfter(
                        (MetaContact)descriptor,
                        date,
                        count));
            }
        }
        LinkedList<Object> resultAsList = new LinkedList<>(result);

        int toIndex = count;
        if(toIndex > resultAsList.size())
            toIndex = resultAsList.size();

        return resultAsList.subList(0, toIndex);
    }

    /**
     * Returns the supplied number of recent records before the given date
     *
     * @param services the services classnames we will query
     * @param descriptor CallPeer address(String),
     *  MetaContact or ChatRoom.
     * @param date messages before date
     * @param count messages count
     * @return Collection sorted result that conists of records returned from
     *  the services we wrap
     * @throws RuntimeException
     */
    public Collection<Object> findLastMessagesBefore(String[] services,
            Object descriptor, Date date, int count)
        throws RuntimeException
    {
        TreeSet<Object> result = new TreeSet<>(new RecordsComparator());
        for (int i = 0; i < services.length; i++)
        {
            String name = services[i];
            Object serv = getService(name);
            if(serv instanceof MessageHistoryService)
            {
                MessageHistoryService mhs = (MessageHistoryService)serv;

                if(descriptor instanceof MetaContact)
                {
                    result.addAll(
                        mhs.findLastMessagesBefore(
                            (MetaContact)descriptor,
                            date,
                            count));
                }
                else if(descriptor instanceof String)
                {
                    result.addAll(
                        mhs.findLastMessagesBefore(
                            (String)descriptor,
                            date,
                            count));
                }
                else if(descriptor instanceof ChatRoom)
                {
                    result.addAll(
                        mhs.findLastMessagesBefore(
                            (ChatRoom)descriptor,
                            date,
                            count));
                }
            }
            else if(serv instanceof FileHistoryService
                    && descriptor instanceof MetaContact)
            {
                result.addAll(
                    ((FileHistoryService)serv).findLastRecordsBefore(
                        (MetaContact)descriptor,
                        date,
                        count));
            }
            else if(serv instanceof CallHistoryService)
            {
                CallHistoryService chs = (CallHistoryService)serv;

                Collection<CallRecord> col = chs.findByEndDate(date);
                if(col.size() > count)
                {
                    List<CallRecord> l = new LinkedList<>(col);
                    result.addAll(l.subList(l.size() - count, l.size()));
                }
                else
                    result.addAll(col);
            }
        }

        LinkedList<Object> resultAsList = new LinkedList<>(result);
        int startIndex = resultAsList.size() - count;

        if(startIndex < 0)
            startIndex = 0;

        return resultAsList.subList(startIndex, resultAsList.size());
    }

    private Object getService(String name)
    {
        Object serv = services.get(name);

        if (serv == null)
        {
            ServiceReference<?> refHistory = bundleContext.getServiceReference(name);

            serv = bundleContext.getService(refHistory);
        }

        return serv;
    }

    private boolean matchCallPeer(List<CallPeerRecord> cps, String keyword)
    {
        Iterator<CallPeerRecord> iter = cps.iterator();
        while (iter.hasNext())
        {
            boolean match = false;
            CallPeerRecord callPeer = iter.next();

            if (callPeer.getPeerAddress().toLowerCase().
                                               contains(keyword.toLowerCase()))
            {
                match = true;
            }
            else
            {
                match = false;
                break;
            }

            if (match)
            {
                return true;
            }
        }

        return false;
    }

    public void serviceChanged(ServiceEvent serviceEvent)
    {
        if(serviceEvent.getType() == ServiceEvent.UNREGISTERING)
        {
            Object sService = bundleContext.getService(
                serviceEvent.getServiceReference());

            services.remove(sService.getClass().getName());
        }
    }

    /**
     * starts the service.
     *
     * @param bc BundleContext
     */
    public void start(BundleContext bc)
    {
        sLog.debug("Starting the call history implementation.");
        this.bundleContext = bc;

        services.clear();

        // start listening for newly register or removed services
        bc.addServiceListener(this);
    }

    /**
     * stops the service.
     *
     * @param bc BundleContext
     */
    public void stop(BundleContext bc)
    {
        bc.removeServiceListener(this);
        services.clear();
    }

    /**
     * Used to compare various records to be ordered in TreeSet according
     * their timestamp. If the timestamps are identical, they will be ordered
     * according to their ID.
     */
    private static class RecordsComparator
        implements Comparator<Object>
    {
        private Date getDate(Object o)
        {
            Date date = new Date(0);
            if(o instanceof MessageEvent)
                date = ((MessageEvent)o).getTimestamp();
            else if(o instanceof CallRecord)
                date = ((CallRecord)o).getStartTime();
            else if(o instanceof FileRecord)
                date = ((FileRecord)o).getDate();
            else
                sLog.debug(
                    "Asked to compare objects of unknown type: " + o.getClass());

            return date;
        }

        private String getId(Object o)
        {
            String id = "";
            if(o instanceof MessageEvent)
                id = ((MessageEvent)o).getSourceMessage().getMessageUID();
            else if(o instanceof CallRecord)
                id = ((CallRecord)o).getCallPeerContactUID();
            else if(o instanceof FileRecord)
                id = ((FileRecord)o).getID();
            else
                sLog.debug(
                    "Asked to compare objects of unknown type: " + o.getClass());

            return id;
        }

        @Override
        public int compare(Object o1, Object o2)
        {
            Date date1 = getDate(o1);
            Date date2 = getDate(o2);

            int result = date1.compareTo(date2);

            // If we return 0 from this comparator, the meta history items
            // disappear from the results, as they are stored in a TreeSet.
            // To avoid this, compare their IDs instead.
            if (result == 0)
            {
                String id1 = getId(o1);
                String id2 = getId(o2);

                result = id1.compareTo(id2);
            }

            return result;
        }
    }
}
