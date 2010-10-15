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
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.builders.LayeredNetworkBuilder;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.NeuronLayer;
import org.simbrain.network.groups.NeuronLayer.LayerType;
import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.Utils;
import org.simbrain.util.table.SimbrainDataTable;
import org.simbrain.util.table.SimbrainJTable;
import org.simbrain.util.table.SimbrainTableListener;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;
import org.simbrain.workspace.gui.GenericFrame;
import org.simbrain.workspace.gui.GenericJFrame;

/**
 * GUI for supervised learning in Simbrain, using back-propagation, LMS, and
 * (eventually) other algorithms. A front end for the trainer class.
 *
 * @author ericneilj
 * @author jeff yoshimi
 * @see org.simbrain.trainer.Trainer
 */
public class TrainerGUI extends JPanel {

    /** Parent frame. */
    GenericFrame parentFrame;

    /** Choices of training algorithms. */
    private String[] trainingAlgorithms = { "Backprop  ", "Least Mean Squares" };

    /** Network selection combo box. */
    private JComboBox cbNetworkChooser = new JComboBox();

    /** Input data window. */
    private TrainerDataWindow inputDataWindow;

    /** Training data window. */
    private TrainerDataWindow trainingDataWindow;

    /** Reference to trainer object. */
    private Trainer trainer;

    /** Reference to workspace object. */
    private Workspace workspace;

    /** Current network. */
    private RootNetwork currentNetwork;

    /** Data for the error graph. */
    private XYSeries graphData;

    /** Text field for setting number of iterations to run. */
    private JTextField tfIterations;

    /** Error label. */
    private JLabel rmsError = new JLabel("Error: --- ");

    /** Update completed boolean value. */
    private boolean updateCompleted = true;

    /**
     * Type of the data window: an input window (showing input data and layer)
     * or a training window (showing training data and an output layer).
     */
    private enum WindowType {Input, Trainer};

