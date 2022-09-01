// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.io.*;
import java.util.*;

import org.jitsi.service.configuration.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.resources.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;

/**
 * Combo box which allows the user to select a directory to save recorded calls
 * to. There are always precisely two entries in the dropdown: the currently
 * selected folder, and a special entry 'Choose folder...' which opens a
 * folder selector dialog to allow the user to pick a new folder.
 */
public class CallRecordingDirSelectorComboBox
    extends PathSelectorComboBox
{
    private static final long serialVersionUID = 0L;

    /**
     * The service used to access configuration data.
     */
    private static final ConfigurationService configService =
                         GeneralConfigPluginActivator.getConfigurationService();

    /**
     * The service used to access resource files.
     */
    private static final ResourceManagementService resourceService =
                         GeneralConfigPluginActivator.getResources();

    /**
     * The <tt>Logger</tt> used by this
     * <tt>CallRecordingDirSelectorComboBox</tt> for logging output.
     */
    private static final Logger logger
        = Logger.getLogger(PathSelectorComboBox.class);

    public CallRecordingDirSelectorComboBox()
    {
        super(new HashMap<>(),
              SipCommFileChooser.DIRECTORIES_ONLY,
              resourceService.getI18NString(
                      "service.gui.CALL_RECORDING_CHOOSE_FOLDER_FAILURE_TITLE"),
              resourceService.getI18NString(
                      "service.gui.CALL_RECORDING_CHOOSE_FOLDER_FAILURE_TEXT"),
              null, true);
        setTooltipsEnabled(true);
        ScaleUtils.scaleFontAsDefault(this);
    }

    @Override
    protected LabelledPath getExistingCustomPath()
    {
        String path = configService.user().getString(Recorder.SAVED_CALLS_PATH,
                                   ConfigurationUtils.DEFAULT_SAVED_CALLS_PATH);
        String name = new File(path).getName();

        logger.debug("Dropdown selector determines call recording folder to " +
                     "be " + path);
        return new LabelledPath(name, path);
    }

    protected void onSelectionChanged(LabelledPath oldValue,
                                      LabelledPath newValue,
                                      int newIndex)
    {
        configService.user().setProperty(Recorder.SAVED_CALLS_PATH, newValue.mPath);
        GeneralConfigPluginActivator.getConferenceService()
                                       .setRecordingsDirectory(newValue.mPath);
    }

    @Override
    protected LabelledPath loadFile(File chosenDir)
    {
        // Before accepting the directory the user chose, we try writing a
        // temp file to it to ensure that the user actually has write
        // permissions on the directory. It's possible that folder permissions
        // might change later, but there's no harm in catching it early if we
        // can.

        logger.user("Location to save call recordings to changed to: " +
                            chosenDir.getAbsolutePath());

        Exception e = null;
        LabelledPath result = null;
        try
        {
            File temp = File.createTempFile("tmp", ".tmp", chosenDir);
            temp.deleteOnExit();
            temp.delete();
            result = super.loadFile(chosenDir);
        }
        catch (IOException ioex)
        {
            logger.debug("No write permission for call recording dir. " +
                "Reverting selection.", e);
            showInvalidFileDialog();
            abort(chosenDir, true);
        }

        return result;
    }
}
