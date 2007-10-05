/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
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
package org.simbrain.world.odorworld;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.StandardDialog;

import com.Ostermiller.util.CSVParser;


/**
 * <b>DialogScript</b> is used to manage world scripts, which control creature behavior in a pre-programmed way.
 */
public class DialogScript extends StandardDialog implements ActionListener {

    /** The initial offset for the dialog. */
    private final int initialDialogPlacement = 500;

    /** The file separator string for the operating system. */
    private static final String FS = System.getProperty("file.separator");

    /** The world for which this dialog is called. */
    private OdorWorldPanel theWorld;

    /** The current directory for which the file chooser is called. */
    private String currentDirectory = "." + FS + "simulations" + FS + "worlds" + FS + "scripts";

    /** The content pane for this dialog. */
    private LabelledItemPanel myContentPane = new LabelledItemPanel();

    /** The button to run the script. */
    private JButton runButton = new JButton("Run");

    /** The button to stop the script. */
    private JButton stopButton = new JButton("Stop");

    /** The button to load the script. */
    private JButton loadButton = new JButton("Load");

    /** The label representing the script file loaded. */
    private JLabel fileLabel = new JLabel("   No script loaded");

    /** The values in the script. */
    private String[][] values = null;

    /** The thread to run. */
    private ScriptThread theThread = null;

    /** Iteration number. */
    private JLabel iterationNumber = new JLabel("Iteration 0");

    /**
     * Constructor built for the odorworld.
     *
     * @param wp the world calling this dialog.
     */
    public DialogScript(final OdorWorldPanel wp) {
        theWorld = wp;
        init();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        setTitle("Script");

        this.setModal(false);
        this.setLocation(initialDialogPlacement, initialDialogPlacement);

        runButton.addActionListener(this);
        loadButton.addActionListener(this);
        stopButton.addActionListener(this);

        myContentPane.addItem("", fileLabel);
        myContentPane.addItem("", runButton);
        myContentPane.addItem("", stopButton);
        myContentPane.addItem("", loadButton);
        myContentPane.addItem("", iterationNumber);

        setContentPane(myContentPane);
    }

    /**
     * Responds to actionevents.
     *
     * @param e the event triggering this method.
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == loadButton) {
            loadScript();
        } else if (o == stopButton) {
            stopScript();
        } else if (o == runButton) {
            runScript();
        }
    }

    /**
     * Load the script from the filesystem.
     */
    private void loadScript() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "csv");
        File tmpFile = chooser.showOpenDialog();

        if (tmpFile != null) {
            readScript(tmpFile);
            currentDirectory = chooser.getCurrentLocation();
        }
    }

    /**
     * Read the script from the loaded file.
     *
     * @param theFile the file to read
     */
    public void readScript(final File theFile) {
        fileLabel.setText("  " + theFile.getName());
        repaint();

        CSVParser theParser = null;

        try {
            theParser = new CSVParser(new FileInputStream(theFile), "", "", "#");
                // # is a comment delimeter in net files
            values = theParser.getAllValues();
        } catch (java.io.FileNotFoundException e) {
            JOptionPane.showMessageDialog(
                                          null, "Could not find script file \n" + theFile, "Warning",
                                          JOptionPane.ERROR_MESSAGE);

            return;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                                          null, "There was a problem opening the script file \n" + theFile, "Warning",
                                          JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();

            return;
        }
    }

    /**
     * Run the loaded script.
     */
    public void runScript() {
        if (values != null) {
            theThread = new ScriptThread(theWorld, values, this);
            theThread.setRunning(true);
            theThread.start();
        }
    }

    /**
     * Stop running the script.
     */
    private void stopScript() {
        if (theThread != null) {
            theThread.setRunning(false);
            theThread = null;
        }
    }

    /**
     * Set the label of the iteration label.
     *
     * @param num the number to put in the label.
     */
    public void setIterationNumber(final int num) {
        iterationNumber.setText("Iteration " + num);
    }
}
