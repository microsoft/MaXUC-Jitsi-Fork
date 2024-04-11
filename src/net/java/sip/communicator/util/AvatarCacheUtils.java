/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import static net.java.sip.communicator.util.PrivacyUtils.*;

import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;

import org.jitsi.service.fileaccess.*;
import org.jitsi.service.resources.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.imageloader.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.wispaservice.WISPAAction;
import net.java.sip.communicator.service.wispaservice.WISPANamespace;

/**
 * The <tt>AvatarCacheUtils</tt> allows to cache an avatar or to obtain the
 * image of a cached avatar by specifying a contact or an account address.
 *
 * @author Yana Stamcheva
 */
public class AvatarCacheUtils
{
    private static final int ONE_DAY_MILLIS = 24 * 3600 * 1000;
    /**
     * The logger for this class.
     */
    private static final Logger logger
        = Logger.getLogger(AvatarCacheUtils.class);

    /**
     * The name (i.e. not the whole path) of the directory in which the avatar
     * files are to be cached for later reuse.
     */
    private static final String AVATAR_DIR = "avatarcache";

    /**
     * The version of the avatar cache, which would must be incremented if changes
     * are made to the avatar storage.
     */
    private static final int CACHE_VERSION = 2;

    /**
     * The desired dimension of the cached avatars for contacts.
     */
    private static final int CONTACT_AVATAR_SIZE = 38;

    /**
     * The desired dimension of the cached avatars for protocol providers i.e.
     * our own avatars.
     *
     * This is larger than for contacts because our own avatar can be used in
     * contexts where larger images are required e.g. Accession Meeting
     * avatars. However, as contact avatars are not used in these contexts,
     * we should save space by storing them at a lower size.
     */
    private static final int PROTOCOL_PROVIDER_AVATAR_SIZE = 96;

    /**
     *  Characters and their replacement in created folder names
     */
    private static final String[][] ESCAPE_SEQUENCES = new String[][]
    {
        {"&", "&_amp"},
        {"/", "&_sl"},
        {"\\\\", "&_bs"},   // the char \
        {":", "&_co"},
        {"\\*", "&_as"},    // the char *
        {"\\?", "&_qm"},    // the char ?
        {"\"", "&_pa"},     // the char "
        {"<", "&_lt"},
        {">", "&_gt"},
        {"\\|", "&_pp"}     // the char |
    };

    /**
     * Replaces the characters that we must escape used for the created
     * filename.
     *
     * @param id the <tt>String</tt> which is to have its characters escaped
     * @return a <tt>String</tt> derived from the specified <tt>id</tt> by
     * escaping characters
     */
    private static String escapeSpecialCharacters(String id)
    {
        String resultId = id;

        for (String[] escapeSequence : ESCAPE_SEQUENCES)
        {
            resultId = resultId.
                    replaceAll(escapeSequence[0], escapeSequence[1]);
        }
        return resultId;
    }

    /**
     * Utility method
     * @return The path to the root of the avatar cache.
     */
    private static String getAvatarDir()
    {
        return AVATAR_DIR + File.separator + CACHE_VERSION;
    }

    /**
     * Utility method - returns the unique account ID of the given protocol
     * provider, with special characters escaped.
     *
     * @param protocolProvider The protocol provider
     * @return the escaped unique account ID of the given protocol provider
     */
    private static String escapeAccountUniqueID(ProtocolProviderService protocolProvider)
    {
        return escapeSpecialCharacters(protocolProvider.getAccountID().
                                                        getAccountUniqueID());
    }

    /**
     * Utility method
     * @param contact
     * @return Filename of the cached avatar for this <tt>contact</tt>
     */
    private static String getAvatarFilename(Contact contact)
    {
        return escapeSpecialCharacters(contact.getAddress());
    }

    /**
     * Utility method
     * @param protocolProvider
     * @return Filename of the cached avatar for this <tt>protocolProvider</tt>
     */
    private static String getAvatarFilename(ProtocolProviderService protocolProvider)
    {
        return escapeAccountUniqueID(protocolProvider);
    }

