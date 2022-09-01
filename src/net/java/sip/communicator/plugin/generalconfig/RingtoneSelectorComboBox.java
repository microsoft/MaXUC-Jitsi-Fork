// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import javax.swing.*;

import org.jitsi.service.audionotifier.*;
import org.jitsi.service.audionotifier.WavData.*;
import org.jitsi.service.audionotifier.WavFileVerifier.*;
import org.jitsi.service.audionotifier.WavFileVerifier.UnsupportedEncodingException;
import org.jitsi.service.configuration.*;
import org.jitsi.util.Logger;
import org.jitsi.util.SoundFileUtils;
import org.jitsi.util.StringUtils;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.notificationconfiguration.*;
import net.java.sip.communicator.plugin.notificationwiring.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.util.*;

/**
 * Dropdown selector to allow the user to choose ringtones from the list of
 * defaults, or add their own ringtone.
 */
public class RingtoneSelectorComboBox extends PathSelectorComboBox
{
    private static final long serialVersionUID = 0L;

    /**
     * Resource key prefix used for all errors loading the ringtone file.
     */
    private static final String WAV_ERROR_PREFIX =
                                          "service.gui.ringtone.WAV_ERROR_MSG_";

    /**
     * Resource key for the error shown when a user-selected wav file has an
     * unsupported sample rate.
     */
    private static final String UNSUPPORTED_SAMPLE_RATE_MSG_KEY =
                                   WAV_ERROR_PREFIX + "UNSUPPORTED_SAMPLE_RATE";

    /**
     * Resource key for the error shown when a user-selected wav file has an
     * unsupported bit depth.
     */
    private static final String UNSUPPORTED_BIT_DEPTH_MSG_KEY =
                                     WAV_ERROR_PREFIX + "UNSUPPORTED_BIT_DEPTH";

    /**
     * Resource key for the error shown when a user-selected wav file has an
     * unsupported number of channels.
     */
    private static final String UNSUPPORTED_NUM_CHANNELS_MSG_KEY =
                                  WAV_ERROR_PREFIX + "UNSUPPORTED_NUM_CHANNELS";

    /**
     * Resource key for the error shown when a user-selected wav file uses an
     * unsupported codec.
     */
    private static final String UNSUPPORTED_ENCODING_MSG_KEY =
                                      WAV_ERROR_PREFIX + "UNSUPPORTED_ENCODING";

    /**
     * Resource key for the error shown when a user-selected wav file cannot be
     * rendered by Accession, for an undetermined reason.
     */
    private static final String GENERIC_UNSUPPORTED_MSG_KEY =
                                       WAV_ERROR_PREFIX + "UNSUPPORTED_GENERIC";

    /**
     * Resource key for the error shown when the metadata of a user-selected
     * wav file cannot be parsed successfully.
     */
    private static final String INVALID_OR_CORRUPT_MSG_KEY =
                                        WAV_ERROR_PREFIX + "INVALID_OR_CORRUPT";

    /**
     * Resource key for the error shown when a user-selected wav file cannot be
     * loaded, due to an IOException.
     */
    private static final String IO_FAILURE_MSG_KEY =
                                                WAV_ERROR_PREFIX + "IO_FAILURE";

    /**
     * The name of the property holding the index of the currently active
     * ringtone.  Obsolete (and kept for back compatibility) since the selected
     * ringtone is stored under the CURRENT_RINGTONE_PATH_PROPNAME property
     */
    private static final String CURRENT_RINGTONE_IDX_PROPNAME =
              "net.java.sip.communicator.plugin.generalconfig.CURRENT_RINGTONE";

    /**
     * The name of the property holding the path of the currently active ringtone.
     */
    private static final String CURRENT_RINGTONE_PATH_PROPNAME =
         "net.java.sip.communicator.plugin.generalconfig.CURRENT_RINGTONE_PATH";

    /**
     * The name of the property indicating that choosing custom ringtones from
     * disc is prohibited.
     */
    private static final String CUSTOM_RINGTONES_DISABLED_PROPNAME =
     "net.java.sip.communicator.plugin.generalconfig.CUSTOM_RINGTONES_DISABLED";

    /**
     * The name of the property holding the URI of the user's custom ringtone,
     * if any.
     */
    private static final String CUSTOM_RINGTONE_URI_PROPNAME =
           "net.java.sip.communicator.plugin.generalconfig.CUSTOM_RINGTONE_URI";

