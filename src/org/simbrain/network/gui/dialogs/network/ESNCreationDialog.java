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
package org.simbrain.network.gui.dialogs.network;


import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import org.simbrain.network.connections.Sparse;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.actions.connection.ConnectionDialog;
import org.simbrain.network.gui.dialogs.connect.SparsePanel;
import org.simbrain.network.neuron_update_rules.LinearRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule;
import org.simbrain.network.neuron_update_rules.SigmoidalRule.SigmoidType;
import org.simbrain.network.subnetworks.EchoStateNetwork;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;
import org.simbrain.util.StopLight;

/**
 * Creates a GUI dialog for the creation of an arbitrary echo-state network.
 *
 * @author ztosi
 */
@SuppressWarnings("serial")
public class ESNCreationDialog extends StandardDialog implements PropertyChangeListener{

    /** Underlying network panel */
    private final NetworkPanel panel;

    /** Dialog panel */
    private LabelledItemPanel esnPanel = new LabelledItemPanel();

    /** Text field: reads in number of input units */
    private JTextField tfNumInputs = new JTextField();

    /** Text field: reads in number of reservoir units */
    private JTextField tfNumReservoir = new JTextField();

    /** Text field: reads in number of output units */
    private JTextField tfNumOutputs = new JTextField();

    /** Text field: reads in desired max eigenvalue */
    private JTextField maxEigenValue = new JTextField();

    /** 
     * Opens a dialog for setting the parameters of the recurrent connections
     * in the reservoir.
     */
    private JButton resSparsity = new JButton("Reservoir");

    /**
     * Opens a dialog for setting the parameters of the connections between
     * the input layer and the reservoir layer.
     */
    private JButton inResSparsity = new JButton("In to Res");

    /**
     * Opens a dialog to set the parameters of the connection between the
     * output layer and the reservoir layer if they exist.
     */
    private JButton backSparsity = new JButton("Out to Res");

    /**
     * A check-box which determines whether or not this ESN will have recurrent
     * output weights
     */
    private JCheckBox recurrentOutputWeights = new JCheckBox();

    /**
     * A check-box which determines whether or not this ESN will have weights
     * from the output layer to the reservoir.
     */
    private JCheckBox backWeights = new JCheckBox();

    /**
     * A check-box which destermines whether or not this ESN will have weights
     * directly from input to output
     */
    private JCheckBox directInOutWeights = new JCheckBox();

    /**
     * Maps string values to corresponding NeuronUpdateRules for the combo-boxes
     * governing desired Neuron type for a given layer
     */
    private HashMap<String, NeuronUpdateRule> boxMap = new HashMap<String, NeuronUpdateRule>();

    // Mapping of Strings to NeuronUpdateRules, currently only Logisitc, Tanh,
    // and Linear neurons are allowed.
    {
        boxMap.put("Linear", new LinearRule());
        SigmoidalRule sig0 = new SigmoidalRule();
        sig0.setType(SigmoidType.LOGISTIC);
        boxMap.put("Logistic", sig0);
        SigmoidalRule sig1 = new SigmoidalRule();
        sig1.setType(SigmoidType.TANH);
        boxMap.put("Tanh", sig1);
    }

    /** String values for combo-boxes (same as key values for boxMap) */
    private String[] nTypeOptions = { "Linear", "Tanh", "Logistic" };

    /** Combo-box governing desired neuron type of the reservoir */
    private JComboBox reservoirNeuronTypes = new JComboBox(nTypeOptions);

    /** Combo-box governing the desired neuron type of the output layer */
    private JComboBox outputNeuronTypes = new JComboBox(nTypeOptions);

    /** A sparse panel for setting the connections between input and 
     * reservoir. */
    private SparsePanel inToRes;
    
    /** A sparse panel for setting the recurrent reservoir connections. */
    private SparsePanel resRecurrent;
    
    /** A sparse panel for setting the connections between output and 
     * reservoir if they exist. */
    private SparsePanel outToRes;
    
    /**
     * A status light indicating whether or not the parameters of the
     * connections between input and reservoir have been properly set.
     */
    private StopLight inToResReady = new StopLight();
    
    /**
     * A status light indicating whether or not the parameters of the recurrent
     * reservoir connections have been properly set.
     */
    private StopLight resRecurrentReady = new StopLight();
    

