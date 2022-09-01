// Copyright (c) Microsoft Corporation. All rights reserved.
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import net.sf.fmj.media.rtp.*;

public class JitterBufferDiags
    extends JFrame
{
    private static final long serialVersionUID = 0L;

    private JPanel mcontentPane;

    private JTextField txtVideoPacketsToDrop;

    private JTextField txtSilencePacketsToInsert;

    private JTextField txtAudioPacketsToDiscard;

    private JCheckBox chckbxReplaceExistingPackets;

    private JCheckBox chckbxAllowFec;

    private JCheckBox chkEnableJitterBuffer;

    /**
     * Launch the application.
     */
    public static void main(String[] args)
    {
        EventQueue.invokeLater(new Runnable()
        {
            public void run()
            {
                try
                {
                    JitterBufferDiags frame = new JitterBufferDiags();
                    frame.setVisible(true);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the frame.
     */
    public JitterBufferDiags()
    {
        setBounds(100, 100, 553, 325);
        mcontentPane = new JPanel();
        mcontentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(mcontentPane);
        mcontentPane.setLayout(null);

        chkEnableJitterBuffer = new JCheckBox("Enable Jitter Buffer");
        chkEnableJitterBuffer.setEnabled(false);
        chkEnableJitterBuffer.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
//                JitterBufferTester.useSimpleJitterBuffer
//                    .set(chkEnableJitterBuffer.isSelected());
            }
        });
        chkEnableJitterBuffer.setBounds(10, 7, 175, 23);
        mcontentPane.add(chkEnableJitterBuffer);

        JButton btnResetJitterBuffer =
            new JButton("RESET Jitter Buffer (Drop all packets in the queue)");
        btnResetJitterBuffer.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JitterBufferTester.pleaseReset.set(true);
            }
        });
        btnResetJitterBuffer.setBounds(194, 11, 333, 42);
        mcontentPane.add(btnResetJitterBuffer);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setBounds(10, 82, 527, 206);
        mcontentPane.add(tabbedPane);

        JPanel pnlVideo = new JPanel();
        FlowLayout flowLayout = (FlowLayout) pnlVideo.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        tabbedPane.addTab("Video", null, pnlVideo, null);

        JLabel lblDrop = new JLabel("Drop");
        pnlVideo.add(lblDrop);

        txtVideoPacketsToDrop = new JTextField();
        txtVideoPacketsToDrop.setEnabled(false);
        pnlVideo.add(txtVideoPacketsToDrop);
        txtVideoPacketsToDrop.setColumns(10);

        JLabel lblPackets = new JLabel("packet(s)");
        pnlVideo.add(lblPackets);

        JButton btnDoIt = new JButton("Do It");
        btnDoIt.setEnabled(false);
        btnDoIt.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
//                JitterBufferTester.videoToDrop.set(Integer
//                    .parseInt(txtVideoPacketsToDrop.getText()));
            }
        });
        pnlVideo.add(btnDoIt);

        JPanel pnlAudio = new JPanel();
        tabbedPane.addTab("Audio", null, pnlAudio, null);
        pnlAudio.setLayout(null);

        JLabel lblNewLabel = new JLabel("Silence");
        lblNewLabel.setBounds(10, 11, 46, 14);
        pnlAudio.add(lblNewLabel);

        JSeparator separator = new JSeparator();
        separator.setBounds(0, 86, 577, 14);
        pnlAudio.add(separator);

        JLabel lblDiscards = new JLabel("Discards");
        lblDiscards.setBounds(10, 99, 46, 14);
        pnlAudio.add(lblDiscards);

        JLabel lblInsert = new JLabel("Insert");
        lblInsert.setBounds(15, 36, 76, 14);
        pnlAudio.add(lblInsert);

        txtSilencePacketsToInsert = new JTextField();
        txtSilencePacketsToInsert.setEnabled(false);
        txtSilencePacketsToInsert.setColumns(10);
        txtSilencePacketsToInsert.setBounds(106, 33, 23, 20);
        pnlAudio.add(txtSilencePacketsToInsert);

        JLabel lblSilencePackets = new JLabel("silence packet(s)");
        lblSilencePackets.setBounds(144, 36, 110, 14);
        pnlAudio.add(lblSilencePackets);

        JButton button = new JButton("Do It");
        button.setEnabled(false);
        button.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
