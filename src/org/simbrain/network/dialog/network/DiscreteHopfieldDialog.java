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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkPanel;
import org.simbrain.network.dialog.network.layout.AbstractLayoutPanel;
import org.simbrain.network.dialog.network.layout.GridLayoutPanel;
import org.simbrain.network.dialog.network.layout.LayoutPanel;
import org.simbrain.network.dialog.network.layout.LineLayoutPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.layouts.Layout;
import org.simnet.networks.DiscreteHopfield;

import com.Ostermiller.util.CSVParser;


/**
 * <b>DiscreteHopfieldDialog</b> is a dialog box for creating discrete hopfield networks.
 */
public class DiscreteHopfieldDialog extends StandardDialog implements ActionListener {

    /** File system seperator. */
    private static final String FS = System.getProperty("file.separator");

    /** Sequential network update order. */
    public static final int SEQUENTIAL = 0;

    /** Random network update order. */
    public static final int RANDOM = 1;

    /** Tabbed pane. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Logic tab panel. */
    private JPanel tabLogic = new JPanel();

    /** Layout tab panel. */
    private JPanel tabLayout = new JPanel();

    /** Logic panel. */
    private LabelledItemPanel logicPanel = new LabelledItemPanel();

    /** Layout panel. */
    private LayoutPanel layoutPanel;

    /** Number of units field. */
    private JTextField numberOfUnits = new JTextField();

    /** Network type combo box. */
    private JComboBox cbUpdateOrder = new JComboBox(new String[] {"Sequential", "Random" });

    /** Open training file button. */
    private JButton trainingFile = new JButton("Set");

    /** Array of string values. */
    private String[][] values = null;

    /** Network panel. */
    private NetworkPanel networkPanel;

    /**
     * This method is the default constructor.
     */
    public DiscreteHopfieldDialog(final NetworkPanel net) {
        networkPanel = net;
        layoutPanel = new LayoutPanel(this, new AbstractLayoutPanel[]{new GridLayoutPanel(), new LineLayoutPanel()});
        init();
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        Layout layout = layoutPanel.getNeuronLayout();
        layout.setInitialLocation(networkPanel.getLastClickedPosition());
        DiscreteHopfield hop = new DiscreteHopfield(Integer.parseInt(numberOfUnits.getText()), layout);
        networkPanel.getNetwork().addNetwork(hop);
        networkPanel.repaint();
        super.closeDialogOk();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        //Initialize Dialog
        setTitle("New Hopfield Network");
        fillFieldValues();
        this.setLocation(500, 0); //Sets location of network dialog

        fillFieldValues();
        trainingFile.addActionListener(this);

        //Set up grapics panel
        logicPanel.addItem("Update order", cbUpdateOrder);
        logicPanel.addItem("Number of Units", numberOfUnits);
        logicPanel.addItem("Set training file", trainingFile);

        //Set up tab panel
        tabLogic.add(logicPanel);
        tabLayout.add(layoutPanel);
        tabbedPane.addTab("Logic", logicPanel);
        tabbedPane.addTab("Layout", layoutPanel);
        setContentPane(tabbedPane);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        DiscreteHopfield dh = new DiscreteHopfield();
        numberOfUnits.setText(Integer.toString(dh.getNumUnits()));
    }

    /**
     * @return the update order.
     */
    public int getType() {
        if (cbUpdateOrder.getSelectedIndex() == 0) {
            return SEQUENTIAL;
        } else {
            return RANDOM;
        }
    }

    /**
     * Responds to actions performed within the dialog.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        loadFile();
    }

    /**
     * Opens the dialog for loading the hopfield training file.
     */
    private void loadFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("." + FS + "simulations" + FS + "networks"));

        int result = chooser.showDialog(this, "Open");

        if (result == JFileChooser.APPROVE_OPTION) {
            readFile(chooser.getSelectedFile());
        }
    }

    /**
     * Reads the hopfield training file.
     *
     * @param theFile The file to be read
     */
    public void readFile(final File theFile) {
        CSVParser theParser = null;

        try {
            theParser = new CSVParser(new FileInputStream(theFile), "", "", "#");
                      // # is a comment delimeter in net files
            values = theParser.getAllValues();
        } catch (java.io.FileNotFoundException e) {
            JOptionPane.showMessageDialog(null, "Could not find the file \n"
                    + theFile, "Warning", JOptionPane.ERROR_MESSAGE);

            return;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "There was a problem opening the file \n" + theFile,
                    "Warning", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return;
        }
    }
}