    /**
     * Utility method for getting the directory path of the cached avatars
     * from the given <tt>protocolProvider</tt>.
     * @param protocolProvider
     * @return The directory path
     */
    private static String getAvatarDirPath(ProtocolProviderService protocolProvider)
    {
        return getAvatarDir() +
               File.separator +
               escapeAccountUniqueID(protocolProvider);
    }

    /**
     * Returns the path of the avatar image stored for the account
     * corresponding to the given <tt>protocolContact</tt>.
     *
     * @param protocolContact the <tt>Contact</tt>, which
     * account avatar image we're looking for
     * @return the path of the avatar image stored for the account
     * corresponding to the given contact
     */
    private static String getAvatarPath(Contact protocolContact)
    {
        return getAvatarDirPath(protocolContact.getProtocolProvider()) +
               File.separator +
               getAvatarFilename(protocolContact);
    }

    /**
     * Returns the path of the avatar image stored for the account
     * corresponding to the given <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, the
     * account avatar image of which we're looking for
     * @return the path of the avatar image stored for the account
     * corresponding to the given protocol provider
     */
    private static String getAvatarPath(
                                    ProtocolProviderService protocolProvider)
    {
        return getAvatarDirPath(protocolProvider) +
               File.separator +
               getAvatarFilename(protocolProvider);
    }

    /**
     * Returns the bytes of the avatar image stored for the account
     * corresponding to the given <tt>protocolProvider</tt>.
     *
     * Returns <tt>null</tt> if no such avatar is cached.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, the
     * account avatar image of which we're looking for
     * @return the bytes of the avatar image stored for the account
     * corresponding to the given <tt>protocolProvider</tt>
     */
    public static BufferedImageFuture getCachedAvatar(
                                    ProtocolProviderService protocolProvider)
    {
        String avatarPath = getAvatarPath(protocolProvider);

        /*
         * Caching a zero-length avatar happens but such an avatar isn't
         * very useful.
         */
        return getLocallyStoredAvatar(avatarPath);
    }

    /**
     * Returns the bytes of the avatar image stored for the account
     * corresponding to the given protocol provider contact.
     *
     * Returns <tt>null</tt> if no such avatar is cached.
     *
     * @param protocolContact the <tt>Contact</tt>, the
     * account avatar image of which we're looking for
     * @return the bytes of the avatar image stored for the account
     * corresponding to the given <tt>protocolContact</tt>
     */
    public static BufferedImageFuture getCachedAvatar(Contact protocolContact)
    {
        String avatarPath = getAvatarPath(protocolContact);

        BufferedImageFuture cachedAvatar = getLocallyStoredAvatar(avatarPath);

        /*
         * Caching a zero-length avatar happens but such an avatar isn't
         * very useful.
         */
        return cachedAvatar;
    }

    /**
     * Returns the file for the avatar stored for the contact
     * corresponding to the given protocol provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt>, which
     * is responsible for the contact address for the avatar image we're looking for
     * @param contactAddress the contact
     * @return the file storing the avatar stored for the contact in the given
     * protocol provider specified. This is <tt>null</tt> if the avatar file
     * is inaccessible.
     */
    public static File getCachedAvatarFile(
        ProtocolProviderService protocolProvider,
        String contactAddress)
    {
        String path = getAvatarDirPath(protocolProvider) +
                      File.separator +
                      escapeSpecialCharacters(contactAddress);

        File avatarFile = null;

        try
        {
            avatarFile = UtilActivator.getFileAccessService()
                .getPrivatePersistentActiveUserFile(path);
        }
        catch (IOException | SecurityException e)
        {
            logger.error("Failed to get file for path: " + path, e);
        }

        return avatarFile;
    }

