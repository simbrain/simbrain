package org.simbrain.network.dialog.synapse;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.pnodes.PNodeNeuron;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.interfaces.*;
import org.simnet.neurons.StandardNeuron;
import org.simnet.synapses.spikeresponders.*;


public class SpikeResponsePanel extends JPanel implements ActionListener {

    public static final String NULL_STRING = "...";

    private JPanel mainPanel = new JPanel();
    private LabelledItemPanel topPanel = new LabelledItemPanel();
    
    private TristateDropDown cbScaleByPSPDiff = new TristateDropDown();
    private JTextField tfPSRestingPotential = new JTextField();
    private JComboBox cbSpikeResponseType = new JComboBox(SpikeResponse.getTypeList());
    
    private AbstractSpikeResponsePanel spikeFunctionPanel;

    private ArrayList spikeResponderList;
    private ArrayList synapseList;
    
    private JDialog parentDialog;
    
    private boolean spikeRespondersHaveChanged = false;
    
    public SpikeResponsePanel(ArrayList synapses, JDialog parent){
        this.setLayout(new BorderLayout());
        synapseList = synapses;
        parentDialog = parent;
	    spikeResponderList = getSpikeResponders();
        initSpikeResponseType();
        fillFieldValues();
        cbSpikeResponseType.addActionListener(this);

	    topPanel.addItem("Scale by psp", cbScaleByPSPDiff);
        topPanel.addItem("Synaptic resting potential ", tfPSRestingPotential);
        topPanel.addItem("Spike response function", cbSpikeResponseType);
        this.add(BorderLayout.NORTH, topPanel);
        mainPanel.add(spikeFunctionPanel);
        this.add(BorderLayout.CENTER, mainPanel);
       
    }
     
    private ArrayList getSpikeResponders() {
    		ArrayList ret = new ArrayList();
    		for (int i = 0; i < synapseList.size(); i++) {
    			ret.add(((Synapse)synapseList.get(i)).getSpikeResponder());
    		}
    		return ret;
    }
    
	 private void initSpikeResponseType() {
	 	
		SpikeResponse spikeResponder = (SpikeResponse)spikeResponderList.get(0);

		if(!NetworkUtils.isConsistent(spikeResponderList, SpikeResponse.class, "getType")) {
			cbSpikeResponseType.addItem(NULL_STRING);
			cbSpikeResponseType.setSelectedIndex(SpikeResponse.getTypeList().length);
			spikeFunctionPanel = new BlankSpikerPanel(); // Simply to serve as an empty panel
		} else if (spikeResponder instanceof Step) {
			cbSpikeResponseType.setSelectedIndex(SpikeResponse.getSpikerTypeIndex(Step.getName()));
			spikeFunctionPanel = new StepSpikerPanel();
			spikeFunctionPanel.setSpikeResponderList(spikeResponderList);
			spikeFunctionPanel.fillFieldValues();
		} else if (spikeResponder instanceof JumpAndDecay) {
			cbSpikeResponseType.setSelectedIndex(SpikeResponse.getSpikerTypeIndex(JumpAndDecay.getName()));
			spikeFunctionPanel = new JumpAndDecayPanel();
			spikeFunctionPanel.setSpikeResponderList(spikeResponderList);
			spikeFunctionPanel.fillFieldValues();
		} else if (spikeResponder instanceof RiseAndDecay) {
			cbSpikeResponseType.setSelectedIndex(SpikeResponse.getSpikerTypeIndex(RiseAndDecay.getName()));
			spikeFunctionPanel = new RiseAndDecayPanel();
			spikeFunctionPanel.setSpikeResponderList(spikeResponderList);
			spikeFunctionPanel.fillFieldValues();
		}
	 }
	 
