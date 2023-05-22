// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.configuration;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

import org.apache.commons.io.FileUtils;
import org.jitsi.service.configuration.*;
import org.jitsi.util.Hasher;

/**
 * V2.20 introduced login/logout with support for multiple users.  As part of
 * that work, the format of the config was changed slightly ("user" and "global"
 * config was introduced).
 * <p/>
 * This class migrates from the pre-2.20 format to the post-2.20 format.
 * <p/>
 * It also moves
 * <ul>
 * <li>ContactList.xml</li>
 * <li>The custom ringtone</li>
 * <li>History</li>
 * <li>The avatar cache</li>
 * </ul>
 */
public class ConfigMigrator implements ConfigMigrationProperties
{
    private static final Logger sLog = Logger.getLogger(ConfigMigrator.class);

    private final ConfigurationService mConfigService;
    private final String mUser;
    private final String mGlobalHomeDir;
    private String mUserHomeDir;

    public ConfigMigrator(ConfigurationService configService)
    {
        mConfigService = configService;
        mUser = mConfigService.global().getString(PROP_USER);
        mGlobalHomeDir = mConfigService.global().getScHomeDirLocation() +
                         File.separator +
                         mConfigService.global().getScHomeDirName() +
                         File.separator;
    }

    /**
     * Migrates the config files files from a pre-login/logout version to a
     * version with login / logout
     */
    public void migrate()
    {
        if (mUser == null || mUser.isEmpty())
        {
            // No user means that we can't migrate.  But it also means that we
            // shouldn't migrate.  There are 2 reasons why it might not be
            // present:
            // 1. The user has logged out.  But in which case log out was an
            //    option that they could choose and so they've already been
            //    migrated.
            // 2. This is the first time the application has started and so
            //    there's no data to migrate.
            sLog.info("No migration to do as no user");
            mConfigService.global().setProperty(PROP_HAS_MIGRATED, true);
        }
        else
        {
            mConfigService.setActiveUser(mUser);
            // Store the active user's DN as a salt to protect Personally Identifiable
            // Information.  If we change active user, then it is right that this will
            // be updated.
            Hasher.setSalt(mUser);

            mUserHomeDir = mConfigService.user().getScHomeDirLocation() +
                           File.separator +
                           mConfigService.user().getScHomeDirName() +
                           File.separator;

            doMigrate(PROP_HAS_MIGRATED, new Runnable()
            {
                @Override
                public void run()
                {
                    // Config is most important, so do that first
                    migrateConfig();

                    // Then history (as it is mostly local, so won't be created)
                    migrateDirectory(PROP_HAS_MIGRATED_HISTORY, "history_ver1.0");

                    // Contact list is mostly stored on server, so isn't too
                    // important.
                    migrateFile(PROP_HAS_MIGRATED_CONTACTS, "contactlist.xml");

                    // It really isn't the end of the world if we fail to
                    // migrate the ringtone.
                    migrateFile(PROP_HAS_MIGRATED_RINGTONE, "ringtone.wav");

                    // We'd like to migrate the avatarcache if we can
                    migrateDirectory(PROP_HAS_MIGRATED_AVATAR_CACHE, "avatarcache");
                }
            });
        }
    }

    /**
     * Move a file from the old location to the new.  The passed in property
     * indicates where to save that this has been successful
     *
     * @param property Where to save that this has been successful
     * @param fileName The name of the file to move
     */
    private void migrateFile(final String property,
                             final String fileName)
    {
        doMigrate(property, new Runnable()
        {
            @Override
            public void run()
            {
                File oldFile = new File(mGlobalHomeDir + fileName);
                File newFile = new File(mUserHomeDir + fileName);

                try
                {
                    if (oldFile.exists())
                    {
                        FileUtils.moveFile(oldFile, newFile);
                    }
                }
                catch (IOException e)
                {
                    sLog.error("Failed to migrate " + fileName, e);
                }
            }
        });
    }

    /**
     * Move a directory from the old location to the new.  The passed in property
     * indicates where to save that this has been successful
     *
     * @param property Where to save that this has been successful
     * @param directory The name of the directory to move
     */
    private void migrateDirectory(final String property,
                                  final String directory)
    {
        doMigrate(property, new Runnable()
        {
            @Override
            public void run()
            {
                File srcDir = new File(mGlobalHomeDir + directory);
                File destDir = new File(mUserHomeDir);
                try
                {
                    FileUtils.moveDirectoryToDirectory(srcDir, destDir, true);
                }
                catch (IOException e)
                {
                    sLog.error("Failed to migrate " + directory, e);
                }
            }
        });
    }

    /**
     * Move Config from the old location to the new
     */
    private void migrateConfig()
    {
        doMigrate(PROP_HAS_MIGRATED_CONFIG, new Runnable()
        {
            @Override
            public void run()
            {
                ScopedConfigurationService global = mConfigService.global();
                ScopedConfigurationService user = mConfigService.user();

                List<String> allProps = global.getAllPropertyNames();
                allProps.removeAll(PROPS_TO_NOT_MOVE);

                for (String prop : allProps)
                {
                    user.setProperty(prop, global.getProperty(prop));
                }

                for (String prop : allProps)
                {
                    global.removeProperty(prop);
                }
            }
        });
    }

    /**
     * Actually does a migration.
     *
     * First checks the property to see if the migration has been performed
     * before.  If not, then it runs (on the same thread) the migrateRunnable.
     *
     * If that succeeds (i.e. no Exceptions are thrown) it stores true in the
     * property.
     *
     * @param property Where to check / store migration success
     * @param migrateRunnable The runnable that performs the migration
     */
    private void doMigrate(String property, Runnable migrateRunnable)
    {
        ScopedConfigurationService cfg = mConfigService.global();

        if (cfg.getBoolean(property, false))
        {
            sLog.info("Migration already done, property: " + property);
        }
        else
        {
            sLog.info("Migrating property " + property);
            migrateRunnable.run();
            cfg.setProperty(property, Boolean.TRUE);
        }
    }
}
