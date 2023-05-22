/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.replacement;

import java.util.*;

/**
 * Interface to get the resources to insert images via a selector box
 */
public interface TabbedImagesSelectorService extends ChatPartReplacementService
{
    /**
     * The source name.
     */
    String SOURCE_NAME = "SOURCE";

    /**
     * Returns the list of tabs containing the images, to be displayed in the selector box
     * @return list of tabs of images
     */
    List<TabOfInsertableIcons> getTabs();

    /**
     * @return true if all the images have been loaded
     */
    boolean allIconsLoaded();
}