    /** A status light indicating whether or not the parameters of the 
     * connections between the output and the reservoir have been properly set.
     * Is not visible if backWeights is not selected.
     */
    private StopLight outToResReady = new StopLight();
    
    /**
     * Creation dialog constructor.
     *
     * @param panel Underlying network panel
     */
    public ESNCreationDialog(final NetworkPanel panel) {
        
    	this.panel = panel;
        
        // Create panels to set individual properties of each user-defined
        // connection
        inToRes = new SparsePanel(new Sparse(), panel);
        resRecurrent = new SparsePanel(new Sparse(), panel);
        outToRes = new SparsePanel(new Sparse(), panel);
             
        // For customized values
        GridBagConstraints gbc = new GridBagConstraints();

        setTitle("Build Echo-State Network ");
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        esnPanel.setMyNextItemRow(1);
        gbc.gridy = esnPanel.getMyNextItemRow();
        // Align to upper left
        gbc.anchor = GridBagConstraints.NORTHWEST;

        // Create section for network parameters
        sectionSeparator("Network Parameters", gbc, 1);

        // Add text-fields
        esnPanel.addItem("Input Nodes:", tfNumInputs);
        esnPanel.addItem(new JLabel("Reservoir Neuron Type:"),
        		reservoirNeuronTypes, 2);
        esnPanel.addItem("Reservoir Nodes:", tfNumReservoir);
        esnPanel.addItem(new JLabel("Output Neuron Type:"),
        		outputNeuronTypes, 2);
        esnPanel.addItem("Output Nodes:", tfNumOutputs);

        // GridBagConstraints for next section
        int row = esnPanel.getMyNextItemRow();
        row += 3;
        // Moves everything down
        esnPanel.setMyNextItemRow(row);
        gbc.gridx = 0;
        gbc.gridy = esnPanel.getMyNextItemRow();

        // Adds section for connectivity parameters
        sectionSeparator("Connectivity Parameters", gbc, row);

        // Add connectivity parameter check-boxes and buttons
        JPanel tempPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tempPanel.add(inToResReady);
        esnPanel.addItem(tempPanel, 2);
        esnPanel.addItem(inResSparsity, 3);
        esnPanel.addItem("Recurrent output weights:", recurrentOutputWeights);
        
        JPanel tempPanel1 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tempPanel1.add(resRecurrentReady);
        esnPanel.addItem(tempPanel1, 2);
        esnPanel.addItem(resSparsity, 3);
        esnPanel.addItem("Direct input to output weights:", directInOutWeights);
        JPanel tempPanel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        tempPanel2.add(outToResReady);
        esnPanel.addItem(tempPanel2, 2);
        esnPanel.addItem(backSparsity, 3);
        
        // Default is disabled
        backSparsity.setEnabled(false);
        outToResReady.setVisible(false);
        esnPanel.addItem("Back weights:", backWeights);
        row = esnPanel.getMyNextItemRow();
        esnPanel.setMyNextItemRow(row++);
        esnPanel.addItem(new JLabel("Spectral radius:"), maxEigenValue, 2);

        addActionListeners();
        addPropertyChangeListeners();

        setContentPane(esnPanel);
        fillFieldValues();

    }
    
