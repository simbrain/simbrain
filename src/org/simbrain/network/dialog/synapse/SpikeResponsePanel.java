package org.simbrain.network.dialog.synapse;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.neuron.AbstractNeuronPanel;
import org.simbrain.network.dialog.neuron.AdditiveNeuronPanel;
import org.simbrain.network.dialog.neuron.BinaryNeuronPanel;
import org.simbrain.network.dialog.neuron.ClampedNeuronPanel;
import org.simbrain.network.dialog.neuron.IntegrateAndFireNeuronPanel;
import org.simbrain.network.dialog.neuron.LinearNeuronPanel;
import org.simbrain.network.dialog.neuron.LogisticNeuronPanel;
import org.simbrain.network.dialog.neuron.RandomNeuronPanel;
import org.simbrain.network.dialog.neuron.SigmoidalNeuronPanel;
import org.simbrain.network.dialog.neuron.SinusoidalNeuronPanel;
import org.simbrain.network.dialog.neuron.StandardNeuronPanel;
import org.simbrain.network.dialog.neuron.StochasticNeuronPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.SpikeResponse;
import org.simnet.neurons.AdditiveNeuron;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.ClampedNeuron;
import org.simnet.neurons.IntegrateAndFireNeuron;
import org.simnet.neurons.LinearNeuron;
import org.simnet.neurons.LogisticNeuron;
import org.simnet.neurons.RandomNeuron;
import org.simnet.neurons.SigmoidalNeuron;
import org.simnet.neurons.SinusoidalNeuron;
import org.simnet.neurons.StandardNeuron;
import org.simnet.neurons.StochasticNeuron;
import org.simnet.util.RandomSource;


public class SpikeResponsePanel extends JPanel implements ActionListener {

    private LabelledItemPanel topPanel = new LabelledItemPanel();
    
    private TristateDropDown cbScaleByPSPDiff = new TristateDropDown();
    private JTextField tfPSRestingPotential = new JTextField();
    private JComboBox cbSpikeResponseType = new JComboBox(SpikeResponse.getTypeList());
    
    private AbstractSpikeResponsePanel spikeFunctionPanel;

    private ArrayList spikerList = new ArrayList();
    private ArrayList selectionList = new ArrayList();
    
    
    public SpikeResponsePanel(){
        this.setLayout(new BorderLayout());
        topPanel.addItem("Scale by synaptic potential difference", cbScaleByPSPDiff);
        topPanel.addItem("Synaptic resting potential ", tfPSRestingPotential);
        topPanel.addItem("Spike response function", cbSpikeResponseType);
        this.add(BorderLayout.NORTH, topPanel);
        spikeFunctionPanel = new StepSpikerPanel();
        this.add(BorderLayout.CENTER, spikeFunctionPanel);
    }
    
    /**
	  * Initialize the main neuron panel based on the type of the selected neurons
	  */
	 public void initSpikerType() {
	 	SpikeResponse spiker = (SpikeResponse)spikerList.get(0);
	 	
//		if(!NetworkUtils.isConsistent(neuron_list, Neuron.class, "getType")) {
//			cbNeuronType.addItem(AbstractNeuronPanel.NULL_STRING);
//			cbNeuronType.setSelectedIndex(Neuron.getTypeList().length);
//			neuronPanel = new ClampedNeuronPanel(); // Simply to serve as an empty panel
//		} 
	 }
	 
	
	public void actionPerformed(ActionEvent e){

	}
	
	public void fillFieldValues(ArrayList spikers){
	    
		SpikeResponse spiker = (SpikeResponse)spikerList.get(0);

		//TODO: init fields to default values
//	    cbDistribution.setSelectedIndex(rand.getDistributionIndex());
//	    isUseBounds.setSelected(rand.isUseBounds());
	    
	    //Handle consistency of multiple selections
//	    if (!NetworkUtils.isConsistent(randomizers, RandomSource.class, "getDistributionIndex")) {
//	        if ((cbDistribution.getItemCount() == RandomSource
//	                .getFunctionList().length)) {
//	            cbDistribution.addItem(NULL_STRING);
//	        }
//	        cbDistribution
//	        .setSelectedIndex(RandomSource.getFunctionList().length);
//	    }
//	    
	}
		
    public void fillDefaultValues() {
    	
    	//I'll worry abou this later
//        SpikeResponse spiker = new SpikeResponse();
//        
//        cbDistribution.setSelectedIndex(rand.getDistributionIndex());
//        isUseBounds.setSelected(rand.isUseBounds());
//        tfLowBound.setText(Double.toString(rand.getLowerBound()));
//        tfUpBound.setText(Double.toString(rand.getUpperBound()));
//        tfStandardDeviation.setText(Double.toString(rand.getStandardDeviation()));
//        tfMean.setText(Double.toString(rand.getMean()));
    }
    
    public void commitSpiker(SpikeResponse spike) {
//        if (cbDistribution.getSelectedItem().equals(NULL_STRING) == false) {
//            rand.setDistributionIndex(cbDistribution.getSelectedIndex());
//        }
//        if (tfLowBound.getText().equals(NULL_STRING) == false) {
//            rand.setLowerBound(Double.parseDouble(tfLowBound.getText()));
//        }
//        if (tfUpBound.getText().equals(NULL_STRING) == false) {
//            rand.setUpperBound(Double.parseDouble(tfUpBound.getText()));
//        }
//        if (tfStandardDeviation.getText().equals(NULL_STRING) == false) {
//            rand.setStandardDeviation(Double.parseDouble(tfStandardDeviation
//                    .getText()));
//        }
//        if (tfMean.getText().equals(NULL_STRING) == false) {
//            rand.setMean(Double.parseDouble(tfMean.getText()));
//        }
//        if ((isUseBounds.getSelectedIndex() == TristateDropDown.NULL) == false) {
//            rand.setUseBounds(isUseBounds.isSelected());
//        }
//
    }
}