    /**
     * Returns the avatar image corresponding to the given avatar path.
     *
     * Returns <tt>null</tt> if no such avatar is cached.
     *
     * @param avatarPath The path to the locally stored avatar.
     * @return the avatar image corresponding to the given avatar path.
     */
    private static BufferedImageFuture getLocallyStoredAvatar(String avatarPath)
    {
        try
        {
            File avatarFile
                = UtilActivator
                    .getFileAccessService()
                        .getPrivatePersistentActiveUserFile(avatarPath);

            if(avatarFile.exists())
            {
                byte[] bs = null;

                try (FileInputStream avatarInputStream = new FileInputStream(
                        avatarFile))
                {
                    int available = avatarInputStream.available();

                    if (available > 0)
                    {
                        bs = new byte[available];
                        avatarInputStream.read(bs);
                    }
                }
                if (bs != null)
                    return BufferedImageAvailableFromBytes.fromBytes(bs);
            }
        }
        catch (IOException | SecurityException ex)
        {
            logger.error(
                    "Could not read avatar image from file " + avatarPath,
                    ex);
        }
        return null;
    }

    /**
     * Stores avatar bytes in the given <tt>Contact</tt>.
     *
     * @param protocolContact The contact in which we store the avatar.
     * @param avatarBytes The avatar image bytes.
     */
    public static void cacheAvatar(Contact protocolContact,
        BufferedImageFuture avatarBytes)
    {
        String avatarDirPath = getAvatarDirPath(
                                    protocolContact.getProtocolProvider());
        String avatarFileName = getAvatarFilename(protocolContact);

        cacheAvatar(avatarDirPath,
                    avatarFileName,
                    avatarBytes,
                    CONTACT_AVATAR_SIZE);
    }
    /**
     * Stores avatar bytes for the account corresponding to the given
     * <tt>protocolProvider</tt>.
     *
     * @param protocolProvider the protocol provider corresponding to the
     * account, which avatar we're storing
     * @param avatarBytes the avatar image bytes
     */
    public static void cacheAvatar(ProtocolProviderService protocolProvider,
                                   BufferedImageFuture avatarBytes)
    {
        String avatarDirPath = getAvatarDirPath(protocolProvider);

        String avatarFileName = getAvatarFilename(protocolProvider);

        cacheAvatar(avatarDirPath,
                    avatarFileName,
                    avatarBytes,
                    PROTOCOL_PROVIDER_AVATAR_SIZE);
    }

    /**
     * Stores avatar bytes for the account corresponding to the given
     * <tt>protocolProvider</tt>.
     *
     * @param avatarDirPath the directory in which the file will be stored
     * @param avatarFileName the name of the avatar file
     * @param avatarBytes the avatar image bytes
     * @param desiredSize the number of pixels per side in the cached avatar
     */
    protected static void cacheAvatar(String avatarDirPath,
                                      String avatarFileName,
                                      BufferedImageFuture avatarBytes,
                                      int desiredSize)
    {
        if (avatarBytes == null)
        {
            return;
        }

        BufferedImageFuture avatar =
            avatarBytes.scaleImageWithinBounds(desiredSize, desiredSize);

        BufferedImage image = avatar.resolve();
        if (image == null)
        {
            logger.warn("Avatar can't be resolved, don't cache it");
            return;
        }

        byte[] byteArray = ImageUtils.toByteArray(image);

        File avatarDir = null;
        File avatarFile = null;
        try
        {
            FileAccessService fileAccessService
                = UtilActivator.getFileAccessService();

            avatarFile
                = fileAccessService.getPrivatePersistentActiveUserFile(
                        avatarDirPath + File.separator + avatarFileName);

            // If no exception was thrown in the above, then the parent
            // directory exists, whereas the file returned may not exist.
            //
            // Hence if the file does not exist, create it.
            if(!avatarFile.exists() && !avatarFile.createNewFile())
            {
                throw new IOException("Failed to create file"
                    + sanitiseFilePath(avatarFile.getAbsolutePath()));
            }

            try (FileOutputStream fileOutStream = new FileOutputStream(
                    avatarFile))
            {
                fileOutStream.write(byteArray);
                fileOutStream.flush();
            }
        }
        catch (IOException | SecurityException ex)
        {
            logger.error(
                    "Failed to store avatar. dir =" + avatarDir
                        + " file=" + avatarFile,
                    ex);
        }
    }

