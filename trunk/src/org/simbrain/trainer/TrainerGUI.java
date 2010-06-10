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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.builders.LayeredNetworkBuilder;
import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.interfaces.Group;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.listeners.GroupListener;
import org.simbrain.network.listeners.NetworkEvent;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.workspace.WorkspaceListener;

/**
 * GUI for supervised learning in Simbrain, using back-propagation, LMS, and
 * (eventually) other algorithms. A front end for the trainer class.
 * 
 * @author ericneilj
 * @author jeff yoshimi
 * @see org.simbrain.trainer.Trainer
 */
public class TrainerGUI extends JPanel {

	private String[] columnNames = {"#","N1","N2","N3","N4"};
	private String[] trainingAlgorithms = {"Backprop  ", "Other"};
	private String[] errorSignal = {"SSE           ", "Other"};
		
	/** Network selection combo box. */
	private JComboBox cbNetworkChooser = new JComboBox();
	
	/** Input layer combo box. */
	private JComboBox cbInputLayer = new JComboBox();

	/** Table displaying input data. */
	private JTable inputDataTable;
		
	/** Output layer combo box. */
    JComboBox cbOutputLayer = new JComboBox();
    
    /** Table displaying training data. */
    private JTable trainingDataTable;

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

	// Sample data to start with
	Object [][] data = {
			{"1",null, null,null,null},
			{"2",null, null,null,null},
			{"3",null, null,null,null},
			{"4",null, null,null,null},
			{"5",null, null,null,null},
			{"6",null, null,null,null},
			{"7",null, null,null,null},
			{"8",null, null,null,null},
			{"9",null, null,null,null},
			{"10",null, null,null,null},
			{"11",null, null,null,null},
			{"12",null, null,null,null},
			{"13",null, null,null,null},
			{"14",null, null,null,null},
			{"15",null, null,null,null},
	};	
	
