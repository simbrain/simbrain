package org.simbrain.network.gui.dialogs;

import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkPanel;
import org.simbrain.network.gui.dialogs.layout.LayoutDialog;
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog;
import org.simbrain.network.gui.nodes.NeuronNode;
import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.layouts.GridLayout;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neurons.LinearNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.StandardDialog;

public class AddNeuronsDialog extends StandardDialog {

	private final NetworkPanel networkPanel;
	
	private LabelledItemPanel addNeuronsPanel = new LabelledItemPanel();
	
	//private JButton selectNeuronType;
	
	//private JButton selectLayout;
	
	private JTextField numNeurons;
	
	//private Layout layout;
	
	//private final NeuronDialog nDialog;
	
	public AddNeuronsDialog(NetworkPanel networkPanel) {
		this.networkPanel = networkPanel;
		
		setTitle("Add Neurons");
		
		GridBagConstraints c = new GridBagConstraints();
		
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		addNeuronsPanel.setMyNextItemRow(1);
		c.gridx = 0;
		c.gridy = addNeuronsPanel.getMyNextItemRow();
		
		numNeurons = new JTextField("25");
		
		addNeuronsPanel.addItem("Number of Neurons: ", numNeurons);
		numNeurons.setVisible(true);
		setContentPane(addNeuronsPanel);
		
	}
	
	protected void closeDialogOk(){
		super.closeDialogOk();
		ArrayList<NeuronNode> nodes = new ArrayList<NeuronNode>();
		
		for(int i = 0; i < Integer.parseInt(numNeurons.getText()); i++){
			Neuron neuron = new Neuron(networkPanel.getRootNetwork(), new LinearNeuron());
			nodes.add(new NeuronNode(networkPanel, neuron));
			networkPanel.getRootNetwork().addNeuron(neuron);
		}

		networkPanel.setSelection(nodes);
		networkPanel.showSelectedNeuronProperties();
		
		LayoutDialog lDialog = new LayoutDialog(networkPanel);
		lDialog.pack();
		lDialog.setLocationRelativeTo(null);
		lDialog.setVisible(true);
		
		addNeuronsPanel.setVisible(false);
		dispose();
		
	}
	
}
