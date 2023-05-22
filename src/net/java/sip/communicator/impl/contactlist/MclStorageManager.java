/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.contactlist;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseDirectoryNumber;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.contactlist.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.util.xml.*;
import org.osgi.framework.*;
import org.w3c.dom.*;

/**
 * The class handles read / write operations over the file where a persistent
 * copy of the meta contact list is stored.
 * <p>
 * The load / resolve strategy that we use when storing contact lists is roughly
 * the following:
 * <p>
 * 1) The MetaContactListService is started. <br>
 * 2) If no file exists for the meta contact list, create one. <br>
 * 3) We receive an OSGI event telling us that a new ProtocolProviderService is
 * registered or we simply retrieve one that was already in the bundle <br>
 * 4) We look through the contact list file and load groups and contacts
 * belonging to this new provider. Unresolved proto groups and contacts will be
 * created for every one of them.
 * <p>
 *
 * @author Emil Ivov
 */
public class MclStorageManager
    implements MetaContactListListener
{
    private static final Logger logger
        = Logger.getLogger(MclStorageManager.class);

    private static final ContactLogger contactLogger = ContactLogger.getLogger();

    /**
     * Indicates whether the storage manager has been properly started or in
     * other words that it has successfully found and read the xml contact list
     * file.
     */
    private boolean started = false;

    /**
     * Indicates whether there has been a change since the last time we stored
     * this contact list. Used by the storage methods.
     */
    private boolean isModified = false;

    /**
     * A currently valid reference to the OSGI bundle context,
     */
    private BundleContext bundleContext = null;

    /**
     * The XML Document containing the contact list file.
     */
    private Document contactListDocument = null;

    /**
     * A reference to the file containing the locally stored meta contact list.
     */
    private File contactlistFile = null;

    /**
     * A reference to the failsafe transaction used with the contactlist file.
     */
    private TransactionBasedFile contactlistTrans = null;

    /**
     * A reference to the MetaContactListServiceImpl that created and started
     * us.
     */
    private MetaContactListServiceImpl mclServiceImpl = null;

    /**
     * The name of the system property that stores the name of the contact list
     * file.
     */
    private static final String DEFAULT_FILE_NAME = "contactlist.xml";

    /**
     * The name of the node that represents the contact list root.
     */
    private static String DOCUMENT_ROOT_NAME = "sip-communicator";

    /**
     * The name of the XML node corresponding to a meta contact group.
     */
    private static final String GROUP_NODE_NAME = "group";

    /**
     * The name of the XML node corresponding to a collection of meta contact
     * subgroups.
     */
    private static final String SUBGROUPS_NODE_NAME = "subgroups";

    /**
     * The name of the XML attribute that contains group names.
     */
    private static final String GROUP_NAME_ATTR_NAME = "name";

    /**
     * The name of the XML attribute that contains group UIDs.
     */
    private static final String GROUP_UID_ATTR_NAME = "uid";

    /**
     * The name of the XML node that contains protocol specific group
     * descriptorS.
     */
    private static final String PROTO_GROUPS_NODE_NAME = "proto-groups";

    /**
     * The name of the XML node that contains A protocol specific group
     * descriptor.
     */
    private static final String PROTO_GROUP_NODE_NAME = "proto-group";

    /**
     * The name of the XML attribute that contains unique identifiers
     */
    private static final String UID_ATTR_NAME = "uid";

    /**
     * The name of the XML attribute that contains unique identifiers for parent
     * contact groups.
     */
    private static final String PARENT_PROTO_GROUP_UID_ATTR_NAME =
        "parent-proto-group-uid";

    /**
     * The name of the XML attribute that contains account identifiers
     * indicating proto group's and proto contacts' owning providers.
     */
    private static final String ACCOUNT_ID_ATTR_NAME = "account-id";

    /**
     * The name of the XML node that contains meta contact details.
     */
    private static final String META_CONTACT_NODE_NAME = "meta-contact";

    /**
     * The name of the XML node that contains meta contact display names.
     */
    private static final String META_CONTACT_DISPLAY_NAME_NODE_NAME =
        "display-name";

    /**
     * The name of the XML attribute that contains true/false, whether
     * this meta contact was renamed by user.
     */
    private static final String USER_DEFINED_DISPLAY_NAME_ATTR_NAME =
        "user-defined";

    /**
     * The name of the XML node that contains meta contact detail.
     */
    private static final String META_CONTACT_DETAIL_NAME_NODE_NAME = "detail";

    /**
     * The name of the XML attribute that contains detail name.
     */
    private static final String DETAIL_NAME_ATTR_NAME = "name";

    /**
     * The name of the XML attribute that contains detail value.
     */
    private static final String DETAIL_VALUE_ATTR_NAME = "value";

    /**
     * The name of the XML node that contains information of a proto contact
     */
    private static final String PROTO_CONTACT_NODE_NAME = "contact";

    /**
     * The name of the XML attribute that contains address information of a proto contact
     */
    private static final String PROTO_CONTACT_ADDRESS_ATTR_NAME = "address";

    /**
     * The name of the XML attribute that contains the display name of a proto contact
     */
    private static final String PROTO_CONTACT_DISPLAY_ATTR_NAME = "display-name";

    /**
     * The name of the XML node that contains information that contacts or
     * groups returned as persistent and that should be used when restoring a
     * contact or a group.
     */
    private static final String PERSISTENT_DATA_NODE_NAME = "persistent-data";

    /**
     * The name of the XML node that contains all meta contact nodes inside a
     * group
     */
    private static final String CHILD_CONTACTS_NODE_NAME = "child-contacts";

    /**
     * A lock that we use when storing the contact list to avoid being exited
     * while in there.
     */
    private static final Object contactListRWLock = new Object();

    /**
     * Determines whether the storage manager has been properly started or in
     * other words that it has successfully found and read the xml contact list
     * file.
     *
     * @return true if the storage manager has been successfully initialized and
     *         false otherwise.
     */
    boolean isStarted()
    {
        return started;
    }

    /**
     * Prepares the storage manager for shutdown.
     */
    public void stop()
    {
        logger.info("Stopping the MCL XML storage manager.");

        this.started = false;
        synchronized (contactListRWLock)
        {
            contactListRWLock.notifyAll();
        }
    }

    /**
     * Initializes the storage manager and makes it do initial load and parsing
     * of the contact list file.
     *
     * @param bc a reference to the currently valid OSGI <tt>BundleContext</tt>
     * @param mclServImpl a reference to the currently valid instance of the
     *            <tt>MetaContactListServiceImpl</tt> that we could use to pass
     *            parsed contacts and contact groups.
     * @throws IOException if the contact list file specified file does not
     *             exist and could not be created.
     */
    void start(BundleContext bc, MetaContactListServiceImpl mclServImpl)
        throws IOException
    {
        bundleContext = bc;

        ConfigurationService configurationService =
                        ServiceUtils.getService(bc, ConfigurationService.class);

        String fileName = configurationService.user().getScHomeDirLocation() +
                          File.separator +
                          configurationService.user().getScHomeDirName() +
                          File.separator +
                          DEFAULT_FILE_NAME;

        // get a reference to the contact list file.
        try
        {
            logger.info("Using contact list file name " + DEFAULT_FILE_NAME);
            contactlistFile = new File(fileName);

            if (!contactlistFile.exists() && !contactlistFile.createNewFile())
                throw new IOException("Failed to create file"
                                          + contactlistFile.getName());
        }
        catch (Exception ex)
        {
            logger.error("Failed to get a reference to the contact list file.",
                ex);
            throw new IOException("Failed to get a reference to the contact "
                + "list file=" + DEFAULT_FILE_NAME + ". error was:" + ex.getMessage());
        }

        contactlistTrans = new TransactionBasedFile(contactlistFile);

        try
        {
            // load the contact list
            DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            if (contactlistFile.length() == 0)
            {
                // if the contact list does not exist - create it.
                logger.error("Creating new contact list object " + DEFAULT_FILE_NAME);
                contactListDocument = builder.newDocument();
                initVirginDocument(mclServImpl, contactListDocument);

                // write the contact list so that it is there for the parser
                storeContactList0();
            }
            else
            {
                try
                {
                    Document rawParsedDocument = builder.parse(contactlistFile);
                    // Remove all the indentation and newlines added to make
                    // the file look nice - otherwise they'll all be written
                    // out again as if they are real content, and themselves
                    // be indented - and so on until the file is full of
                    // whitespace
                    XMLUtils.cleanupWhitespace(rawParsedDocument);
                    contactListDocument = rawParsedDocument;
                }
                catch (Throwable ex)
                {
                    logger.error("Error parsing configuration file", ex);
                    logger.error("Creating replacement file");
                    logContactListRecreation();

                    // re-create and re-init the new document
                    contactlistFile.delete();
                    contactlistFile.createNewFile();
                    contactListDocument = builder.newDocument();
                    initVirginDocument(mclServImpl, contactListDocument);

                    // write the contact list so that it is there for the parser
                    storeContactList0();
                }
            }
        }
        catch (ParserConfigurationException ex)
        {
            // it is not highly probable that this might happen - so lets just
            // log it.
            logger.error("Error finding configuration for default parsers", ex);
        }

        mclServImpl.addMetaContactListListener(this);
        this.mclServiceImpl = mclServImpl;
        started = true;
        this.launchStorageThread();
    }

    /**
     * Stores the contact list in its current state.
     *
     */
    private void scheduleContactListStorage()
    {
        synchronized (contactListRWLock)
        {
            if (!isStarted())
                return;

            this.isModified = true;
        }
    }

    /**
     * Writes the contact list on the hard disk.
     *
     * @throws IOException in case writing fails.
     */
    private void storeContactList0() throws IOException
    {
        logger.trace("storing contact list. because is started =="
            + isStarted());
        logger.trace("storing contact list. because is modified =="
            + isModified);
        if (isStarted())
        {
            // begin a new transaction
            synchronized (contactlistTrans)
            {
                try
                {
                    contactlistTrans.beginTransaction();
                }
                catch (IllegalStateException e)
                {
                    logger.error("the contactlist file is still being changed", e);
                }

                // really write the modification
                OutputStream stream = contactlistTrans.getOutputStream();
                XMLUtils.indentedWriteXML(contactListDocument, stream);
                stream.close();

                // commit the changes
                try
                {
                    contactlistTrans.commitTransaction(false);
                }
                catch (IllegalStateException e)
                {
                    logger.error("the contactlist file is not edited", e);
                }
            }
        }
    }

    /**
     * Launches a separate thread that waits on the contact list rw lock and
     * when notified stores the contact list in case there have been
     * modifications since last time it saved.
     */
    private void launchStorageThread()
    {
        new Thread("MCLStorageThread")
        {
            public void run()
            {
                try
                {
                    synchronized (contactListRWLock)
                    {
                        while (isStarted())
                        {
                            contactListRWLock.wait(5000);
                            if (isModified)
                            {
                                storeContactList0();
                                isModified = false;
                            }
                        }
                    }
                }
                catch (IOException | InterruptedException ex)
                {
                    logger.error("Storing contact list failed", ex);
                    started = false;
                }
            }
        }.start();
    }

    /**
     * Stops the storage manager and performs a final write
     */
    public void storeContactListAndStopStorageManager()
    {
        synchronized (contactListRWLock)
        {
            logger.info("Storing contact list and stopping");

            if (!isStarted())
                return;

            started = false;

            // make sure everyone gets released after we finish.
            contactListRWLock.notifyAll();

            // write the contact list ourselves before we go out..
            try
            {
                storeContactList0();
            }
            catch (IOException ex)
            {
                logger.error("Failed to store contact list before stopping", ex);
            }
        }
    }

    /**
     * update persistent data in the dom object model for the given metacontact
     * and its contacts
     *
     * @param metaContact MetaContact target meta contact
     */
    private void updatePersistentDataForMetaContact(MetaContact metaContact)
    {
        contactLogger.note(metaContact, "Updating persistent data");
        Element metaContactNode = findMetaContactNode(metaContact.getMetaUID());

        Iterator<Contact> iter = metaContact.getContacts();
        while (iter.hasNext())
        {
            Contact item = iter.next();
            contactLogger.note(metaContact, "Updating node for " + item);
            String persistentData = item.getPersistentData();

            /*
             * TODO If persistentData is null and persistentDataNode exists and
             * contains non-null text, will persistentDataNode be left without
             * updating?
             */
            if (persistentData != null)
            {
                Element currentNode =
                    XMLUtils.locateElement(metaContactNode,
                        PROTO_CONTACT_NODE_NAME,
                        PROTO_CONTACT_ADDRESS_ATTR_NAME, item.getAddress());

                if (currentNode == null)
                {
                    // There's no node for this contact, so there's no data to
                    // update.  It's probably because the contact has been
                    // removed but the MetaContact hasn't yet been updated
                    contactLogger.warn(metaContact,
                                     "Unable to find contact node for " + item);
                    continue;
                }

                Element persistentDataNode =
                    XMLUtils.findChild(currentNode, PERSISTENT_DATA_NODE_NAME);

                // if node does not exist - create it
                if (persistentDataNode == null)
                {
                    persistentDataNode =
                        contactListDocument
                            .createElement(PERSISTENT_DATA_NODE_NAME);

                    currentNode.appendChild(persistentDataNode);
                }

                XMLUtils.setText(persistentDataNode, persistentData);
            }
        }
    }

    /**
     * Fills the document with the tags necessary for it to be filled properly
     * as the meta contact list evolves.
     *
     * @param mclServImpl the meta contact list service to use when
     *            initializing the document.
     * @param contactListDoc the document to init.
     */
    private void initVirginDocument(MetaContactListServiceImpl mclServImpl,
        Document contactListDoc)
    {
        logger.warn("Creating virgin contactlist.xml document.");
        Element root = contactListDoc.createElement(DOCUMENT_ROOT_NAME);

        contactListDoc.appendChild(root);

        // create the rootGroup
        Element rootGroup =
            createMetaContactGroupNode(mclServImpl.getRoot());

        root.appendChild(rootGroup);
    }

    /**
     * Parses the contact list file and calls corresponding "add" methods
     * belonging to <tt>mclServiceImpl</tt> for every meta contact and meta
     * contact group stored in the (contactlist.xml) file that correspond to a
     * provider caring the specified <tt>accountID</tt>.
     *
     * @param accountID the identifier of the account whose contacts we're
     *            interested in.
     * @throws XMLException if a problem occurs while parsing contact list
     *             contents.
     */
    void extractContactsForAccount(String accountID) throws XMLException
    {
        logger.info("Extracting contacts for " +
                    sanitiseDirectoryNumber(accountID));
        if (!isStarted())
            return;

        try
        {
            Element root = findMetaContactGroupNode(
                                     mclServiceImpl.getRoot().getMetaUID());

            if (root == null)
            {
                // If there is no root, there is definitely something wrong
                // really broken file will create it again
                logger.fatal("Recreating contactlist file as it is broken " +
                                         mclServiceImpl.getRoot().getMetaUID());
                logContactListRecreation();

                DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                contactListDocument = builder.newDocument();

                initVirginDocument(mclServiceImpl, contactListDocument);

                // write the contact list so that it is there for the parser
                storeContactList0();
            }
            else
            {
                // if there is root lets parse it
                // parse the group node and extract all its child groups and
                // contacts
                processGroupXmlNode(
                               mclServiceImpl, accountID, root, null, null);

                // now save the contact list in case it has changed
                scheduleContactListStorage();
            }
        }
        catch (Throwable e)
        {
            // catch everything because we MUST NOT disturb the thread
            // initializing the meta CL for a new provider with null point
            // exceptions or others of the sort
            logger.error("Exception loading contacts for account "
                        + sanitiseDirectoryNumber(accountID), e);
            throw new XMLException("Failed to extract contacts for account "
                        + sanitiseDirectoryNumber(accountID), e);
        }
    }

    /**
     * Create the diagnostic and log events for handling the case that we've had
     * to recreate the contact list file
     */
    private void logContactListRecreation()
    {
        logger.error("Logging contact list recreation", new Exception());

        // Only thing we can do is to copy the old contactlist.xml and log the
        // error.
        if (contactlistFile != null)
        {
            logger.info("Copying old contact file");
            File dest = new File(contactlistFile.getAbsolutePath() + ".copy");
            try
            {
                FileUtils.copyFile(contactlistFile, dest);
            }
            catch (IOException e)
            {
                logger.error("Unable to copy contact list file", e);
            }
        }
        else
        {
            logger.error("Old contact file did not exist");
        }
    }

    /**
     * Parses <tt>groupNode</tt> and all of its subnodes, creating corresponding
     * instances through <tt>mclServiceImpl</tt> as children of
     * <tt>parentGroup</tt>
     *
     * @param mclServImpl the <tt>MetaContactListServiceImpl</tt> for
     *            creating new contacts and groups.
     * @param accountID a String identifier of the account whose contacts we're
     *            interested in.
     * @param groupNode the XML <tt>Element</tt> that points to the group we're
     *            currently parsing.
     * @param parentGroup the <tt>MetaContactGroupImpl</tt> where we should be
     *            creating children.
     * @param parentProtoGroups a Map containing all proto groups that could be
     *            parents of any groups parsed from the specified groupNode. The
     *            map binds UIDs to group references and may be null for top
     *            level groups.
     */
    private void processGroupXmlNode(MetaContactListServiceImpl mclServImpl,
        String accountID, Element groupNode, MetaContactGroupImpl parentGroup,
        Map<String, ContactGroup> parentProtoGroups)
    {
        // first resolve the group itself.(unless this is the meta contact list
        // root which is already resolved)
        MetaContactGroupImpl currentMetaGroup;

        // in this map we store all proto groups that we find in this meta group
        // (unless this is the MCL root)in order to pass them as parent
        // references to any subgroups.
        Map<String, ContactGroup> protoGroupsMap =
                new Hashtable<>();

        if (parentGroup == null)
        {
            currentMetaGroup = mclServImpl.rootMetaGroup;
        }
        else
        {
            String groupMetaUID =
                XMLUtils.getAttribute(groupNode, GROUP_UID_ATTR_NAME);
            String groupDisplayName =
                XMLUtils.getAttribute(groupNode, GROUP_NAME_ATTR_NAME);

            // create the meta group
            currentMetaGroup =
                mclServImpl.loadStoredMetaContactGroup(parentGroup,
                    groupMetaUID, groupDisplayName, this);
            // extract and load one by one all proto groups in this meta group.
            Node protoGroupsNode =
                XMLUtils.findChild(groupNode, PROTO_GROUPS_NODE_NAME);

            NodeList protoGroups = protoGroupsNode.getChildNodes();

            for (int i = 0; i < protoGroups.getLength(); i++)
            {
                Node currentProtoGroupNode = protoGroups.item(i);

                if (currentProtoGroupNode.getNodeType() != Node.ELEMENT_NODE)
                    continue;

                String groupAccountID =
                    XMLUtils.getAttribute(currentProtoGroupNode,
                        ACCOUNT_ID_ATTR_NAME);

                if (!accountID.equals(groupAccountID))
                    continue;

                String protoGroupUID =
                    XMLUtils.getAttribute(currentProtoGroupNode, UID_ATTR_NAME);

                String parentProtoGroupUID =
                    XMLUtils.getAttribute(currentProtoGroupNode,
                        PARENT_PROTO_GROUP_UID_ATTR_NAME);

                Element persistentDataNode =
                    XMLUtils.findChild((Element) currentProtoGroupNode,
                        PERSISTENT_DATA_NODE_NAME);

                String persistentData = "";

                if (persistentDataNode != null)
                {
                    persistentData = XMLUtils.getText(persistentDataNode);
                }

                // try to find the parent proto group for the one we're
                // currently
                // parsing.
                ContactGroup parentProtoGroup = null;
                if (parentProtoGroups != null && parentProtoGroups.size() > 0)
                    parentProtoGroup =
                        parentProtoGroups.get(parentProtoGroupUID);

                // create the proto group
                ContactGroup newProtoGroup =
                    mclServImpl.loadStoredContactGroup(currentMetaGroup,
                        protoGroupUID, parentProtoGroup, persistentData,
                        accountID);

                protoGroupsMap.put(protoGroupUID, newProtoGroup);
            }

            // if this is not the meta contact list root and if it doesn't
            // contain proto groups of the account we're currently loading then
            // we don't need to recurese it since it cound contain any child
            // contacts of the same account.
            if (protoGroupsMap.size() == 0)
                return;
        }

        // we have parsed groups now go over the children
        Node childContactsNode =
            XMLUtils.findChild(groupNode, CHILD_CONTACTS_NODE_NAME);

        NodeList childContacts =
            (childContactsNode == null) ? null : childContactsNode
                .getChildNodes();

        // go over every meta contact, extract its details and its encapsulated
        // proto contacts
        for (int i = 0; childContacts != null && i < childContacts.getLength(); i++)
        {
            Node currentMetaContactNode = childContacts.item(i);

            if (currentMetaContactNode.getNodeType() != Node.ELEMENT_NODE)
                continue;

            try
            {
                String uid =
                    XMLUtils
                        .getAttribute(currentMetaContactNode, UID_ATTR_NAME);

                Element displayNameNode =
                    XMLUtils.findChild((Element) currentMetaContactNode,
                        META_CONTACT_DISPLAY_NAME_NODE_NAME);

                String displayName = XMLUtils.getText(displayNameNode);

                boolean isDisplayNameUserDefined =
                    Boolean.valueOf(displayNameNode
                            .getAttribute(USER_DEFINED_DISPLAY_NAME_ATTR_NAME));

                // extract a map of all encapsulated proto contacts
                List<MclStorageManager.StoredProtoContactDescriptor> protoContacts =
                    extractProtoContacts((Element) currentMetaContactNode,
                        accountID, protoGroupsMap);

                // if the size of the map is 0 then the meta contact does not
                // contain any contacts matching the currently parsed account
                // id.
                if (protoContacts.size() < 1)
                    continue;

                // Extract contact details.
                Map<String, List<String>> details = null;
                try
                {
                    List<Element> detailsNodes =
                        XMLUtils.findChildren((Element) currentMetaContactNode,
                            META_CONTACT_DETAIL_NAME_NODE_NAME);
                    if (detailsNodes.size() > 0)
                    {
                        details = new Hashtable<>();
                        for (Element e : detailsNodes)
                        {
                            String name = e.getAttribute(DETAIL_NAME_ATTR_NAME);
                            String value
                                = e.getAttribute(DETAIL_VALUE_ATTR_NAME);

                            List<String> detailsObj = details.get(name);
                            if (detailsObj == null)
                            {
                                List<String> ds = new ArrayList<>();
                                ds.add(value);
                                details.put(name, ds);
                            }
                            else
                                detailsObj.add(value);
                        }
                    }
                }
                catch (Exception ex)
                {
                    // catch any exception from loading contacts
                    // that will prevent loading the contact
                    logger.error("Cannot load details for contact node "
                        + currentMetaContactNode, ex);
                }

                // pass the parsed proto contacts to the mcl service
                mclServImpl.loadStoredMetaContact(
                    currentMetaGroup, uid,
                    displayName, isDisplayNameUserDefined,
                    details, protoContacts, accountID, this);
            }
            catch (Throwable thr)
            {
                // if we fail parsing a meta contact, we should remove it so
                // that
                // it stops causing trouble, and let other meta contacts load.
                logger.error("Failed to parse meta contact "
                    + currentMetaContactNode
                    + ". Will remove and continue with other contacts", thr);

                // remove the node so that it doesn't cause us problems again
                if (currentMetaContactNode.getParentNode() != null)
                {
                    try
                    {
                        currentMetaContactNode.getParentNode().removeChild(
                            currentMetaContactNode);
                    }
                    catch (Throwable throwable)
                    {
                        // hmm, failed to remove the faulty node. we must be
                        // in some kind of serious troble (but i don't see
                        // what we can do about it)
                        logger.error("Failed to remove meta contact node "
                            + currentMetaContactNode, throwable);
                    }
                }
            }
        }

        // now, last thing that's left to do - go over all subgroups if any
        Node subgroupsNode = XMLUtils.findChild(groupNode, SUBGROUPS_NODE_NAME);

        if (subgroupsNode == null)
            return;

        NodeList subgroups = subgroupsNode.getChildNodes();

        // recurse for every sub meta group
        for (int i = 0; i < subgroups.getLength(); i++)
        {
            Node currentGroupNode = subgroups.item(i);

            if (currentGroupNode.getNodeType() != Node.ELEMENT_NODE
                || !currentGroupNode.getNodeName().equals(GROUP_NODE_NAME))
                continue;

            try
            {
                processGroupXmlNode(mclServImpl, accountID,
                    (Element) currentGroupNode, currentMetaGroup,
                    protoGroupsMap);
            }
            catch (Throwable throwable)
            {
                // catch everything and bravely continue with remaining groups
                // and contacts
                logger.error("Failed to process group node " + currentGroupNode
                    + ". Removing.", throwable);

                // remove the node so that it doesn't cause us problems again
                if (currentGroupNode.getParentNode() != null)
                {
                    try
                    {
                        currentGroupNode.getParentNode().removeChild(
                            currentGroupNode);
                    }
                    catch (Throwable thr)
                    {
                        // hmm, failed to remove the faulty node. we must be
                        // in some kind of serious troble (but i don't see
                        // what we can do about it)
                        logger.error("Failed to remove group node "
                            + currentGroupNode, thr);
                    }
                }
            }
        }
    }

    /**
     * Returns all proto contacts that are encapsulated inside the meta contact
     * represented by <tt>metaContactNode</tt> and that originate from the
     * account with id - <tt>accountID</tt>. The returned list contains contact
     * contact descriptors as elements. In case the meta contact does not
     * contain proto contacts originating from the specified account, an empty
     * list is returned.
     * <p>
     *
     * @param metaContactNode the Element whose proto contacts we'd like to
     *            extract.
     * @param accountID the id of the account whose contacts we're interested
     *            in.
     * @param protoGroups a map binding proto group UIDs to protogroups, that
     *            the method could use i n order to fill in the corresponding
     *            field in the contact descriptors.
     * @return a java.util.List containing contact descriptors.
     */
    private List<MclStorageManager.StoredProtoContactDescriptor>
                    extractProtoContacts(Element metaContactNode,
                                         String accountID,
                                         Map<String, ContactGroup> protoGroups)
    {
        String uid = XMLUtils.getAttribute( metaContactNode, "uid");
        contactLogger.note("Extracting proto contacts for " + uid);
        List<StoredProtoContactDescriptor> protoContacts =
                new LinkedList<>();

        NodeList children = metaContactNode.getChildNodes();
        List<Node> duplicates = new LinkedList<>();

        for (int i = 0; i < children.getLength(); i++)
        {
            Node currentNode = children.item(i);

            //for some reason every now and then we would get a null nodename
            //... go figure
            if (currentNode.getNodeName() == null)
                continue;

            if (currentNode.getNodeType() != Node.ELEMENT_NODE
                || !currentNode.getNodeName().equals(PROTO_CONTACT_NODE_NAME))
                continue;

            String contactAccountID =
                XMLUtils.getAttribute(currentNode, ACCOUNT_ID_ATTR_NAME);

            if (!accountID.equals(contactAccountID))
                continue;

            String contactAddress =
                XMLUtils.getAttribute(currentNode,
                    PROTO_CONTACT_ADDRESS_ATTR_NAME);

            if (StoredProtoContactDescriptor.findContactInList(contactAddress,
                protoContacts) != null)
            {
                // this is a duplicate. mark for removal and continue
                contactLogger.note("Found duplicate node for " + uid);
                duplicates.add(currentNode);
                continue;
            }

            String protoGroupUID =
                XMLUtils.getAttribute(currentNode,
                    PARENT_PROTO_GROUP_UID_ATTR_NAME);
            Element persistentDataNode =
                XMLUtils.findChild((Element) currentNode,
                    PERSISTENT_DATA_NODE_NAME);

            String persistentData =
                (persistentDataNode == null) ? "" : XMLUtils
                    .getText(persistentDataNode);

            protoContacts.add(new StoredProtoContactDescriptor(contactAddress,
                persistentData, protoGroups.get(protoGroupUID)));
        }

        // remove all duplicates
        for (Node node : duplicates)
        {
            contactLogger.note("Removing duplicate " + node);
            metaContactNode.removeChild(node);
        }
        return protoContacts;
    }

    /**
     * Creates a node element corresponding to <tt>protoContact</tt>.
     *
     * @param protoContact the Contact whose element we'd like to create
     * @return a XML Element corresponding to <tt>protoContact</tt>.
     */
    private Element createProtoContactNode(Contact protoContact)
    {
        Element protoContactElement =
            contactListDocument.createElement(PROTO_CONTACT_NODE_NAME);

        // set attributes
        XMLUtils.setAttribute(protoContactElement,
                              PROTO_CONTACT_ADDRESS_ATTR_NAME,
                              protoContact.getAddress());

        // The display name for the protocontact is only stored to aid diagnosability.
        XMLUtils.setAttribute(protoContactElement,
                              PROTO_CONTACT_DISPLAY_ATTR_NAME,
                              protoContact.getDisplayName());

        XMLUtils.setAttribute(protoContactElement,
                              ACCOUNT_ID_ATTR_NAME,
                              protoContact.getProtocolProvider()
                                  .getAccountID()
                                  .getAccountUniqueID());

        if (protoContact.getParentContactGroup() == null)
        {
            contactLogger.note("the following contact looks weird:" + protoContact);
            contactLogger.note("group:" + protoContact.getParentContactGroup());
        }

        XMLUtils.setAttribute(protoContactElement,
                              PARENT_PROTO_GROUP_UID_ATTR_NAME,
                              protoContact.getParentContactGroup().getUID());

        // append persistent data child node
        String persistentData = protoContact.getPersistentData();
        if ((persistentData != null) && (persistentData.length() != 0))
        {
            Element persDataNode =
                contactListDocument.createElement(PERSISTENT_DATA_NODE_NAME);

            XMLUtils.setText(persDataNode, persistentData);

            protoContactElement.appendChild(persDataNode);
        }

        return protoContactElement;
    }

    /**
     * Creates a new XML <code>Element</code> corresponding to
     * <tt>protoGroup</tt> or <tt>null</tt> if <tt>protoGroup</tt> is not
     * eligible for serialization in its current state.
     *
     * @param protoGroup
     *            the <code>ContactGroup</code> which a corresponding XML
     *            <code>Element</code> is to be created for
     * @return a new XML <code>Element</code> corresponding to
     *         <tt>protoGroup</tt> or <tt>null</tt> if <tt>protoGroup</tt> is
     *         not eligible for serialization in its current state
     */
    private Element createProtoContactGroupNode(ContactGroup protoGroup)
    {
        Element protoGroupElement =
            contactListDocument.createElement(PROTO_GROUP_NODE_NAME);

        // set attributes
        XMLUtils.setAttribute(protoGroupElement,
                              UID_ATTR_NAME,
                              protoGroup.getUID());

        XMLUtils.setAttribute(protoGroupElement,
                              ACCOUNT_ID_ATTR_NAME,
                              protoGroup.getProtocolProvider()
                                  .getAccountID()
                                  .getAccountUniqueID());

        /*
         * The Javadoc on ContactGroup#getParentContactGroup() states null may
         * be returned. Prevent a NullPointerException.
         */
        ContactGroup parentContactGroup = protoGroup.getParentContactGroup();
        if (parentContactGroup != null)
        {
            XMLUtils.setAttribute(protoGroupElement,
                                  PARENT_PROTO_GROUP_UID_ATTR_NAME,
                                  parentContactGroup.getUID());
        }

        // append persistent data child node
        String persistentData = protoGroup.getPersistentData();
        if ((persistentData != null) && (persistentData.length() != 0))
        {
            Element persDataNode =
                contactListDocument.createElement(PERSISTENT_DATA_NODE_NAME);

            XMLUtils.setText(persDataNode, persistentData);

            protoGroupElement.appendChild(persDataNode);
        }

        return protoGroupElement;
    }

    /**
     * Creates a meta contact node element corresponding to <tt>metaContact</tt>
     * .
     *
     *
     * @param metaContact the MetaContact that the new node is about
     * @return the XML Element containing the persistent version of
     *         <tt>metaContact</tt>
     */
    private Element createMetaContactNode(MetaContact metaContact)
    {
        Element metaContactElement =
            this.contactListDocument.createElement(META_CONTACT_NODE_NAME);

        XMLUtils.setAttribute(metaContactElement,
                              UID_ATTR_NAME,
                              metaContact.getMetaUID());

        // create the display name node
        Element displayNameNode =
            contactListDocument
                .createElement(META_CONTACT_DISPLAY_NAME_NODE_NAME);

        displayNameNode.appendChild(contactListDocument
            .createTextNode(XMLUtils.sanitize(metaContact.getDisplayName())));

        if(((MetaContactImpl)metaContact).isDisplayNameUserDefined())
        {
            XMLUtils.setAttribute(displayNameNode,
                                  USER_DEFINED_DISPLAY_NAME_ATTR_NAME,
                                  Boolean.TRUE.toString());
        }

        metaContactElement.appendChild(displayNameNode);

        Iterator<Contact> contacts = metaContact.getContacts();

        while (contacts.hasNext())
        {
            Contact contact = contacts.next();
            Element contactElement = createProtoContactNode(contact);
            metaContactElement.appendChild(contactElement);
        }

        // Finally, add the persistent data:
        for (String detailName : metaContact.getAllDetails())
        {
            for (String detail : metaContact.getDetails(detailName))
            {
                Element detailElement = contactListDocument
                             .createElement(META_CONTACT_DETAIL_NAME_NODE_NAME);

                XMLUtils.setAttribute(detailElement,
                                      DETAIL_NAME_ATTR_NAME,
                                      detailName);
                XMLUtils.setAttribute(detailElement,
                                      DETAIL_VALUE_ATTR_NAME,
                                      detail);

                metaContactElement.appendChild(detailElement);
            }
        }

        return metaContactElement;
    }

    /**
     * Creates a meta contact group node element corresponding to
     * <tt>metaGroup</tt>.
     *
     * @param metaGroup the MetaContactGroup that the new node is about
     * @return the XML Element containing the persistent version of
     *         <tt>metaGroup</tt>
     */
    private Element createMetaContactGroupNode(MetaContactGroup metaGroup)
    {
        Element metaGroupElement =
            this.contactListDocument.createElement(GROUP_NODE_NAME);

        XMLUtils.setAttribute(metaGroupElement,
                              GROUP_NAME_ATTR_NAME,
                              metaGroup.getGroupName());

        XMLUtils.setAttribute(metaGroupElement,
                              UID_ATTR_NAME,
                              metaGroup.getMetaUID());

        // create and fill the proto groups node
        Element protoGroupsElement =
            this.contactListDocument.createElement(PROTO_GROUPS_NODE_NAME);
        metaGroupElement.appendChild(protoGroupsElement);

        Iterator<ContactGroup> protoGroups = metaGroup.getContactGroups();

        while (protoGroups.hasNext())
        {
            ContactGroup group = protoGroups.next();

            // ignore if the proto group is not persistent:
            if (!group.isPersistent())
                continue;

            // ignore if no name
            if (group.getGroupName() == null)
                continue;

            Element protoGroupEl = createProtoContactGroupNode(group);
            protoGroupsElement.appendChild(protoGroupEl);
        }

        // create and fill the sub groups node

        Element subgroupsElement =
            this.contactListDocument.createElement(SUBGROUPS_NODE_NAME);
        metaGroupElement.appendChild(subgroupsElement);

        Iterator<MetaContactGroup> subgroups = metaGroup.getSubgroups();

        while (subgroups.hasNext())
        {
            MetaContactGroup subgroup = subgroups.next();

            if (subgroup.getGroupName() != null &&
                subgroup.getMetaUID() != null)
            {
                Element subgroupEl = createMetaContactGroupNode(subgroup);
                subgroupsElement.appendChild(subgroupEl);
            }
            else
            {
                // Ignore bad groups, there's nothing we can do about it
                logger.error("Attempted to create group with no name or UID "
                                                                    + subgroup);
            }
        }

        // create and fill child contacts node

        Element childContactsElement =
            this.contactListDocument.createElement(CHILD_CONTACTS_NODE_NAME);
        metaGroupElement.appendChild(childContactsElement);

        Iterator<MetaContact> childContacts = metaGroup.getChildContacts();

        while (childContacts.hasNext())
        {
            MetaContact metaContact = childContacts.next();
            Element metaContactEl = createMetaContactNode(metaContact);
            childContactsElement.appendChild(metaContactEl);
        }

        return metaGroupElement;
    }

    /**
     * Indicates that a MetaContact has been successfully added to the
     * MetaContact list.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactAdded(MetaContactEvent evt)
    {
        contactLogger.note(evt.getSourceMetaContact(),
            "Added metacontact to contactlist.xml");

        Element parentGroupNode =
            findMetaContactGroupNode(evt.getParentGroup().getMetaUID());

        // not sure what to do in case of null. we'll be logging an internal
        // err for now and that's all.
        if (parentGroupNode == null)
        {
            logger.error("Couldn't find parent of a newly added contact: "
                + evt.getSourceMetaContact());
            logger.trace("The above exception occurred with the "
                         + "following stack trace: ",
                         new Exception());
            return;
        }

        parentGroupNode =
            XMLUtils.findChild(parentGroupNode, CHILD_CONTACTS_NODE_NAME);

        Element metaContactElement =
            createMetaContactNode(evt.getSourceMetaContact());

        parentGroupNode.appendChild(metaContactElement);

        scheduleContactListStorage();
    }

    /**
     * Creates XML nodes for the source metacontact group, its child meta
     * contacts and associated protogroups and adds them to the xml contact
     * list.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupAdded(MetaContactGroupEvent evt)
    {
        // if the group was created as an encapsulator of a non persistent proto
        // group then we'll ignore it.
        if (evt.getSourceProtoGroup() != null
            && !evt.getSourceProtoGroup().isPersistent())
            return;

        MetaContactGroup parentGroup =
            evt.getSourceMetaContactGroup().getParentMetaContactGroup();

        Element parentGroupNode =
            findMetaContactGroupNode(parentGroup.getMetaUID());

        // not sure what to do in case of null. we'll be logging an internal
        // err for now and that's all.
        if (parentGroupNode == null)
        {
            logger.error("Couldn't find parent of a newly added group: "
                + parentGroup);
            return;
        }

        Element newGroupElement =
            createMetaContactGroupNode(evt.getSourceMetaContactGroup());

        Element subgroupsNode =
            XMLUtils.findChild(parentGroupNode, SUBGROUPS_NODE_NAME);

        subgroupsNode.appendChild(newGroupElement);

        scheduleContactListStorage();
    }

    /**
     * Removes the corresponding node from the xml document.
     *
     * @param evt the MetaContactGroupEvent containing the corresponding contact
     */
    public void metaContactGroupRemoved(MetaContactGroupEvent evt)
    {
        Element metaContactGroupNode =
            findMetaContactGroupNode(evt.getSourceMetaContactGroup()
                .getMetaUID());

        // not sure what to do in case of null. we'll be loggin an internal err
        // for now and that's all.
        if (metaContactGroupNode == null)
        {
            logger.error("Save after removing an MN group. Groupt not found: "
                + evt.getSourceMetaContactGroup());
            return;
        }

        // remove the meta contact node.
        metaContactGroupNode.getParentNode().removeChild(metaContactGroupNode);

        scheduleContactListStorage();
    }

    /**
     * Moves the corresponding node from its old parent to the node
     * corresponding to the new parent meta group.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactMoved(MetaContactMovedEvent evt)
    {
        Element metaContactNode =
            findMetaContactNode(evt.getSourceMetaContact().getMetaUID());
        Element newParentNode =
            findMetaContactGroupNode(evt.getNewParent().getMetaUID());

        if (newParentNode == null)
        {
            logger.error("Save after metacontact moved. new parent not found: "
                + evt.getNewParent());
            logger.trace("The above exception has occurred with the "
                                +"following stack trace",
                                new Exception());
            return;
        }

        // in case of null this is a case of moving from non persistent group
        // to a persistent one.
        if(metaContactNode == null)
        {
            // create new node
            metaContactNode = createMetaContactNode(evt.getSourceMetaContact());
        }
        else
        {
            metaContactNode.getParentNode().removeChild(metaContactNode);
        }

        updateParentsForMetaContactNode(metaContactNode, evt.getNewParent());

        Element childContacts =
            XMLUtils.findChild(newParentNode, CHILD_CONTACTS_NODE_NAME);

        childContacts.appendChild(metaContactNode);

        scheduleContactListStorage();
    }

    /**
     * Traverses all contact elements of the metaContactNode argument and
     * updates theirs parent proto group uid-s to point to the first contact
     * group that is encapsulated by the newParent meta group and that belongs
     * to the same account as the contact itself.
     *
     * @param metaContactNode the meta contact node whose child contacts we're
     *            to update.
     * @param newParent a reference to the <tt>MetaContactGroup</tt> where
     *            metaContactNode was moved.
     */
    private void updateParentsForMetaContactNode(Element metaContactNode,
        MetaContactGroup newParent)
    {
        NodeList children = metaContactNode.getChildNodes();

        for (int i = 0; i < children.getLength(); i++)
        {
            Node currentNode = children.item(i);

            if (currentNode.getNodeType() != Node.ELEMENT_NODE
                || !currentNode.getNodeName().equals(PROTO_CONTACT_NODE_NAME))
                continue;

            Element contactElement = (Element) currentNode;

            String attribute =
                contactElement.getAttribute(PARENT_PROTO_GROUP_UID_ATTR_NAME);

            if (attribute == null || attribute.trim().length() == 0)
                continue;

            String contactAccountID =
                contactElement.getAttribute(ACCOUNT_ID_ATTR_NAME);

            // find the first protogroup originating from the same account as
            // the one that the current contact belongs to.
            Iterator<ContactGroup> possibleParents =
                newParent.getContactGroupsForAccountID(contactAccountID);

            String newParentUID = possibleParents.next().getUID();

            XMLUtils.setAttribute(contactElement,
                                  PARENT_PROTO_GROUP_UID_ATTR_NAME,
                                  newParentUID);
        }
    }

    /**
     * Removes the corresponding node from the xml document.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactRemoved(MetaContactEvent evt)
    {
        Element metaContactNode =
            findMetaContactNode(evt.getSourceMetaContact().getMetaUID());

        contactLogger.note(evt.getSourceMetaContact(),
            "Removing metacontact from contactlist.xml");

        // not sure what to do in case of null. we'll be loggin an internal err
        // for now and that's all.
        if (metaContactNode == null)
        {
            logger.error("Save after metacontact removed. Contact not found: "
                + evt.getSourceMetaContact());
            return;
        }

        // remove the meta contact node.
        metaContactNode.getParentNode().removeChild(metaContactNode);

        scheduleContactListStorage();
    }

    /**
     * Changes the display name attribute of the specified meta contact node.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactRenamed(MetaContactRenamedEvent evt)
    {
        Element metaContactNode =
            findMetaContactNode(evt.getSourceMetaContact().getMetaUID());

        contactLogger.note(evt.getSourceMetaContact(),
            "Renaming metacontact in contactlist.xml");

        // not sure what to do in case of null. we'll be loggin an internal err
        // for now and that's all.
        if (metaContactNode == null)
        {
            logger.error("Save after rename failed. Contact not found: "
                + evt.getSourceMetaContact());
            return;
        }

        Element displayNameNode =
            XMLUtils.findChild(metaContactNode,
                META_CONTACT_DISPLAY_NAME_NODE_NAME);

        if(((MetaContactImpl)evt.getSourceMetaContact())
                .isDisplayNameUserDefined())
        {
            XMLUtils.setAttribute(displayNameNode,
                                  USER_DEFINED_DISPLAY_NAME_ATTR_NAME,
                                  Boolean.TRUE.toString());
        }
        else
        {
            displayNameNode.removeAttribute(
                USER_DEFINED_DISPLAY_NAME_ATTR_NAME);
        }

        XMLUtils.setText(displayNameNode, evt.getNewDisplayName());

        updatePersistentDataForMetaContact(evt.getSourceMetaContact());

        scheduleContactListStorage();
    }

    /**
     * Updates the data stored for the contact that caused this event.
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void protoContactModified(ProtoContactEvent evt)
    {
        Element metaContactNode =
            findMetaContactNode(evt.getParent().getMetaUID());

        contactLogger.note(evt.getProtoContact(),
            "Modifying protocontact in contactlist.xml");

        // not sure what to do in case of null. we'll be logging an internal err
        // for now and that's all.
        if (metaContactNode == null)
        {
            logger.error("Save after proto contact modification failed. "
                + "Contact not found: " + evt.getParent());
            return;
        }

        updatePersistentDataForMetaContact(evt.getParent());

        // i don't think we could do anything else in addition to updating the
        // persistent data.

        scheduleContactListStorage();
    }

    /**
     * Indicates that a MetaContact has been modified.
     *
     * @param evt the MetaContactModifiedEvent containing the corresponding
     *            contact
     */
    public void metaContactModified(MetaContactModifiedEvent evt)
    {
        String name = evt.getModificationName();
        MetaContact sourceMetaContact = evt.getSourceMetaContact();

        contactLogger.note(sourceMetaContact, "Modified, element " + name +
                            " from: " + evt.getOldValue() + " to: " + evt.getNewValue());

        Element metaContactNode =
                            findMetaContactNode(sourceMetaContact.getMetaUID());

        // not sure what to do in case of null. we'll be logging an internal err
        // for now and that's all.
        if (metaContactNode == null)
        {
            logger.error("Save after rename failed. Contact not found: "
                                                           + sourceMetaContact);
            return;
        }

        boolean isChanged = false;

        // First, make sure that we remove all the old details from the list:
        List<Element> nodes =
            XMLUtils.locateElements(metaContactNode,
                META_CONTACT_DETAIL_NAME_NODE_NAME,
                DETAIL_NAME_ATTR_NAME, name);

        if (nodes != null)
        {
            for (Element e : nodes)
            {
                metaContactNode.removeChild(e);
                isChanged = true;
            }
        }

        // Now add each value
        List<String> details = sourceMetaContact.getDetails(name);
        for (String detail : details)
        {
            Element detailElement =
                contactListDocument
                    .createElement(META_CONTACT_DETAIL_NAME_NODE_NAME);

            XMLUtils.setAttribute(detailElement,
                                  DETAIL_NAME_ATTR_NAME,
                                  name);
            XMLUtils.setAttribute(detailElement,
                                  DETAIL_VALUE_ATTR_NAME,
                                  detail);

            metaContactNode.appendChild(detailElement);
            isChanged = true;
        }

        if (!isChanged)
            return;

        scheduleContactListStorage();
    }

    /**
     * Removes the corresponding node from the xml contact list.
     *
     * @param evt a reference to the corresponding <tt>ProtoContactEvent</tt>
     */
    public void protoContactRemoved(ProtoContactEvent evt)
    {
        Element oldMcNode =
            findMetaContactNode(evt.getOldParent().getMetaUID());

        contactLogger.note(evt.getProtoContact(),
            "Removing protocontact from contactlist.xml");

        // not sure what to do in case of null. we'll be logging an internal err
        // for now and that's all.
        if (oldMcNode == null)
        {
            logger.error("Failed to find meta contact (old parent): "
                + oldMcNode);
            return;
        }

        Element protoNode =
            XMLUtils.locateElement(oldMcNode, PROTO_CONTACT_NODE_NAME,
                PROTO_CONTACT_ADDRESS_ATTR_NAME, evt.getProtoContact()
                    .getAddress());

        if (protoNode != null)
        {
            if (protoNode.getParentNode() != null)
            {
                protoNode.getParentNode().removeChild(protoNode);
            }
            else
            {
                logger.error("Failed to remove protoNode " + protoNode +
                                   " from MetaContactNode " + oldMcNode +
                                   " because parent node could not be found.");
            }
        }
        else
        {
            logger.error("Failed to remove protoNode from MetaContactNode " +
                      oldMcNode + " because the protoNode could not be found");
        }

        scheduleContactListStorage();
    }

    /**
     * We simply ignore - we're not interested in this kind of events.
     *
     * @param evt the <tt>MetaContactGroupEvent</tt> containing details of this
     *            event.
     */
    public void childContactsReordered(MetaContactGroupEvent evt)
    {
        // ignore - not interested in such kind of events
    }

    /**
     * Determines the exact type of the change and acts accordingly by either
     * updating group name or .
     *
     * @param evt the MetaContactListEvent containing the corresponding contact
     */
    public void metaContactGroupModified(MetaContactGroupEvent evt)
    {
        MetaContactGroup mcGroup = evt.getSourceMetaContactGroup();
        Element mcGroupNode = findMetaContactGroupNode(mcGroup.getMetaUID());

        // not sure what to do in case of null. we'll be logging an internal err
        // for now and that's all.
        if (mcGroupNode == null)
        {
            logger.error("Failed to find meta contact group: " + mcGroup);
            logger.trace(
                    "The above error occurred with the following stack trace: ",
                    new Exception());
            return;
        }

        switch (evt.getEventID())
        {
        case MetaContactGroupEvent.CONTACT_GROUP_RENAMED_IN_META_GROUP:
        case MetaContactGroupEvent.CONTACT_GROUP_REMOVED_FROM_META_GROUP:
        case MetaContactGroupEvent.CONTACT_GROUP_ADDED_TO_META_GROUP:
            // the fact that a contact group was added or removed to a
            // meta group may imply substantial changes in the child contacts
            // and the layout of any possible subgroups, so
            // to make things simple, we'll remove the existing meta contact
            // group node and re-create it according to its current state.
            Node parentNode = mcGroupNode.getParentNode();

            parentNode.removeChild(mcGroupNode);

            Element newGroupElement = createMetaContactGroupNode(mcGroup);

            parentNode.appendChild(newGroupElement);

            scheduleContactListStorage();
            break;
        case MetaContactGroupEvent.META_CONTACT_GROUP_RENAMED:
            XMLUtils.setAttribute(mcGroupNode,
                                  GROUP_NAME_ATTR_NAME,
                                  mcGroup.getGroupName());
            break;
        }

        scheduleContactListStorage();
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * added to the list of protocol specific buddies in this
     * <tt>MetaContact</tt>
     *
     * @param evt a reference to the corresponding <tt>ProtoContactEvent</tt>
     */
    public void protoContactAdded(ProtoContactEvent evt)
    {
        Element mcNode = findMetaContactNode(evt.getParent().getMetaUID());

        contactLogger.note(evt.getProtoContact(),
            "Proto contact added to contactlist.xml");

        // not sure what to do in case of null. we'll be logging an internal err
        // for now and that's all.
        if (mcNode == null)
        {
            logger.error("Failed to find meta contact: " + evt.getParent());
            return;
        }

        Element protoNode = createProtoContactNode(evt.getProtoContact());

        mcNode.appendChild(protoNode);

        scheduleContactListStorage();
    }

    /**
     * Indicates that a protocol specific <tt>Contact</tt> instance has been
     * moved to a new <tt>MetaContact</tt>.
     *
     * @param evt a reference to the <tt>ProtoContactMovedEvent</tt> instance.
     */
    public void protoContactMoved(ProtoContactEvent evt)
    {
        MetaContact oldParent = evt.getOldParent();
        MetaContact newParent = evt.getNewParent();
        contactLogger.note(evt.getProtoContact(),
            "Protocontact moved in contactlist.xml: " +
                    " moved from " + oldParent +
                    " to " + newParent);

        // not sure what to do in case of null. we'll be logging an internal err
        // for now and that's all.
        Element newMcNode = null;
        if (newParent != null)
        {
            newMcNode = findMetaContactNode(newParent.getMetaUID());
        }

        if (newMcNode == null)
        {
            logger.error("Failed to find meta contact (new parent): "
                + newParent);
            return;
        }

        Element oldMcNode = null;
        if (oldParent != null)
        {
            oldMcNode = findMetaContactNode(oldParent.getMetaUID());
        }

        if (oldMcNode == null)
        {
            // No old MetaContact node was found.  Attempt to treat this event
            // in the same way as a protoContactAdded event.
            logger.warn("Failed to find meta contact (old parent): "
                + oldParent + ", treating as a protoContactAdded event");
            protoContactAdded(evt);
        }
        else
        {
            Element protoNode =
                XMLUtils.locateElement(oldMcNode, PROTO_CONTACT_NODE_NAME,
                    PROTO_CONTACT_ADDRESS_ATTR_NAME, evt.getProtoContact()
                        .getAddress());

            if (protoNode == null)
            {
                logger.error("Failed to find node for the proto contact " +
                                                         evt.getProtoContact());
                return;
            }

            protoNode.getParentNode().removeChild(protoNode);

            // update parent attr and append the contact to its new parent node.
            XMLUtils.setAttribute(protoNode,
                                  PARENT_PROTO_GROUP_UID_ATTR_NAME,
                                  evt.getProtoContact()
                                     .getParentContactGroup()
                                     .getUID());

            newMcNode.appendChild(protoNode);

            scheduleContactListStorage();
        }
    }

    /**
     * Returns the node corresponding to the meta contact with the specified uid
     * or null if no such node was found.
     *
     * @param metaContactUID the UID String of the meta contact whose node we
     *            are looking for.
     * @return the node corresponding to the meta contact with the specified UID
     *         or null if no such contact was found in the meta contact list
     *         file.
     */
    private Element findMetaContactNode(String metaContactUID)
    {
        Element root = (Element) contactListDocument.getFirstChild();

        return XMLUtils.locateElement(root, META_CONTACT_NODE_NAME,
            UID_ATTR_NAME, metaContactUID);
    }

    /**
     * Returns the node corresponding to the meta contact with the specified uid
     * or null if no such node was found.
     *
     * @param metaContactGroupUID the UID String of the meta contact whose node
     *            we are looking for.
     * @return the node corresponding to the meta contact group with the
     *         specified UID or null if no such group was found in the meta
     *         contact list file.
     */
    private Element findMetaContactGroupNode(String metaContactGroupUID)
    {
        Element root = (Element) contactListDocument.getFirstChild();

        return XMLUtils.locateElement(root, GROUP_NODE_NAME, UID_ATTR_NAME,
            metaContactGroupUID);
    }

    /**
     * Removes the file where we store contact lists.
     */
    void removeContactListFile()
    {
        logger.error("Deleting contact list file!", new Exception());
        this.contactlistFile.delete();
    }

    /**
     * Contains details parsed out of the contact list xml file, necessary for
     * creating unresolved contacts.
     */
    static class StoredProtoContactDescriptor
    {
        String contactAddress = null;

        String persistentData = null;

        ContactGroup parentProtoGroup = null;

        StoredProtoContactDescriptor(String contactAddress,
            String persistentData, ContactGroup parentProtoGroup)
        {
            this.contactAddress = contactAddress;
            this.persistentData = persistentData;
            this.parentProtoGroup = parentProtoGroup;
        }

        /**
         * Returns a string representation of the descriptor.
         *
         * @return a string representation of the descriptor.
         */
        public String toString()
        {
            return "StoredProtocoContactDescriptor[ "
                + " contactAddress="
                + contactAddress
                + " persistenData="
                + persistentData
                + " parentProtoGroup="
                + ((parentProtoGroup == null) ? "" : parentProtoGroup
                    .getGroupName()) + "]";
        }

        /**
         * Utility method that allows us to verify whether a ContactDescriptor
         * corresponding to a particular contact is already in a descriptor list
         * and thus eliminate duplicates.
         *
         * @param contactAddress the address of the contact whose descriptor we
         *            are looking for.
         * @param list the <tt>List</tt> of
         *            <tt>StoredProtoContactDescriptor</tt> that we are supposed
         *            to search for <tt>contactAddress</tt>
         * @return a <tt>StoredProtoContactDescriptor</tt> corresponding to
         *         <tt>contactAddress</tt> or <tt>null</tt> if no such
         *         descriptor exists.
         */
        private static StoredProtoContactDescriptor findContactInList(
            String contactAddress, List<StoredProtoContactDescriptor> list)
        {
            if (list != null && list.size() > 0)
            {
                for (StoredProtoContactDescriptor desc : list)
                {
                    if (desc.contactAddress.equals(contactAddress))
                        return desc;
                }
            }
            return null;
        }
    }

    /**
     * Indicates that a new avatar is available for a <tt>MetaContact</tt>.
     * @param evt the <tt>MetaContactAvatarUpdateEvent</tt> containing details
     * of this event
     */
    public void metaContactAvatarUpdated(MetaContactAvatarUpdateEvent evt)
    {
        // TODO Store MetaContact avatar.
    }
}
