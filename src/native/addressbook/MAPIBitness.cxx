/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.

#include "MAPIBitness.h"

#define WIN32_LEAN_AND_MEAN
#include <windows.h>
#include <Msi.h>
#include <stdio.h>
#include <Tchar.h>

/**
 * Checks the bitness of the Outlook installation and of the Jitsi executable.
 *
 * @author Vincent Lucas
 */

/**
 * The number of registries known for the different Outlook version.
 */
int nbOutlookRegister = 5;


/**
 * The registries known for the different Outlook version.
 */
TCHAR outlookRegister[][MAX_PATH] = {
    TEXT("{5812C571-53F0-4467-BEFA-0A4F47A9437C}"), // Outlook 2016
    TEXT("{E83B4360-C208-4325-9504-0D23003A74A5}"), // Outlook 2013
    TEXT("{1E77DE88-BCAB-4C37-B9E5-073AF52DFD7A}"), // Outlook 2010
    TEXT("{24AAE126-0911-478F-A019-07B875EB9996}"), // Outlook 2007
    TEXT("{BC174BAD-2F53-4855-A1D5-0D575C19B1EA}")  // Outlook 2003
};

/**
 * Returns the bitness of the Outlook installation.
 *
 * @return 64 if Outlook 64 bits version is installed. 32 if Outlook 32 bits
 * version is installed. -1 otherwise.
 */
int MAPIBitness_getOutlookBitnessVersion(JavaLogger* logger)
{
    DWORD pathLength = 0;
    int rc;
    
    for(int i = 0; 
        i < nbOutlookRegister;
        ++i)
    {
        logger->debug("Examining Outlook version: %s", outlookRegister[i]);
        
        rc = MsiProvideQualifiedComponent(
                    outlookRegister[i],
                    TEXT("outlook.x64.exe"),
                    (DWORD) INSTALLMODE_DEFAULT,
                    NULL,
                    &pathLength);
        logger->debug("64 bit lookup result %d", rc);
        if (rc == ERROR_SUCCESS)
        {
            logger->debug("Found 64-bit Outlook: %s", outlookRegister[i]);
            return 64;
        }
    
        rc = MsiProvideQualifiedComponent(
                outlookRegister[i],
                TEXT("outlook.exe"),
                (DWORD) INSTALLMODE_DEFAULT,
                NULL,
                &pathLength);
        logger->debug("32 bit lookup result %d", rc);
        
        if (rc == ERROR_SUCCESS)
        {
            logger->debug("Found 32-bit Outlook: %s", outlookRegister[i]);
            return 32;
        }
    }

    // The above doesn't always work.  As a back up - look for the bitness value in the registry.
    logger->info("Didn't find any Outlook bitness, look for the Bitness registry key");
    int bitness = findBitnessRegEntry(logger);
    
    logger->info("Returning bitness %d", bitness);

    return bitness;
}

/**
 * Try and determine the bitness of Microsoft Outlook installed by looking for a 'Bitness' registry key
 * in HKEY_LOCAL_MACHINE\SOFTWARE\WOW6432Node\Microsoft\Office
 *
 * @return 64 if Outlook 64 bits version is installed. 32 if Outlook 32 bits
 * version is installed. -1 otherwise.
 */
