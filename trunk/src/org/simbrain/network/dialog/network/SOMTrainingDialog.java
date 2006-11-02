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
import org.simnet.networks.SOM;

/**

 * <b>SOMTrainingDialog</b> is a dialog box for training SOM networks.
 */
public class SOMTrainingDialog extends StandardDialog implements
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
    private LabelledItemPanel parametersPanel = new LabelledItemPanel();

    /** Tabbed Panel. */
    private JTabbedPane tabbedPane = new JTabbedPane();

    /** Input file button. */
    private JButton jbInputsFile = new JButton("None selected");

    /** Epochs field. */
    private JTextField tfEpochs = new JTextField();

    /** Learning Rate field. */
    private JTextField tfLearningRate = new JTextField();

    /** Neighborhood Size field. */
    private JTextField tfNeighborhoodSize = new JTextField();

    /** Data interval field. */
    private JTextField tfDataInterval = new JTextField();

    /** AlphaDecayRate value field. */
    private JTextField tfAlphaDecayRate = new JTextField();

    /** NeighborhoodDecayAmount value field. */
    private JTextField tfNeigborhoodDecayAmount = new JTextField();

    /** Randomize button. */
    private JButton jbReset = new JButton("Reset");

    /** Train button. */
    private JButton jbTrain = new JButton("Train");

    /** Play button. */
    private JButton jbPlay = new JButton(ResourceManager.getImageIcon("Play.png"));

    /** Step button. */
    private JButton jbStep = new JButton(ResourceManager.getImageIcon("Step.png"));

    /** Epochs label. */
    private JLabel epochs = new JLabel();

    /** Learning Rate label. */
    private JLabel learningRate = new JLabel();

    /** Neighborhood Size label. */
    private JLabel neighborhoodSize = new JLabel();

    /** Inputs training value. */
    private double[][] inputsTrain;

    /** Update completed boolean value. */
    private boolean updateCompleted = false;

    /** SOM network. */
    private SOM som;

    /** Location of SOM directory. */
    private static String somDirectory =  NetworkPreferences.getCurrentSOMDirectory();

    /** SOM training dialog thread. */
    private SOMTDialogThread theThread = null;

    /**
     * This method is the default constructor.
     *
     * @param som SOM network
     */
    public SOMTrainingDialog(final SOM som) {

        this.som = som;
        //Initialize Dialog
        setTitle("Train SOM Network");
        fillFieldValues();
        fillLabelValues();

        //Set up top panel
        topPanel.addItem("Input file", jbInputsFile);
        topPanel.addItem("Reset network", jbReset);

        //Set up bottom panel
        bottomPanel.addItem("Epochs", epochs);
        bottomPanel.addItem("Learning Rate", learningRate);
        bottomPanel.addItem("Neighborhood Size", neighborhoodSize);

        //Setup panels for tabs
        createUserPanel();
        createBatchPanel();
        createParametersPanel();

        //Create tabs
        tabbedPane.addTab("User", userPanel);
        tabbedPane.addTab("Batch", batchPanel);
        tabbedPane.addTab("Parameters", parametersPanel);

        jbInputsFile.addActionListener(this);
        jbReset.addActionListener(this);
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
        userPanel.addItem("Data Interval", tfDataInterval);
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
    private void createParametersPanel() {
        parametersPanel.addItem("Learning Rate", tfLearningRate);
        parametersPanel.addItem("Neighborhood Size", tfNeighborhoodSize);
        parametersPanel.addItem("Learning Decay Rate", tfAlphaDecayRate);
        parametersPanel.addItem("Neighborhood Decay Amount", tfNeigborhoodDecayAmount);
    }

    /**
     * Called when dialog closes.
     */
    protected void closeDialogOk() {
        som.setInitAlpha(Double.parseDouble(tfLearningRate.getText()));
        som.setInitNeighborhoodSize(Double.parseDouble(tfNeighborhoodSize.getText()));
        som.setAlphaDecayRate(Double.parseDouble(tfAlphaDecayRate.getText()));
        som.setNeighborhoodDecayAmount(Integer.parseInt(tfNeigborhoodDecayAmount.getText()));
        NetworkPreferences.setCurrentSOMDirectory(getSOMDirectory());
        stopThread();
        super.closeDialogOk();
    }

    /**
     * @see StandardDialog
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
     * Sets current input and output training files.
     */
    private void checkTrainingFiles() {
        if (som.getTrainingINFile() != null) {
            setInputTraining(som.getTrainingINFile());
        }
    }

    /**
     * Sets the input training file.
     *
     * @param theFile The file to set input training
     */
    private void setInputTraining(final File theFile) {
        inputsTrain = Utils.getDoubleMatrix(theFile);
        jbInputsFile.setText(theFile.getName());
        som.setTrainingInputs(inputsTrain);
        som.setNumInputVectors(inputsTrain.length);
        som.setTrainingINFile(theFile);
    }

    /**
     * Responds to action within the dialog.
     *
     * @param e Action event
     */
    public void actionPerformed(final ActionEvent e) {
        Object o = e.getSource();

        if (o == jbInputsFile) {
            SFileChooser chooser = new SFileChooser(getSOMDirectory(), "csv");
            File theFile = chooser.showOpenDialog();

            if (theFile == null) {
                return;
            }

            setSOMDirectory(chooser.getCurrentLocation());
            setInputTraining(theFile);
        } else if (o == jbReset) {
            som.reset();
            som.fireNetworkChanged();
        } else if (o == jbTrain) {
            setValues();
            som.train();
            som.fireNetworkChanged();
            learningRate.setText(Double.toString(som.getAlpha()));
            epochs.setText(Integer.toString(som.getEpochs()));
            neighborhoodSize.setText(Double.toString(som.getNeighborhoodSize()));
            bottomPanel.repaint();
        } else if (o == jbPlay) {
            setValues();
            if (theThread == null) {
                theThread = new SOMTDialogThread(this);
            }

            if (!theThread.isRunning()) {
                jbPlay.setIcon(ResourceManager.getImageIcon("Stop.png"));
                theThread.setRunning(true);
                theThread.start();
            } else {
                jbPlay.setIcon(ResourceManager.getImageIcon("Play.png"));

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
        if (inputsTrain != null) {
            som.iterate();
            epochs.setText(Integer.toString(som.getEpochs()));
            learningRate.setText(Double.toString(som.getAlpha()));
            neighborhoodSize.setText(Double.toString(som.getNeighborhoodSize()));            updateCompleted = true;
            bottomPanel.repaint();
        }
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        tfEpochs.setText("" + som.getBatchSize());
        tfLearningRate.setText("" + som.getInitAlpha());
        tfNeighborhoodSize.setText("" + som.getInitNeighborhoodSize());
        tfDataInterval.setText("" + som.getDataInterval());
        tfAlphaDecayRate.setText(Double.toString(som.getAlphaDecayRate()));
        tfNeigborhoodDecayAmount.setText(Integer.toString(som.getNeighborhoodDecayAmount()));
        this.checkTrainingFiles();
    }

    /**
     * Populate labels with current data.
     */
    public void fillLabelValues() {
        epochs.setText(Integer.toString(som.getEpochs()));
        learningRate.setText(Double.toString(som.getAlpha()));
        neighborhoodSize.setText(Double.toString(som.getNeighborhoodSize()));            updateCompleted = true;
    }

    /**
     * Set projector values based on fields.
     */
    public void setValues() {
        som.setBatchSize(Integer.parseInt(tfEpochs.getText()));
        som.setInitAlpha(Double.parseDouble(tfLearningRate.getText()));
        som.setInitNeighborhoodSize(Double.parseDouble(tfNeighborhoodSize.getText()));
        som.setAlphaDecayRate(Double.parseDouble(tfAlphaDecayRate.getText()));
        som.setNeighborhoodDecayAmount(Integer.parseInt(tfNeigborhoodDecayAmount.getText()));
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
     * SOMTDialogThreadcreates a system thread to be run when training SOM networks.
     *
     */
    public class SOMTDialogThread extends Thread {

        /** SOM training dialog. */
        private SOMTrainingDialog dialog = null;

        /** Is thread running boolean value. */
        private volatile boolean isRunning = false;

        /**
         * Runs the thread.
         */
        private Runnable iterate = new Runnable() {
            public void run() {
                dialog.iterate();
            }
        };

        /**
         * SOM training dialog thread constructor.
         *
         * @param dialog Dialog to run backprop
         */
        public SOMTDialogThread(final SOMTrainingDialog dialog) {
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
     * Set the SOM directory.
     *
     * @param currentLocation the current location of the backprop dir.
     */
    private void setSOMDirectory(final String currentLocation) {
        somDirectory = currentLocation;
    }

    /**
     * Get the SOM directory.
     *
     * @return the location of the backprop directory.
     */
    private String getSOMDirectory() {
        return somDirectory;
    }

}
