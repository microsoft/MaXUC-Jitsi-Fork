// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility functions for sanitising config files.
 *
 * Note, there is an [almost] duplicate of this class in libjitsi/org.jitsi.util. Please
 * keep in sync (or maybe we can commonize).
 */
public class ConfigFileSanitiser
{
    private static final Logger sLog = Logger.getLogger(ConfigFileSanitiser.class);

    /**
     * @param inputFile the config file to sanitise
     * @param dirtyStrings config key suffixes that when present in the file mean it is dirty
     * @return whether the input file was dirty
     */
    public static boolean isDirty(File inputFile, String[] dirtyStrings)
    {
        sLog.debug("Check if file is dirty");
        return sanitiseOrCheckFile(inputFile, null, dirtyStrings);
    }

    /**
     * Writes a sanitised version of the input file removing any line in the input file
     * that appears to contain security info.
     *
     * @param inputFile the config file to sanitise
     * @param outputFile where to write the sanitised file
     * @param dirtyStrings strings to be removed
     */
    public static void sanitiseFile(File inputFile, File outputFile, String[] dirtyStrings)
    {
        sLog.debug("Sanitise config file");
        sanitiseOrCheckFile(inputFile, outputFile, dirtyStrings);
    }

    /**
     * Removes any line in the passed file that appears to contain security info.
     *
     * Will perform a dry run (no writing) if no output file is provided.
     *
     * @param inputFile the file to sanitise
     * @param outputFile where to write the sanitised file, or null to prevent writing
     *        (i.e. just perform a check).
     * @param dirtyStrings an array of config key suffixes considered dangerous
     * @return whether the input file was dirty (only valid for config files)
     */
    private static boolean sanitiseOrCheckFile(File inputFile,
                                          File outputFile,
                                          String[] dirtyStrings)
    {
        boolean writeOutput = outputFile != null;
        boolean wasDirty = false;

        BufferedWriter outputBufferedWriter = null;
        BufferedReader inputBufferedReader = null;

        if (inputFile == null || !inputFile.exists())
        {
            sLog.error("Failed to find input config file: " + inputFile);
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

        try
        {
            inputBufferedReader  = new BufferedReader(new FileReader(inputFile));
            outputBufferedWriter = writeOutput ? new BufferedWriter(new FileWriter(outputFile)) : null;

            for (String line; (line = inputBufferedReader.readLine()) != null; )
            {
                // Only copy across safe lines
                String key = line.split("=", 2)[0];
                boolean removeLine = false;

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
                    sLog.warn("Dirty config found: " + key);
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
        finally
        {
            if (inputBufferedReader != null)
            {
                try
                {
                    inputBufferedReader.close();
                }
                catch (IOException e)
                {
                    sLog.error("Cannot close input config file", e);
                }
            }

            if (outputBufferedWriter != null)
            {
                try
                {
                    outputBufferedWriter.close();
                }
                catch (IOException e)
                {
                    sLog.error("Cannot close sanitised config file", e);
                }
            }
        }

        return wasDirty;
    }
}
