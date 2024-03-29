/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
// Portions (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.plugin.desktoputil.wizard;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.wizard.*;

import org.jitsi.service.neomedia.*;

import ch.imvs.sdes4j.srtp.*;

/**
 * Contains the security settings for SIP media encryption.
 *
 * @author Ingo Bauersachs
 * @author Vincent Lucas
 */
public class SecurityPanel
    extends TransparentPanel
    implements ActionListener,
        TableModelListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    private final SecurityAccountRegistration regform;

    private JPanel pnlAdvancedSettings;
    private JCheckBox enableDefaultEncryption;
    private JCheckBox enableSipZrtpAttribute;
    private JComboBox<SavpOption> cboSavpOption;
    private JTable tabCiphers;
    private CipherTableModel cipherModel;
    private JLabel cmdExpandAdvancedSettings;

    /**
     * The TableModel used to configure the encryption protocols preferences.
     */
    private EncryptionConfigurationTableModel encryptionConfigurationTableModel;

    /**
     * JTable with 2 buttons (up and down) which able to enable encryption
     * protocols and to choose their priority order.
     */
    private PriorityTable encryptionProtocolPreferences;

    /**
     * The encryption protocols managed by this SecurityPanel.
     */
    private static final String[] ENCRYPTION_PROTOCOLS = {"ZRTP", "SDES"};

    /**
     * Boolean used to display or not the SAVP options (only useful for SIP, not
     * for XMPP).
     */
    private boolean displaySavpOtions;

    private static class SavpOption
    {
        int option;

        SavpOption(int option)
        {
            this.option = option;
        }

        @Override
        public String toString()
        {
            return UtilActivator.getResources().getI18NString(
                "plugin.sipaccregwizz.SAVP_OPTION_" + option);
        }
    }

    private static class Entry
    {
        String cipher;
        Boolean enabled;

        public Entry(String cipher, boolean enabled)
        {
            this.cipher = cipher;
            this.enabled = enabled;
        }
    }

    private static class CipherTableModel extends AbstractTableModel
    {
        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 0L;

        private List<Entry> data = new ArrayList<>();
        private final String defaultCiphers = UtilActivator.getResources()
            .getSettingsString(SDesControl.SDES_CIPHER_SUITES);

        CipherTableModel(String ciphers)
        {
            loadData(ciphers);
        }

        public void loadData(String ciphers)
        {
            data.clear();

            if(defaultCiphers == null)
                return;

            if(ciphers == null)
                ciphers = defaultCiphers;
            //TODO the available ciphers should come from SDesControlImpl
            data.add(new Entry(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80, ciphers
                .contains(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_80)));
            data.add(new Entry(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32, ciphers
                .contains(SrtpCryptoSuite.AES_CM_128_HMAC_SHA1_32)));
            data.add(new Entry(SrtpCryptoSuite.F8_128_HMAC_SHA1_80, ciphers
                .contains(SrtpCryptoSuite.F8_128_HMAC_SHA1_80)));
            fireTableDataChanged();
        }

        @Override
        public Class<?> getColumnClass(int columnIndex)
        {
            switch(columnIndex)
            {
                case 0:
                    return Boolean.class;
                case 1:
                    return String.class;
            }
            return null;
        }

        public int getRowCount()
        {
            return data.size();
        }

        public int getColumnCount()
        {
            return 2;
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex)
        {
            return (columnIndex == 0);
        }

        public Object getValueAt(int rowIndex, int columnIndex)
        {
            Entry e = data.get(rowIndex);
            switch(columnIndex)
            {
                case 0:
                    return e.enabled;
                case 1:
                    return e.cipher;
            }
            return null;
        }

        @Override
        public void setValueAt(Object value, int rowIndex, int columnIndex)
        {
            if ((columnIndex == 0) && (value instanceof Boolean))
            {
                Entry e = data.get(rowIndex);
                e.enabled = (Boolean)value;
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }

        String getEnabledCiphers()
        {
            StringBuilder sb = new StringBuilder();
            for (Entry e : data)
            {
                if(e.enabled)
                {
                    sb.append(e.cipher);
                    sb.append(',');
                }
            }
            if(sb.length() == 0)
            {
                return sb.toString();
            }
            else
            {
                return sb.substring(0, sb.length()-1);
            }
        }
    }

    /**
     * Initiates a panel to configure the security (ZRTP and SDES) for SIP or
     * XMPP protocols.
     *
     * @param regform The registration form of the account to configure.
     * @param displaySavpOptions Boolean used to display or not the SAVP options
     * (only useful for SIP, not for XMPP).
     */
    public SecurityPanel(
            SecurityAccountRegistration regform,
            boolean displaySavpOptions)
    {
        super(new BorderLayout(10, 10));

        this.regform = regform;
        this.displaySavpOtions = displaySavpOptions;
        initComponents();
    }

    private void initComponents()
    {
        setLayout(new BorderLayout());
        final JPanel mainPanel = new TransparentPanel();
        add(mainPanel, BorderLayout.NORTH);

        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;

        //general encryption option
        enableDefaultEncryption = new SIPCommCheckBox(UtilActivator
            .getResources()
              .getI18NString("plugin.sipaccregwizz.ENABLE_DEFAULT_ENCRYPTION"),
            regform.isDefaultEncryption());
        enableDefaultEncryption.addActionListener(this);
        mainPanel.add(enableDefaultEncryption, c);

        //warning message and button to show advanced options
        JLabel lblWarning = new JLabel();
        lblWarning.setBorder(new EmptyBorder(10, 5, 10, 0));
        lblWarning.setText(UtilActivator.getResources().getI18NString(
            "plugin.sipaccregwizz.SECURITY_WARNING",
            new String[]{
                UtilActivator.getResources().getSettingsString(
                    "service.gui.APPLICATION_NAME")
            }
        ));
        c.gridy++;
        mainPanel.add(lblWarning, c);

        cmdExpandAdvancedSettings = new JLabel();
        cmdExpandAdvancedSettings.setBorder(new EmptyBorder(0, 5, 0, 0));
        UtilActivator.getResources()
        .getImage("service.gui.icons.RIGHT_ARROW_ICON")
        .addToLabel(cmdExpandAdvancedSettings);
        cmdExpandAdvancedSettings.setText(UtilActivator.getResources()
            .getI18NString("plugin.sipaccregwizz.SHOW_ADVANCED"));
        cmdExpandAdvancedSettings.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                UtilActivator.getResources()
                .getImage(pnlAdvancedSettings.isVisible()
                            ? "service.gui.icons.RIGHT_ARROW_ICON"
                            : "service.gui.icons.DOWN_ARROW_ICON")
                .addToLabel(cmdExpandAdvancedSettings);
                pnlAdvancedSettings.setVisible(
                    !pnlAdvancedSettings.isVisible());

                pnlAdvancedSettings.revalidate();
            }
        });
        c.gridy++;
        mainPanel.add(cmdExpandAdvancedSettings, c);

        pnlAdvancedSettings = new TransparentPanel();
        pnlAdvancedSettings.setLayout(new GridBagLayout());
        pnlAdvancedSettings.setVisible(false);
        c.gridy++;
        mainPanel.add(pnlAdvancedSettings, c);

        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        pnlAdvancedSettings.add(new JSeparator(), c);

        // Encryption protcol preferences.
        JLabel lblEncryptionProtocolPreferences = new JLabel();
        lblEncryptionProtocolPreferences.setText(UtilActivator.getResources()
            .getI18NString(
                "plugin.sipaccregwizz.ENCRYPTION_PROTOCOL_PREFERENCES"));
        c.gridy++;
        pnlAdvancedSettings.add(lblEncryptionProtocolPreferences, c);

        int nbEncryptionProtocols = ENCRYPTION_PROTOCOLS.length;
        String[] encryptions = new String[nbEncryptionProtocols];
        boolean[] selectedEncryptions = new boolean[nbEncryptionProtocols];

        this.encryptionConfigurationTableModel
            = new EncryptionConfigurationTableModel(
                    encryptions,
                    selectedEncryptions);
        loadEncryptionProtocols(new HashMap<>(),
                                new HashMap<>());
        this.encryptionProtocolPreferences = new PriorityTable(
                    this.encryptionConfigurationTableModel,
                    60);
        this.encryptionConfigurationTableModel.addTableModelListener(this);
        c.gridy++;
        pnlAdvancedSettings.add(this.encryptionProtocolPreferences, c);

        //ZRTP
        JLabel lblZrtpOption = new JLabel();
        lblZrtpOption.setBorder(new EmptyBorder(5, 5, 5, 0));
        lblZrtpOption.setText(UtilActivator.getResources()
            .getI18NString("plugin.sipaccregwizz.ZRTP_OPTION"));
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        pnlAdvancedSettings.add(lblZrtpOption, c);
        c.gridx = 1;
        pnlAdvancedSettings.add(new JSeparator(), c);

        enableSipZrtpAttribute = new SIPCommCheckBox(
            UtilActivator.getResources()
            .getI18NString("plugin.sipaccregwizz.ENABLE_SIPZRTP_ATTRIBUTE"),
            regform.isSipZrtpAttribute());
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        pnlAdvancedSettings.add(enableSipZrtpAttribute, c);

        //SDES
        JLabel lblSDesOption = new JLabel();
        lblSDesOption.setBorder(new EmptyBorder(5, 5, 5, 0));
        lblSDesOption.setText( UtilActivator.getResources().getI18NString(
            "plugin.sipaccregwizz.SDES_OPTION"));
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 1;
        pnlAdvancedSettings.add(lblSDesOption, c);
        c.gridx = 1;
        pnlAdvancedSettings.add(new JSeparator(), c);

        JLabel lblCipherInfo = new JLabel();
        lblCipherInfo.setText(UtilActivator.getResources()
            .getI18NString("plugin.sipaccregwizz.CIPHER_SUITES"));
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        pnlAdvancedSettings.add(lblCipherInfo, c);

        cipherModel = new CipherTableModel(regform.getSDesCipherSuites());
        tabCiphers = new JTable(cipherModel);
        tabCiphers.setShowGrid(false);
        tabCiphers.setTableHeader(null);
        TableColumnModel tableColumnModel = tabCiphers.getColumnModel();
        TableColumn tableColumn = tableColumnModel.getColumn(0);
        tableColumn.setMaxWidth(tableColumn.getMinWidth());
        JScrollPane scrollPane = new JScrollPane(tabCiphers);
        scrollPane.setPreferredSize(new Dimension(tabCiphers.getWidth(), 100));
        c.gridy++;
        pnlAdvancedSettings.add(scrollPane, c);

        //SAVP selection
        c.gridx = 0;
        c.gridwidth = 1;
        JLabel lblSavpOption = new JLabel();
        lblSavpOption.setBorder(new EmptyBorder(5, 5, 5, 0));
        lblSavpOption.setText( UtilActivator.getResources().getI18NString(
            "plugin.sipaccregwizz.SAVP_OPTION"));
        if(this.displaySavpOtions)
        {
            c.gridy++;
            pnlAdvancedSettings.add(lblSavpOption, c);
        }
        c.gridx = 1;
        if(this.displaySavpOtions)
        {
            pnlAdvancedSettings.add(new JSeparator(), c);
        }

        cboSavpOption = new JComboBox<>(new SavpOption[]{
                new SavpOption(0),
                new SavpOption(1),
                new SavpOption(2)
        });
        c.gridx = 0;
        c.gridwidth = 2;
        c.insets = new Insets(0, 20, 0, 0);
        if(this.displaySavpOtions)
        {
            c.gridy++;
            pnlAdvancedSettings.add(cboSavpOption, c);
        }
    }

    /**
     * Saves the user input when the "Next" wizard buttons is clicked.
     *
     * @param registration the SIPAccountRegistration
     * @return
     */
    public boolean commitPanel(SecurityAccountRegistration registration)
    {
        registration.setDefaultEncryption(enableDefaultEncryption.isSelected());
        registration.setEncryptionProtocols(
                encryptionConfigurationTableModel.getEncryptionProtocols());
        registration.setEncryptionProtocolStatus(
                encryptionConfigurationTableModel
                    .getEncryptionProtocolStatus());
        registration.setSipZrtpAttribute(enableSipZrtpAttribute.isSelected());
        registration.setSavpOption(((SavpOption) cboSavpOption
            .getSelectedItem()).option);
        registration.setSDesCipherSuites(cipherModel.getEnabledCiphers());

        return true;
    }

    /**
     * Loads the account with the given identifier.
     * @param accountID the account identifier
     */
    public void loadAccount(AccountID accountID)
    {
        enableDefaultEncryption.setSelected(
                accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                        true));

        Map<String, Integer> encryptionProtocols
            = accountID.getIntegerPropertiesByPrefix(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL,
                    true);
        Map<String, Boolean> encryptionProtocolStatus
            = accountID.getBooleanPropertiesByPrefix(
                    ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS,
                    true,
                    false);
        this.loadEncryptionProtocols(
                encryptionProtocols,
                encryptionProtocolStatus);

        enableSipZrtpAttribute.setSelected(
                accountID.getAccountPropertyBoolean(
                        ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
                        true));
        cboSavpOption.setSelectedIndex(
                accountID.getAccountPropertyInt(
                        ProtocolProviderFactory.SAVP_OPTION,
                        ProtocolProviderFactory.SAVP_OFF));
        cipherModel.loadData(
                accountID.getAccountPropertyString(
                        ProtocolProviderFactory.SDES_CIPHER_SUITES));
        loadStates();
    }

    public void actionPerformed(ActionEvent e)
    {
        if(e.getSource() == enableDefaultEncryption)
        {
            loadStates();
        }
        else if(e.getSource() == cmdExpandAdvancedSettings)
        {
            pnlAdvancedSettings.setVisible(!pnlAdvancedSettings.isVisible());
        }
    }

    public void tableChanged(TableModelEvent e)
    {
        if(e.getSource() == this.encryptionConfigurationTableModel)
        {
            loadStates();
        }
    }

    private void loadStates()
    {
        boolean b = enableDefaultEncryption.isSelected();
        cboSavpOption.setEnabled(b);
        this.encryptionProtocolPreferences.setEnabled(b);
        enableSipZrtpAttribute.setEnabled(
                b
                && this.encryptionConfigurationTableModel
                    .isEnabledLabel("ZRTP"));
        tabCiphers.setEnabled(
                b
                && this.encryptionConfigurationTableModel
                    .isEnabledLabel("SDES"));
    }

    /**
     * Loads the list of enabled and disabled encryption protocols with their
     * priority.
     *
     */
    private void loadEncryptionProtocols(
            Map<String, Integer> encryptionProtocols,
            Map<String, Boolean> encryptionProtocolStatus)
    {
        int nbEncryptionProtocols = ENCRYPTION_PROTOCOLS.length;
        String[] encryptions = new String[nbEncryptionProtocols];
        boolean[] selectedEncryptions = new boolean[nbEncryptionProtocols];

        // Load stored values.
        int prefixeLength
            = ProtocolProviderFactory.ENCRYPTION_PROTOCOL.length() + 1;
        String encryptionProtocolPropertyName;
        String name;
        int index;
        boolean enabled;
        Iterator<String> encryptionProtocolNames
            = encryptionProtocols.keySet().iterator();
        while(encryptionProtocolNames.hasNext())
        {
            encryptionProtocolPropertyName = encryptionProtocolNames.next();
            index = encryptionProtocols.get(encryptionProtocolPropertyName);
            // If the property is set.
            if(index != -1)
            {
                name = encryptionProtocolPropertyName.substring(prefixeLength);
                if (isExistingEncryptionProtocol(name))
                {
                    enabled = encryptionProtocolStatus.get(
                            ProtocolProviderFactory.ENCRYPTION_PROTOCOL_STATUS
                            + "."
                            + name);
                    encryptions[index] = name;
                    selectedEncryptions[index] = enabled;
                }
            }
        }

        // Load default values.
        String encryptionProtocol;
        boolean set;
        int j = 0;
        for(int i = 0; i < ENCRYPTION_PROTOCOLS.length; ++i)
        {
            encryptionProtocol = ENCRYPTION_PROTOCOLS[i];
            // Specify a default value only if there is no specific value set.
            if(!encryptionProtocols.containsKey(
                        ProtocolProviderFactory.ENCRYPTION_PROTOCOL
                            + "."
                            + encryptionProtocol))
            {
                set = false;
                // Search for the first empty element.
                while(j < encryptions.length && !set)
                {
                    if(encryptions[j] == null)
                    {
                        encryptions[j] = encryptionProtocol;
                        // By default only ZRTP is set to true.
                        selectedEncryptions[j]
                            = encryptionProtocol.equals("ZRTP");
                        set = true;
                    }
                    ++j;
                }
            }
        }

        this.encryptionConfigurationTableModel.init(
                encryptions,
                selectedEncryptions);
    }

    private boolean isExistingEncryptionProtocol(String protocol)
    {
        for (String key : ENCRYPTION_PROTOCOLS)
        {
            if (key.equals(protocol))
            {
                return true;
            }
        }

        return false;
    }
}
