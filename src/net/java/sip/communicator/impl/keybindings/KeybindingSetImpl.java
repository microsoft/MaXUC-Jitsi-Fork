/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.keybindings;

import java.io.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.service.keybindings.*;

/**
 * Default implementation for the wrapper of keybinding sets.
 *
 * @author Damian Johnson
 */
class KeybindingSetImpl
    extends KeybindingSet
{
    private LinkedHashMap<KeyStroke, String> bindings;

    private Category category;

    /**
     * Destination where custom bindings are saved, null if it couldn't be
     * secured.
     */
    private File customFile;

    /**
     * Flag indicating that the associated service has been stopped.
     */
    private boolean isInvalidated = false;

    KeybindingSetImpl(Map<KeyStroke, String> initial, Category category,
        File saveDst)
    {
        this.bindings = new LinkedHashMap<>(initial);
        this.category = category;
        this.customFile = saveDst;
    }

    /**
     * Provides current keybinding mappings.
     *
     * @return mapping of keystrokes to the string representation of the actions
     *         they perform
     */
    public LinkedHashMap<KeyStroke, String> getBindings()
    {
        return new LinkedHashMap<>(this.bindings);
    }

    /**
     * Resets the bindings and notifies the observer's listeners if they've
     * changed. If the bindings can be written then they will be.
     *
     * @param newBindings new keybindings to be held
     */
    public void setBindings(Map<KeyStroke, String> newBindings)
    {
        if (!this.bindings.equals(newBindings))
        {
            this.bindings = new LinkedHashMap<>(newBindings);
            setChanged();
            notifyObservers(this);
        }
    }

    /**
     * Provides the portion of the UI to which the bindings belong.
     *
     * @return binding category
     */
    public Category getCategory()
    {
        return this.category;
    }

    /**
     * Provides if the keybindings can be written when changed or not.
     *
     * @return true if bindings can be written when changed, false otherwise
     */
    boolean isWritable()
    {
        return !this.isInvalidated && this.customFile != null;
    }

    /**
     * Provides the file where custom bindings are to be saved.
     *
     * @return custom bindings save destination
     */
    File getCustomFile()
    {
        return this.customFile;
    }

    /**
     * Invalidates reference to custom output, preventing further writes.
     */
    void invalidate()
    {
        this.isInvalidated = true;
    }
}