    /**
     * The name of the property holding the user-visible display name of the
     * user's custom ringtone, if any.
     */
    private static final String CUSTOM_RINGTONE_NAME_PROPNAME =
          "net.java.sip.communicator.plugin.generalconfig.CUSTOM_RINGTONE_NAME";

    /**
     * The filename to use for our copy of the audio file provided by the user
     * as a custom ringtone.
     */
    private static final String CUSTOM_RINGTONE_FILENAME = "customRingtone.wav";

    /**
     * The service used to handle notification events. We use this to register
     * and unregister ringtones as incoming call notifications.
     */
    private static final NotificationService notificationService =
        GeneralConfigPluginActivator.getNotificationService();

    /**
     * The configuration service.
     */
    private static final ConfigurationService configService =
        GeneralConfigPluginActivator.getConfigurationService();

    /**
     * The <tt>Logger</tt> used by this <tt>RingtoneSelectorComboBox</tt> for
     * logging output.
     */
    private static final Logger logger
        = Logger.getLogger(RingtoneSelectorComboBox.class);

    /**
     * The title of the dialog shown when the user selects an invalid file.
     */
    private static final String INVALID_FILE_DIALOG_TITLE_TEXT =
        Resources.getResources().getI18NString("service.gui.INVALID_AUDIO_FILE");

    /**
     * The text of the dialog shown when the user selects an invalid file.
     */
    private static final String INVALID_FILE_DIALOG_BODY_TEXT =
        Resources.getResources().getI18NString(
                                      "service.gui.INVALID_AUDIO_FILE_MESSAGE");

    /**
     * The text of the dialog shown when the user selects a valid file but we
     * fail to load it.
     */
    private static final String LOAD_FAILED_TITLE =
        Resources.getResources().getI18NString(
                                      "service.gui.ERROR_CHANGING_RINGTONE");

    /**
     * Maximum length of a ringtone name before we replace terminal characters
     * with an ellipsis in the dropdown selector.
     * If this length is exceeded, only the first L-3 chars of the name will
     * be visible, followed by '...' (where L is this constant).
     */
    private static final int MAX_RINGTONE_NAME_LENGTH = 20;

    /**
     * A reference to the button used to preview ringtones - when the currently
     * selected ringtone is changed, we need to halt any playing previews.
     */
    private RingtonePreviewButton mPreviewButton;

    /**
     * Dropdown selector which allows the user to select the active ringtone
     * used by the app. Default options from the bundled resources are presented
     * along with the option to choose a wav file from the hard drive.
     *
     * @param defaultRingtones
     */
    public RingtoneSelectorComboBox(Map<String, String> defaultRingtones,
                                    RingtonePreviewButton previewButton)
    {
        super(defaultRingtones,
              SipCommFileChooser.FILES_ONLY,
              INVALID_FILE_DIALOG_TITLE_TEXT,
              INVALID_FILE_DIALOG_BODY_TEXT,
              new SoundFilter(new String[] {SoundFileUtils.wav}),
              false);

        ScaleUtils.scaleFontAsDefault(this);

        mPreviewButton = previewButton;

        AccessibilityUtils.setNameAndDescription(
            this,
            Resources.getResources().getI18NString("service.gui.RINGTONE_SELECTOR_LABEL"),
            Resources.getResources().getI18NString("service.gui.RINGTONE_SELECTOR_LABEL_DESCRIPTION"));

        setPathSelectorDialogTitle(Resources.getResources().getI18NString(
                                              "service.gui.CHOOSE_SOUND_FILE"));
    }

    @Override
    protected int getDefaultIndex()
    {
        // We should preselect the ringtone which config lists as currently
        // active.
        return getActiveRingtoneIdxFromConfig();
    }