    /**
     * Deletes the cached avatar of the given protocol provider.
     * @param protocolProvider
     */
    public static void deleteCachedAvatar(ProtocolProviderService protocolProvider)
    {
        deleteCachedAvatar(getAvatarPath(protocolProvider), protocolProvider);
    }

    /**
     * Deletes the cached avatar for the specified contact.
     * @param protocolContact
     */
    public static void deleteCachedAvatar(Contact protocolContact)
    {
        deleteCachedAvatar(getAvatarPath(protocolContact), protocolContact.getProtocolProvider());
        MetaContact metaContact = UtilActivator.getContactListService().findMetaContactByContact(protocolContact);
        if (metaContact != null)
        {
            metaContact.clearCachedAvatar();
            UtilActivator.getWISPAService().notify(WISPANamespace.CONTACTS, WISPAAction.DATA, metaContact);
        }
    }

    /**
     * Deletes the cached avatar on the given path.
     * @param avatarPath
     */
    private static void deleteCachedAvatar(String avatarPath, ProtocolProviderService protocolProviderService)
    {
        String sanitisedPath = sanitiseAvatarPath(avatarPath, protocolProviderService);

        logger.info("Delete avatar " + sanitisedPath + " from the cache");

        try
        {
            File avatarFile =
                    UtilActivator.getFileAccessService().
                        getPrivatePersistentActiveUserFile(avatarPath);

            if (avatarFile.exists())
            {
                boolean success = avatarFile.delete();

                if (!success)
                {
                    throw new IOException("Failed to delete file: " + sanitisedPath);
                }
            }
        }
        catch (IOException | SecurityException e)
        {
            logger.error("Failed to delete avatar. File=" + sanitisedPath, e);
        }
    }

    /**
     * Returns whether the caller should regard the cached avatar for this
     * protocol provider to be invalidated
     *
     * For example, the avatar could be out of date.
     *
     * @param protocolProvider
     * @return Whether should invalidate
     */
    public static boolean shouldInvalidateCache(
                                ProtocolProviderService protocolProvider)
    {
        return shouldInvalidateCache(getAvatarPath(protocolProvider));
    }

    /**
     * Returns whether should invalidate the avatar on the given path.
     *
     * For example, the avatar could be out of date.
     *
     * @param avatarPath
     * @return Whether should invalidate
     */
    private static boolean shouldInvalidateCache(String avatarPath)
    {
        try
        {
            // If the file is older than 1 day, we should expire it so that
            // we pick up any server-side changes.
            File avatarFile =
                UtilActivator.getFileAccessService().getPrivatePersistentActiveUserFile(
                                                                    avatarPath);

            BasicFileAttributes attr = Files.readAttributes(
                                avatarFile.toPath(), BasicFileAttributes.class);
            long modifiedDate = attr.lastModifiedTime().toMillis();
            logger.debug("Avatar '" + sanitiseChatAddress(avatarPath) + "' last modified " +
                         (System.currentTimeMillis() - modifiedDate) +
                         " ms ago");

            return modifiedDate + ONE_DAY_MILLIS < System.currentTimeMillis();
        }
        catch (IOException | SecurityException e)
        {
            logger.error("Error performing expiry check for cached avatar " +
                         sanitiseChatAddress(avatarPath), e);
            return false;
        }
    }

    /**
     * We need to hash the two PIIs after "avatarcache/2/".
     */
    private static String sanitiseAvatarPath(String avatarPath, ProtocolProviderService protocolProviderService)
    {
        int lastSeparatorIndex = avatarPath.lastIndexOf(File.separator);
        String protocolUniqueId = escapeSpecialCharacters(
                protocolProviderService.getAccountID().getLoggableAccountID());
        String accountUniqueId = avatarPath.substring(lastSeparatorIndex + 1);

        return getAvatarDir() + File.separator +
               protocolUniqueId + File.separator +
               sanitisePeerId(accountUniqueId);
    }
}
