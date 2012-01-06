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

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.groups.FeedForward;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.trainers.IterableAlgorithm;
import org.simbrain.network.trainers.Trainer;
import org.simbrain.network.trainers.TrainerListener;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.resource.ResourceManager;
import org.simbrain.util.ClassDescriptionPair;
import org.simbrain.util.Utils;
import org.simbrain.util.genericframe.GenericFrame;
import org.simbrain.util.genericframe.GenericJFrame;

/**
 * GUI for supervised learning in Simbrain, using back-propagation, LMS, and
 * (eventually) other algorithms. A GUI front end for the trainer class.
 *
 * @author Jeff Yoshimi
 */
public class TrainerPanel extends JPanel {

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

    /** Data panel. */
    private JPanel dataPanel = new JPanel();

    /** Combo box for training algorithm. */
    private JComboBox cbTrainingAlgorithm;

    /** Combo box for training algorithm. */
    private JComboBox cbDataFormat;

    /** Parent frame. */
    private GenericFrame parentFrame;

    /**
     * Type of the data viewer: an input data viewer or training data viewer.
     */
    enum DataFormat {
        LOAD_DATA {
            public String toString() {
                return "Load data";
            }
        },
        SINGLE_STEP {
            public String toString() {
                return "Use current activations";
            }
        };
    };

    /** Panel showing the current "run control" */
    private JPanel currentTrainerRunControls;

    /** Indicates that (an iterative) training algorithm is running. */
    private JLabel runningLabel = new JLabel();

    /**
     * Type of the data viewer: an input data viewer or training data viewer.
     */
    public enum TrainerDataType {
        Input, Trainer
    };

    /** Reference to parent panel. Used as a reference for displaying the trainer panel. */
    private final NetworkPanel panel;
    