    /**
     * Check config to obtain any custom ringtone previously added by the user.
     *
     * @return The Ringtone from config, or null if no custom ringtone was
     * found.
     */
    @Override
    protected LabelledPath getExistingCustomPath()
    {
        String ringtoneURI =
                      configService.user().getString(CUSTOM_RINGTONE_URI_PROPNAME, "");
        String ringtoneName =
                     configService.user().getString(CUSTOM_RINGTONE_NAME_PROPNAME, "");

        if (ringtoneURI.isEmpty())
        {
            logger.debug("No custom ringtone found in config.");
            return null;
        }

        // Try to get the name of the ringtone from config, defaulting to the
        // 'name' part of the URI if absent.
        ringtoneName = ringtoneName.isEmpty() ? new File(ringtoneURI).getName():
                                                ringtoneName;

        LabelledPath customRingtone;

        logger.debug("The ringtoneURI was: " + ringtoneURI);
        // We've already checked the URI is not empty, so we now remove the
        // 'file:' from the front of the string to give the file location.
        File ringtoneFile = new File(URLDecoder.decode(ringtoneURI.substring(5),
                                                       StandardCharsets.UTF_8));
        if (ringtoneFile.exists() && ringtoneFileRendersSuccessfully(ringtoneURI))
        {
            customRingtone = new LabelledPath(ringtoneName, ringtoneURI);
            logger.debug("Loaded custom ringtone " + customRingtone.mPath +
                         " successfully from config.");
        }
        else
        {
            // Set the ringtone to the default ringtone, which is MaX UC if it
            // exists, else it is the first ringtone alphabetically from our
            // list of default ringtones.
            String path = getPaths().get(getDefaultIndex()).mPath;
            logger.info("The custom ringtone is invalid: replacing by ringtone with path " + path);
            configService.user().setProperty(CURRENT_RINGTONE_PATH_PROPNAME, path);
            notificationService.registerNewRingtoneNotification(path);
            customRingtone = null;
        }

        return customRingtone;
    }

    /**
     * Currently the "MaX UC" ringtone is at index 11. Return that as default
     * unless that is not a valid path, in which case, return 0. This is only
     * called if all other methods of determining the ringtone have failed.
     *
     * @return The default ringtone index for our client.
     */
    private int getDefaultRingtoneIndex()
    {
        return (getPaths().size() >= 11) ? 11 : 0;
    }

    /**
     * Check config to determine which ringtone was active when the client was
     * last open; or else the default ringtone if the client hasn't been run
     * before.
     *
     * @return The index in the dropdown selector corresponding to the ringtone
     * to make active.
     */
    private int getActiveRingtoneIdxFromConfig()
    {
        int ringtoneIdx;

        // The path of the active ringtone is stored in config thus we must look
        // through all the ringtones to find the one with the same path. However
        // we historically stored the index of the ringtone (this fails when more
        // ringtones are added).  To cope with upgrade therefore we must consider
        // the index if there is no stored path.
        // First look to the path:
        String ringtonePath = configService.user().getString(CURRENT_RINGTONE_PATH_PROPNAME);

        if (ringtonePath == null)
        {
            // No stored ringtone path; try to get one from the notification
            // service.
            SoundNotificationAction incomingCallHdlr = null;

            if (notificationService != null)
            {
                incomingCallHdlr = (SoundNotificationAction)
                    (notificationService.getEventNotificationAction(
                                              NotificationManager.INCOMING_CALL,
                                              NotificationAction.ACTION_SOUND));
            }

            if (incomingCallHdlr != null)
            {
                ringtonePath = incomingCallHdlr.getDescriptor();
                logger.warn("No stored ringtone path; got " + ringtonePath +
                            " from the notification service");

                if (ringtonePath != null)
                {
                    configService.user().setProperty(CURRENT_RINGTONE_PATH_PROPNAME,
                                              ringtonePath);
                }
            }
        }

        if (ringtonePath != null)
        {
            // Got a stored ringtone path - use it
            logger.debug("Selected ringtone path: " + ringtonePath);
            List<LabelledPath> paths = getPaths();

            ringtoneIdx = -1;

            for (int i = 0; i < paths.size(); i++)
            {
                if (ringtonePath.equals(paths.get(i).mPath))
                {
                    ringtoneIdx = i;
                    break;
                }
            }

            if (ringtoneIdx == -1)
            {
                // We've not found the index matching the wav file, it's an error:
                logger.warn("Selected ringtone path not available " + ringtonePath);
                ringtoneIdx = getDefaultRingtoneIndex();
            }
        }
        else
        {
            logger.info("Ringtone path was null, and failed to get existing " +
                        "ringtone from the notification service; try lookup " +
                         " based on index");
            ringtoneIdx = configService.user().getInt(CURRENT_RINGTONE_IDX_PROPNAME, -1);

            if (ringtoneIdx == -1)
            {
                // Could not find a previous ringtone, so use the default.
                logger.debug("No previous ringtone in config; using default.");
                ringtoneIdx = getDefaultRingtoneIndex();
            }
            else if (ringtoneIdx >= getPaths().size())
            {
                logger.warn("Previous ringtone [index " + ringtoneIdx +
                                             "] no longer available - use default");
                ringtoneIdx = getDefaultRingtoneIndex();
            }
            else
            {
                String path = getPaths().get(ringtoneIdx).mPath;
                logger.debug("Ringtone index is valid: " + ringtoneIdx +
                             "; path is " + path);
                configService.user().setProperty(CURRENT_RINGTONE_PATH_PROPNAME,
                                          path);
            }
        }

        return ringtoneIdx;
    }

