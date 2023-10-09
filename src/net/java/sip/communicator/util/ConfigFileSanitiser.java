// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.json.simple.JSONObject;

import static java.util.Collections.emptySet;
import static net.java.sip.communicator.util.PrivacyUtils.*;
import static org.jitsi.util.SanitiseUtils.*;
import static org.jitsi.util.Hasher.logHasher;

import org.jitsi.util.Hasher;
import org.jitsi.util.StringUtils;

/**
 * Utility functions for sanitising config files.
 */
public class ConfigFileSanitiser
{
    private static final Logger sLog = Logger.getLogger(ConfigFileSanitiser.class);

    /**
     * The provisioning.cos.BASE_INFO data we receive from SIP-PS is in a JSON format.
     * The values associated with the following set of keys in the JSON need to be hashed
     * for logging purposes.
     */
    private static final Set<String> BASE_INFO_HASHED_PROPERTIES = Set.of(
            "DisplayName",
            "PhoneNumber"
    );

    /**
     * The provisioning.cos.EAS_BG_INFO data we receive from SIP-PS is in a JSON format.
     * The values associated with the following set of keys in the JSON need to be hashed
     * for logging purposes.
     */
    private static final Set<String> EAS_BG_INFO_HASHED_PROPERTIES = Set.of(
            "AdministrationDepartment",
            "Department",
            "BusinessGroup"
    );

    /**
     * The provisioning.cos.GROUP_INFO data we receive from SIP-PS is in a JSON format.
     * The values associated with the following set of keys in the JSON need to be hashed
     * for logging purposes.
     */
    private static final Set<String> GROUP_INFO_HASHED_PROPERTIES = Set.of(
            "BusinessGroupName",
            "Department",
            "MetaSwitchName",
            "PersistentProfile",
            "line"
    );

    /**
     * The provisioning.cos.GROUP_INFO data we receive from SIP-PS is in a JSON format.
     * The values associated with the following set of keys in the JSON need to be removed
     * for logging purposes.
     */
    private static final Set<String> GROUP_INFO_REMOVE = Set.of(
            "SIPUserName",
            "SubscriberTimezone"
    );

    private static final List<String> ACCOUNT_PROPERTIES = List.of(
            ACTIVE_USER, AUTHORIZATION_NAME, CUSTOM_STATUS,
            DIAL_IN_1, DIAL_IN_2, DISPLAY_NAME, EAS_BUSINESS_GROUP,
            GLOBAL_DISPLAY_NAME, NUMBER, PASSWORD, USERNAME
    );

    /**
     * @param inputFile the config file to sanitise
     * @param dirtyStrings config key suffixes that when present in the file mean it is dirty
     * @return whether the input file was dirty
     */
    public static boolean isConfigFileDirty(File inputFile, String[] dirtyStrings)
    {
        sLog.debug("Check if file is dirty");
        return sanitiseOrCheckConfigFile(inputFile, null, dirtyStrings, false);
    }

    /**
     * Writes a sanitised version of the input file removing any line in the input file
     * that appears to contain security info.
     * When hashPII is true, also hashes any items of PII.
     *
     * @param inputFile the config file to sanitise
     * @param outputFile where to write the sanitised file
     * @param dirtyStrings strings to be removed
     * @param hashPII when true, also hash PII in the config file
     */
    public static void sanitiseConfigFile(File inputFile, File outputFile, String[] dirtyStrings, boolean hashPII)
    {
        sLog.debug("Sanitise config file");
        sanitiseOrCheckConfigFile(inputFile, outputFile, dirtyStrings, hashPII);
    }

