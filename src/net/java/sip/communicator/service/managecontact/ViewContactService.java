// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.service.managecontact;

import java.awt.*;

import net.java.sip.communicator.service.contactlist.*;

/**
 * Service for creating a "view contact" window
 */
public interface ViewContactService
{
    /**
     * Create a view contact window.
     *
     * @param parentWindow    The parent of this window.
     * @param metaContact    The metacontact to display
     * @return the newly created view contact window
     */
    Window createViewContactWindow(Frame parentWindow, MetaContact metaContact);
}
