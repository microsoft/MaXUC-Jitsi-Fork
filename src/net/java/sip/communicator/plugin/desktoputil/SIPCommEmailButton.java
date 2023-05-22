// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil;

import java.awt.event.*;

import net.java.sip.communicator.util.*;

/**
 * A button that, when pressed, opens a draft email with the given subject and
 * body.  If no subject has been set, the button will be disabled.
 */
public class SIPCommEmailButton extends SIPCommButton
                               implements ActionListener
{
    private static final long serialVersionUID = 1L;

    private static final Logger sLog = Logger.getLogger(SIPCommEmailButton.class);

    /**
     * The text to be used as the email subject.
     */
    private String mEmailSubject;

    /**
     * The text to be used as the email body.
     */
    private String mEmailBody;

    /**
     * Creates a new SIPCommEmailButton with images accessed via a resource
     * with the given prefix but with no subject or body.
     *
     * @param imageResPrefix the prefix of the resource for the images for this
     * button.
     */
    public SIPCommEmailButton(String imageResPrefix)
    {
        super(imageResPrefix);

        sLog.debug("Creating new button to create an email");
        addActionListener(this);
        setEnabled(false);
    }

    /**
     * Sets the email contents and disables the button if the subject is null.
     *
     * @param subjectText the subject
     * @param bodyText the body
     */
    public void setEmailContents(String subjectText, String bodyText)
    {
        mEmailSubject = subjectText;
        mEmailBody = bodyText;
        setEnabled(mEmailSubject != null);
    }

    @Override
    public void actionPerformed(ActionEvent evt)
    {
        EmailUtils.createEmail(null, mEmailSubject, mEmailBody);
    }
}