	/**
	 * Default constructor.
	 */
	public TrainerGUI(Workspace workspace) {

	    // Initial setup
		this.workspace = workspace;
		workspace.addListener(workspaceListener);
				
		// Initialize combo box action listeners
		cbNetworkChooser.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                updateCurrentNetwork();
                updateLayerBoxes();
            }
		});
        cbInputLayer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                updateInputTable();
            }
        });		
        cbOutputLayer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                updateOutputTable();
            }
        });

		//Main Panel
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BorderLayout());
		//mainPanel.setBorder(BorderFactory.createTitledBorder("Backprop properties"));
		
		//Top Panel
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		//topPanel.setPreferredSize(new Dimension(800, 200));	
		
		//Network and Algorithm Selector
		LabelledItemPanel netSelect = new LabelledItemPanel();
		netSelect.setBorder(BorderFactory.createTitledBorder("Training properties"));
		netSelect.setLayout(new FlowLayout(FlowLayout.LEFT));
		netSelect.setPreferredSize(new Dimension(140, 220));
		JLabel netSelectLabel = new JLabel("Network");
		netSelect.add(netSelectLabel);        
		netSelect.add(cbNetworkChooser);
		JLabel algoSelectLabel = new JLabel("Training Algorithm");
		netSelect.add(algoSelectLabel);
		JComboBox algoSelectCombo = new JComboBox(trainingAlgorithms);
		netSelect.add(algoSelectCombo);
		JButton properties = new JButton("Properties");
		netSelect.add(properties);
		JLabel error = new JLabel("Error    ");
		netSelect.add(error);
		JComboBox errorCombo = new JComboBox(errorSignal);
		netSelect.add(errorCombo);
		topPanel.add("West", netSelect);
		
		//Graph
		JPanel trainerDisplay = new JPanel();
		trainerDisplay.setLayout(new BorderLayout());
		XYSeriesCollection series = new XYSeriesCollection();
		graphData = new XYSeries(1);
		series.addSeries(graphData);		
		JFreeChart chart = ChartFactory.createXYLineChart(
	            "Error", // Title
	            "Iterations", // x-axis Label
	            "Error", // y-axis Label
	            series, // Dataset
	            PlotOrientation.VERTICAL, // Plot Orientation
	            false, // Show Legend
	            true, // Use tooltips
	            false // Configure chart to generate URLs?
	        );		
		ChartPanel chartPanel = new ChartPanel(chart);
		chartPanel.setPreferredSize(new Dimension(800,200));
		JPanel runButtons = new JPanel();
        JButton runButton = new JButton("Run");
        runButtons.add(runButton);
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (trainer != null) {
                    trainer.train(Integer.parseInt(tfIterations.getText()));
                }
            }
        });
        JButton initButton = new JButton("Init");
        runButtons.add(initButton);
        initButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                currentNetwork.randomizeBiases(-1, 1);
                trainer.init();
            }
        });
        
        tfIterations = new JTextField("300");
        runButtons.add(tfIterations);
        runButtons.add(new JButton("Clear"));
        trainerDisplay.add("Center",chartPanel);
        trainerDisplay.add("South",runButtons);
    	topPanel.add("East", trainerDisplay);
				
		// Split Pane (Main Center Panel)
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		splitPane.setBorder(null);
		splitPane.setResizeWeight(.5); //keeps divider centered on resize
		JPanel leftPanel = new JPanel();
		leftPanel.setBorder(BorderFactory.createTitledBorder("Input data"));
		JPanel rightPanel = new JPanel();
		rightPanel.setBorder(BorderFactory.createTitledBorder("Training data"));
		leftPanel.setLayout(new BorderLayout());
		rightPanel.setLayout(new BorderLayout());
		splitPane.setLeftComponent(leftPanel);
		splitPane.setRightComponent(rightPanel);		
		
	    // Input Data 
		DefaultTableModel inputDataModel = new DefaultTableModel(data, columnNames);
        inputDataTable = new JTable(inputDataModel);
        inputDataTable.setGridColor(Color.LIGHT_GRAY);
        TableColumn inColumn = null;
        inColumn = inputDataTable.getColumnModel().getColumn(0);
        inColumn.setPreferredWidth(18);
        JScrollPane leftScroll = new JScrollPane(inputDataTable);
        leftScroll.setPreferredSize(new Dimension(400, 200));
        JPanel leftMenuPanel = new JPanel();
		leftMenuPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		JLabel inputLabel = new JLabel("Input Layer:");
		leftMenuPanel.add(inputLabel);
		leftMenuPanel.add(cbInputLayer);
		JButton leftSave = new JButton("Save");
		leftMenuPanel.add(leftSave);
		JButton leftImport = new JButton("Import");
		leftMenuPanel.add(leftImport);
		leftImport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                loadData(inputDataTable);
            }
		});		
        leftPanel.add("North", leftMenuPanel);
        leftPanel.add("Center", leftScroll);
        
        // Training Data
        DefaultTableModel trainingDataModel = new DefaultTableModel(data, columnNames);
        trainingDataTable = new JTable(trainingDataModel);
        trainingDataTable.setGridColor(Color.LIGHT_GRAY);
        TableColumn outColumn = null;
        outColumn = trainingDataTable.getColumnModel().getColumn(0);
        outColumn.setPreferredWidth(18);
        JScrollPane rightScroll = new JScrollPane(trainingDataTable);
        rightScroll.setPreferredSize(new Dimension(400, 200));
		JPanel rightMenuPanel = new JPanel();
		rightMenuPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		JLabel outputLabel = new JLabel("Output Layer:");
		rightMenuPanel.add(outputLabel);
		rightMenuPanel.add(cbOutputLayer);
		JButton rightSave = new JButton("Save");
		rightMenuPanel.add(rightSave);		
		JButton rightImport = new JButton("Import");
		rightMenuPanel.add(rightImport);
		rightImport.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
	                loadData(trainingDataTable);
	            }	            
	        });
		rightPanel.add("North", rightMenuPanel);
		rightPanel.add("Center", rightScroll);
		
		//Bottom Button Panel 
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(new JButton("Cancel"));
		bottomPanel.add(new JButton("Ok"));
		
		// Put it all together
		mainPanel.add("North", topPanel);
		mainPanel.add("Center", splitPane);
		mainPanel.add("South", bottomPanel);
		add(mainPanel);		
        resetNetworkSelectionBox();
	}
	
	
	/**
	 * Load data into a JTable.
	 *
	 * @param table table to laod data in to
	 */
	private void loadData(JTable table) {

        SFileChooser chooser = new SFileChooser(".", "Comma Separated Values",
                "csv");
        File theFile = chooser.showOpenDialog();
        if (theFile == null) {
            return;
        }

        // Load data in to trainer
        if (trainer != null) {
            if (table == inputDataTable) {
                trainer.setInputData(theFile);
            } else if (table == trainingDataTable) {
                trainer.setTrainingData(theFile);
            }
        }
        
        double[][] doubleVals = Utils.getDoubleMatrix(theFile);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setNumRows(doubleVals.length);
        model.setColumnCount(doubleVals[0].length + 1);
        for (int i = 0; i < doubleVals.length; i++) {
            for (int j = 0; j < doubleVals[i].length + 1; j++) {
                if (j == 0) {
                    model.setValueAt(i + 1, i, j); // Row number column
                } else {
                    model.setValueAt(doubleVals[i][j - 1], i, j);
                }
            }
        }
    }
	
    /**
     * User has changed the current network in the network selection combo box.
     * Make appropriate changes.
     */
	private void updateCurrentNetwork() {
	    
	    Object object = cbNetworkChooser.getSelectedItem(); 
	    if (object instanceof NetworkComponent) {
	        currentNetwork = ((NetworkComponent)object).getRootNetwork();
	    } else {
	        currentNetwork = null;	 
	        return;
	    }
        updateInputTable();
        updateOutputTable();

        // Initialize trainer
        if (trainer == null) {
            trainer = new BackpropTrainer(currentNetwork);          
            trainer.listeners.add(new TrainerListener() {

                public void errorUpdated(double error) {
                    graphData.add(trainer.getIteration() , error);
                }
                
            });
        } else {
            trainer.setNetwork(currentNetwork);
        }
        
	    //TODO: Remove old listener
	    //previousNetwork.removeGroupListener(previousListener)
	    currentNetwork.addGroupListener(new GroupListener() {

            public void groupAdded(NetworkEvent<Group> e) {
                updateLayerBoxes();            
             }

            public void groupChanged(NetworkEvent<Group> networkEvent) {
                updateLayerBoxes();
             }

            public void groupRemoved(NetworkEvent<Group> e) {
                updateLayerBoxes();
            }
	    });
	    
	}
	
	/**
	 *  Reset the network selection combo box
	 */
	private void resetNetworkSelectionBox() {
	    
	        cbNetworkChooser.removeAllItems();
	        for (WorkspaceComponent component : workspace
                    .getComponentList(NetworkComponent.class)) {
	            cbNetworkChooser.addItem(component);	            
	        }
	        // TODO: This does not seem to work.  Test: Set box to network 3, add a network, it resets to 1
	        if (currentNetwork != null) {
	            cbNetworkChooser.setSelectedItem(currentNetwork);	            
	        } else {
	            if (cbNetworkChooser.getItemCount() >= 1)  {
	                cbNetworkChooser.setSelectedIndex(1);
	            }
	        }
	        
	        updateInputTable();
	        updateOutputTable();
	}
	

    /**
     * Update input data table
     */
    private void updateInputTable() {
        NeuronGroup group = (NeuronGroup) cbInputLayer.getSelectedItem();
        if (group != null) {
            if (trainer != null) {
                trainer.setInputLayer(group);    
            }
            updateTable(group, inputDataTable);
        }
    }
    
    /**
     * Update training data table
     */
    private void updateOutputTable() {
        NeuronGroup group = (NeuronGroup) cbOutputLayer.getSelectedItem();
        if (group != null) {
            if (trainer != null) {
                trainer.setOutputLayer(group);
            }
            updateTable(group, trainingDataTable);
        }
    }
	
    /**
     * Update the input layer and output layer combo boxes (when groups are
     * added, removed, or changed in the current network).
     */
	private void updateLayerBoxes() {
	    
	    if (currentNetwork != null) {
	        cbInputLayer.removeAllItems();
	        for (Group group : currentNetwork.getGroupList()) {
	            if (group instanceof NeuronGroup) {
	                cbInputLayer.addItem(group);	                
	            }
	        }
	        cbOutputLayer.removeAllItems();
	        for (Group group : currentNetwork.getGroupList()) {
                if (group instanceof NeuronGroup) {
                    cbOutputLayer.addItem(group);
                }
	        }  
	        updateInputTable();
	        updateOutputTable();
	    }
	}
	
	/**
	 * Update table using the supplied neuron group.
	 */
	private void updateTable(Group group, JTable table) {
	          
	    int groupSize = group.getNeuronList().size();
	    int tableSize = table.getColumnCount();

	    // If there are more neurons than columns in the table, enlarge the table
	    if (groupSize > tableSize) {
	        ((DefaultTableModel)table.getModel()).setColumnCount(groupSize); 
	        ((DefaultTableModel)table.getModel()).fireTableStructureChanged();
	    }

	    // Rename column headings
	    // Note the for loop starts at column 1 (column 0 is the "#" value)
        // TODO: Get neurons in proper order
	    Iterator<Neuron> neuronIterator = group.getNeuronList().iterator();
        for (int i = 1; i < tableSize; i++) { 
            if (neuronIterator.hasNext()) {
                table.getColumnModel().getColumn(i).setHeaderValue(neuronIterator.next().getDescription());                
            } else {
                // Table columns not assigned to a neuron
                table.getColumnModel().getColumn(i).setHeaderValue("--");
            }
	    }
        table.getTableHeader().resizeAndRepaint();
        
	}

    /**
     * Listen to the workspace. When components are added update the network
     * selection combo box.
     */
    private final WorkspaceListener workspaceListener = new WorkspaceListener() {

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
                if (((NetworkComponent)workspaceComponent).getRootNetwork() == currentNetwork) {
                    currentNetwork = null;
                }
            }
            resetNetworkSelectionBox();
        }
    };
    
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
        int[] nodesPerLayer = new int[]{2,4,4,1};
        builder.setNodesPerLayer(nodesPerLayer);
        builder.buildNetwork(network);
        NetworkComponent networkComponent = new NetworkComponent("Net 1", network);
        workspace.addWorkspaceComponent(networkComponent);

        // Make network 2
        RootNetwork network2 = new RootNetwork();
        LayeredNetworkBuilder builder2 = new LayeredNetworkBuilder();
        int[] nodesPerLayer2 = new int[]{12,4, 8};
        builder2.setNodesPerLayer(nodesPerLayer2);
        builder2.buildNetwork(network2);
        NetworkComponent networkComponent2 = new NetworkComponent("Net 2", network2);
        workspace.addWorkspaceComponent(networkComponent2);

        
        JFrame topFrame = new JFrame();
		TrainerGUI trainer = new TrainerGUI(workspace);
		topFrame.setContentPane(trainer);
        topFrame.pack();
        topFrame.setVisible(true);
	}

}
