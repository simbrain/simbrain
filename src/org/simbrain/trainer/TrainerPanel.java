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
package org.simbrain.trainer;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.simbrain.network.builders.LayeredNetworkBuilder;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.NeuronLayer;
import org.simbrain.network.groups.NeuronLayer.LayerType;
import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.plot.timeseries.TimeSeriesModel;
import org.simbrain.plot.timeseries.TimeSeriesPlotPanel;
import org.simbrain.util.Utils;
import org.simbrain.util.table.SimbrainDataTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainTableListener;

/**
 * GUI for supervised learning in Simbrain, using back-propagation, LMS, and
 * (eventually) other algorithms. A front end for the trainer class.
 *
 * @author ericneilj
 * @author jeff yoshimi
 * @see org.simbrain.trainer.Trainer
 */
public class TrainerPanel extends JPanel {

    /** Choices of training algorithms. */
    // TODO: Get this list from Trainer.java
    private String[] trainingAlgorithms = { "Backprop  ", "Least Mean Squares" };

    /** Input data window. */
    private TrainerDataWindow inputDataWindow;

    /** Training data window. */
    private TrainerDataWindow trainingDataWindow;

    /** Reference to trainer object. */
    private Trainer trainer;

    /** Data for the error graph. */
    private TimeSeriesModel model;

    /** Text field for setting number of iterations to run. */
    private JTextField tfIterations;

    /** Error label. */
    private JLabel rmsError = new JLabel("Error: --- ");

    /** Update completed boolean value. */
    private boolean updateCompleted = true;

    /** Top jpanel. */
    private JPanel topItems;

    /**
     * Type of the data window: an input window (showing input data and layer)
     * or a training window (showing training data and an output layer).
     */
    private enum WindowType {
        Input, Trainer
    };

    /**
     * The observer of the current network.If groups are added or removed in the
     * current network, this must be reflected in the Input and output layer
     * combo boxes.
     */
    private GroupListener groupListener = new GroupListener() {

        /**
         * {@inheritDoc}
         */
        public void groupAdded(NetworkEvent<Group> e) {
            updateGroupComboBoxes();
        }

        /**
         * {@inheritDoc}
         */
        public void groupChanged(NetworkEvent<Group> networkEvent) {
            // No need to change anything, since this currently
            // just makes the group smaller, so no change needed table.
        }

        /**
         * {@inheritDoc}
         */
        public void groupRemoved(NetworkEvent<Group> e) {
            updateGroupComboBoxes();
        }

        /**
         * {@inheritDoc}
         */
        public void groupParameterChanged(NetworkEvent<Group> networkEvent) {
            // No implementation
        }
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
        mainPanel.setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        // Top items
        topItems = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topItems.setBorder(BorderFactory.createEtchedBorder());
        topItems.setPreferredSize(new Dimension(550, 45));

        topItems.add(new JLabel("Learning rule"));
        JComboBox cbTrainingAlgorithm = new JComboBox(trainingAlgorithms);
        topItems.add(cbTrainingAlgorithm);
        JButton properties = new JButton(
                TrainerGuiActions.getPropertiesDialogAction(this));
        topItems.add(properties);
        topPanel.add("North", topItems);
        topPanel.add("Center", createGraphPanel());

        // Split Pane (Contains two data tables)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBorder(null);
        // keeps divider centered on resize
        splitPane.setResizeWeight(.5);

        // Data windows
        inputDataWindow = new TrainerDataWindow(WindowType.Input);
        trainingDataWindow = new TrainerDataWindow(WindowType.Trainer);
        splitPane.setLeftComponent(inputDataWindow);
        splitPane.setRightComponent(trainingDataWindow);

        // Initialize tables

        // Bottom Button Panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JButton("Cancel"));
        bottomPanel.add(new JButton("Ok"));

        // Put it all together
        mainPanel.add("North", topPanel);
        mainPanel.add("Center", splitPane);
        add(mainPanel);

        // Initialize panel with trainer data
        initializePanelFromTrainer();

        // Add trainer listener
        initializeTrainerListener();

    }