	 private void changeSpikeResponders() {
	 	if(cbSpikeResponseType.getSelectedItem().toString().equalsIgnoreCase(Step.getName())) {
		 	for (int i = 0; i < spikeResponderList.size(); i++) {
		 		((Synapse)synapseList.get(i)).setSpikeResponder(new Step());
		 	}	 		
	 	} else if(cbSpikeResponseType.getSelectedItem().toString().equalsIgnoreCase(JumpAndDecay.getName())) {
		 	for (int i = 0; i < spikeResponderList.size(); i++) {
		 		((Synapse)synapseList.get(i)).setSpikeResponder(new JumpAndDecay());
		 	}		 		
	 	} else if(cbSpikeResponseType.getSelectedItem().toString().equalsIgnoreCase(RiseAndDecay.getName())) {
		 	for (int i = 0; i < spikeResponderList.size(); i++) {
		 		((Synapse)synapseList.get(i)).setSpikeResponder(new RiseAndDecay());
		 	}
	 	}

	 }
	 
	
	 public void actionPerformed(ActionEvent e) {

	 	spikeRespondersHaveChanged = true;
	 	
	 	if(cbSpikeResponseType.getSelectedItem().equals(Step.getName())){
	 		mainPanel.remove(spikeFunctionPanel);
	 		spikeFunctionPanel = new StepSpikerPanel();
	 		spikeFunctionPanel.fillDefaultValues();
			mainPanel.add(spikeFunctionPanel);
	 	} else if (cbSpikeResponseType.getSelectedItem().equals(JumpAndDecay.getName())){
	 		mainPanel.remove(spikeFunctionPanel);
	 		spikeFunctionPanel = new JumpAndDecayPanel();
	 		spikeFunctionPanel.fillDefaultValues();
			mainPanel.add(spikeFunctionPanel);
	 	} else if  (cbSpikeResponseType.getSelectedItem().equals(RiseAndDecay.getName())){
	 		mainPanel.remove(spikeFunctionPanel);
	 		spikeFunctionPanel = new RiseAndDecayPanel();
	 		spikeFunctionPanel.fillDefaultValues();
			mainPanel.add(spikeFunctionPanel);
		}
	 	parentDialog.pack();
	 	parentDialog.repaint();
	 }
	 
	public void fillFieldValues(){
	    
		SpikeResponse spikeResponder = (SpikeResponse)spikeResponderList.get(0);
		
	 	cbScaleByPSPDiff.setSelected(spikeResponder.isScaleByPSPDifference());
	 	tfPSRestingPotential.setText(Double.toString(spikeResponder.getPsRestingPotential()));
	 	
	 	spikeFunctionPanel.fillFieldValues();
		
        //Handle consistency of multiple selections
		if(!NetworkUtils.isConsistent(spikeResponderList, SpikeResponse.class, "isScaleByPSPDifference")) {
            cbScaleByPSPDiff.setNull();
        }
		if(!NetworkUtils.isConsistent(spikeResponderList, SpikeResponse.class, "getPsRestingPotential")) {
			tfPSRestingPotential.setText(NULL_STRING);
		}

	}
		    
    public void commitChanges() {
	    for (int i = 0; i < spikeResponderList.size(); i++) {
			SpikeResponse spikeResponder = (SpikeResponse)spikeResponderList.get(0);
			if (cbScaleByPSPDiff.isNull() == false) {
				spikeResponder.setScaleByPSPDifference(cbScaleByPSPDiff.isSelected());
			}
			if (tfPSRestingPotential.getText().equals(NULL_STRING) == false) {
				spikeResponder.setPsRestingPotential(Double.parseDouble(tfPSRestingPotential.getText()));
			}    	    
	    }
	    if (spikeRespondersHaveChanged) {
		    changeSpikeResponders();
	    }
	    spikeFunctionPanel.setSpikeResponderList(getSpikeResponders());
	    spikeFunctionPanel.commitChanges();
		
    }
	/**
	 * @return Returns the spikerList.
	 */
	public ArrayList getSpikeResponderList() {
		return spikeResponderList;
	}
	/**
	 * @param spikerList The spikerList to set.
	 */
	public void setSpikeResponderList(ArrayList spikerList) {
		this.spikeResponderList = spikerList;
	}
}
