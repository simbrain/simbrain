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
package org.simbrain.world.odorworld;

import com.Ostermiller.util.CSVParser;

import org.simbrain.network.NetworkPanel;

import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.StandardDialog;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.File;
import java.io.FileInputStream;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;


/**
 * <b>DialogScript</b> is used to manage world scripts, which control creature behavior in a pre-programmed way
 */
public class DialogScript extends StandardDialog implements ActionListener {
    private static final String FS = System.getProperty("file.separator");
    private OdorWorld theWorld;
    private String currentDirectory = "." + FS + "simulations" + FS + "worlds";
    private LabelledItemPanel myContentPane = new LabelledItemPanel();
    private JButton runButton = new JButton("Run");
    private JButton stopButton = new JButton("Stop");
    private JButton loadButton = new JButton("Load");
    private JLabel fileLabel = new JLabel("   No script loaded");
    private String[][] values = null;
    private ScriptThread theThread = null;

    public DialogScript(OdorWorld wp) {
        theWorld = wp;
        init();
    }

    /**
     * This method initialises the components on the panel.
     */
    private void init() {
        setTitle("Script");

        this.setModal(false);
        this.setLocation(500, 500);

        runButton.addActionListener(this);
        loadButton.addActionListener(this);
        stopButton.addActionListener(this);

        myContentPane.addItem("", fileLabel);
        myContentPane.addItem("", runButton);
        myContentPane.addItem("", stopButton);
        myContentPane.addItem("", loadButton);

        setContentPane(myContentPane);
    }

    public void actionPerformed(ActionEvent e) {
        Object o = e.getSource();

        if (o == loadButton) {
            loadScript();
        } else if (o == stopButton) {
            stopScript();
        } else if (o == runButton) {
            runScript();
        }
    }

    private void loadScript() {
        SFileChooser chooser = new SFileChooser(currentDirectory, "csv");
        File tmpFile = chooser.showOpenDialog();

        if (tmpFile != null) {
            readScript(tmpFile);
            currentDirectory = chooser.getCurrentLocation();
        }
    }

    public void readScript(File theFile) {
        fileLabel.setText("  " + theFile.getName());
        repaint();

        CSVParser theParser = null;

        try {
            theParser = new CSVParser(new FileInputStream(theFile), "", "", "#"); // # is a comment delimeter in net files
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

    public void runScript() {
        if (values != null) {
            for (int i = 0; i < theWorld.getCommandTargets().size(); i++) {
                NetworkPanel np = (NetworkPanel) theWorld.getCommandTargets().get(i);
                np.clearAll();
            }

            theThread = new ScriptThread(theWorld, values);
            theThread.setRunning(true);
            theThread.start();
        }
    }

    private void stopScript() {
        if (theThread != null) {
            theThread.setRunning(false);
            theThread = null;
        }
    }
}