    /**
     * Removes any line in the passed file that appears to contain security info.
     * When hashPII is true, also hashes any items of PII.
     *
     * Will perform a dry run (no writing) if no output file is provided.
     *
     * @param inputFile the file to sanitise
     * @param outputFile where to write the sanitised file, or null to prevent writing
     *        (i.e. just perform a check).
     * @param dirtyStrings an array of config key suffixes considered dangerous
     * @param hashPII when true, also hash PII in the config file
     * @return whether the input file was dirty (only valid for config files)
     */
    private static boolean sanitiseOrCheckConfigFile(File inputFile,
                                                     File outputFile,
                                                     String[] dirtyStrings,
                                                     boolean hashPII)
    {
        boolean writeOutput = outputFile != null;
        boolean wasDirty = false;

        if (inputFile == null || !inputFile.exists())
        {
            sLog.error("Failed to find input config file: " +
                       (inputFile != null ? inputFile.getName() : "null"));
            // File can't be dirty if it doesn't exist!
            return false;
        }

        if (writeOutput && outputFile.exists())
        {
            // Need to remove the existing output file in order to write the new one.
            // This shouldn't happen as we should tidy-up each time we write a safe file.
            sLog.warn("Need to delete old safe file");
            outputFile.delete();
        }

        try(BufferedReader inputBufferedReader  = new BufferedReader(new FileReader(inputFile));
            BufferedWriter outputBufferedWriter = writeOutput ? new BufferedWriter(new FileWriter(outputFile)) : null;)
        {
            for (String line; (line = inputBufferedReader.readLine()) != null; )
            {
                // values in property files could be escaped, particularly a colon escaped with a backslash,
                // so unescape it before check.
                String[] splitParts = unescapeColon(line).split("=", 2);
                String key = splitParts[0];
                String value = splitParts.length > 1 ? splitParts[1] : "";
                boolean removeLine = false;

                // Only copy across safe lines
                for (String dirtyString : dirtyStrings)
                {
                    if (key.endsWith(dirtyString))
                    {
                        removeLine = true;
                        break;
                    }
                }

                if (removeLine)
                {
                    sLog.interval(5, "Dirty config found.");
                    wasDirty = true;

                    if (!writeOutput)
                    {
                        // Found some dirt and only checking,
                        // so we don't need to look any further.
                        break;
                    }
                }
                else if (writeOutput)
                {
                    if (hashPII && (isPrivacyProperty(key)))
                    {
                        // Sanitise the key and value separately.
                        line = sanitiseConfigPropertyForLogging(key) + "=" +
                               sanitiseConfigValueForLogging(key, value);
                    }
                    outputBufferedWriter.write(line);
                    outputBufferedWriter.newLine();
                }
            }
        }
        catch (FileNotFoundException e)
        {
            sLog.error("Cannot find config file to include", e);
        }
        catch (IOException e)
        {
            sLog.error("Cannot read or write config to include", e);
        }

        return wasDirty;
    }

