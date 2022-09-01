/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.credentialsstorage;

import org.jitsi.service.configuration.*;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements {@link CredentialsStorageService} to load and store user
 * credentials from/to the {@link ConfigurationService}.
 *
 * @author Dmitri Melnikov
 */
public class CredentialsStorageServiceImpl
    implements CredentialsStorageService
{
    private static Map<String, String> passwordMap = new HashMap<>();
    private static final Logger sLog = Logger.getLogger(CredentialsStorageServiceImpl.class);

    private final ConfigurationService sCfgService =
                                 CredentialsStorageActivator.getConfigService();

    /**
     * The global credentials service.
     */
    private final ScopedCredentialsStorageService mGlobal;

    /**
     * The user credentials service.
     */
    private ScopedCredentialsStorageService mUser = null;

    public CredentialsStorageServiceImpl()
    {
        mGlobal = new ScopedCredentialsStorageServiceImpl(sCfgService.global());

        if (sCfgService.user() != null)
        {
            sLog.info("User exists, thus creating user scoped creds");
            setActiveUser();
        }
    }

    public ScopedCredentialsStorageService global()
    {
        return mGlobal;
    }

    public ScopedCredentialsStorageService user()
    {
        return mUser;
    }

    public void setActiveUser()
    {
        // We never swap users without a restart
        if (mUser == null)
        {
            sLog.info("Create user credentials storage");
            ScopedConfigurationService userCfg = sCfgService.user();
            mUser = new ScopedCredentialsStorageServiceImpl(userCfg);
        }

        storePassword();
    }

    /**
     * Stores the password locally in a map whilst waiting for the user configuration
     * to be set up. Once it has been set up, the values in this map
     * are moved to the user config.
     * @param propertyName The password property name
     * @param password The password being stored
     */
    public void storePasswordLocally(String propertyName, String password)
    {
        passwordMap.put(propertyName, password);
    }

    /**
     * Moves any locally stored passwords into the user config
     */
    private void storePassword()
    {
        for (Map.Entry<String, String> entry : passwordMap.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue();

            sLog.debug("Storing " + key + " in the user config");
            mUser.storePassword(key, value);
            passwordMap.remove(key, value);
        }
    }
}