    /**
     * Default constructor.
     */
    public TrainerGUI(Workspace workspace, GenericFrame frame) {

        // Initial setup
        this.workspace = workspace;
        workspace.addListener(workspaceListener);
        this.parentFrame = frame;

        // Initialize combo box action listeners
        cbNetworkChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                // If there are no networks in the workspace, return.
                Object object = cbNetworkChooser.getSelectedItem();
                if (object instanceof NetworkComponent) {
                    setNetwork(((NetworkComponent) object).getRootNetwork());
                }
            }
        });

        // Main Panel
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        // Top Panel
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());
        // topPanel.setPreferredSize(new Dimension(800, 200));

        // Top items
        JPanel topItems = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topItems.setBorder(BorderFactory.createEtchedBorder());
        LabelledItemPanel netSelect = new LabelledItemPanel();
        netSelect.setLayout(new FlowLayout(FlowLayout.LEFT));
        // netSelect.setPreferredSize(new Dimension(140, 220));
        JLabel netSelectLabel = new JLabel("Select Root Network");
        topItems.add(netSelectLabel);
        topItems.add(cbNetworkChooser);

        topItems.add(new JLabel("Training Algorithm"));
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
        splitPane.setResizeWeight(.5); // keeps divider centered on resize

        // Data windows
        inputDataWindow = new TrainerDataWindow(WindowType.Input);
        trainingDataWindow = new TrainerDataWindow(WindowType.Trainer);
        splitPane.setLeftComponent(inputDataWindow);
        splitPane.setRightComponent(trainingDataWindow);

        // Bottom Button Panel
        JPanel bottomPanel = new JPanel();
        bottomPanel.add(new JButton("Cancel"));
        bottomPanel.add(new JButton("Ok"));

        // Put it all together
        mainPanel.add("North", topPanel);
        mainPanel.add("Center", splitPane);
        add(mainPanel);

        // Initialize selection box
        resetNetworkSelectionBox();

        // Initialize menus
        createMenus();

    }

    /**
     * Create the graph panel.
     *
     * @return the graph panel
     */
    private JPanel createGraphPanel() {

        // Graph
        JPanel graphPanel = new JPanel();
        graphPanel.setBorder(BorderFactory
                .createTitledBorder("Error / Trainer Controls"));
        graphPanel.setLayout(new BorderLayout());
        XYSeriesCollection series = new XYSeriesCollection();
        graphData = new XYSeries(1);
        series.addSeries(graphData);
        JFreeChart chart = ChartFactory.createXYLineChart(null, // Title
                "Iterations", // x-axis Label
                "Error", // y-axis Label
                series, // Dataset
                PlotOrientation.VERTICAL, // Plot Orientation
                false, // Show Legend
                true, // Use tooltips
                false // Configure chart to generate URLs?
                );
        chart.setBackgroundPaint(null);
        // chart.getXYPlot().getRangeAxis().setUpperBound(1); TODO: Make
        // autorange a dialog option
        ChartPanel centerPanel = new ChartPanel(chart);
        centerPanel.setPreferredSize(new Dimension(centerPanel
                .getPreferredSize().width, 200));

        // Make button panel
        JPanel buttonPanel = new JPanel();

        // Run
        buttonPanel.add(new JButton(TrainerGuiActions.getRunAction(this)));

        // Batch
        buttonPanel.add(new JButton(TrainerGuiActions.getBatchTrainAction(this)));

        // Iterations
        tfIterations = new JTextField("300");
        buttonPanel.add(new JLabel("Iterations"));
        buttonPanel.add(tfIterations);

        // Error
        buttonPanel.add(rmsError);

        // Randomize
        buttonPanel.add(new JButton(TrainerGuiActions.getRandomizeNetworkAction(this)));

        // Clear
        buttonPanel.add(new JButton(TrainerGuiActions.getClearGraphAction(this)));

        // Finish up panel
        graphPanel.add("Center", centerPanel);
        graphPanel.add("South", buttonPanel);
        return graphPanel;
    }

    /**
     * Create menus.
     */
    private void createMenus() {
        JMenuBar menuBar = new JMenuBar();

        // File Menu
        JMenu fileMenu = new JMenu("File");
        fileMenu.add(new JMenuItem("Open")); // TODO
        fileMenu.add(new JMenuItem("Save")); // TODO
        fileMenu.add(new JMenuItem("Save as...")); // TODO
        menuBar.add(fileMenu);

        // File Menu
        JMenu buildMenu = new JMenu("Build");
        JMenuItem threeLayerItem = new JMenuItem(
                TrainerGuiActions.getBuildThreeLayerAction(this));
        buildMenu.add(threeLayerItem);
        JMenuItem multiLayerItem = new JMenuItem(
                TrainerGuiActions.getBuildMultiLayerAction(this));
        buildMenu.add(multiLayerItem);
        menuBar.add(buildMenu);

        parentFrame.setJMenuBar(menuBar);

    }

    /**
     * Associate this trainer gui with a specific network.
     *
     * @param network the network to associated the network with.
     */
    public final void setNetwork(final RootNetwork network) {
        currentNetwork = network;
    }

    /**
     * Perform initialization required when the current network is changed.
     */
    private void initializeCurrentNetwork() {

        if (currentNetwork == null) {
            return;
        }

        // Create a new trainer if needed; else re-initialize the current
        // trainer
        if (trainer == null) {
            trainer = new BackpropTrainer(currentNetwork);
            trainer.addListener(new TrainerListener() {
                public void errorUpdated(final double error) {
                    graphData.add(trainer.getIteration(), error);
                }
            });
        } else {
            trainer.setNetwork(currentNetwork);
            inputDataWindow.updateParentTrainerGroups();
            trainingDataWindow.updateParentTrainerGroups();
        }

        // Update the input and output tables in the GUI
        updateGroupComboBoxes();
        inputDataWindow.initializeSelectedGroup();
        trainingDataWindow.initializeSelectedGroup();

        // Initialize trainer, but only after layers are updated
        trainer.init();

        // TODO: When the currentnetwork is changed, this listener should be removed.
        // E.g.: previousNetwork.removeGroupListener(previousListener)

        // If groups are added or removed in the current network, this must be
        // reflected in the Input and output layer combo boxes.
        currentNetwork.addGroupListener(new GroupListener() {

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
                updateGroupComboBoxes();
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
        });

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
     * Reset the network selection combo box.
     */
    private void resetNetworkSelectionBox() {

        cbNetworkChooser.removeAllItems();
        for (WorkspaceComponent component : workspace
                .getComponentList(NetworkComponent.class)) {
            cbNetworkChooser.addItem(component);
        }
        // TODO: This does not seem to work. Test: Set box to network 3, add a
        // network, it resets to 1
        if (currentNetwork != null) {
            //System.out.println("current net: " + currentNetwork);
            cbNetworkChooser.setSelectedItem(currentNetwork);
        } else {
            //System.out.println("current net is null");
            if (cbNetworkChooser.getItemCount() >= 1) {
                cbNetworkChooser.setSelectedIndex(1);
            }
        }

        initializeCurrentNetwork();
    }

    /**
     * Listen to the workspace. When components are added update the network
     * selection combo box.
     */
    private WorkspaceListener workspaceListener = new WorkspaceListener() {

        /**
         * Clear the Simbrain desktop.
         */
        public void workspaceCleared() {
            resetNetworkSelectionBox();
        }

        @SuppressWarnings("unchecked")
        public void componentAdded(final WorkspaceComponent workspaceComponent) {
            resetNetworkSelectionBox();
        }

        @SuppressWarnings("unchecked")
        public void componentRemoved(final WorkspaceComponent workspaceComponent) {
            if (workspaceComponent instanceof NetworkComponent) {
                if (((NetworkComponent) workspaceComponent).getRootNetwork() == currentNetwork) {
                    currentNetwork = null;
                }
            }
            resetNetworkSelectionBox();
        }
    };

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
    public final void iterate() {
        trainer.train(1);
        updateErrorField();
        graphData.add(trainer.getIteration(), trainer.getCurrentError());
    }

    /**
     * @return boolean updated completed.
     */
    public final boolean isUpdateCompleted() {
        return updateCompleted;
    }

    /**
     * Sets updated completed value.
     *
     * @param updateCompleted
     *            Updated completed value to be set
     */
    public final void setUpdateCompleted(final boolean updateCompleted) {
        this.updateCompleted = updateCompleted;
    }

    /**
     * Clear the graph data and reset trainer iteration.
     */
    public final void clearGraph() {
        graphData.clear();
        if (trainer != null) {
            trainer.setIteration(0);
        }
    }

    /**
     * @return the currentNetwork
     */
    public final RootNetwork getCurrentNetwork() {
        return currentNetwork;
    }

    /**
     * @return the trainer
     */
    public final Trainer getTrainer() {
        return trainer;
    }

    /**
     * @return the workspace
     */
    public final Workspace getWorkspace() {
        return workspace;
    }

    /**
     * Get the SimbrainJTable associated with the input data for this trainer.
     *
     * @return the input data table
     */
    public final SimbrainJTable getInputData() {
        return inputDataWindow.getDataTable();
    }

    /**
     * Get the SimbrainJTable associated with the training data for this
     * trainer.
     *
     * @return the training data table
     */
    public final SimbrainJTable getTrainingData() {
        return trainingDataWindow.getDataTable();
    }

    /**
     * Check to see where the provided table has as many columns as there are
     * neurons in provided neuron layer. If not, modify the table to match the
     * neuron layer.
     *
     * @param dataTable the table to check
     * @param group the neuron layer to check
     */
    private void reconcileTableWithLayer(final SimbrainJTable dataTable, final NeuronGroup group) {

        // Modify the table so that it has exactly as many columns as there
        // are neurons in this group.
        int groupSize = group.getNeuronList().size();
        int tableSize = dataTable.getColumnCount() - 1; // First column is "#" sign
        if (groupSize != tableSize) {
            //TODO: Once this is cleared up, fold  sysout in to the option pane message
            System.out.println("Layer neurons:" + groupSize + " Table Columns: " + tableSize);
//            JOptionPane.showMessageDialog((Component) parentFrame,
//                    "The table and current layer are incompatible and so the data in this table " +
//                    "is being modified to match the layer.",
//                    "Warning",
//                    JOptionPane.WARNING_MESSAGE);
            dataTable.getData().modifyRowsColumns(
                    dataTable.getData().getRowCount(), groupSize, 0);
        }

        // Rename column headings
        // Note the for loop starts at column 1 (column 0 shows the row
        // number)
        Iterator<Neuron> neuronIterator = group.getNeuronList().iterator();
        for (int i = 1; i < dataTable.getColumnCount(); i++) {
            if (neuronIterator.hasNext()) {
                dataTable
                        .getColumnModel()
                        .getColumn(i)
                        .setHeaderValue(neuronIterator.next().getDescription());
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

        /** Left scroll pane. */
        private JScrollPane scrollPane;

        /** Trainer vs. Input window. */
        private WindowType type;

        /**
         * Constructor for Trainer Data Window.
         *
         * @param name name of window
         */
        TrainerDataWindow(final WindowType type) {
            this.type = type;
            setLayout(new BorderLayout());
            dataTable = new SimbrainJTable(new SimbrainDataTable(5, 4));
            scrollPane = new JScrollPane(dataTable);
            scrollPane.setPreferredSize(new Dimension(100, 100));
            setBorder(BorderFactory.createTitledBorder("" + type.name() + " Table"));
            JPanel menuPanel = new JPanel();
            menuPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
            menuPanel.add(new JLabel(type.name() + " Layer:"));
            menuPanel.add(groupComboBox);
            menuPanel.add(dataTable.getToolbarCSV());
            add("North", menuPanel);
            add("Center", scrollPane);

            groupComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent arg0) {
                    updateParentTrainerGroups();
                }
            });

            dataTable.getData().addListener(new SimbrainTableListener() {

                /**
                 * {@inheritDoc}
                 */
                public void columnAdded(int column) {
                    updateParentTrainerData();
                    // TODO: Possibly call reconcileTableWithLayer
                }

                /**
                 * {@inheritDoc}
                 */
                public void columnRemoved(int column) {
                    updateParentTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void rowAdded(int row) {
                    updateParentTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void rowRemoved(int row) {
                    updateParentTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void itemChanged(int row, int column) {
                    updateParentTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void structureChanged() {
                    updateParentTrainerData();
                }

                /**
                 * {@inheritDoc}
                 */
                public void dataChanged() {
                    updateParentTrainerData();
                }

            });

        }

        /**
         * Get the current neuron group associated with layer selection combo box.
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
         * Update the datatable in the trainer.
         */
        private void updateParentTrainerData() {
            if (trainer != null) {
                if (type == WindowType.Input) {
                    trainer.setInputData(dataTable.getData().asArray());
                } else if (type == WindowType.Trainer) {
                    trainer.setTrainingData(dataTable.getData().asArray());
                }
            }
        }

        /**
         * Helper method which checks all the items in the groupComboBox, and
         * returns the first NeuronLayer whose layer type is appropriate to this
         * window (input for input, output for training). Returns null if no
         * match.
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
         * See if the current neuron group has any input / output layers in it
         * and if so make that the currently selected item. Otherwise just use
         * the first item. Only use when the groupComboBox is being re-created.
         */
        public void initializeSelectedGroup() {
            NeuronGroup newGroup = getMatchingGroup();
            if (newGroup != null) {
                groupComboBox.setSelectedItem(newGroup);
            } else {
                groupComboBox.setSelectedIndex(groupComboBox.getItemCount() - 1);
            }
            if (groupHasChanged()) {
                updateParentTrainerGroups();
            }
        }

        /**
         * Update the input layer and output layer combo boxes (when groups are
         * added, removed, or changed in the current network).
         */
        private void repopulateGroupComboBox() {
            if (currentNetwork != null) {
                groupComboBox.removeAllItems();
                for (Group group : currentNetwork.getGroupList()) {
                    if (group instanceof NeuronGroup) {
                        groupComboBox.addItem(group);
                    }
                }
            }
            if (groupHasChanged()) {
                updateParentTrainerGroups();
            }
        }

        /**
         * Compares the current object in the group selection box, with the
         * corresponding object in the trainer. Returns trues if they don't
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
         * This is called when the neuron group associated with this data window
         * may have changed (e.g. when the combo box is used).
         *
         * If it has changed, the table be updated appropriately.The tricky
         * thing is that the group might have more or less neurons than there
         * are columns than are in the table
         */
        private void updateParentTrainerGroups() {

            if (trainer == null) {
                return;
            }
            NeuronGroup group = getCurrentNeuronGroup();

            if (group == null) {
                return;
            }

            reconcileTableWithLayer(dataTable, group);
            if (type == WindowType.Input) {
                trainer.setInputLayer(group);
            } else if (type == WindowType.Trainer) {
                trainer.setOutputLayer(group);
            }

        }

        /**
         * Return the underlying SimbrainJTable.
         *
         * @return the table
         */
        public SimbrainJTable getDataTable() {
            return dataTable;
        }

        /**
         * Returns an array representation of the data.
         *
         * @return
         */
        public double[][] getData() {
            return dataTable.getData().asArray();
        }

    }

    /**
     * Test GUI.
     *
     * @param args
     */
    public static void main(String[] args) {
        Workspace workspace = new Workspace();

        // Make network 1
        RootNetwork network = new RootNetwork();
        LayeredNetworkBuilder builder = new LayeredNetworkBuilder();
        int[] nodesPerLayer = new int[] { 2, 4, 4, 1 };
        builder.setNodesPerLayer(nodesPerLayer);
        builder.buildNetwork(network);
        NetworkComponent networkComponent = new NetworkComponent("Net 1",
                network);
        workspace.addWorkspaceComponent(networkComponent);

        // Make network 2
        RootNetwork network2 = new RootNetwork();
        LayeredNetworkBuilder builder2 = new LayeredNetworkBuilder();
        int[] nodesPerLayer2 = new int[] { 6, 4, 8 };
        builder2.setNodesPerLayer(nodesPerLayer2);
        builder2.buildNetwork(network2);
        NetworkComponent networkComponent2 = new NetworkComponent("Net 2",
                network2);
        workspace.addWorkspaceComponent(networkComponent2);

        GenericJFrame topFrame = new GenericJFrame();
        TrainerGUI trainer = new TrainerGUI(workspace, topFrame);
        topFrame.setContentPane(trainer);
        topFrame.pack();
        topFrame.setVisible(true);
    }

}
