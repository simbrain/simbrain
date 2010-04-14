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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.SFileChooser;
import org.simbrain.util.Utils;
import org.simbrain.workspace.Workspace;

/**
 * GUI for trainer component.
 *
 * @author ericneilj
 */
public class TrainerGUI extends JFrame {

	private String[] inputLayer = {"Group 1", "Group 2"};
	private String[] outputLayer = {"Group 1", "Group 2"};
	private String[] columnNames = {"#","N1","N2","N3","N4"};
	private String[] network = {"Network 1", "Network 2"};
	private String[] trainingAlgorithms = {"Backprop  ", "Other"};
	private String[] errorSignal = {"SSE           ", "Other"};
	
	/** Reference to trainer object. */
	private Trainer trainer;
	
	/** Reference to workspace object. */
	private Workspace workspace; 
	
	
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
	public TrainerGUI() {

		setTitle("Backprop Trainer Prototype");

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
		JComboBox netSelectCombo = new JComboBox(network);
		netSelect.add(netSelectCombo);
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

		// Set up Plot with (currently) test data
		XYSeriesCollection series = new XYSeriesCollection();
		XYSeries series1 = new XYSeries(1);
		series.addSeries(series1);		
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
		JPanel runButtons = new JPanel();
        runButtons.add(new JButton("Run"));
        runButtons.add(new JButton("Stop"));
        runButtons.add(new JButton("Step"));
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
		
	      // Left Table
        final JTable  leftTable = new JTable(data, columnNames);
        leftTable.setGridColor(Color.LIGHT_GRAY);
        TableColumn inColumn = null;
        inColumn = leftTable.getColumnModel().getColumn(0);
        inColumn.setPreferredWidth(18);
        JScrollPane leftScroll = new JScrollPane(leftTable);
        leftScroll.setPreferredSize(new Dimension(400, 200));
        
		// Left top panel
		JPanel leftMenuPanel = new JPanel();
		leftMenuPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		JLabel inputLabel = new JLabel("Input Layer:");
		leftMenuPanel.add(inputLabel);
		JComboBox inputLayerCombo = new JComboBox(inputLayer);
		inputLayerCombo.setSelectedIndex(0);
		leftMenuPanel.add(inputLayerCombo);
		JButton leftSave = new JButton("Save");
		leftMenuPanel.add(leftSave);
		JButton leftImport = new JButton("Import");
		leftMenuPanel.add(leftImport);
		leftImport.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                SFileChooser chooser = new SFileChooser(".",
                        "Comma Separated Values", "csv");
                    File theFile = chooser.showOpenDialog();

                    if (theFile == null) {
                        return;
                    }                    
                    DefaultTableModel model  = new DefaultTableModel();
                    double[][]  doubleVals = Utils.getDoubleMatrix(theFile);
                    model.setNumRows(doubleVals.length);
                    model.setColumnCount(doubleVals[0].length);                    
                    for (int i = 0; i < doubleVals.length; i++) {
                        for (int j = 0; j < doubleVals[i].length; j++) {                            
                            model.setValueAt(doubleVals[i][j],i,j);
                        }
                    }
                    leftTable.setModel(model);                
            }
		    
		});
		
        leftPanel.add("North", leftMenuPanel);
        leftPanel.add("Center", leftScroll);

        
        // Right Table
        final JTable  rightTable = new JTable(data, columnNames);
        rightTable.setGridColor(Color.LIGHT_GRAY);
        TableColumn outColumn = null;
        outColumn = rightTable.getColumnModel().getColumn(0);
        outColumn.setPreferredWidth(18);
        JScrollPane rightScroll = new JScrollPane(rightTable);
        rightScroll.setPreferredSize(new Dimension(400, 200));
  	
		// Right top panel
		JPanel rightMenuPanel = new JPanel();
		rightMenuPanel.setLayout(new FlowLayout(FlowLayout.LEADING));
		JLabel outputLabel = new JLabel("Output Layer:");
		rightMenuPanel.add(outputLabel);
		JComboBox outputLayerCombo = new JComboBox(outputLayer);
		outputLayerCombo.setSelectedIndex(1);
		rightMenuPanel.add(outputLayerCombo);
		JButton rightSave = new JButton("Save");
		rightMenuPanel.add(rightSave);		
		JButton rightImport = new JButton("Import");
		rightMenuPanel.add(rightImport);
		rightImport.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent arg0) {
	                SFileChooser chooser = new SFileChooser(".",
	                        "Comma Separated Values", "csv");
	                    File theFile = chooser.showOpenDialog();

	                    if (theFile == null) {
	                        return;
	                    }
	                    DefaultTableModel model  = new DefaultTableModel();
	                    double[][]  doubleVals = Utils.getDoubleMatrix(theFile);
	                    model.setNumRows(doubleVals.length);
	                    model.setColumnCount(doubleVals[0].length);
	                    for (int i = 0; i < doubleVals.length; i++) {
	                        for (int j = 0; j < doubleVals[i].length; j++) {
	                            model.setValueAt(doubleVals[i][j],i,j);
	                        }
	                    }
	                    rightTable.setModel(model);    
	            }
	            
	        });
		
		
		rightPanel.add("North", rightMenuPanel);
		rightPanel.add("Center", rightScroll);
		
		//Bottom Panel Placed in Main Panel
		JPanel bottomPanel = new JPanel();
		bottomPanel.add(new JButton("Cancel"));
		bottomPanel.add(new JButton("Ok"));
		
		// Put it all together
		mainPanel.add("North", topPanel);
		mainPanel.add("Center", splitPane);
		mainPanel.add("South", bottomPanel);
		add(mainPanel);

		pack();

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);
		setVisible(true);
	}

	/**
	 * Test GUI.
	 * @param args
	 */
	public static void main(String[] args) {
		new TrainerGUI();
	}

}