    /**
     * Given a propertyName in our config, this method will sanitise the associated value. We provide
     * propertyName as a parameter, as this will allow us to infer the format of the value parameter,
     * and thus what sanitisation functions we need to apply to the latter.
     *
     * @param propertyName the config property name
     * @param value the value associated with the property
     * @return the sanitised value associated with the property
     */
    public static String sanitiseConfigValueForLogging(final String propertyName, final String value)
    {
        if (StringUtils.isNullOrEmpty(value))
        {
            return value;
        }

        String sanitisedValue;
        if (propertyName.contains(BASE_INFO))
        {
            // provisioning.cos.BASE_INFO. These values arrive as JSON attribute-value pairs.
            sanitisedValue = sanitiseJSON(value,
                                 BASE_INFO_HASHED_PROPERTIES,
                                 emptySet(),
                                 ConfigFileSanitiser::sanitiseUnderscoreObject);
        }
        else if (propertyName.contains(CHATROOM))
        {
            // We want to hash the chatroom ID (or, if propertyName is the chatRoomSubject,
            // then just the subject of the chatroom).
            if (propertyName.contains(CHAT_SUBJECT))
            {
                sanitisedValue = logHasher(value);
            }
            else
            {
                sanitisedValue = sanitiseChatRoom(value);
            }
        }
        else if (propertyName.contains(EAS_BG_INFO))
        {
            // provisioning.cos.EAS_BG_INFO. These values arrive as JSON attribute-value pairs.
            sanitisedValue = sanitiseJSON(value,
                                 EAS_BG_INFO_HASHED_PROPERTIES,
                                 emptySet());
        }
        else if (propertyName.contains(ENTITY_CAPS_MANAGER))
        {
            // String relating to Jabber. Exposes chat address.
            sanitisedValue = sanitiseChatAddress(value);
        }
        else if (propertyName.contains(ACCOUNT_UID) || propertyName.contains(USER_ID))
        {
            if (SUBSCRIBER_DN_ONLY.matcher(value).matches())
            {
                // only DN - hash the whole string
                sanitisedValue = logHasher(value);
            }
            else
            {
                // keep prefix/suffix and hash DN only
                sanitisedValue = sanitise(value, SUBSCRIBER_DN_ONLY, str -> "_" + logHasher(str));
            }
        }
        else if (GUI_ACC.matcher(propertyName).find())
        {
            // These lines are of the form
            // "net.java.sip.communicator.impl.gui.accounts.acc1651050737630=ctd2345550000"
            // The number following the "acc" is UTC time (not Personal Data), so we do NOT
            // hash the property itself. Only its value is of interest here.

            // Some of these values may have "bg" or "ctd" prefixes on the DN (i.e. ctd2345550000 above).
            // Replacing the DN with an alphanumeric hash may make it harder to
            // distinguish when searching the logs. Add an underscore to make parsing simpler.
            sanitisedValue = sanitise(value, SUBSCRIBER_DN_ONLY, str -> "_" + logHasher(str));
        }
        else if (propertyName.contains(GROUP_INFO))
        {
            // provisioning.cos.GROUP_INFO. These values arrive as JSON attribute-value pairs.
            sanitisedValue = sanitiseJSON(value,
                                 GROUP_INFO_HASHED_PROPERTIES,
                                 GROUP_INFO_REMOVE);
        }
        else if (ACCOUNT_PROPERTIES.stream().anyMatch(propertyName::contains))
        {
            sanitisedValue = logHasher(value);
        }
        else if (propertyName.contains(CTD_MY_PHONES))
        {
            sanitisedValue = logHasher(value);
        }
        else if (propertyName.contains(SCHEDULED_ASSISTANTS) && value.startsWith("[") && value.endsWith("]"))
        {
            String[] values = value.substring(1, value.length()-1).split(", ");
            sanitisedValue = "[" + sanitiseValuesInList(List.of(values), Hasher::logHasher) + "]";
        }
        else
        {
            String withoutLastComponent = propertyName.substring(0, propertyName.lastIndexOf(".") + 1);
            if (!isPrivacyProperty(withoutLastComponent))
            {
                // For "net.java.sip.communicator.impl.protocol.sip.acc1234567=acc1234567"
                sanitisedValue = sanitise(value, SUBSCRIBER_DN_ONLY, str -> "_" + logHasher(str));
            }
            else
            {
                sanitisedValue = value;
            }
        }

        return sanitisedValue;
    }

    /**
     * Sanitises any propertyName from the server config by hashing
     * the Directory Number from any property we receive that is in the list of
     * server properties known to expose this value.
     *
     * @param propertyName the config property name
     * @return the sanitised propertyName
     */
    public static String sanitiseConfigPropertyForLogging(String propertyName)
    {
        return sanitiseFirstPatternMatch(propertyName, PRIVACY_PATTERNS, str -> "_" + logHasher(str));
    }

    /**
     * In some cases, the JSON we need to sanitise is in a horrid format, where multiple values in different JSONObjects
     * are mapped with the same key "_".
     *
     * For example:
     * "SensitiveEmail":{"_":"(we need to hash this)@email.com"},"SensitiveDN":{"_":(we need to hash this user DN)}, ...
     * This is a custom sanitiser that we pass to SanitiseUtils.sanitiseJSONData() to handle this case.
     *
     * @param obj JSON data to sanitise
     */
    private static String sanitiseUnderscoreObject(Object obj)
    {
        if (obj instanceof JSONObject && ((JSONObject) obj).containsKey("_"))
        {
            return sanitiseJSON((JSONObject) obj, Set.of("_"), emptySet());
        }

        return logHasher(obj);
    }

    /**
     * JSON content stored in a property file has colon character escaped with a forward slash.
     * This method unescapes it by removing the slash.
     */
    private static String unescapeColon(String value)
    {
        return value.replaceAll("\\\\:", ":");
    }
}
