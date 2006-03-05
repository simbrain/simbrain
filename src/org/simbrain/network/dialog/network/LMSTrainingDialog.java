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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.simbrain.network.NetworkPreferences;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.Utils;
import org.simnet.networks.LMSNetwork;


/**
 * <b>LMSTraningDialog</b> is a dialog box for training LMSNetwork networks.
 */
public class LMSTrainingDialog extends StandardDialog implements
        ActionListener {

    /** The visual container for the sub panels. */
    private Box mainPanel = Box.createVerticalBox();

    /** Top panel. */
    private LabelledItemPanel topPanel = new LabelledItemPanel();

    /** Bottom panel. */
    private LabelledItemPanel bottomPanel = new LabelledItemPanel();

    /** User panel. */
    private LabelledItemPanel userPanel = new LabelledItemPanel();

    /** Batch panel. */
    private LabelledItemPanel batchPanel = new LabelledItemPanel();

    /** Properties panel. */
    private LabelledItemPanel propsPanel = new LabelledItemPanel();

    /** Tabbed Panel. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Input file button. */
    private JButton jbInputsFile = new JButton("None selected");

    /** Output file button. */
    private JButton jbOutputsFile = new JButton("None selected");

    /** Ephchs field. */
    private JTextField tfEpochs = new JTextField();

    /** Eta field. */
    private JTextField tfEta = new JTextField();

    /** Randomize button. */
    private JButton jbRandomize = new JButton("Randomize");

    /** Train button. */
    private JButton jbTrain = new JButton("Train");

    /** Play button. */
    private JButton jbPlay = new JButton(ResourceManager.getImageIcon("Play.gif"));

    /** Step button. */
    private JButton jbStep = new JButton(ResourceManager.getImageIcon("Step.gif"));

    /** Error label. */
    private JLabel rmsError = new JLabel();

    /** Inputs training value. */
    private double[][] inputsTrain;

    /** Outputs training value. */
    private double[][] outputsTrain;

    /** Update completed boolean value. */
    private boolean updateCompleted = false;

    /** LMSNetwork network. */
    private LMSNetwork lms;

    /** Location of backprop directory. */
    private static String backpropDirectory =  NetworkPreferences.getCurrentBackpropDirectory();

    /** Backprop training dialog thread. */
    private BPTDialogThread theThread = null;

    /**
     * This method is the default constructor.
     *
     * @param lms Backprop network
     */
    public LMSTrainingDialog(final LMSNetwork lms) {

        this.lms = lms;
        //Initialize Dialog
        setTitle("Train LMS Network");
        fillFieldValues();

        //Set up top panel
        topPanel.addItem("Input file", jbInputsFile);
        topPanel.addItem("Output file", jbOutputsFile);
        topPanel.addItem("Randomize network", jbRandomize);

        //Set up bottom panel
        bottomPanel.addItem("RMSError", rmsError);

        //Setup panels for tabs
        createUserPanel();
        createBatchPanel();
        createPropsPanel();

        //Create tabs
        tabbedPane.addTab("User", userPanel);
        tabbedPane.addTab("Batch", batchPanel);
        tabbedPane.addTab("Props", propsPanel);

        jbInputsFile.addActionListener(this);
        jbOutputsFile.addActionListener(this);
        jbRandomize.addActionListener(this);
        jbTrain.addActionListener(this);
        jbPlay.addActionListener(this);
        jbStep.addActionListener(this);

        mainPanel.add(topPanel);
        mainPanel.add(tabbedPane);
        mainPanel.add(bottomPanel);
        setContentPane(mainPanel);
    }

    /**
     * Creates user panel.
     */
    private void createUserPanel() {
        userPanel.addItem("Play/Stop", jbPlay);
        userPanel.addItem("Step", jbStep);
    }

    /**
     * Creates batch panel.
     */
    private void createBatchPanel() {
        batchPanel.addItem("Epochs", tfEpochs);
        batchPanel.addItem("Train network", jbTrain);
    }

    /**
     * Creates properties panel.
     */
    private void createPropsPanel() {
        propsPanel.addItem("Learning rate", tfEta);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        lms.setEta(Double.parseDouble(tfEta.getText()));
        NetworkPreferences.setCurrentBackpropDirectory(getBackropDirectory());
        stopThread();
        super.closeDialogOk();
    }

    /**
     * @see StandardDialog.
     */
    protected void closeDialogCancel() {
        stopThread();
        super.closeDialogCancel();
    }

    /**
     * Stops the training when dialog is closed.
     */
    private void stopThread() {
        if (theThread != null) {
            theThread.setRunning(false);
            theThread = null;
        }
    }

    /**
     * Responds to action within the dialog.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == jbInputsFile) {
            SFileChooser chooser = new SFileChooser(getBackropDirectory(), "csv");
            File theFile = chooser.showOpenDialog();

            if (theFile == null) {
                return;
            }

            setBackropDirectory(chooser.getCurrentLocation());
            inputsTrain = Utils.getDoubleMatrix(theFile);
            jbInputsFile.setText(theFile.getName());
            lms.setTrainingInputs(inputsTrain);
        } else if (o == jbOutputsFile) {
            SFileChooser chooser = new SFileChooser(getBackropDirectory(), "csv");
            File theFile = chooser.showOpenDialog();

            if (theFile == null) {
                return;
            }

            setBackropDirectory(chooser.getCurrentLocation());
            outputsTrain = Utils.getDoubleMatrix(theFile);
            jbOutputsFile.setText(theFile.getName());
            lms.setTrainingOutputs(outputsTrain);
        } else if (o == jbRandomize) {
            lms.randomizeWeights();
            lms.fireNetworkChanged();
        } else if (o == jbTrain) {
            setValues();
            lms.train();
            lms.fireNetworkChanged();
            rmsError.setText(Double.toString(lms.getRMSError()));
            bottomPanel.repaint();
        } else if (o == jbPlay) {
            setValues();

            if (theThread == null) {
                theThread = new BPTDialogThread(this);
            }

            if (!theThread.isRunning()) {
                jbPlay.setIcon(ResourceManager.getImageIcon("Stop.gif"));
                theThread.setRunning(true);
                theThread.start();
            } else {
                jbPlay.setIcon(ResourceManager.getImageIcon("Play.gif"));

                if (theThread == null) {
                    return;
                }

                theThread.setRunning(false);
                theThread = null;
            }
        } else if (o == jbStep) {
            setValues();
            iterate();
        }
    }

    /**
     * Iterate network training.
     */
    public void iterate() {
        if (outputsTrain != null || inputsTrain != null) {
            lms.iterate();
            rmsError.setText(Double.toString(lms.getRMSError()));
            updateCompleted = true;
            bottomPanel.repaint();
            lms.fireNetworkChanged();
        }
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfEpochs.setText("" + lms.getEpochs());
        tfEta.setText("" + lms.getEta());
    }

    /**
     * Set projector values based on fields.
     */
    public void setValues() {
        lms.setEpochs(Integer.parseInt(tfEpochs.getText()));
        lms.setEta(Double.parseDouble(tfEta.getText()));
    }

    /**
     * @return boolean updated completed.
     */
    public boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Sets updated completed value.
     *
     * @param updateCompleted Updated completed value to be set
     */
    public void setUpdateCompleted(final boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
    }

    /**
     * BPTDialogThreadcreates a system thread to be run when training backprop networks.
     *
     */
    public class BPTDialogThread extends Thread {

        /** Backprop training dialog. */
        private LMSTrainingDialog dialog = null;

        /** Is thread running boolean value. */
        private volatile boolean isRunning = false;

        /**
         * Runs the thread.
         */
        Runnable iterate = new Runnable() {
            public void run() {
                dialog.iterate();
            }
        };

        /**
         * Backprop training dialog thread constructor.
         *
         * @param dialog Dialog to run backprop
         */
        public BPTDialogThread(final LMSTrainingDialog dialog) {
            this.dialog = dialog;
        }

        /**
         * Run the thread.
         */
        public void run() {
            try {
                while (isRunning) {
                    dialog.setUpdateCompleted(false);
                    SwingUtilities.invokeLater(iterate);

                    while (!dialog.isUpdateCompleted()) {
                        sleep(10);
                    }
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        /**
         * @return boolean running value.
         */
        public boolean isRunning() {
            return isRunning;
        }

        /**
         * Sets if runnable method is running.
         *
         * @param isRunning Running value to be set
         */
        public void setRunning(final boolean isRunning) {
            this.isRunning = isRunning;
        }
    }

    /**
     * Set the backprop directory.
     *
     * @param currentLocation the current location of the backprop dir.
     */
    private void setBackropDirectory(final String currentLocation) {
        backpropDirectory = currentLocation;
    }

    /**
     * Get the backprop directory.
     *
     * @return the location of the backprop directory.
     */
    private String getBackropDirectory() {
        return backpropDirectory;
    }

}
