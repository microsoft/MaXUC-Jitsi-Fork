// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.generalconfig;

import java.awt.event.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;
import org.jitsi.util.OSUtils;

/**
 * The PathSelectorComboBox allows the user to select from one or more paths
 * from a combo box, with a special entry in the dropdown which opens a file
 * selector, allowing the user to add a new path to the list.
 *
 * The PathSelectorComboBox can be provided with several default paths, but
 * can only maintain one user-selected path at a time, which will be overwritten
 * by subsequent selections by the user.
 *
 * Subclasses can override methods in this superclass to easily perform
 * validation of the selected file/folder (and to roll back state if the
 * selection is invalid), to specify whether it is files or folders that are to
 * be selected (and the format of files which are permitted), and to determine
 * the user-visible names given to paths in the dropdown selector.
 */
public abstract class PathSelectorComboBox extends JComboBox<String>
                                           implements KeyListener
{
    private static final long serialVersionUID = 1L;

    private static final Logger logger
        = Logger.getLogger(PathSelectorComboBox.class);

    /**
     * The 'Select file...' text to use in the dropdown if we are in file
     * selection mode.
     */
    private static final String SELECT_FILE_TEXT =
              Resources.getResources().getI18NString("service.gui.SELECT_FILE");

    /**
     * The 'Select folder...' text to use in the dropdown if we are in folder
     * selection mode.
     */
    private static final String SELECT_FOLDER_TEXT =
            Resources.getResources().getI18NString("service.gui.SELECT_FOLDER");

    /**
     * Holds either SELECT_FILE_TEXT or SELECT_FOLDER_TEXT, depending on
     * whether we are set up to select files or folders.
     */
    private final String mSelectPathText;

    /**
     * The title of the file/folder selector dialog that appears when the user
     * opts to select a path themselves.
     */
    private String mPathSelectorDialogTitle;

    /**
     * The filter used to determine what format of files are to be permitted.
     */
    private SipCommFileFilter mFileFilter;

    /**
     * Whether or not the 'All files (*.*)' option in the format dropdown of the
     * file selector is to be provided. This does not affect MacOSX, which never
     * offers an 'All files' option if the format has been specified.
     */
    private boolean mUseAllFilesFilter = true;

    /**
     * The title of the dialog that is shown if validation of the selected path
     * fails.
     */
    private final String mInvalidChoiceTitle;

    /**
     * The body text of the dialog that is shown if validation of the selected
     * path fails.
     */
    private final String mInvalidChoiceText;

    /**
     * Whether we are in folder selection or file selection mode.
     * This should be one of {@link SipCommFileChooser.FILES_ONLY}
     * or {@link SipCommFileChooser.DIRECTORIES_ONLY}.
     */
    private final int mSelectionMode;

    /**
     * The model which holds the contents of the dropdown.
     */
    private final DefaultComboBoxModel<String> mPathSelectorModel;

    /**
     * The paths that the user is able to choose from.
     */
    private final List<LabelledPath> mPaths;

    /**
     * Holds the path that the user chose for themselves in the file/folder
     * selector dialog, if any.
     */
    private LabelledPath mCustomPath = null;

    /**
     * The currently-chosen path. This is not necessarily the same as the
     * currently selected entry in the dropdown, which may for example be
     * 'Choose file...' if the user has clicked to make a custom choice of path
     * and is currently in the path selector dialog.
     */
    private LabelledPath mCurrentPath = null;

    /**
     * The listener used to handle changes to, and interactions with,
     * the path-selector dropdown.
     */
    private final DropdownListener mDropdownListener = new DropdownListener();

    /**
     * The thread used to validate and load custom paths selected by the user.
     * It is enforced that only one PathLoaderThread can be running at a given
     * time.
     */
    private PathLoaderThread mPathLoader;

    /**
     * The lock used when accessing the mPathLoader variable.
     */
    private final Object sPathLoaderLock = new Object();

    /**
     * Whether or not hovertext should be shown in the dropdown selector.
     * If <tt>true</tt>, then hovering over the selector will show the full
     * path of the currently-selected entry.
     */
    private boolean mTooltipsEnabled = false;

    /**
     * Create the PathSelectorComboBox.
     *
     * @param defaultPaths The default, immutable paths to be displayed in the
     * dropdown, keyed by the display name that will be used for the entry when
     * shown in the dropdown list.
     *
     * @param selectionMode Whether we are to select just files or just folders.
     * This should be one of {@link SipCommFileChooser.FILES_ONLY}
     * or {@link SipCommFileChooser.DIRECTORIES_ONLY}.
     *
     * @param invalidChoiceTitle The title of the error dialog shown if the
     * user selects an invalid path.
     *
     * @param invalidChoiceText The body text of the error dialog shown if the
     * user selects an invalid path.
     */
    public PathSelectorComboBox(Map<String, String> defaultPaths,
                                int selectionMode,
                                String invalidChoiceTitle,
                                String invalidChoiceText,
                                SipCommFileFilter filter,
                                boolean useAllFilesFilter)
    {
        mInvalidChoiceTitle = invalidChoiceTitle;
        mInvalidChoiceText = invalidChoiceText;
        mSelectionMode = selectionMode;
        mFileFilter = filter;
        mUseAllFilesFilter = useAllFilesFilter;
        addKeyListener(this);

        mPaths = new ArrayList<>();
        // Store all the default paths.
        for (String label : defaultPaths.keySet())
        {
            mPaths.add(new LabelledPath(label, defaultPaths.get(label)));
        }

        mPathSelectorModel = new DefaultComboBoxModel<>();
        setModel(mPathSelectorModel);

        // Show either 'Choose file...' or 'Choose folder...' at the end of the
        // dropdown list, depending on which selection mode we are in.
        mSelectPathText = (selectionMode == SipCommFileChooser.FILES_ONLY) ?
                                                             SELECT_FILE_TEXT :
                                                             SELECT_FOLDER_TEXT;

        // Default to titling the path selector dialog with the same text as the
        // 'Choose x...' entry in the dropdown.
        mPathSelectorDialogTitle = mSelectPathText;

        // Listen for changes in the currently selected item, as well as for
        // the dropdown menu being shown and hidden.
        addItemListener(mDropdownListener);
        addPopupMenuListener(mDropdownListener);
    }

    public void selectDefaultItem()
    {

        ArrayList<String> pathNames = new ArrayList<>();
        mPaths.forEach(labelledPath -> pathNames.add(labelledPath.mLabel));

        if (checkCustomPathsAllowed())
        {
            // The user is permitted to select their own paths, so we check to
            // see if such a selection has been made previously, and restore it
            // if so.
            mCustomPath = getExistingCustomPath();

            if (mCustomPath != null)
            {
                pathNames.add(mCustomPath.mLabel);
                mPaths.add(mCustomPath);
            }

            // Add a pseudo-entry to the list that the user can select in order
            // to open the path selector dialog and choose their own path from
            // disk.
            pathNames.add(mSelectPathText);
        }

        // Purge and populate the dropdown selector
        mPathSelectorModel.addAll(pathNames);

        // Determine which entry in the list should be initially selected, and
        // select it.
        LabelledPath defaultItem = mPaths.get(getDefaultIndex());
        mCurrentPath = defaultItem;
        mPathSelectorModel.setSelectedItem(defaultItem.mLabel);
    }

    /**
     * Determines if the user has previously made a selection for the custom
     * path (for example, such a selection may be stored in config), and
     * returns it.
     *
     * @return A <tt>LabelledPath</tt> to prepopulate the custom entry in the
     * dropdown, or else <tt>null</tt> to start with no custom entry.
     */
    protected abstract LabelledPath getExistingCustomPath();

    /**
     * Returns the currently-selected path. Note that this may not be the
     * same as the currently-selected member of the dropdown: for example if the
     * user is in the process of selecting a file from the dialog, then the
     * dropdown will show 'Choose file...'.
     *
     * @return The currently-selected path.
     */
    public String getCurrentPath()
    {
        return mCurrentPath.mPath;
    }

    /**
     * The index of the dropdown entry that should be initially selected when
     * the dropdown is first populated.
     *
     * @return The index to pre-select.
     */
    protected int getDefaultIndex()
    {
        // Default to the first entry in the list.
        return 0;
    }

    /**
     * @return The entries in the paths list
     */
    protected List<LabelledPath> getPaths()
    {
        return mPaths;
    }

    /**
     * Determines if the user is to be permitted to select their own files and
     * folders from disk, in addition to the default entries in the list.
     *
     * @return True if the user is permitted to select their own paths.
     */
    protected boolean checkCustomPathsAllowed()
    {
        // Default behaviour is to unconditionally allow users to make custom
        // selections.
        return true;
    }

    /**
     * Sets the title text used for the file/folder selector dialog.
     * @param title
     */
    protected void setPathSelectorDialogTitle(String title)
    {
        mPathSelectorDialogTitle = title;
    }

    /**
     * Open a file chooser to allow the user to select a file or folder from
     * disk.
     *
     * Executes on the EDT, regardless of the calling thread.
     */
    private void dispatchSelectPathAction()
    {
        // Ensure we are on the EDT thread; if not, call ourselves back on it.
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                   dispatchSelectPathAction();
               }
            });
            return;
        }

        mDropdownListener.ignoreItemEventsUntilNextDropdown();

        // Show the dialog, and obtain the file selected, if any.
        SipCommFileChooser fileChooser =
            GenericFileDialog.create(null,
                                     mPathSelectorDialogTitle,
                                     SipCommFileChooser.LOAD_FILE_OPERATION,
                                     System.getProperty("user.home"));
        fileChooser.setSelectionMode(mSelectionMode);

        if (mFileFilter != null)
        {
            fileChooser.useAllFileFilter(mUseAllFilesFilter);
            fileChooser.addFilter(mFileFilter);
            fileChooser.setFileFilter(mFileFilter);
        }

        File selectedFile = fileChooser.getFileFromDialog();

        if (selectedFile != null)
        {
            // Validate and load the selected file.
            new PathLoaderThread(selectedFile).start();
        }
        else
        {
            // User cancelled the dialog, so revert the dropdown selector.
            mPathSelectorModel.setSelectedItem(mCurrentPath.mLabel);
        }
    }

    /**
     * Validates the specified path; and if it passes, loads it into the
     * dropdown selector (replacing the last custom selection, if any) and makes
     * it the current selection.
     *
     * @param selection The path to validate and load.
     */
    private void processFile(File selection)
    {
        // Get either a LabelledPath containing the original selection and a
        // display name to show in the dropdown, or <tt>null</tt> if the file
        // was invalid.
        LabelledPath path = loadFile(selection);

        if (path != null)
        {
            if (mCustomPath != null)
            {
                // Remove the old custom path from the dropdown and from the
                // list of paths.
                mPathSelectorModel.removeElement(mCustomPath.mLabel);
                mPaths.remove(mCustomPath);
            }

            // Remove the 'Choose file...' entry in the dropdown before adding
            // an entry for the new custom path.
            mPathSelectorModel.removeElement(mSelectPathText);

            // Add the custom path to our list, and preselect it.
            mCustomPath = path;
            mPathSelectorModel.addElement(mCustomPath.mLabel);
            mPaths.add(mCustomPath);
            mPathSelectorModel.setSelectedItem(path.mLabel);

            // Add 'Select file...' back on the end of the dropdown list.
            mPathSelectorModel.addElement(mSelectPathText);

            if (mTooltipsEnabled)
            {
                // Set the hovertext of the dropdown selector to be the full
                // path of this selection.
                setToolTipText(mCustomPath.mPath);
            }

            // Record that the specified path is now the current selection, and
            // perform any action that the change of selection has made
            // necessary.
            LabelledPath oldValue = mCurrentPath;
            mCurrentPath = mCustomPath;
            onSelectionChanged(oldValue,
                               mCurrentPath,
                               mPathSelectorModel.getIndexOf(
                                                          mCurrentPath.mLabel));
        }
    }

    /**
     * Perform any necessary validation of the given path, and then decorate
     * it with a display name before returning it.
     * If the file was invalid, this method should call 'abort', and return
     * null. It is necessary that the method make the abort call, since it is up
     * to the caller to decide whether to call <tt>abort(file, false)</tt> or
     * <tt>abort(file, true)</tt>.
     *
     * @param path The path to validate and load.
     * @return A <tt>LabelledPath</tt> containing the specified path as well as
     * the display name to show in the dropdown, or <tt>null</tt> if the file
     * was invalid.
     */
    protected LabelledPath loadFile(File path)
    {
        // Default behaviour is to simply return the file's path and name with
        // no side-effects or validation.

        return new LabelledPath(path.getName(), path.getAbsolutePath());
    }

    /**
     * Displays a dialog to inform the user that they have selected an invalid
     * file or folder. Always runs in the EDT, regardless of the calling thread.
     */
    protected void showInvalidFileDialog()
    {
        if (!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
               public void run()
               {
                   showInvalidFileDialog();
               }
            });
            return;
        }
        new ErrorDialog(mInvalidChoiceTitle, mInvalidChoiceText).showDialog();
    }

    /**
     * To be called during 'loadFile(file)' if the file is invalid.
     * Shows the 'invalid selection' dialog, and reverts the dropdown selector,
     * either to the previous value, or to the first value in the list (which
     * may be required if the nature of the failure prohibits successful
     * rollback to the previous entry).
     *
     * @param path The invalid path selected by the user.
     * @param revertToPrevious <tt>true</tt> if the selector should be rolled
     * back to the previous value; <tt>false</tt> if the nature of the failure
     * makes this impossible and we have to fall back to the first entry in the
     * list.
     */
    protected void abort(File path,
                         final boolean revertToPrevious)
    {
        logger.debug("User selected an invalid file/folder: " + path);

        SwingUtilities.invokeLater(new Runnable()
        {
            public void run()
            {
                if (revertToPrevious)
                {
                    mPathSelectorModel.setSelectedItem(mCurrentPath.mLabel);
                }
                else
                {
                    // Revert to the default selection rather than the user's
                    // selection - it is assumed that the failure could have
                    // broken their current choice.
                    LabelledPath oldValue = mCurrentPath;
                    mCurrentPath = mPaths.get(0);

                    if (mTooltipsEnabled)
                    {
                        setToolTipText(mCurrentPath.mPath);
                    }

                    mPathSelectorModel.setSelectedItem(mCurrentPath.mLabel);
                    onSelectionChanged(oldValue, mCurrentPath, 0);

                    // Remove the previous value, since it's possible that the
                    // failure broke it.
                    mPaths.remove(oldValue);
                    mPathSelectorModel.removeElement(oldValue.mLabel);
                }
            }
        });
    }

    /**
     * Set whether to show hovertext for the selector or not. If <tt>true</tt>,
     * we show the full path of the current selection as a tooltip on mouseover.
     *
     * @param enabled <tt>true</tt> if hovertext should be shown.
     */
    public void setTooltipsEnabled(boolean enabled)
    {
        mTooltipsEnabled = enabled;

        if (enabled)
        {
            setToolTipText(mCurrentPath.mPath);
        }
        else
        {
            setToolTipText(null);
        }
    }

    /**
     * Called when the active path has been changed, either by the user, or as
     * a result of rolling back the selector to a previous value on error.
     *
     * @param oldValue The previously-active path.
     * @param newValue The newly-active path.
     * @param newIndex The index in the dropdown of the newly-active path.
     */
    protected void onSelectionChanged(LabelledPath oldValue,
                                      LabelledPath newValue,
                                      int newIndex)
    {
        // Default behaviour is to do nothing.
    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (OSUtils.IS_WINDOWS && !isPopupVisible() &&
            (e.getKeyCode() == KeyEvent.VK_UP ||
            e.getKeyCode() == KeyEvent.VK_DOWN ))
        {
            this.showPopup();
        }

        if (e.getKeyCode() == KeyEvent.VK_SPACE ||
            e.getKeyCode() == KeyEvent.VK_ENTER)
        {
            if (getSelectedItem().equals(mSelectPathText))
            {
                dispatchSelectPathAction();
            }
            else
            {
                int selected = getSelectedIndex();
                LabelledPath selectedPath = mPaths.get(selected);

                if (!mCurrentPath.equals(selectedPath))
                {
                    onSelectionChanged(mCurrentPath, mPaths.get(selected), selected);
                }
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e){}

    @Override
    public void keyTyped(KeyEvent e){}

    /**
     * This allows for us to override this method, for example to check that all
     * of the entries are still valid when it becomes visible.
     */
    protected void onPopupMenuWillBecomeVisible()
    {
        // Do nothing extra in this class.
    }

    /**
     * Listener for the dropdown menu. Detects selection / deselection of list
     * items, and the showing/hiding of the dropdown menu.
     */
    private class DropdownListener
        implements ItemListener, PopupMenuListener
    {
        /**
         * Stores whether we are currently listening for events or not.
         */
        private boolean mItemEventListenerEnabled;

        /**
         * Suppress handling of changes in the selected item, until the next
         * time the dropdown is made visible.
         */
        public void ignoreItemEventsUntilNextDropdown()
        {
            mItemEventListenerEnabled = false;
        }

        /**
         * Fired if the state of an item has changed; i.e. it has been selected
         * or deselected.
         */
        public void itemStateChanged(ItemEvent ev)
        {
            if (!mItemEventListenerEnabled)
            {
                // We're disabled, so do nothing.
                return;
            }

            if (ev.getStateChange() == ItemEvent.DESELECTED)
            {
                // We only care about new selections, so do nothing.
                return;
            }

            int index = PathSelectorComboBox.this.getSelectedIndex();

            if (getSelectedItem().equals(mSelectPathText))
            {
                dispatchSelectPathAction();
            }
            else
            {
                // The user selected a file or folder from the dropdown, so try
                // to change the current path.
                LabelledPath oldValue = mCurrentPath;
                mCurrentPath = mPaths.get(index);

                if (mTooltipsEnabled)
                    setToolTipText(mCurrentPath.mPath);

                onSelectionChanged(oldValue, mCurrentPath, index);
            }
        }

        public void popupMenuWillBecomeVisible(PopupMenuEvent arg0)
        {
            mItemEventListenerEnabled = true;
            onPopupMenuWillBecomeVisible();
        }

        public void popupMenuCanceled(PopupMenuEvent arg0)
        {
            // No action required.
        }
        public void popupMenuWillBecomeInvisible(PopupMenuEvent arg0)
        {
            // No action required.
        }
    }

    /**
     * Thread used to validate and load the user's selections made in the file
     * chooser dialog.
     */
    private class PathLoaderThread extends Thread
    {
        /**
         * The file or folder selected by the user.
         */
        private File mPath;

        /**
         * Create a <tt>PathLoaderThread</tt> in order to validate and load the
         * specified path.
         * @param path
         */
        public PathLoaderThread(File path)
        {
            mPath = path;

            setName("PathLoaderThread");
            setDaemon(true);
        }

        @Override
        public void run()
        {
            // Ensure that we only have one instance of the loader running at a
            // time.
            synchronized (sPathLoaderLock)
            {
                if (mPathLoader != null)
                {
                    return;
                }
            }

            mPathLoader = this;

            // Now actually do the work.
            try
            {
                processFile(mPath);
            }
            finally
            {
                synchronized (sPathLoaderLock)
                {
                    mPathLoader = null;
                }
            }
        }
    }

    /**
     * A wrapper class for the user's selected path that allows it to be
     * associated with a user-visible name to show in the dropdown selector.
     */
    protected static class LabelledPath
    {
        /**
         * The user-visible name of this path, to show as an entry in the
         * dropdown.
         */
        public final String mLabel;

        /**
         * The full path.
         */
        public final String mPath;

        public LabelledPath(String label, String path)
        {
            mLabel = label;
            mPath = path;
        }

        public String toString()
        {
            return mLabel + ":" + mPath;
        }
    }
}