int findBitnessRegEntry(JavaLogger* logger)
{
    HKEY regKey;
    int bitness = -1;

    LONG regResult = RegOpenKeyEx(HKEY_LOCAL_MACHINE,
                                  _T("SOFTWARE\\WOW6432Node\\Microsoft\\Office"),
                                  0,
                                  KEY_ENUMERATE_SUB_KEYS,
                                  &regKey);

    if (ERROR_SUCCESS == regResult)
    {
        logger->debug("Opened HKLM\\Software\\WOW6432Node\\Microsoft\\Office");

        DWORD bitnessValueType;
        DWORD bitnessValueSize;
        LPSTR bitnessValue = (LPSTR)::malloc(10);

        DWORD i = 0;
        TCHAR installRootKeyName[255 // The size limit of key name as documented in MSDN
                                 + 20 // \Outlook\InstallRoot
                                 + 1]; // The terminating null character

        // Iterate through subkeys looking for a key called "Outlook" with value "Bitness"
        while (bitness == -1)
        {
            LONG regEnumKeyEx;
            DWORD subkeyNameLength = 255 + 1;
            HKEY installRootKey;

            regEnumKeyEx = RegEnumKeyEx(
                        regKey,
                        i,
                        installRootKeyName,
                        &subkeyNameLength,
                        NULL,
                        NULL,
                        NULL,
                        NULL);

            if (regEnumKeyEx == ERROR_NO_MORE_ITEMS)
            {
                logger->warn("No more items");
                break;
            }

            i++;
            if (regEnumKeyEx != ERROR_SUCCESS)
            {
                logger->error("Failed with error: 0x%lx", regEnumKeyEx);
                continue;
            }

            logger->debug("Opened Path for %s", installRootKeyName);

            memcpy(installRootKeyName + subkeyNameLength, _T("\\Outlook"), 8 * sizeof(TCHAR));
             *(installRootKeyName + subkeyNameLength + 8) = 0;

            LONG result = RegOpenKeyEx(
                            regKey,
                            installRootKeyName,
                            0,
                            KEY_QUERY_VALUE,
                            &installRootKey);

            if (result == ERROR_SUCCESS)
            {
                logger->debug("Opened %s", installRootKeyName);

                result = RegQueryValueEx(
                                installRootKey,
                                _T("Bitness"),
                                NULL,
                                &bitnessValueType,
                                (LPBYTE) bitnessValue,
                                &bitnessValueSize);

                if ((result == ERROR_SUCCESS) &&
                    (bitnessValueType == REG_SZ) &&
                    (bitnessValueSize))
                {
                    logger->info("Found bitness %s", bitnessValue);

                    if (strcmp(bitnessValue, "x86") == 0)
                    {
                        logger->debug("32 bit");
                        bitness = 32;
                    }
                    else
                    {
                        logger->debug("64 bit");
                        bitness = 64;   
                    }
                }

                RegCloseKey(installRootKey);
            }
        }

        RegCloseKey(regKey);
    }
    else
    {
        logger->debug("No HKLM\\Software\\WOW6432Node\\Microsoft\\Office - assuming 32 bit");
        bitness = 32;
    }

    return bitness;
}

/**
 * Returns the Outlook version installed.
 *
 * @return 2013 for "Outlook 2013", 2010 for "Outlook 2010", 2007 for "Outlook
 * 2007" or 2003 for "Outlook 2003". -1 otherwise.
 */
int MAPIBitness_getOutlookVersion(JavaLogger* logger)
{
    int outlookVersions[] = {
        2016, // Outlook 2016
        2013, // Outlook 2013
        2010, // Outlook 2010
        2007, // Outlook 2007
        2003 // Outlook 2003
    };
    DWORD pathLength = 0;

    for(int i = 0; i < nbOutlookRegister; ++i)
    {
        if(MsiProvideQualifiedComponent(
                    outlookRegister[i],
                    TEXT("outlook.x64.exe"),
                    (DWORD) INSTALLMODE_DEFAULT,
                    NULL,
                    &pathLength)
                == ERROR_SUCCESS)
        {
            logger->info("Found outlook.x64.exe %d: %s", outlookVersions[i], outlookRegister[i]);
            return outlookVersions[i];
        }
        else if(MsiProvideQualifiedComponent(
                    outlookRegister[i],
                    TEXT("outlook.exe"),
                    (DWORD) INSTALLMODE_DEFAULT,
                    NULL,
                    &pathLength)
                == ERROR_SUCCESS)
        {
            logger->info("Found outlook.exe %d: %s", outlookVersions[i], outlookRegister[i]);
            return outlookVersions[i];
        }
    }

    logger->info("Didn't find any outlook version number");
    return -1;
}