//                JitterBufferTester.audioSilenceToInsert.set(Integer
//                    .parseInt(txtSilencePacketsToInsert.getText()));
            }
        });
        button.setBounds(419, 33, 93, 23);
        pnlAudio.add(button);

        JLabel lblDiscard = new JLabel("Discard");
        lblDiscard.setBounds(10, 122, 76, 14);
        pnlAudio.add(lblDiscard);

        txtAudioPacketsToDiscard = new JTextField();
        txtAudioPacketsToDiscard.setColumns(10);
        txtAudioPacketsToDiscard.setBounds(101, 119, 23, 20);
        pnlAudio.add(txtAudioPacketsToDiscard);

        JLabel lblPackets_1 = new JLabel("packet(s)");
        lblPackets_1.setBounds(139, 122, 110, 14);
        pnlAudio.add(lblPackets_1);

        JButton button_3 = new JButton("Do It");
        button_3.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                JitterBufferTester.audioPacketsToDiscard.set(Integer
                    .parseInt(txtAudioPacketsToDiscard.getText()));
            }
        });
        button_3.setBounds(414, 119, 98, 23);
        pnlAudio.add(button_3);

        chckbxReplaceExistingPackets =
            new JCheckBox("Replace existing packets");
        chckbxReplaceExistingPackets.setEnabled(false);
        chckbxReplaceExistingPackets.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
//                JitterBufferTester.audioSilenceReplaceExisting
//                    .set(chckbxReplaceExistingPackets.isSelected());
            }
        });
        chckbxReplaceExistingPackets.setBounds(264, 7, 212, 23);
        pnlAudio.add(chckbxReplaceExistingPackets);

        chckbxAllowFec = new JCheckBox("Allow FEC");
        chckbxAllowFec.addChangeListener(new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                JitterBufferTester.audioDiscardAllowFec.set(chckbxAllowFec
                    .isSelected());
            }
        });

        chckbxAllowFec.setBounds(264, 95, 212, 23);
        pnlAudio.add(chckbxAllowFec);

        JButton btnResetNumbersBelow = new JButton("Reset");
        btnResetNumbersBelow.addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                // Reset the text fields
                init();

                // Reset the underlying control values - these are different to
                // the values in init.
                JitterBufferTester.audioDiscardAllowFec.set(true);
                JitterBufferTester.audioPacketsToDiscard.set(0);
//                JitterBufferTester.audioSilenceReplaceExisting.set(true);
//                JitterBufferTester.audioSilenceToInsert.set(0);
                JitterBufferTester.pleaseReset.set(false);
//                JitterBufferTester.useSimpleJitterBuffer.set(false);
//                JitterBufferTester.videoToDrop.set(0);
            }
        });
        btnResetNumbersBelow.setBounds(10, 30, 175, 23);
        mcontentPane.add(btnResetNumbersBelow);

        JLabel lblOnlyDoOne =
            new JLabel("Only do one thing at a time and reset in between");
        lblOnlyDoOne.setBounds(10, 64, 517, 14);
        mcontentPane.add(lblOnlyDoOne);

        init();
    }

    void init()
    {
        txtSilencePacketsToInsert.setText("1");
        txtAudioPacketsToDiscard.setText("1");
        txtVideoPacketsToDrop.setText("1");
        chkEnableJitterBuffer.setSelected(true);
        chckbxReplaceExistingPackets.setSelected(true);
        chckbxAllowFec.setSelected(true);
    }
}