    /**
     * Changes the active ringtone to the one specified.
     *
     * @param ringtone The ringtone to use.
     */
    private void changeRingtone(LabelledPath ringtone)
    {
        String ringtonePath = ringtone.mPath;
        configService.user().setProperty(CURRENT_RINGTONE_PATH_PROPNAME, ringtonePath);

        if (notificationService != null)
        {
            notificationService.registerNewRingtoneNotification(ringtonePath);
        }
    }

    /**
     * Checks config to determine whether the user is to be allowed to choose
     * sound files to use as their ringtone. By default, they are allowed to.
     *
     * @return True iff the user is permitted to choose custom ringtones.
     */
    @Override
    protected boolean checkCustomPathsAllowed()
    {
        boolean allowed = !configService.user().getBoolean(
                                     CUSTOM_RINGTONES_DISABLED_PROPNAME, false);
        logger.debug("Custom ringtones allowed? " + allowed);

        return allowed;
    }

    /**
     * Determines if the specified file is suitable for use as a ringtone,
     * by trying to render it and checking that no error occurs.
     * This process doesn't play any audio to the user; the output of the
     * rendering is dropped.
     *
     * @param path The file to check.
     * @return True if the given file is valid.
     */
    public boolean ringtoneFileRendersSuccessfully(String path)
    {
        if (path == null)
        {
            return false;
        }

        AudioNotifierService audioNotifServ =
                     GeneralConfigPluginActivator.getAudioNotifierService();
        SCAudioClip testAudio = audioNotifServ.createAudio(path);
        return testAudio.testRender();
    }

    /**
     * Checks that the given file is valid; and if it is, loads it and adds
     * it to the dropdown list.
     */
    @Override
    protected LabelledPath loadFile(File file)
    {
        boolean previousEntryIsValid = true;
        Exception error;

        try
        {
            return loadFileInTry(file);
        }
        catch (UnsupportedSampleRateException e)
        {
            error = e;

            String[] args = new String[]
            {
                e.mInvalidValue.toString(),
                StringUtils.join(
                                WavFileVerifier.SUPPORTED_SAMPLE_RATES_HZ, ", ")
            };

            showLoadFailureDialog(
                    Resources.getString(UNSUPPORTED_SAMPLE_RATE_MSG_KEY, args));
        }
        catch (UnsupportedBitDepthException e)
        {
            error = e;

            String[] args = new String[]
            {
                e.mInvalidValue.toString(),
                StringUtils.join(WavFileVerifier.SUPPORTED_BIT_DEPTHS, ", ")
            };

            showLoadFailureDialog(
                  Resources.getString(UNSUPPORTED_BIT_DEPTH_MSG_KEY, args));
        }
        catch (UnsupportedNumberOfChannelsException e)
        {
            error = e;

            String[] args = new String[]
            {
                e.mInvalidValue.toString(),
                Integer.toString(WavFileVerifier.MIN_NUM_CHANNELS),
                Integer.toString(WavFileVerifier.MAX_NUM_CHANNELS)
            };

            showLoadFailureDialog(
                  Resources.getString(UNSUPPORTED_NUM_CHANNELS_MSG_KEY, args));
        }
        catch (UnsupportedEncodingException e)
        {
            error = e;
            showLoadFailureDialog(
                             Resources.getString(UNSUPPORTED_ENCODING_MSG_KEY));
        }
        catch (UnsupportedWavFileException e)
        {
            error = e;
            showLoadFailureDialog(
                              Resources.getString(GENERIC_UNSUPPORTED_MSG_KEY));
        }
        catch (InvalidWavDataException e)
        {
            error = e;
            showLoadFailureDialog(
                               Resources.getString(INVALID_OR_CORRUPT_MSG_KEY));
        }
        catch (IOException e)
        {
            error = e;
            showLoadFailureDialog(Resources.getString(IO_FAILURE_MSG_KEY));
        }
        catch (SelectedRingtoneInvalidated e)
        {
            error = e;
            showLoadFailureDialog(Resources.getString(IO_FAILURE_MSG_KEY));
            previousEntryIsValid = false;
        }

        // If we get this far, the load failed because we hit an exception.
        logger.error("Wav file load failed", error);
        abort(file, previousEntryIsValid);
        return null;
    }

