package net.java.sip.communicator.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import static net.java.sip.communicator.util.PrivacyUtils.sanitiseFilePath;

import com.google.common.annotations.VisibleForTesting;

/**
 * Java crash logs are generated when JVM crashes. Clean up usernames from file
 * paths as well as the USERNAME property in these logs.
 */
public class JavaCrashLogSanitiser
{
    private static final Logger sLog = Logger.getLogger(JavaCrashLogSanitiser.class);
    public static void sanitiseJavaCrashLogFile(File inputFile, File outputFile)
    {
        sLog.debug("Sanitise java crash log: " + (inputFile != null ? inputFile.getName() : "null"));

        if (inputFile == null || !inputFile.exists())
        {
            sLog.error("Failed to find input java crash log file");
            return;
        }

        if (outputFile == null)
        {
            sLog.error("Output java crash log file is null");
            return;
        }

        if (outputFile.exists())
        {
            sLog.warn("Need to delete old safe file");
            outputFile.delete();
        }

        try (
            BufferedReader inputBufferedReader =
                new BufferedReader(new FileReader(inputFile));
            BufferedWriter outputBufferedWriter =
                new BufferedWriter(new FileWriter(outputFile)))
        {
            for (String line; (line = inputBufferedReader.readLine()) != null;)
            {
                outputBufferedWriter.write(sanitiseJavaCrashLogLine(line));
                outputBufferedWriter.newLine();
            }
        }
        catch (FileNotFoundException e)
        {
            sLog.error("Cannot find java crash log file to include", e);
        }
        catch (IOException e)
        {
            sLog.error("Cannot read or write java crash log to include", e);
        }
    }

    @VisibleForTesting
    static String sanitiseJavaCrashLogLine(String inputString)
    {
        // Remove logged username in the format USERNAME=(username)
        String usernameRemovedLine = inputString.replaceAll("(?i)(?<=USERNAME=).+", PrivacyUtils.REDACTED);
        // Clean username from logged file paths
        return sanitiseFilePath(usernameRemovedLine);
    }
}
