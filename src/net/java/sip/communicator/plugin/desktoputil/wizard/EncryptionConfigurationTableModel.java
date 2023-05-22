/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil.wizard;

import java.util.*;

import net.java.sip.communicator.plugin.desktoputil.*;

/**
 * Implements {@link javax.swing.table.TableModel} for encryption configuration (ZRTP, SDES and
 * MIKEY).
 *
 * @author Lyubomir Marinov
 * @author Vincent Lucas
 */
public class EncryptionConfigurationTableModel
    extends MoveableTableModel
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * The encryption protocol names.
     */
    private String[] encryptionProtocols;

    /**
     * The encryption protocol status (enabled / disabled).
     */
    private boolean[] encryptionProtocolStatus;

    /**
     * Creates a new table model in order to manage the encryption protocols and
     * the corresponding priority.
     *
     * @param encryptionProtocols The encryption protocol names.
     * @param encryptionProtocolStatus The encryption protocol status (enabled
     * / disabled).
     */
    public EncryptionConfigurationTableModel(
            String[] encryptionProtocols,
            boolean[] encryptionProtocolStatus)
    {
        this.init(encryptionProtocols, encryptionProtocolStatus);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex)
    {
        return
            (columnIndex == 0)
                ? Boolean.class
                : super.getColumnClass(columnIndex);
    }

    public int getColumnCount()
    {
        return 2;
    }

    /**
     * Returns the number of row in this table model.
     *
     * @return the number of row in this table model.
     */
    public int getRowCount()
    {
        return encryptionProtocols.length;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return (columnIndex == 0);
    }

    public Object getValueAt(int rowIndex, int columnIndex)
    {
        switch (columnIndex)
        {
            case 0:
                return encryptionProtocolStatus[rowIndex];
            case 1:
                return encryptionProtocols[rowIndex];
            default:
                return null;
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex)
    {
        if ((columnIndex == 0) && (value instanceof Boolean))
        {
            this.encryptionProtocolStatus[rowIndex]
                = (Boolean) value;

            // We fire the update event before setting the configuration
            // property in order to have more reactive user interface.
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    /**
     * Move the row.
     *
     * @param rowIndex index of the row
     * @param up true to move up, false to move down
     * @return the next row index
     */
    public int move(int rowIndex, boolean up)
    {
        int toRowIndex;
        if (up)
        {
            toRowIndex = rowIndex - 1;
            if (toRowIndex < 0)
                throw new IllegalArgumentException("rowIndex");
        }
        else
        {
            toRowIndex = rowIndex + 1;
            if (toRowIndex >= getRowCount())
                throw new IllegalArgumentException("rowIndex");
        }

        // Swaps the selection list.
        boolean tmpSelectionItem = this.encryptionProtocolStatus[rowIndex];
        this.encryptionProtocolStatus[rowIndex]
            = this.encryptionProtocolStatus[toRowIndex];
        this.encryptionProtocolStatus[toRowIndex] = tmpSelectionItem;

        // Swaps the label list.
        String tmpLabel = this.encryptionProtocols[rowIndex];
        this.encryptionProtocols[rowIndex]
            = this.encryptionProtocols[toRowIndex];
        this.encryptionProtocols[toRowIndex] = tmpLabel;

        fireTableRowsUpdated(rowIndex, toRowIndex);
        return toRowIndex;
    }

    /**
     * Returns the map between encryption protocol names and their priority
     * order.
     *
     * @return The map between encryption protocol names and their priority
     * order.
     */
    public Map<String, Integer> getEncryptionProtocols()
    {
        Map<String, Integer> encryptionProtocolMap
            = new HashMap<>(this.encryptionProtocols.length);
        for(int i = 0; i < this.encryptionProtocols.length; ++i)
        {
            encryptionProtocolMap.put(
                    this.encryptionProtocols[i],
                    new Integer(i));
        }
        return encryptionProtocolMap;
    }

    /**
     * Returns the map between encryption protocol names and their status.
     *
     * @return The map between encryption protocol names and their status.
     */
    public Map<String, Boolean> getEncryptionProtocolStatus()
    {
        Map<String, Boolean> encryptionProtocolStatusMap
            = new HashMap<>(
                this.encryptionProtocolStatus.length);
        for(int i = 0; i < this.encryptionProtocolStatus.length; ++i)
        {
            encryptionProtocolStatusMap.put(
                    encryptionProtocols[i],
                    new Boolean(encryptionProtocolStatus[i]));
        }
        return encryptionProtocolStatusMap;
    }

    /**
     * Returns if the label is enabled or disabled.
     *
     * @param label The label to be determined as enabled or disabled.
     *
     * @return True if the label given in parameter is enabled. False,
     * otherwise.
     */
    public boolean isEnabledLabel(String label)
    {
        for(int i = 0; i < this.encryptionProtocols.length; ++i)
        {
            if(this.encryptionProtocols[i].equals(label))
            {
                return this.encryptionProtocolStatus[i];
            }
        }
        return false;
    }

    /**
     * Initiates this table model in order to manage the encryption protocols
     * and the corresponding priority.
     *
     * @param encryptionProtocols The encryption protocol names.
     * @param encryptionProtocolStatus The encryption protocol status (enabled
     * / disabled).
     */
    public void init(
            String[] encryptionProtocols,
            boolean[] encryptionProtocolStatus)
    {
        this.encryptionProtocols = encryptionProtocols;
        this.encryptionProtocolStatus = encryptionProtocolStatus;
    }
}
