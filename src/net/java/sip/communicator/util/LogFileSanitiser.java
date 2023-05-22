// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import static net.java.sip.communicator.util.PrivacyUtils.getLoggableCPURL;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Utility function for sanitising log files.
 */
public class LogFileSanitiser
{
    private static final Logger sLog = Logger.getLogger(LogFileSanitiser.class);

    /**
     * Writes a sanitised version of the input file redacting the session ID.
     *
     * @param inputFile the log file to sanitise
     * @param outputFile where to write the sanitised file
     */
    public static void sanitiseFile(File inputFile, File outputFile)
    {
        sLog.debug("Sanitise log file");

        BufferedWriter outputBufferedWriter = null;
        BufferedReader inputBufferedReader = null;

        if (inputFile == null || !inputFile.exists())
        {
            sLog.error("Failed to find input config file: " + inputFile);
            // File can't be dirty if it doesn't exist!
            return;
        }

        if (outputFile == null)
        {
            sLog.error("Failed to find output config file: " + outputFile);
            return;
        }

        if (outputFile.exists())
        {
            // Need to remove the existing output file in order to write the new one.
            // This shouldn't happen as we should tidy-up each time we write a safe file.
            sLog.warn("Need to delete old safe file");
            outputFile.delete();
        }

        try
        {
            inputBufferedReader  = new BufferedReader(new FileReader(inputFile));
            outputBufferedWriter = new BufferedWriter(new FileWriter(outputFile));
            sLog.debug("process log file");

            for (String line; (line = inputBufferedReader.readLine()) != null; )
            {
                // Sanitise any CommPortal URLs in each log line
                outputBufferedWriter.write(getLoggableCPURL(line));
                outputBufferedWriter.newLine();
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
    }
}