    /**
     * Initialize the panel using trainer data.
     */
    private void initializePanelFromTrainer() {
        if (trainer != null) {
            if (trainer.getInputData() != null) {
                inputDataWindow.setData(trainer.getInputData());
            }
            if (trainer.getTrainingData() != null) {
                trainingDataWindow.setData(trainer.getTrainingData());
            }
            if (trainer.getInputLayer() != null) {
                inputDataWindow.setLayer(trainer.getInputLayer());
            }
            if (trainer.getInputData() != null) {
                trainingDataWindow.setLayer(trainer.getOutputLayer());
            }
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
            public void errorUpdated(double error) {
                model.update();
                model.addData(0, trainer.getIteration(), error);
            }

            /*
             * {@inheritDoc}
             */
            public void networkChanged(Network oldNetwork, Network newNetwork) {

                if (model != null) {
                    model.clearData();
                }

                // Remove group listener from old network
                if (oldNetwork != null) {
                    oldNetwork.getRootNetwork().removeGroupListener(
                            groupListener);
                }

                if (newNetwork == null) {
                    // Clear combo box windows
                    inputDataWindow.repopulateGroupComboBox();
                    trainingDataWindow.repopulateGroupComboBox();
                    return;
                }

                // Listen for group change events
                newNetwork.getRootNetwork().addGroupListener(groupListener);

                // Update the input and output tables in the GUI
                updateGroupComboBoxes();
                inputDataWindow.initializeSelectedGroup();
                trainingDataWindow.initializeSelectedGroup();
            }

            // TODO: Currently data and layers are only changed from this panel,
            // so no need to respond to these events. But should check that new
            // data is different from existing data and then update if needed.

            /*
             * {@inheritDoc}
             */
            public void inputDataChanged(double[][] inputData) {
                // TODO Auto-generated method stub
            }

            /*
             * {@inheritDoc}
             */
            public void trainingDataChanged(double[][] inputData) {
                // TODO Auto-generated method stub
            }

            /*
             * {@inheritDoc}
             */
            public void inputLayerChanged(List<Neuron> inputLayer) {
                // TODO Auto-generated method stub
            }

            /*
             * {@inheritDoc}
             */
            public void outputLayerChanged(List<Neuron> trainingLayer) {
                // TODO Auto-generated method stub
            }
        });
    }

    /**
     * Adds a JComponent to the top panel on the left side. Currently used to
     * add a network selection box.
     *
     * @param component the component to add.
     */
    void addTopItem(JComponent component) {
        topItems.add(component, 0);
    }

    /**
     * Create the central panel, which displays the current error.
     *
     * @return the graph panel
     */
    private JPanel createGraphPanel() {

        // Configure time series model
        model = new TimeSeriesModel(1);
        model.setRangeLowerBound(0);
        model.setRangeUpperBound(1);
        model.setAutoRange(false);
        model.setWindowSize(1000);

        // Configure time series plot
        TimeSeriesPlotPanel graphPanel = new TimeSeriesPlotPanel(model);
        graphPanel.setBorder(BorderFactory
                .createTitledBorder("Error / Trainer Controls"));
        graphPanel.getChartPanel().getChart().setTitle("");
        graphPanel.getChartPanel().getChart().getXYPlot().getDomainAxis()
                .setLabel("Iterations");
        graphPanel.getChartPanel().getChart().getXYPlot().getRangeAxis()
                .setLabel("Error");
        graphPanel.getChartPanel().getChart().removeLegend();
        graphPanel.setPreferredSize(new Dimension(
                graphPanel.getPreferredSize().width, 250));

        // Customize button panel; first remove all buttons
        graphPanel.removeAllButtonsFromToolBar();

        // Run
        graphPanel.getButtonPanel().add(
                new JButton(TrainerGuiActions.getRunAction(this)));

        // Batch
        graphPanel.getButtonPanel().add(
                new JButton(TrainerGuiActions.getBatchTrainAction(this)));

        // Iterations
        tfIterations = new JTextField("300");
        graphPanel.getButtonPanel().add(new JLabel("Iterations"));
        graphPanel.getButtonPanel().add(tfIterations);

        // Error
        graphPanel.getButtonPanel().add(rmsError);

        // Randomize
        graphPanel.getButtonPanel().add(
                new JButton(TrainerGuiActions.getRandomizeNetworkAction(this)));

        // Add clear and prefs button
        graphPanel.addClearGraphDataButton();
        graphPanel.addPreferencesButton();

        return graphPanel;
    }

    /**
     * Update the input layer and output layer combo boxes (when groups are
     * added, removed, or changed in the current network).
     */
    public void updateGroupComboBoxes() {
        inputDataWindow.repopulateGroupComboBox();
        trainingDataWindow.repopulateGroupComboBox();
    }

    /**
     * Update error text field.
     */
    private void updateErrorField() {
        rmsError.setText("Error:" + Utils.round(trainer.getCurrentError(), 6));
    }

    /**
     * Batch train network, using text field.
     */
    public final void batchTrain() {
        if (trainer != null) {
            trainer.train(Integer.parseInt(tfIterations.getText()));
            updateErrorField();
        }
    }

    /**
     * Iterate the trainer one time and update graphics.
     */
    final void iterate() {
        trainer.train(1);
        updateErrorField();
        model.addData(0, trainer.getIteration(), trainer.getCurrentError());
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
    }

    /**
     * @return the trainer
     */
    final Trainer getTrainer() {
        return trainer;
    }

    /**
     * Get the SimbrainJTable associated with the input data for this trainer.
     *
     * @return the input data table
     */
    final SimbrainJTable getInputData() {
        return inputDataWindow.getDataTable();
    }

    /**
     * Get the SimbrainJTable associated with the training data for this
     * trainer.
     *
     * @return the training data table
     */
    final SimbrainJTable getTrainingData() {
        return trainingDataWindow.getDataTable();
    }

    /**
     * When the group or table is changed, check to see that they are
     * consistent, using the following rule: (1) if the layer has more neurons
     * than the table has columns, then expand the table accordingly. (2) if the
     * layer has fewer neurons than the table has columns, then leave the table
     * as is.
     *
     * @param dataTable the table to check
     * @param group the neuron layer to check
     */
    private void reconcileTableWithLayer(final SimbrainJTable dataTable,
            final NeuronGroup group) {

        // Size of group (number of neurons)
        int layerSize;

        // Initialize layer size and neuron iterator to 0 if group is null
        Iterator<Neuron> neuronIterator;
        if (group != null) {
            layerSize = group.getNeuronList().size();
            neuronIterator = group.getNeuronList().iterator();
        } else {
            layerSize = 0;
            neuronIterator = Collections.EMPTY_LIST.iterator();
        }

        // Size of column (First column is "#" sign so it's disregarded)
        int tableSize = dataTable.getColumnCount() - 1;

        // If layer size is greater than table size, expand the table
        if (layerSize > tableSize) {

            // Notify user about what's happening
            System.out.println("Table / Layer conflict.  Layer has "
                    + layerSize + " neurons, table has " + tableSize
                    + " columns. Modifying table to have " + layerSize
                    + " columns.");

            // Modify the table
            dataTable.getData().modifyRowsColumns(
                    dataTable.getData().getRowCount(), layerSize, 0);

        }

        // Rename column headings
        // Note that the loop starts at column 1 (column 0 shows the row
        // number)
        for (int i = 1; i < dataTable.getColumnCount(); i++) {
            if (neuronIterator.hasNext()) {
                dataTable.getColumnModel().getColumn(i)
                        .setHeaderValue(neuronIterator.next().getId());
            } else {
                dataTable.getColumnModel().getColumn(i).setHeaderValue("" + i);
            }
        }
        dataTable.getTableHeader().resizeAndRepaint();

    }

    /**
     * A Panel which represents a table of data in relation to a group of
     * neurons. Each neuron in the group is associated with one row of data.
     */
    private class TrainerDataWindow extends JPanel {

        /** Input layer combo box. */
        private JComboBox groupComboBox = new JComboBox();

        /** Table displaying input data. */
        private SimbrainJTable dataTable;

        /** Scroll pane. */
        private JScrollPane scrollPane;

        /** Trainer vs. Input window. */
        private WindowType type;

        /**
         * Constructor for Trainer Data Window.
         *
         * @param type type of window
         */
        TrainerDataWindow(final WindowType type) {
            this.type = type;
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(350,200));
            dataTable = new SimbrainJTable(new SimbrainDataTable(10, 4));
            scrollPane = new JScrollPane(dataTable);
            scrollPane.setPreferredSize(new Dimension(100, 100));
            setBorder(BorderFactory.createTitledBorder("" + type.name()
                    + " Table"));
            JPanel menuPanel = new JPanel();
            menuPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
            menuPanel.add(new JLabel(type.name() + " Layer:"));
            menuPanel.add(groupComboBox);
            menuPanel.add(dataTable.getToolbarCSV());
            add("North", menuPanel);
            add("Center", scrollPane);

            groupComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    updateTrainerLayer();
                }
            });

            // Add listener to data table
            dataTable.getData().addListener(new SimbrainTableListener() {

                /**
                 * {@inheritDoc}
                 */
                public void columnAdded(int column) {
                    updateTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void columnRemoved(int column) {
                    updateTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void rowAdded(int row) {
                    updateTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void rowRemoved(int row) {
                    updateTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void cellDataChanged(int row, int column) {
                    updateTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void tableStructureChanged() {
                    updateTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void tableDataChanged() {
                    updateTrainerData();
                }

            });

        }

        /**
         * Set the currently selected item in the combo box.
         *
         * @param layer the item to select
         */
        public void setLayer(List<Neuron> layer) {
            // TODO: Check whether the box contains this item
            groupComboBox.setSelectedItem(layer);
        }

        /**
         * Get the current neuron group associated with the layer selection
         * combo box.
         *
         * @return the selected group
         */
        private NeuronGroup getCurrentNeuronGroup() {
            if (groupComboBox.getSelectedItem() instanceof NeuronGroup) {
                return (NeuronGroup) groupComboBox.getSelectedItem();
            } else {
                return null;
            }
        }

        /**
         * Updates the parent trainer's input or training data, when the table
         * data change.
         */
        private void updateTrainerData() {
            if (trainer != null) {
                if (getCurrentNeuronGroup() != null) {
                    reconcileTableWithLayer(dataTable, getCurrentNeuronGroup());
                }
                if (type == WindowType.Input) {
                    trainer.setInputData(dataTable.getData().asArray());
                } else if (type == WindowType.Trainer) {
                    trainer.setTrainingData(dataTable.getData().asArray());
                }
            }
        }

        /**
         * Updates the parent trainer's input or output layer, when the group
         * combo box changes.
         */
        private void updateTrainerLayer() {
            if (trainer != null) {
                NeuronGroup group = getCurrentNeuronGroup();
                reconcileTableWithLayer(dataTable, group);
                if (group != null) {
                    if (type == WindowType.Input) {
                        trainer.setInputLayer(group);
                    } else if (type == WindowType.Trainer) {
                        trainer.setOutputLayer(group);
                    }
                }
            }
        }

        /**
         * See if the current neuron group has any input / output layers in it
         * and if so make that the currently selected item. Otherwise just use
         * the first item.
         */
        public void initializeSelectedGroup() {

            // TODO: Should only be used when the groupComboBox is being
            // re-created

            NeuronGroup newGroup = getMatchingGroup();
            if (newGroup != null) {
                groupComboBox.setSelectedItem(newGroup);
            } else {
                groupComboBox
                        .setSelectedIndex(groupComboBox.getItemCount() - 1);
            }
            if (groupHasChanged()) {
                updateTrainerLayer();
            }
        }

        /**
         * Helper method which looks for input layers (for an input window) or
         * output layer (for a training window). Returns null if no match.
         *
         * @return the first matching layer, or null if none are found
         */
        private NeuronGroup getMatchingGroup() {
            for (int i = 0; i < groupComboBox.getItemCount(); i++) {
                Object object = groupComboBox.getItemAt(i);
                if (object instanceof NeuronLayer) {
                    NeuronLayer layer = (NeuronLayer) object;
                    if (type == WindowType.Input) {
                        if (layer.getType() == LayerType.Input) {
                            return layer;
                        }
                    } else if (type == WindowType.Trainer) {
                        if (layer.getType() == LayerType.Output) {
                            return layer;
                        }
                    }
                }
            }
            return null;
        }

        /**
         * Update the input layer and output layer combo boxes (when groups are
         * added, removed, or changed in the current network).
         */
        private void repopulateGroupComboBox() {
            groupComboBox.removeAllItems();
            if (trainer.getNetwork() != null) {
                for (Group group : trainer.getNetwork().getGroupList()) {
                    if (group instanceof NeuronGroup) {
                        groupComboBox.addItem(group);
                    }
                }
            }
            initializeSelectedGroup();
            if (groupHasChanged()) {
                updateTrainerLayer();
            }
        }

        /**
         * Compares the current object in the group selection box, with the
         * corresponding object in the trainer. Returns true if they don't
         * match.
         *
         * @return true if the group has changed, false otherwise
         */
        private boolean groupHasChanged() {
            NeuronGroup group = getCurrentNeuronGroup();
            boolean groupHasChanged;
            if (type == WindowType.Input) {
                groupHasChanged = (group != trainer.getInputLayer());
            } else {
                groupHasChanged = (group != trainer.getOutputLayer());
            }
            return groupHasChanged;
        }

        /**
         * Return the underlying SimbrainJTable.
         *
         * @return the table
         */
        SimbrainJTable getDataTable() {
            return dataTable;
        }

        /**
         * Returns an array representation of the data.
         *
         * @return the table data
         */
        double[][] getData() {
            return dataTable.getData().asArray();
        }

        /**
         * Set underlying model data.
         *
         * @param data
         */
        public void setData(double[][] data) {
            dataTable.getData().setData(data);
        }

    }

    /**
     * Test GUI.
     *
     * @param args
     */
    public static void main(String[] args) {
        // TODO: The test case below is sensitive to the order of things, so
        // that listeners are initialized properly. It should not be so brittle.
        RootNetwork network = new RootNetwork();
        BackpropTrainer trainer = new BackpropTrainer();
        TrainerPanel trainerPanel = new TrainerPanel(trainer);
        trainer.setNetwork(network);
        LayeredNetworkBuilder builder = new LayeredNetworkBuilder();
        int[] nodesPerLayer = new int[] { 2, 4, 4, 1 };
        builder.setNodesPerLayer(nodesPerLayer);
        builder.buildNetwork(network);
        JFrame topFrame = new JFrame();
        topFrame.setContentPane(trainerPanel);
        topFrame.pack();
        topFrame.setVisible(true);
    }

}
