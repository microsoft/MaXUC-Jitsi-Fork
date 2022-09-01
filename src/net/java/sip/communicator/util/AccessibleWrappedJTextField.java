// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.util;

import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleText;
import javax.swing.*;

// We wrap the JTextField for two reason
// - to log accessible accesses
// - to override the AccessibleRole
public class AccessibleWrappedJTextField extends JTextField
{
    private WrappedAccessibleJTextField accessibleContext;
    AccessibleRole role;

    private static final Logger sLogger =
            Logger.getLogger(AccessibleWrappedJTextField.class.getName());

    public AccessibleWrappedJTextField(AccessibleRole role)
    {
        this.role = role;
    }

    @Override
    public AccessibleContext getAccessibleContext()
    {
        if (accessibleContext == null)
        {
            accessibleContext = new WrappedAccessibleJTextField();
        }

        return accessibleContext;
    }

    // This must be an inner class, since AccessibleJTextField is protected
    class WrappedAccessibleJTextField extends AccessibleJTextField
    {
        @Override
        public AccessibleRole getAccessibleRole()
        {
            AccessibleRole role = AccessibleWrappedJTextField.this.role;
            sLogger.debug("Role is " + role);
            return role;
        }

        @Override
        public AccessibleText getAccessibleText()
        {
            AccessibleText text = super.getAccessibleText();

            sLogger.debug("Text is " + text.getAtIndex(SENTENCE, 0));
            return text;
        }

        @Override
        public String getAccessibleDescription()
        {
            String description = super.getAccessibleDescription();

            sLogger.debug("Description is " + description);
            return description;
        }

        @Override
        public String getAccessibleName()
        {
            String name = super.getAccessibleName();

            sLogger.debug("Name is " + name);
            return name;
        }
    }
}
