package org.simbrain.network.dialog.neuron;

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.neurons.NakaRushtonNeuron;

public class NakaRushtonNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfMaxSpikeRate = new JTextField();
    private JTextField tfSteepness = new JTextField();
    private JTextField tfSemiSaturation = new JTextField();
    private JTextField tfTimeConstant = new JTextField();
    
    public NakaRushtonNeuronPanel(){
        this.addItem("Maximum spike rate", tfMaxSpikeRate);
        this.addItem("Steepness", tfSteepness);
        this.addItem("Semi-saturation constant", tfSemiSaturation);
        this.addItem("Time Constant", tfTimeConstant);
    }
    
    public void fillFieldValues() {
        NakaRushtonNeuron neuron_ref = (NakaRushtonNeuron)neuron_list.get(0);
        
        tfMaxSpikeRate.setText(Double.toString(neuron_ref.getMaximumSpikeRate()));
        tfSemiSaturation.setText(Double.toString(neuron_ref.getSemiSaturationConstant()));
        tfSteepness.setText(Double.toString(neuron_ref.getSteepness()));
        tfTimeConstant.setText(Double.toString(neuron_ref.getTimeConstant()));

        //Handle consistency of multiple selections
        if(!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getMaximumSpikeRate")) {
            tfMaxSpikeRate.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getSemiSaturationConstant")) {
            tfSemiSaturation.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getSteepness")) {
            tfSteepness.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getTimeConstant")) {
            tfTimeConstant.setText(NULL_STRING);
        }

    }

    public void fillDefaultValues() {
        NakaRushtonNeuron neuron_ref = new NakaRushtonNeuron();
        tfMaxSpikeRate.setText(Double.toString(neuron_ref.getMaximumSpikeRate()));
        tfSemiSaturation.setText(Double.toString(neuron_ref.getSemiSaturationConstant()));
        tfSteepness.setText(Double.toString(neuron_ref.getSteepness()));
        tfTimeConstant.setText(Double.toString(neuron_ref.getTimeConstant()));
    }

    public void commitChanges() {
        
        for (int i = 0; i < neuron_list.size(); i++) {
            NakaRushtonNeuron neuron_ref = (NakaRushtonNeuron) neuron_list.get(i);

            if (tfMaxSpikeRate.getText().equals(NULL_STRING) == false) {
                neuron_ref.setMaximumSpikeRate(Double.parseDouble(tfMaxSpikeRate
                        .getText()));
            }
            if (tfSemiSaturation.getText().equals(NULL_STRING) == false) {
                neuron_ref.setSemiSaturationConstant(Double.parseDouble(tfSemiSaturation
                        .getText()));
            }
            if (tfSteepness.getText().equals(NULL_STRING) == false) {
                neuron_ref.setSteepness(Double.parseDouble(tfSteepness
                        .getText()));
            }
            if (tfTimeConstant.getText().equals(NULL_STRING) == false) {
                neuron_ref.setTimeConstant(Double.parseDouble(tfTimeConstant
                        .getText()));
            }
        }

    }

}