    private LabelledPath loadFileInTry(File file)
        throws IOException, InvalidWavDataException,
               UnsupportedWavFileException, SelectedRingtoneInvalidated
    {
        logger.info("Loading custom ringtone from file " + file);
        String path = file.toURI().toString();

        WavData wavData = new WavData(file);
        WavFileVerifier.assertWavFileIsSupported(wavData);

        // As a final safety check, try to render the WAV file without playing
        // it, to ensure it's definitely playable.
        if (!ringtoneFileRendersSuccessfully(path))
        {
            logger.error("WAV file failed to render. Abort load.");
            throw new UnsupportedWavFileException();
        }

        String ringtoneName = file.getName();
        logger.debug("Ringtone " + ringtoneName + " was valid.");

        // Copy the ringtone file to the app directory so that it can be played
        // even if the original is tampered with.
        String homeDirLocation = configService.user().getScHomeDirLocation();
        String homeDirName = configService.user().getScHomeDirName();
        File target = new File(homeDirLocation +
                               File.separator +
                               homeDirName +
                               File.separator +
                               CUSTOM_RINGTONE_FILENAME);

        try
        {
            FileUtils.copyFile(file, target);
        }
        catch (IOException e)
        {
            // If the user previously had a custom ringtone selected, it is now
            // invalid; flag this by throwing a SelectedRingtoneInvalidated
            // exception.
            logger.error("Failed to save user-selected ringtone.", e);
            throw new SelectedRingtoneInvalidated(e);
        }

        String targetPath = target.toURI().toString();

        // Truncate the name of the ringtone if it is too long.
        ringtoneName = (ringtoneName.length() <= MAX_RINGTONE_NAME_LENGTH) ?
                        ringtoneName :
                        ringtoneName.substring(0, MAX_RINGTONE_NAME_LENGTH) +
                            "...";

        LabelledPath userTone = new LabelledPath(ringtoneName, targetPath);

        configService.user().setProperty(CUSTOM_RINGTONE_URI_PROPNAME, targetPath);
        configService.user().setProperty(CUSTOM_RINGTONE_NAME_PROPNAME, ringtoneName);

        logger.info("Ringtone " + ringtoneName + " loaded successfully.");
        return userTone;
    }

    @Override
    protected void onSelectionChanged(LabelledPath oldValue,
                                      LabelledPath newValue,
                                      int newIndex)
    {
        // The user has changed the entry selected in the dropdown, so stop
        // playing any audio previews.
        mPreviewButton.stopAudio();

        logger.user("Ringtone changed from " + oldValue + " to " + newValue);
        changeRingtone(newValue);
    }

    /**
     * Creates and shows the dialog which tells the user that their custom
     * ringtone failed to load.
     * This should be used only for IO errors; invalid ringtone files should
     * be indicated by calling abort(file, true, *).
     */
    private void showLoadFailureDialog(final String msg)
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                   showLoadFailureDialog(msg);
               }
            });
            return;
        }
        ErrorDialog dialog = new ErrorDialog(null,
                                             LOAD_FAILED_TITLE,
                                             msg);
        dialog.setAlwaysOnTop(true);
        dialog.setResizable(false);
        dialog.setVisible(true);
    }

    /**
     * Thrown if a ringtone load fails in such a way that the currently selected
     * ringtone may now be invalid.
     */
    private static class SelectedRingtoneInvalidated extends Exception
    {
        private static final long serialVersionUID = 0L;

        public SelectedRingtoneInvalidated(Throwable t)
        {
            super(t);
        }
    }

    /**
     * Called when the ringtone selector dropdown is made visible. This allows
     * us to ensure we don't update the dropdown box directly after this
     * happens, by setting mJustUpdatedRingtone. This prevents a race condition
     * when we are changing the ringtone.
     */
    @Override
    protected void onPopupMenuWillBecomeVisible()
    {
        super.onPopupMenuWillBecomeVisible();
        GeneralConfigurationPanel.mJustUpdatedRingtone.set(true);
    }
}
