/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.gui.trainer;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Constructor;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.builders.LayeredNetworkBuilder;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.trainers.Backprop;
import org.simbrain.network.trainers.IterableAlgorithm;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.ClassDescriptionPair;
import org.simbrain.util.Utils;

/**
 * GUI for supervised learning in Simbrain, using back-propagation, LMS, and
 * (eventually) other algorithms. A GUI front end for the trainer class.
 *
 * @author jeff yoshimi
 */
public class TrainerPanel extends JDialog {

    /** Data window. */
    private JPanel dataWindow;

    /** Reference to trainer object. */
    private Trainer trainer;

    /** Data for the error graph. */
    private TimeSeriesModel model;

    /** Text field for setting number of iterations to run. */
    private JTextField tfIterations;

    /** Error label. */
    private JLabel rmsError = new JLabel("Error: ----- ");

    /** Update completed boolean value. */
    private boolean updateCompleted = true;

    /** Top panel. */
    private JPanel topItems;

    /** Run panel; changes depending on learning rule selected. */
    private JPanel runPanel;

    /** Combo box for training algorithm. */
    private JComboBox cbTrainingAlgorithm;

    /** Panel showing the current "run control" */
    private JPanel currentTrainerRunControls;

    /** Indicates that (an iterative) training algorithm is running. */
    private JLabel runningLabel = new JLabel();

    /**
     * Current input data file. When re-opening input data uses this as a
     * pointer.
     */
    private File currentInputFile;

    /**
     * Current training data file. When re-opening input data uses this as a
     * pointer.
     */
    private File currentTrainingFile;

    /**
     * Type of the data viewer: an input data viewer or training data viewer.
     */
    public enum TrainerDataType {
        Input, Trainer
    };

