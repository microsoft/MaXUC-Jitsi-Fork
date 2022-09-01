/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.neomedia;

import java.awt.*;

import javax.swing.*;

/**
 * A custom <tt>ListCellRenderer</tt> for the audio device configuration combo
 * boxes to handle devices that are unavailable - these are greyed out in the
 * combo box and cannot be selected.
 */
public class DeviceConfigListCellRenderer extends JLabel
    implements ListCellRenderer<Object>
{
    private static final long serialVersionUID = 1L;

    private final ListCellRenderer<? super Object> delegate;
    private final DeviceConfigurationComboBoxModel model;

    public DeviceConfigListCellRenderer(ListCellRenderer<? super Object> delegate,
        DeviceConfigurationComboBoxModel model)
    {
        this.delegate = delegate;
        this.model = model;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends Object> list,
                                                  Object value,
                                                  int index,
                                                  boolean isSelected,
                                                  boolean cellHasFocus)
    {
        // The ListCellRenderer JavaDoc says that value is 'The value returned
        // by list.getModel().getElementAt(index).' In testing this was not
        // the case, so explicitly make it so here
        value = list.getModel().getElementAt(index);
        Component component = delegate.getListCellRendererComponent(
            list, value, index, isSelected, cellHasFocus);

        // Grey out any unavailable devices so they cannot be selected.
        boolean isActive = false;
        if (value != null)
        {
            isActive = model.isDeviceAvailable(value.toString());
        }

        component.setFocusable(isActive);
        component.setEnabled(isActive);
        component.setBackground((isActive && isSelected) ?
            list.getSelectionBackground() : list.getBackground());

        return component;
    }
}