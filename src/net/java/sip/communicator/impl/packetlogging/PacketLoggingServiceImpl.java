/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.packetlogging;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.service.protocol.Call;
import net.java.sip.communicator.service.protocol.CallConference;
import net.java.sip.communicator.util.*;

import org.apache.commons.collections4.QueueUtils;
import org.apache.commons.collections4.queue.CircularFifoQueue;

import org.jitsi.service.configuration.ConfigurationService;
import org.jitsi.service.packetlogging.*;

/**
 * Packet Logging Service implementation dumping logs in
 * pcap(tcpdump/wireshark) format file.
 *
 * @author Damian Minkov
 *
 * I tried to UT this class on 11/06/2020, and found it difficult. The problem
 * is that it's doing 2 things - one of its jobs is maintaining a queue of
 * packets and media to log, and running a separate thread to take items off the
 * queue and process them. The other responsibility is knowing how to take a
 * packet and turn it into a line in a .pcap file. With both of these features
 * intermingled it was difficult to UT. I would suggest that the best way to
 * proceed would be to split this into 2 classes - one for handling the queues,
 * and another for saving packets to file.
 */
public class PacketLoggingServiceImpl
    implements PacketLoggingService
{
    /**
     * Should we collect media data? Either all the time or if the user is
     * recording? If both of these are false, we don't show the option to send
     * it either. PROPERTY_CAPTURE_MEDIA results in recording all calls into a
     * media buffer and so must only ever be set on internal development
     * brandings for testing purposes.
     */
    public static final String PROPERTY_CAPTURE_MEDIA =
        "net.java.sip.communicator.impl.packetlogging.CAPTURE_MEDIA";

    public static final String PROPERTY_MANUAL_CAPTURE_MEDIA =
        "net.java.sip.communicator.impl.packetlogging.MANUAL_CAPTURE_MEDIA";

    public enum PCapLogType
    {
        jitsi,media
    }

    /**
     * Our Logger.
     */
    private static final Logger logger
            = Logger.getLogger(PacketLoggingServiceImpl.class);

    /**
     * The OutputStream we are currently writing to.
     */
    private Map<PCapLogType, FileOutputStream> outputStreams = new HashMap<>(2);

    /**
     * The thread that queues packets and saves them to file.
     */
    private SaverThread saverThread = new SaverThread("PacketSaverThread");

    /**
     * The current configuration.
     */
    private PacketLoggingConfiguration packetLoggingConfiguration = null;

    /**
     * A circular queue for storing media information
     *
     * 6000 for 2 mins (50/sec * 60 sec * 2 minutes)
     *
     * Note: This is used for both inbound and outbound media so this will be
     * one minute in each direction.
     */
    private Queue<Packet> mediaQueue =
        QueueUtils.synchronizedQueue(new CircularFifoQueue<>(6000));

    /**
     * The fake ethernet header we use as template.
     */
    private static final byte[] fakeEthernetHeader =
        new byte[]{
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x08, (byte)0x00
        };

    /**
     * The fake ipv4 header we use as template.
     */
    private static final byte[] ipHeaderTemplate =
        new byte[]{
                (byte)0x45, (byte)0x00,
                (byte)0x03, (byte)0x48, (byte)0xc9, (byte)0x14,
                (byte)0x00, (byte)0x00, (byte)0x35,(byte)0x11,
                (byte)0x00, (byte)0x00, // check sum
                (byte)0xd5, (byte)0xc0, (byte)0x3b, (byte)0x4b,//src
                (byte)0xc0, (byte)0xa8, (byte)0x00, (byte)0x34 //dst
        };

    /**
     * The fake ipv6 header we use as template.
     */
    private static final byte[] ip6HeaderTemplate =
        new byte[]{
                (byte)0x60, (byte)0x00, (byte)0x00, (byte)0x00, // version, traffic, flowable
                (byte)0x00, (byte)0x00, // length
                (byte)0x11, // next header
                (byte)0x40, // hop limit
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // src
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // src
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // src
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // src
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // dst
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // dst
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // dst
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00 // dst
        };

    /**
     * The fake udp header we use as template.
     */
    private static final byte[] udpHeaderTemplate =
        new byte[]{
                (byte)0x13, (byte)0xc4,
                (byte)0x13, (byte)0xc4,
                (byte)0x03, (byte)0x34,
                (byte)0x00, (byte)0x00// checksum
        };

    /**
     * The fake tcp header we use as template.
     */
    private static final byte[] tcpHeaderTemplate =
            new byte[]{
                (byte)0xb7, (byte)0x61, // src port
                (byte)0x13, (byte)0xc4, // dst port
                (byte)0x4f, (byte)0x20, (byte)0x37, (byte)0x3b, // seq number
                (byte)0x11, (byte)0x1d, (byte)0xbc, (byte)0x54, // ack number
                (byte)0x80, // header length
                (byte)0x18, // flags
                (byte)0x00, (byte)0x2e, // windows size
                (byte)0xac, (byte)0x78, // check sum
                (byte)0x00, (byte)0x00,
                (byte)0x01, (byte)0x01, (byte)0x08, (byte)0x0a, // options
                (byte)0x00, (byte)0x06, (byte)0xd4, (byte)0x48, // options
                (byte)0x6e, (byte)0xcc, (byte)0x76, (byte)0xbd  // options
            };

    /**
     * Using this object to lock and protectd the two counters
     * used for tcp seq and ack numbers.
     */
    private final Object tcpCounterLock = new Object();

    /**
     * The seq that the sender will send.
     */
    private long srcCount = 1;

    /**
     * This is the ack number send from the sender.
     */
    private long dstCount = 1;

    /**
     * A counter watching how much has been written to the file.
     */
    private Map<PCapLogType, Long> written = new HashMap<>(PCapLogType.values().length);

    /**
     * All the files we can use for writing.
     */
    private Map<PCapLogType, File[]> files = new HashMap<>(PCapLogType.values().length);

    /**
     * If the user is recording the call. Used to decide whether to capture media packets.
     */
    private boolean manualRecording = false;

    /**
     * The configuration service
     */
    private static final ConfigurationService configService = PacketLoggingActivator.getConfigurationService();

    /**
     * Whether or not to always capture media. This should never be set except on development
     * brandings for compliance reasons.
     */
    private static final boolean captureMedia = configService.global().getBoolean(PROPERTY_CAPTURE_MEDIA, false);

    /**
     * Whether or not to capture media while the user is recording.
     */
    private static final boolean captureMediaManual = configService.global().getBoolean(PROPERTY_MANUAL_CAPTURE_MEDIA, false);

    /**
     * Starting the packet logger. Generating the files we can use,
     * rotate any previous files and open the current file for writing.
     */
    public void start()
    {
        saverThread.start();
    }

    /**
     * Generates the files we will later use for writing.
     * @throws Exception
     */
    private void getFileNames(PCapLogType logType)
        throws Exception
    {
        int fileCount = getConfiguration().getLogfileCount();
        File[] localFiles = new File[fileCount];
        files.put(logType, localFiles);

        for(int i = 0; i < fileCount; i++)
        {
            localFiles[i]
                = PacketLoggingActivator.getFileAccessService().getPrivatePersistentFile(
                        PacketLoggingActivator.LOGGING_DIR_NAME
                            + File.separator
                            + logType.toString()
                            + i
                            + ".pcap");
        }
    }

    /**
     * Rotates any existing files and use the newly created first one
     * for writing.
     * @throws IOException
     */
    private OutputStream rotateFiles(PCapLogType logType)
        throws IOException
    {
        FileOutputStream outputStream = outputStreams.get(logType);

        if(outputStream != null)
        {
            outputStream.flush();
            outputStream.close();
        }

        File[] localFiles = files.get(logType);

        for (int i = getConfiguration().getLogfileCount() - 2; i >= 0; i--)
        {
            File f1 = localFiles[i];
            File f2 = localFiles[i+1];

            if (f1.exists())
            {
                if (f2.exists())
                    f2.delete();
                f1.renameTo(f2);
            }
        }

        outputStream = new FileOutputStream(localFiles[0]);
        outputStreams.put(logType, outputStream);
        written.put(logType, 0L);
        createGlobalHeader(outputStream);

        return outputStream;
    }

    /**
     * Stops the packet logging.
     */
    public void stop()
    {
        saverThread.stopRunning();

//        for (FileOutputStream outputStream : outputStreams.values()) //Handle
// closing the media stream ourselves as we might want to continue writing to
// it after the packet logging service is stopped???
        FileOutputStream outputStream = outputStreams.get(PCapLogType.jitsi);
        {
        if(outputStream != null)
        {
            try
            {
                outputStream.flush();
                outputStream.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
            finally
            {
                outputStream = null;
            }
        }
        }
    }

    /**
     * Creates pcap file global header.
     * @throws IOException
     */
    private void createGlobalHeader(OutputStream outputStream)
            throws IOException
    {
        /* magic number(swapped) */
        outputStream.write(0xd4);
        outputStream.write(0xc3);
        outputStream.write(0xb2);
        outputStream.write(0xa1);

        /* major version number */
        outputStream.write(0x02);
        outputStream.write(0x00);

        /* minor version number */
        outputStream.write(0x04);
        outputStream.write(0x00);

        /* GMT to local correction */
        outputStream.write(0x00);
        outputStream.write(0x00);
        outputStream.write(0x00);
        outputStream.write(0x00);

        /* accuracy of timestamps */
        outputStream.write(0x00);
        outputStream.write(0x00);
        outputStream.write(0x00);
        outputStream.write(0x00);

        /* max length of captured packets, in octets */
        outputStream.write(0xff);
        outputStream.write(0xff);
        outputStream.write(0x00);
        outputStream.write(0x00);

        /* data link type(ethernet) */
        outputStream.write(0x01);
        outputStream.write(0x00);
        outputStream.write(0x00);
        outputStream.write(0x00);
    }

    /**
     * Checks is logging globally enabled for and is it currently available for
     * the given service.
     *
     * @param protocol that is checked.
     * @return is logging enabled.
     */
    @Override
    public boolean isLoggingEnabled(ProtocolName protocol)
    {
        PacketLoggingConfiguration cfg = getConfiguration();

        if (cfg.isGlobalLoggingEnabled())
        {
            switch(protocol)
            {
                case SIP:
                    return cfg.isSipLoggingEnabled();
                case JABBER:
                    return cfg.isJabberLoggingEnabled();
                // isRTPLoggingEnabled always returns True. Whether we log
                // packets is actually controlled by branding options
                // through allowedToCaptureMedia or allowedToWriteMedia.
                case RTP:
                    return cfg.isRTPLoggingEnabled();
                default:
                    /*
                     * It may seem like it was unnecessary to invoke
                     * getConfiguration and isGlobalLoggingEnabled prior to
                     * checking that the specified protocol is supported but,
                     * actually, there are no other ProtocolName values.
                     */
                    return false;
            }
        }
        else
            return false;
    }

    /**
     * GUI lets us know the user has started recording.
     */
    public void startRecording()
    {
        manualRecording = true;
    }

    /**
     * GUI lets us know the user has stopped recording.
     */
    public void stopRecording()
    {
        manualRecording = false;
    }

    /**
     * Log a packet with all the required information.
     *
     * @param protocol the source protocol that logs this packet.
     * @param sourceAddress the source address of the packet.
     * @param sourcePort the source port of the packet.
     * @param destinationAddress the destination address.
     * @param destinationPort the destination port.
     * @param transport the transport this packet uses.
     * @param sender are we the sender of the packet or not.
     * @param packetContent the packet content.
     */
    @Override
    public void logPacket(
            ProtocolName protocol,
            byte[] sourceAddress, int sourcePort,
            byte[] destinationAddress, int destinationPort,
            TransportName transport,
            boolean sender,
            byte[] packetContent)
    {
        this.logPacket(protocol, sourceAddress, sourcePort,
                destinationAddress, destinationPort,
                transport, sender,
                packetContent, 0, packetContent.length);
    }

    /**
     * Log a packet with all the required information.
     *
     * @param protocol the source protocol that logs this packet.
     * @param sourceAddress the source address of the packet.
     * @param sourcePort the source port of the packet.
     * @param destinationAddress the destination address.
     * @param destinationPort the destination port.
     * @param transport the transport this packet uses.
     * @param sender are we the sender of the packet or not.
     * @param packetContent the packet content.
     * @param packetOffset the packet content offset.
     * @param packetLength the packet content length.
     */
    @Override
    public void logPacket(
            ProtocolName protocol,
            byte[] sourceAddress,
            int sourcePort,
            byte[] destinationAddress,
            int destinationPort,
            TransportName transport,
            boolean sender,
            byte[] packetContent,
            int packetOffset,
            int packetLength)
    {
        if (packetContent != null)
        {
            // Sometimes this method gets passed duff data (for example if we
            // fail to parse a packet). Be defensive and don't save the packet
            // if there's no data.
            saverThread.queuePacket(
                new Packet(protocol,
                           sourceAddress,
                           sourcePort,
                           destinationAddress,
                           destinationPort,
                           transport,
                           sender,
                           packetContent,
                           packetOffset,
                           packetLength,
                           System.currentTimeMillis()));
        }
    }

    /**
     * Returns the current Packet Logging Configuration.
     *
     * @return the Packet Logging Configuration.
     */
    @Override
    public PacketLoggingConfiguration getConfiguration()
    {
        if(packetLoggingConfiguration == null)
            packetLoggingConfiguration = new PacketLoggingConfigurationImpl();

        return packetLoggingConfiguration;
    }

    /**
     * Dump the packet to the output file stream.
     *
     * @param packet the packet to save.
     * @throws Exception when error occurs saving to file stream or when
     *  rotating files.
     */
    private void savePacket(Packet packet, PCapLogType logType)
        throws Exception
    {
        byte[] sourceAddress = packet.sourceAddress;
        byte[] destinationAddress = packet.destinationAddress;

        if ((sourceAddress == null) || (destinationAddress == null))
        {
            logger.warn("Unable to log packet with null address (source = " +
                sourceAddress + ", destination = " + destinationAddress + ").");
            return;
        }

        // if one of the addresses is ipv4 we are using ipv4,
        // local udp addresses come as 0.0.0.0.0....0.0.0 when
        // ipv6 is enabled in the underlying os
        boolean isIPv4 = sourceAddress.length == 4 ||
                         destinationAddress.length == 4;

        byte[] ipHeader;

        if(isIPv4)
        {
            ipHeader = new byte[ipHeaderTemplate.length];
            System.arraycopy(
                    ipHeaderTemplate, 0, ipHeader, 0, ipHeader.length);
            System.arraycopy(sourceAddress,
                    0,
                    ipHeader,
                    12,
                    4);
            System.arraycopy(destinationAddress,
                    0,
                    ipHeader,
                    16,
                    4);
        }
        else
        {
            ipHeader = new byte[ip6HeaderTemplate.length];
            System.arraycopy(
                    ip6HeaderTemplate, 0, ipHeader, 0, ipHeader.length);
            System.arraycopy(sourceAddress,
                    0,
                    ipHeader,
                    8,
                    16);

            System.arraycopy(destinationAddress,
                    0,
                    ipHeader,
                    24,
                    16);
        }

        byte[] transportHeader;
        short len;
        if(packet.transport == TransportName.UDP)
        {
            byte[] udpHeader = new byte[udpHeaderTemplate.length];
            transportHeader = udpHeader;
            System.arraycopy(udpHeaderTemplate, 0,
                    udpHeader, 0, udpHeader.length);

            writeShort(packet.sourcePort, udpHeader, 0);
            writeShort(packet.destinationPort, udpHeader, 2);
            len = (short)(packet.packetLength + udpHeader.length);
            writeShort(len, udpHeader, 4);
        }
        else
        {
            transportHeader = new byte[tcpHeaderTemplate.length];
            System.arraycopy(tcpHeaderTemplate, 0, transportHeader,
                   0, transportHeader.length);

            writeShort(packet.sourcePort, transportHeader, 0);
            writeShort(packet.destinationPort, transportHeader, 2);

            len = (short)(packet.packetLength + transportHeader.length);

            if(packet.sender)
            {
                long seqnum;
                long acknum;
                synchronized(tcpCounterLock)
                {
                    seqnum = srcCount;
                    srcCount += packet.packetLength;
                    acknum = dstCount;
                }

                intToBytes((int)(seqnum & 0xffffffff),
                       transportHeader, 4);
                intToBytes((int)(acknum & 0xffffffff),
                       transportHeader, 8);
            }
            else
            {
                long seqnum;
                long acknum;
                synchronized(tcpCounterLock)
                {
                    seqnum = dstCount;
                    dstCount += packet.packetLength;
                    acknum = srcCount;
                }

                intToBytes((int)(seqnum & 0xffffffff),
                       transportHeader, 4);
                intToBytes((int)(acknum & 0xffffffff),
                       transportHeader, 8);
            }
        }

        // now set ip header total length
        if(isIPv4)
        {
            short ipTotalLen = (short)(len + ipHeader.length);
            writeShort(ipTotalLen, ipHeader, 2);

            if(packet.transport == TransportName.UDP)
                ipHeader[9] = (byte)0x11;
            else
                ipHeader[9] = (byte)0x06;

           int chk2 = computeChecksum(ipHeader);
           ipHeader[10] = (byte) (chk2 >> 8);
           ipHeader[11] = (byte) (chk2 & 0xff);
        }
        else
        {
            writeShort(len, ipHeader, 4);

            if(packet.transport == TransportName.UDP)
                ipHeader[6] = (byte)0x11;
            else
                ipHeader[6] = (byte)0x06;
        }

        long current = packet.timestamp;
        int tsSec = (int)(current/1000);
        int tsUsec = (int)((current%1000) * 1000);
        int feakHeaderLen = fakeEthernetHeader.length +
                ipHeader.length + transportHeader.length;
        int inclLen = packet.packetLength + feakHeaderLen;
        int origLen = inclLen;

        synchronized(this)
        {
            // open files only if needed
            OutputStream outputStream = outputStreams.get(logType);

            if (outputStream == null)
            {
                getFileNames(logType);
                outputStream = rotateFiles(logType);// this one opens the file for write
            }

            long limit = getConfiguration().getLimit();

            if((limit > 0) && (written.get(logType) > limit))
            {
                outputStream = rotateFiles(logType);
            }

            addInt(tsSec, outputStream);
            addInt(tsUsec, outputStream);
            addInt(inclLen, outputStream);
            addInt(origLen, outputStream);

            outputStream.write(fakeEthernetHeader);
            outputStream.write(ipHeader);
            outputStream.write(transportHeader);
            outputStream.write(
                    packet.packetContent,
                    packet.packetOffset,
                    packet.packetLength);
            outputStream.flush();

            written.put(logType, written.get(logType) + inclLen + 16);
        }
    }

    /**
     * Writes int to the file. Used for packet headers.
     * @param d the value to write.
     * @throws IOException
     */
    private void addInt(int d, OutputStream outputStream)
        throws IOException
    {
        outputStream.write ((d & 0xff));
        outputStream.write(((d & 0xff00) >> 8));
        outputStream.write(((d & 0xff0000) >> 16));
        outputStream.write(((d & 0xff000000) >> 24));
    }

    /**
     * Converts a 32-bit word representation of an IPv4 address to a
     * byte array.
     *
     * @param address The 32-bit word representation of the IPv4 address.
     * @param data The byte array in which to store the IPv4 data.
     * @param offset The offset into the array where the data start.
     */
    private static void intToBytes(int address, byte[] data,
                                   int offset)
    {
        data[offset] = (byte)(0xff & (address >>> 24));
        data[offset + 1] = (byte)(0xff & (address >>> 16));
        data[offset + 2] = (byte)(0xff & (address >>> 8));
        data[offset + 3] = (byte)(0xff & address);
    }

    /**
     * Puts the short value to the array.
     * @param value value to convert to bytes.
     * @param data destination data
     * @param offset offset in the data
     */
    private static void writeShort(int value, byte[] data, int offset)
    {
        data[offset] = (byte) (value >> 8);
        data[offset + 1] = (byte) value;
    }

    /**
     * Calculates checksums assuming the checksum is a 16-bit header field.
     */
    private int computeChecksum(byte[] data)
    {
        int total = 0;
        int i = 0;

        // Don't Skip existing checksum cause its set to 0000
        int imax = data.length - (data.length % 2);

        while(i < imax)
            total+=(((data[i++] & 0xff) << 8) | (data[i++] & 0xff));

        if(i < data.length)
            total+=((data[i] & 0xff) << 8);

        // Fold to 16 bits
        while((total & 0xffff0000) != 0)
            total = (total & 0xffff) + (total >>> 16);

        total = (~total & 0xffff);

        return total;
    }

    /**
     * The data we receive and that we will dump in a file.
     */
    private static class Packet
    {
        /**
         * The source address of the packet.
         */
        byte[] sourceAddress;

        /**
         * The source port of the packet.
         */
        int sourcePort;

        /**
         * The destination address of the packet.
         */
        byte[] destinationAddress;

        /**
         * The destination port of the packet.
         */
        int destinationPort;

        /**
         * Is the packet a udp one.
         */
        TransportName transport;

        /**
         * Are we sending the packet, or false if we are receiving.
         */
        boolean sender;

        /**
         * Array containing packet content.
         */
        byte[] packetContent;

        /**
         * The offset in the packetContent where packet content is.
         */
        int packetOffset;

        /**
         * The length of the packet content.
         */
        int packetLength;

        /**
         * Timestamp
         */
        long timestamp;

        /**
         * Creates a packet with the needed data.
         * @param protocol the source protocol that logs this packet.
         * @param sourceAddress The source address of the packet.
         * @param sourcePort The source port of the packet.
         * @param destinationAddress The destination address of the packet.
         * @param destinationPort The destination port of the packet.
         * @param transport the transport this packet uses.
         * @param sender Are we sending the packet,
         *  or false if we are receiving.
         * @param packetContent Array containing packet content.
         * @param packetOffset The offset in the packetContent
         *  where packet content is.
         * @param packetLength The length of the packet content.
         */
        private Packet(ProtocolName protocol,
                       byte[] sourceAddress,
                       int sourcePort,
                       byte[] destinationAddress,
                       int destinationPort,
                       TransportName transport,
                       boolean sender,
                       byte[] packetContent,
                       int packetOffset,
                       int packetLength,
                       long timestamp)
        {
            this.sourceAddress = sourceAddress;
            this.sourcePort = sourcePort;
            this.destinationAddress = destinationAddress;
            this.destinationPort = destinationPort;
            this.transport = transport;
            this.sender = sender;
            this.packetContent = packetContent;
            this.packetOffset = packetOffset;
            this.packetLength = packetLength;
            this.timestamp = timestamp;
        }
    }

    /**
     * Dumps packet in separate thread so we don't block
     * our calling thread.
     */
    private class SaverThread
        extends Thread
    {
        /**
         * start/stop indicator.
         */
        private boolean stopped = true;

        /**
         * List of packets queued to be written in the file.
         */
        private final List<Packet> pktsToSave = new ArrayList<>();

        public SaverThread(String name)
        {
            super(name);
        }

        /**
         * Sends instant messages in separate thread so we don't block
         * our calling thread.
         */
        public void run()
        {
            stopped = false;
            while(!stopped)
            {
                Packet pktToSave;

                synchronized(this)
                {
                    if(pktsToSave.isEmpty())
                    {
                        try
                        {
                            wait();
                        }
                        catch (InterruptedException iex)
                        {
                        }
                        continue;
                    }

                    pktToSave = pktsToSave.remove(0);
                }

                if(pktToSave != null)
                {
                    try
                    {
                        savePacket(pktToSave, PCapLogType.jitsi);
                    }
                    catch(Throwable t)
                    {
                        /*
                         * XXX ThreadDeath must be rethrown; otherwise, the
                         * related Thread will not die.
                         */
                        if (t instanceof ThreadDeath)
                            throw (ThreadDeath) t;
                        else
                            logger.error("Error writing packet to file", t);
                    }
                }
            }
        }

        /**
         * Interrupts this sender so that it would no longer send messages.
         */
        public synchronized void stopRunning()
        {
            stopped = true;
            notifyAll();
        }

        /**
         * Schedule new packet for save.
         * @param packet new packet to save.
         */
        public synchronized void queuePacket(Packet packet)
        {
            pktsToSave.add(packet);
            notifyAll();
        }
    }

    /**
     * @return should we buffer and store media packets?
     */
    @Override
    public boolean allowedToCaptureMedia()
    {
        boolean rc = false;

        // If we should always capture media packets then return true.
        if (captureMedia)
        {
            rc = true;
        }
        // If we're only buffering while the user is recording then only do so when there's only one
        // conference.
        // When we decide whether or not to buffer the media packet we don't know which call the packet
        // is from and if there's multiple calls ongoing it's entirely reasonable that one is being
        // recorded and the other isn't. There's no sensible way to tie a packet back to a specific call
        // and so for safety we don't record any packets in this instance. We check distinct conferences
        // rather than distinct calls so we can still capture media in three-way call scenarios.
        else if (captureMediaManual && manualRecording)
        {
            Collection<Call> inProgressCalls = PacketLoggingActivator.getUIService().getInProgressCalls();
            Set<CallConference> inProgressConferences = new HashSet<>();
            for (Call inProgressCall : inProgressCalls)
            {
                inProgressConferences.add(inProgressCall.getConference());
            }

            if (inProgressConferences.size() < 2)
            {
                rc = true;
            }
        }

        return rc;
    }

    /**
     * @return should we dump any captured media packets?
     */
    public boolean allowedToWriteMedia()
    {
        return captureMedia || captureMediaManual;
    }

    @Override
    public void dumpMediaBuffer()
    {
        if (allowedToWriteMedia())
        {
            try
            {
                Object[] mediaQueueArray = mediaQueue.toArray();

                for (Object aBuf : mediaQueueArray)
                {
                    Packet packet = (Packet) aBuf;
                    savePacket(packet, PCapLogType.media);
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            FileOutputStream os = outputStreams.get(PCapLogType.media);
            if (os != null)
            {
                try
                {
                    os.flush();
                    os.close();
                    outputStreams.remove(PCapLogType.media);
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                os = null;
            }

            mediaQueue.clear();
        }
    }

    /**
     * Buffer some sent/received media into our local buffer to be written out
     * if the user submits the media as part of a problem report.
     *
     * @param packetContent The contents of the packet to buffer
     * @param timeStamp The send/receive timestamp of the packet
     * @param sourceAddress The IP address the packet was sent from
     * @param sourcePort The port the packet was sent from
     * @param destinationAddress The IP address the packet was sent to
     * @param destinationPort The port the packet was sent to
     */
    @SuppressWarnings("unchecked")
    @Override
    public void bufferMedia(byte[] packetContent,
                            long timeStamp,
                            byte[] sourceAddress,
                            int sourcePort,
                            byte[] destinationAddress,
                            int destinationPort)
    {
        if (allowedToCaptureMedia())
        {
            Packet packet =
                new Packet(ProtocolName.RTP,
                           sourceAddress,
                           sourcePort,
                           destinationAddress,
                           destinationPort,
                           TransportName.UDP,
                           false,
                           packetContent,
                           0,
                           packetContent.length,
                           timeStamp);
            mediaQueue.add(packet);
        }
    }
}