    /**
     * Adds all appropriate action listeners to each button and/or checkbox.
     */
    private void addActionListeners(){
    	
    	//Enables/Disables the button for setting parameters from outputs to
    	//inputs.
        backWeights.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                if (backWeights.isSelected()) {
                    backSparsity.setEnabled(true);
                    outToResReady.setVisible(true);       
                } else {
                    backSparsity.setEnabled(false);
                    outToResReady.setVisible(false); 
                }
            }
        });
        
        //Opens the sparse panels for each of the user defined connections
        resSparsity.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent arg0) {
        		ConnectionDialog dialog = new ConnectionDialog(panel,
        				resRecurrent);
        		dialog.getOkButton().addActionListener(resRecurrentReady);
        		dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
        	}
        });
        inResSparsity.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent arg0) {
        		ConnectionDialog dialog = new ConnectionDialog(panel,
        				inToRes);
        		dialog.getOkButton().addActionListener(inToResReady);
        		dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
        	}
        });     
        backSparsity.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent arg0) {
        		ConnectionDialog dialog = new ConnectionDialog(panel,
        				outToRes);
        		dialog.getOkButton().addActionListener(outToResReady);
        		dialog.setLocationRelativeTo(null);
                dialog.pack();
                dialog.setVisible(true);
        	}
        });
        
    }
    
    /**
     * Adds ESNCreationDialog as a proptery change listener to each of the 
     * StopLights. This allows ESNCreationDialog to perform a check to
     * determine whether or not enough parameters have been set to create
     * an ESN. 
     */
    private void addPropertyChangeListeners(){

        outToResReady.addPropertyChangeListener(this);
        resRecurrentReady.addPropertyChangeListener(this);
        inToResReady.addPropertyChangeListener(this);
        
    }

    /*
     * (non-Javadoc)
     * 
     * Checks if each of the used connections has been properly set and either
     * enables or disables the Ok button accordingly.
     * 
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		
		boolean check1 = inToResReady.getState();
		boolean check2 = resRecurrentReady.getState();
		boolean check3 = outToResReady.getState() ||
				!backSparsity.isSelected();
		
		if(check1 && check2 && check3){
			enableOkButton();
		} else {
			disableOkButton();
		}
		
	}
    
    /**
     * Creates a new dialog section given a title and using a JSeparator.
     * TODO: This should really be in utils somewhere. Perhaps added to
     * LabelledItemPanel or made into its own class?
     *
     * @param label name of the section
     * @param gbc current GridBagConstraints, to align label and separators
     * @param cRow current row relative to LabeledItemPanel
     */
    public void sectionSeparator(String label, GridBagConstraints gbc,
    		int cRow) {
        // Section label
        esnPanel.add(new JLabel(label), gbc);

        // Place separator directly below label
        cRow++;
        esnPanel.setMyNextItemRow(cRow);
        gbc.gridy = esnPanel.getMyNextItemRow();

        // Add separators incrementing grix each time to cover each column
        esnPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        gbc.gridx = 1;
        esnPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);
        gbc.gridx = 2;
        esnPanel.add(new JSeparator(JSeparator.HORIZONTAL), gbc);

        // Ensures section content will be below section separator
        cRow++;
        esnPanel.setMyNextItemRow(cRow);
        // Reset column value
        gbc.gridx = 0;
    }

    /**
     * Populate fields with default data.
     */
    public void fillFieldValues() {
      
        disableOkButton(); // Connection parameters have not been set...
        tfNumInputs.setText("" + 1);
        tfNumReservoir.setText("" + 64);
        tfNumOutputs.setText("" + 1);
        recurrentOutputWeights.setSelected(false);
        directInOutWeights.setSelected(false);
        backWeights.setSelected(false);
        maxEigenValue.setText("" + 0.98);

    }

    /*
     * (non-Javadoc)
     *
     * @see org.simbrain.util.StandardDialog#closeDialogOk()
     */
    @Override
    protected void closeDialogOk() {

        try {

            if (Integer.parseInt(tfNumReservoir.getText()) < 10) {
                JOptionPane.showMessageDialog(null,
                        "Too few reservoir neurons", "Warning!",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Initialize logical network builder
            EchoStateNetwork esn = new EchoStateNetwork(
                    panel.getNetwork(),
                    // Get layer size values from fields...
                    Integer.parseInt(tfNumInputs.getText()),
                    Integer.parseInt(tfNumReservoir.getText()),
                    Integer.parseInt(tfNumOutputs.getText()),
                    panel.getLastClickedPosition());

            esn.setSpectralRadius(Double.parseDouble(maxEigenValue.getText()));
            esn.setRecurrentOutWeights(recurrentOutputWeights.isSelected());
            esn.setDirectInOutWeights(directInOutWeights.isSelected());
            esn.setBackWeights(backWeights.isSelected());
            NeuronUpdateRule resUp = boxMap.get(reservoirNeuronTypes
                    .getSelectedItem());
            esn.setReservoirNeuronType(resUp);
            NeuronUpdateRule outUp = boxMap.get(outputNeuronTypes
                    .getSelectedItem());
            esn.setOutputNeuronType(outUp);
            
            // Build network

            esn.buildNetwork();
            esn.connectLayers(inToRes.getConnection(),
            		resRecurrent.getConnection(), outToRes.getConnection());
            esn.addToParentNetwork();
            dispose();

        } catch (NumberFormatException nfe) {
            JOptionPane.showMessageDialog(null, "Inappropriate Field Values:"
                    + "\nNetwork construction failed.", "Error",
                    JOptionPane.ERROR_MESSAGE);
           nfe.printStackTrace();
        }

    }

}
