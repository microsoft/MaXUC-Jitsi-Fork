// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import java.io.*;
import java.util.*;

/**
 * Detect the Locale from the environment and set the required Java properties.
 *
 * In theory, the JRE should be able to pick up the locale from the OS, but
 * this isn't working the Apple JRE we're currently using.
 *
 * Docs on the net suggest that <locale>.ljproj directories are required in the
 * app bundle - but we have these and the locale still isn't being correctly
 * passed through.
 */
public class MacLocaleFixer
{
    private static final Logger logger = Logger.getLogger(MacLocaleFixer.class);

    private static final ProcessLogger processLogger = ProcessLogger.getLogger();

    /**
     * Run a command on the command line and return a string of the result.
     *
     * @param cmdLine The command to run
     * @return The stdout from running the command.
     *
     * @throws IOException if the command failed to run.
     */
    private String cmdExec(String cmdLine) throws IOException
    {
        processLogger.traceExec(cmdLine);
        String line;
        StringBuilder output = new StringBuilder();
        Process p = Runtime.getRuntime().exec(cmdLine);
        BufferedReader input =
            new BufferedReader(new InputStreamReader(p.getInputStream()));
        while ((line = input.readLine()) != null)
        {
            output.append(line).append('\n');
        }

        input.close();
        ProcessUtils.dispose(p);
        logger.trace("Command result " + output);
        return output.toString();
    }

    /**
     * Read the locales from the OS and set the required Java properties.
     */
    public void fixLocales()
    {
        try
        {
            String rawLocale =
                cmdExec("defaults read .GlobalPreferences AppleLocale");

            String language = rawLocale.split("_")[0].trim();
            String country = rawLocale.split("_")[1].trim();

            Locale.setDefault(new Locale(language, country));
            System.setProperty("user.country", country);
            System.setProperty("user.language", language);

            logger.info(String.format(
                "Fixed locale on Mac. Country=%s Language=%s",
                country,
                language));
        }
        catch (Throwable e)
        {
            logger.warn("Failed to fix locale on Mac", e);
        }
    }
}