    /**
     * Construct a trainer panel around a trainer object.
     *
     * @param trainer the trainer this panel represents
     */
    public TrainerPanel(final Trainer trainer) {

        // Initial setup
        this.trainer = trainer;

        // Main Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(3, 1));

        // Top items
        topItems = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topItems.setBorder(BorderFactory.createTitledBorder("Learning Rule"));
        cbTrainingAlgorithm = new JComboBox(Trainer.getRuleList());
        cbTrainingAlgorithm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                trainerChanged();
            }
        });
        topItems.add(cbTrainingAlgorithm);
        JButton properties = new JButton(
                TrainerGuiActions.getPropertiesDialogAction(this));
        topItems.add(properties);
        mainPanel.add(topItems);

        // Center run panel
        runPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        runPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        mainPanel.add("Center", runPanel);

        // Data windows
        dataWindow = new JPanel();
        dataWindow.setBorder(BorderFactory.createTitledBorder("Data"));
        dataWindow.add(new JButton(TrainerGuiActions.getEditDataAction(this,
                TrainerDataType.Input)));
        dataWindow.add(new JButton(TrainerGuiActions.getEditDataAction(this,
                TrainerDataType.Trainer)));
        mainPanel.add(dataWindow);

        // Add trainer listener
        initializeTrainerListener();

        // Initialize trainer specific panels
        trainerChanged();

        // Add mainPanel
        add(mainPanel);

    }

    /**
     * Change the trainer based on the combo box selection.
     */
    private void trainerChanged() {

        // Reset the trainer object
        Class<?> selectedTrainer = ((ClassDescriptionPair) cbTrainingAlgorithm
                .getSelectedItem()).getTheClass();
        Trainer oldTrainer = trainer;
        try {
            // Get the copy constructor and invoke it
            Constructor<?> trainerConstructor = selectedTrainer
                    .getConstructor(Trainer.class);
            trainer = (Trainer) trainerConstructor.newInstance(oldTrainer);
            trainer.init();
            trainer.getNetwork().clearActivations();
            trainer.getNetwork().clearBiases();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Update graphics panel
        if (currentTrainerRunControls != null) {
            runPanel.remove(currentTrainerRunControls);
        }
        if (trainer instanceof IterableAlgorithm) {
            currentTrainerRunControls = createRunPanelIterable();
        } else {
            currentTrainerRunControls = createRunPanelNonIterable();

        }
        runPanel.add(currentTrainerRunControls);
        pack();
    }

    /**
     * Initialize the trainer listener. Update the panel based on changes that
     * occur in the trainer.
     */
    private void initializeTrainerListener() {

        trainer.addListener(new TrainerListener() {

            /*
             * {@inheritDoc}
             */
            public void errorUpdated() {
                if (trainer instanceof IterableAlgorithm) {
                    model.update();
                    IterableAlgorithm theTrainer = (IterableAlgorithm) trainer;
                    model.addData(0, theTrainer.getIteration(),
                            theTrainer.getError());
                }
            }

            /*
             * {@inheritDoc}
             */
            public void inputDataChanged(double[][] inputData) {
                //System.out.println("Input Data Changed");
            }

            /*
             * {@inheritDoc}
             */
            public void trainingDataChanged(double[][] inputData) {
                //System.out.println("Training Data Changed");
            }

        });
    }

    /**
     * Create the "run" panel for non-iterable learning algorithms.
     *
     * @return the panel
     */
    private JPanel createRunPanelNonIterable() {
        JPanel runPanel = new JPanel();
        JButton apply = new JButton("Apply");
        apply.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent arg0) {
                trainer.apply();
                trainer.getNetwork().getRootNetwork().fireNetworkChanged();
            }

        });
        runPanel.add(apply);
        return runPanel;
    }

    /**
     * Create the run panel for iterable algorithms.
     *
     * @return the panel
     */
    private JPanel createRunPanelIterable() {

        JPanel iterationPanel = new JPanel();

        // TODO
        // model = new TimeSeriesModel(1);
        // model.setRangeLowerBound(0);
        // model.setRangeUpperBound(1);
        // model.setAutoRange(false);
        // model.setWindowSize(1000);
        // Configure time series plot
        // TimeSeriesPlotPanel graphPanel = new TimeSeriesPlotPanel(model);
        // graphPanel.getChartPanel().getChart().setTitle("");
        // graphPanel.getChartPanel().getChart().getXYPlot().getDomainAxis()
        // .setLabel("Iterations");
        // graphPanel.getChartPanel().getChart().getXYPlot().getRangeAxis()
        // .setLabel("Error");
        // graphPanel.getChartPanel().getChart().removeLegend();
        // graphPanel.setPreferredSize(new Dimension(
        // graphPanel.getPreferredSize().width, 250));

        // Customize button panel; first remove all buttons
        // graphPanel.removeAllButtonsFromToolBar();
        // Add clear and prefs button
        // graphPanel.addClearGraphDataButton();
        // graphPanel.addPreferencesButton();

        // Run
        iterationPanel.add(new JButton(TrainerGuiActions.getRunAction(this)));
        iterationPanel.add(new JButton(TrainerGuiActions.getStepAction(this)));
        runningLabel.setIcon(ResourceManager.getImageIcon("Throbber.gif"));
        runningLabel.setVisible(false);
        iterationPanel.add(runningLabel);

        // Batch
        // iterationPanel.add(new
        // JButton(TrainerGuiActions2.getBatchTrainAction(this)));

        // Iterations
        // tfIterations = new JTextField("300");
        // iterationPanel.add(new JLabel("Iterations"));
        // iterationPanel.add(tfIterations);

        // Error
        iterationPanel.add(rmsError);

        // Randomize (de-activate depending...)
        iterationPanel.add(new JButton(TrainerGuiActions
                .getRandomizeNetworkAction(this)));

        return iterationPanel;
    }

    /**
     * Update error text field.
     */
    private void updateErrorField() {
        if (trainer instanceof IterableAlgorithm) {
            rmsError.setText("Error:"
                    + Utils.round(((IterableAlgorithm) trainer).getError(), 4));
        }
    }

    /**
     * Batch train network, using text field.
     */
    public final void batchTrain() {
        if (trainer != null) {
            // trainer.train(Integer.parseInt(tfIterations.getText()));
            updateErrorField();
        }
    }

    /**
     * @return the currentInputFile
     */
    public File getCurrentInputFile() {
        return currentInputFile;
    }

    /**
     * @param currentInputFile the currentInputFile to set
     */
    public void setCurrentInputFile(File currentInputFile) {
        this.currentInputFile = currentInputFile;
    }

    /**
     * @return the currentTrainingFile
     */
    public File getCurrentTrainingFile() {
        return currentTrainingFile;
    }

    /**
     * @param currentTrainingFile the currentTrainingFile to set
     */
    public void setCurrentTrainingFile(File currentTrainingFile) {
        this.currentTrainingFile = currentTrainingFile;
    }

    /**
     * Iterate the trainer one time and update graphics.
     */
    final void iterate() {
        trainer.apply();
        updateErrorField();
        // model.addData(0, trainer.getIteration(), trainer.getCurrentError());
    }

    /**
     * @return boolean updated completed.
     */
    final boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Sets updated completed value.
     *
     * @param updateCompleted Updated completed value to be set
     */
    final void setUpdateCompleted(final boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
        if (runningLabel != null) {
            if (updateCompleted) {
                runningLabel.setVisible(false);
            } else {
                runningLabel.setVisible(true);
            }
            pack();
        }
    }

    /**
     * @return the trainer
     */
    final Trainer getTrainer() {
        return trainer;
    }

    /**
     * Test GUI.
     *
     * @param args
     */
    public static void main(String[] args) {
        RootNetwork network = new RootNetwork();
        LayeredNetworkBuilder builder = new LayeredNetworkBuilder();
        int[] nodesPerLayer = new int[] { 2, 1 };
        builder.setNodesPerLayer(nodesPerLayer);
        builder.buildNetwork(network);
        Backprop trainer = new Backprop(network, network.getGroup("Group_1")
                .getNeuronList(), network.getGroup("Group_2").getNeuronList());
        TrainerPanel trainerPanel = new TrainerPanel(trainer);
        trainerPanel.pack();
        trainerPanel.setVisible(true);
    }

}
