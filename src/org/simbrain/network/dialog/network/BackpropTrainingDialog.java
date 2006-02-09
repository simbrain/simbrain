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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.simbrain.resource.ResourceManager;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simnet.networks.Backprop;


/**
 * <b>BackpropTrainingDialog</b> is a dialog box for training backprop networks.
 */
public class BackpropTrainingDialog extends StandardDialog implements
        ActionListener {

    /** Main panel. */
    private LabelledItemPanel mainPanel = new LabelledItemPanel();

    /** Input file button. */
    private JButton jbInputsFile = new JButton("None selected");

    /** Output file button. */
    private JButton jbOutputsFile = new JButton("None selected");

    /** Ephchs field. */
    private JTextField tfEpochs = new JTextField();

    /** Eta field. */
    private JTextField tfEta = new JTextField();

    /** Mu field. */
    private JTextField tfMu = new JTextField();

    /** Error interval field. */
    private JTextField tfErrorInterval = new JTextField();

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

    /** Backprop network. */
    private Backprop backprop;

    /** Backprop training dialog thread. */
    private BPTDialogThread theThread = null;

    /**
     * This method is the default constructor.
     *
     * @param backprop Backprop network
     */
    public BackpropTrainingDialog(final Backprop backprop) {
        this.backprop = backprop;
        backprop.buildSnarliNetwork();
        //Initialize Dialog
        setTitle("Train Backprop Network");
        fillFieldValues();
        this.setLocation(600, 0); //Sets location of network dialog

        //Set up grapics panel
        mainPanel.addItem("Input file", jbInputsFile);
        mainPanel.addItem("Output file", jbOutputsFile);
        mainPanel.addItem("Epochs", tfEpochs);
        mainPanel.addItem("Learning rate", tfEta);
        mainPanel.addItem("Momentum", tfMu);
        mainPanel.addItem("Error Interval", tfErrorInterval);
        mainPanel.addItem("Randomize network", jbRandomize);
        mainPanel.addItem("Train network", jbTrain);
        mainPanel.addItem("Play/Stop", jbPlay);
        mainPanel.addItem("Step", jbStep);
        mainPanel.addItem("RMSError", rmsError);

        jbInputsFile.addActionListener(this);
        jbOutputsFile.addActionListener(this);
        jbRandomize.addActionListener(this);
        jbTrain.addActionListener(this);
        jbPlay.addActionListener(this);
        jbStep.addActionListener(this);

        setContentPane(mainPanel);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
      super.closeDialogOk();
    }

    /**
     * Responds to action within the dialog.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
//        Object o = e.getSource();
//
//        if (o == jbInputsFile) {
//            SFileChooser chooser = new SFileChooser(networkPanel.getBackropDirectory(), "csv");
//            File theFile = chooser.showOpenDialog();
//
//            if (theFile == null) {
//                return;
//            }
//
//            networkPanel.setBackropDirectory(chooser.getCurrentLocation());
//            inputs_train = Utils.getDoubleMatrix(theFile);
//            jbInputsFile.setText(theFile.getName());
//            theNet.setTrainingInputs(inputs_train);
//        } else if (o == jbOutputsFile) {
//            SFileChooser chooser = new SFileChooser(networkPanel.getBackropDirectory(), "csv");
//            File theFile = chooser.showOpenDialog();
//
//            if (theFile == null) {
//                return;
//            }
//
//            networkPanel.setBackropDirectory(chooser.getCurrentLocation());
//            outputs_train = Utils.getDoubleMatrix(theFile);
//            jbOutputsFile.setText(theFile.getName());
//            theNet.setTrainingOutputs(outputs_train);
//        } else if (o == jbRandomize) {
//            theNet.randomize();
//            networkPanel.renderObjects();
//            networkPanel.repaint();
//        } else if (o == jbTrain) {
//            setValues();
//            theNet.train();
//            networkPanel.renderObjects();
//            networkPanel.repaint();
//        } else if (o == jbPlay) {
//            setValues();
//
//            if (theThread == null) {
//                theThread = new BPTDialogThread(this);
//            }
//
//            if (!theThread.isRunning()) {
//                jbPlay.setIcon(ResourceManager.getImageIcon("Stop.gif"));
//                theThread.setRunning(true);
//                theThread.start();
//            } else {
//                jbPlay.setIcon(ResourceManager.getImageIcon("Play.gif"));
//
//                if (theThread == null) {
//                    return;
//                }
//
//                theThread.setRunning(false);
//                theThread = null;
//            }
//        } else if (o == jbStep) {
//            setValues();
//            iterate();
//        }
    }

    /**
     * Iterate network training.
     */
    public void iterate() {
        backprop.iterate();
//        networkPanel.renderObjects();
        rmsError.setText(Double.toString(backprop.getOut().getRMSError()));
        updateCompleted = true;
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfEpochs.setText("" + backprop.getEpochs());
        tfEta.setText("" + backprop.getEta());
        tfMu.setText("" + backprop.getMu());
        tfErrorInterval.setText("" + backprop.getErrorInterval());
    }

    /**
     * Set projector values based on fields.
     */
    public void setValues() {
        backprop.setEpochs(Integer.parseInt(tfEpochs.getText()));
        backprop.setEta(Double.parseDouble(tfEta.getText()));
        backprop.setMu(Double.parseDouble(tfMu.getText()));
        backprop.setErrorInterval(Integer.parseInt(tfErrorInterval.getText()));
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
        private BackpropTrainingDialog dialog = null;

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
        public BPTDialogThread(final BackpropTrainingDialog dialog) {
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
}
