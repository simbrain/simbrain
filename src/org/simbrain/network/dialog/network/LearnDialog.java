/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.dialog.network;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.simbrain.network.NetworkPanel;

import com.Ostermiller.util.CSVParser;


/**
 * <b>LearnDialog</b>  Not currently documented.
 */
public class LearnDialog extends JDialog implements ActionListener {
    private JPanel topPanel = new JPanel();
    private JPanel bottomPanel = new JPanel();
    private JLabel fileLabel = new JLabel();
    private JComboBox cbLayer1 = null;
    private JComboBox cbLayer2 = null;
    private JButton runButton = new JButton("Connect"); // This will later be a run button
    private JButton setFileButton = new JButton("TrainingFile");
    private File theFile = null;
    private static final String FS = System.getProperty("file.separator");

    /**
     * @param owner
     * @param thePanel
     */
    public LearnDialog(Frame owner, NetworkPanel thePanel) {
        super(owner);

        //        cbLayer1 = new JComboBox(thePanel.getNetwork().getLayers());
//        cbLayer2 = new JComboBox(thePanel.getNetwork().getLayers());
//        
        runButton.addActionListener(this);
        setFileButton.addActionListener(this);

        this.getContentPane().setLayout(new GridLayout(2, 1));
        topPanel.setLayout(new GridLayout(3, 2));
        topPanel.setMinimumSize(new Dimension(300, 150));

        topPanel.add(new JLabel("   Source Layer"));
        topPanel.add(cbLayer1);
        topPanel.add(new JLabel("   Target Layer"));
        topPanel.add(cbLayer2);
        topPanel.add(new JLabel("    Weight File:"));
        topPanel.add(fileLabel);

        bottomPanel.add(runButton);
        bottomPanel.add(setFileButton);

        this.getContentPane().add(topPanel);
        this.getContentPane().add(bottomPanel);
        setVisible(true);
        pack();

        this.repaint();
    }

    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();

        if (o instanceof JButton) {
            if (o.equals(setFileButton)) {
                getFile();
                fileLabel.setText(theFile.getName());
            } else if (o.equals(runButton)) {
//                //NeuronLayer srcLayer = net_ref.getNetwork().getLayer((String)cbLayer1.getSelectedItem());
//                NeuronLayer tarLayer = net_ref.getNetwork().getLayer((String)cbLayer2.getSelectedItem());
//                srcLayer.setLayer(new String[]{"", "0", "0", "Linear", "Sigmoidal", "0", "-1", "1", "1", "0", "0", ".1", "0", "0"});
//				tarLayer.setLayer(new String[]{"", "0", "0", "Linear", "Sigmoidal", "0", "-1", "1", "1", "0", "0", ".1", "0", "0"});
//                net_ref.connectLayers(srcLayer, tarLayer, currMatrix);
//                net_ref.repaint();
            }
        }
    }

    private void getFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("." + FS + "simulations" + FS + "networks"));

        int result = chooser.showDialog(this, "Open");

        if (result == JFileChooser.APPROVE_OPTION) {
            theFile = chooser.getSelectedFile();
        }

        String[][] values = null;
        CSVParser theParser = null;

        try {
            theParser = new CSVParser(new FileInputStream(theFile), "", "", "#");
            values = theParser.getAllValues();
        } catch (Exception e) {
            System.out.println("Could not open file stream: " + e.toString());
        }

        double[][] dblValues = new double[values.length][values[0].length];

        for (int i = 0; i < values.length; i++) {
            for (int j = 0; j < values[i].length; j++) {
                dblValues[i][j] = Double.parseDouble(values[i][j]);
            }
        }
    }
}