    /**
     * Construct a trainer panel around a trainer object.
     *
     * @param networkPanel the parent network panel
     * @param trainer the trainer this panel represents
     */
    public TrainerPanel(final NetworkPanel networkPanel, final Trainer trainer) {

        // Initial setup
        this.trainer = trainer;
        this.panel = networkPanel;

        // Main Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Top items
        topItems = new JPanel();
        topItems.setLayout(new BoxLayout(topItems, BoxLayout.Y_AXIS));
        topItems.setBorder(BorderFactory.createTitledBorder("Learning Rule"));
        JPanel comboBoxPlusButton = new JPanel();
        comboBoxPlusButton.setAlignmentX(CENTER_ALIGNMENT);
        cbTrainingAlgorithm = new JComboBox(Trainer.getRuleList());
        cbTrainingAlgorithm.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                trainerChanged();
            }
        });
        comboBoxPlusButton.add(cbTrainingAlgorithm);
        JButton properties = new JButton(
                TrainerGuiActions.getPropertiesDialogAction(this));
        comboBoxPlusButton.add(properties);
        topItems.add(comboBoxPlusButton);
        mainPanel.add(topItems);

        // Center run panel
        runPanel = new JPanel();
        runPanel.setLayout(new BoxLayout(runPanel, BoxLayout.Y_AXIS));
        runPanel.setBorder(BorderFactory.createTitledBorder("Controls"));
        mainPanel.add("Center", runPanel);

        // Data windows
        dataWindow = new JPanel(new BorderLayout());
        dataWindow.setBorder(BorderFactory.createTitledBorder("Data"));
        dataWindow.setLayout(new BoxLayout(dataWindow, BoxLayout.Y_AXIS));
        cbDataFormat = new JComboBox(DataFormat.values());
        cbDataFormat.setAlignmentX(CENTER_ALIGNMENT);
        JPanel buffer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buffer.add(cbDataFormat);
        dataWindow.add("North", buffer);
        dataWindow.add(dataPanel);

        cbDataFormat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                refreshDataPanel();
            }
        });

        mainPanel.add(dataWindow);

        // Add trainer listener
        initializeTrainerListener();

        // Initialize trainer specific panels
        refreshDataPanel();

        // Add mainPanel
        add(mainPanel);

    }

    /**
     * Refresh the data panel.
     */
    private void refreshDataPanel() {
        dataPanel.removeAll();
        if (cbDataFormat.getSelectedItem() == DataFormat.LOAD_DATA) {
            JPanel loadPanel = new LoadDataPanel();
            dataPanel.add(loadPanel);
        } else if (cbDataFormat.getSelectedItem() == DataFormat.SINGLE_STEP) {
            JPanel singleStepPanel = new SingleStepPanel();
            dataPanel.add(singleStepPanel);
        }
        trainerChanged();
    }

    /**
     * Panel for single step training.
     */
    class SingleStepPanel extends JPanel {

        /**
         * Construct panel.
         */
        public SingleStepPanel() {
            final JLabel currentInput = new JLabel("Input: ");
            final JLabel currentTraining = new JLabel("Training: ");

            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            JButton setDataButton = new JButton("Set Data");
            setDataButton.setAlignmentX(CENTER_ALIGNMENT);
            currentInput.setAlignmentX(CENTER_ALIGNMENT);
            currentTraining.setAlignmentX(CENTER_ALIGNMENT);
            this.add(setDataButton);
            this.add(currentInput);
            this.add(currentTraining);
            setDataButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    double[] inputVector = Network
                            .getActivationVector(trainer.getInputLayer());
                    double[] trainingVector = Network
                            .getActivationVector(trainer.getOutputLayer());
                    currentInput.setText("Input: "
                            + Utils.getVectorString(inputVector, ","));
                    currentTraining.setText("Target:  "
                            + Utils.getVectorString(trainingVector, ","));
                    trainer.setInputData(new double[][] { inputVector});
                    trainer.setTrainingData(new double[][] { trainingVector});
                    //parentFrame.pack();
                }
            });
        }

    }

    /**
     * Panel for editing or loading data.
     */
    class LoadDataPanel extends JPanel {

        /**
         * Construct panel.
         */
        public LoadDataPanel() {
            super();
            add(new JButton(TrainerGuiActions.getEditDataAction(
                    TrainerPanel.this, TrainerDataType.Input)));
            add(new JButton(TrainerGuiActions.getEditDataAction(
                    TrainerPanel.this, TrainerDataType.Trainer)));
        }

    }

    /**
     * Change the trainer based on the combo box selection.
     */
    private void trainerChanged() {
                
        // Update the training method
        String name = ((ClassDescriptionPair) cbTrainingAlgorithm
                .getSelectedItem()).getSimpleName();
        trainer.setTrainingMethod(name);
        if (cbDataFormat.getSelectedItem() != DataFormat.SINGLE_STEP) {
            trainer.getNetwork().clearActivations();
            trainer.getNetwork().clearBiases();
        }

        // Update graphics panel
        if (currentTrainerRunControls != null) {
            runPanel.remove(currentTrainerRunControls);
        }

        // Set run controls
        if (trainer.getTrainingMethod() instanceof IterableAlgorithm) {
            currentTrainerRunControls = createRunPanelIterable();
        } else {
            currentTrainerRunControls = createRunPanelNonIterable();
        }
        runPanel.add(currentTrainerRunControls);
        if (parentFrame != null) {
            parentFrame.pack();            
        }
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
                if (trainer.getTrainingMethod() instanceof IterableAlgorithm) {
                    if (model != null) {
                        model.update();
                        IterableAlgorithm theTrainer = (IterableAlgorithm) trainer;
                        model.addData(0, theTrainer.getIteration(),
                                theTrainer.getError());
                    }
                    updateErrorField();
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
                trainer.update();
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
        if (trainer.getTrainingMethod() instanceof IterableAlgorithm) {
            rmsError.setText("Error:"
                    + Utils.round(((IterableAlgorithm) trainer
                            .getTrainingMethod()).getError(), 4));
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
     * Iterate the trainer one time and update graphics.
     */
    final void iterate() {
        trainer.update();
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
            parentFrame.pack();
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
        int[] topology = new int[] { 2, 2, 1 };
        network.addGroup(new FeedForward(network, topology, null));
        NeuronGroup inputs = (NeuronGroup) network.getGroup("Group_2");
        NeuronGroup outputs = (NeuronGroup) network.getGroup("Group_4");
        Trainer trainer = new Trainer(network, inputs.getNeuronList(),
                outputs.getNeuronList(), "Backprop");
        GenericJFrame frame = new GenericJFrame();
        TrainerPanel trainerPanel = new TrainerPanel(null, trainer);
        trainerPanel.setFrame(frame);
        frame.setContentPane(trainerPanel);
        frame.pack();
        frame.setVisible(true);
    }

    /**
     * @param parentFrame the parentFrame to set
     */
    public void setFrame(GenericFrame parentFrame) {
        this.parentFrame = parentFrame;
        parentFrame.setTitle("Train " + trainer.getTopologyDescription() + " net");          
    }

    /**
     * Return references to parent network panel.
     *
     * @return network panel.
     */
    public NetworkPanel getNetworkPanel() {
        return panel;
    }

}
