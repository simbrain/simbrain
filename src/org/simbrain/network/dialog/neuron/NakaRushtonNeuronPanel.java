package org.simbrain.network.dialog.neuron;

import java.util.ArrayList;

import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simbrain.network.dialog.RandomPanel;
import org.simbrain.util.LabelledItemPanel;
import org.simbrain.util.TristateDropDown;
import org.simnet.neurons.NakaRushtonNeuron;

public class NakaRushtonNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfMaxSpikeRate = new JTextField();
    private JTextField tfSteepness = new JTextField();
    private JTextField tfSemiSaturation = new JTextField();
    private JTextField tfTimeConstant = new JTextField();
    private TristateDropDown tsNoise = new TristateDropDown();
    private JTabbedPane tabbedPane = new JTabbedPane();
    private LabelledItemPanel mainTab = new LabelledItemPanel();
    private RandomPanel randTab = new RandomPanel(true);
    
    public NakaRushtonNeuronPanel(){
        this.add(tabbedPane);
        mainTab.addItem("Maximum spike rate", tfMaxSpikeRate);
        mainTab.addItem("Steepness", tfSteepness);
        mainTab.addItem("Semi-saturation constant", tfSemiSaturation);
        mainTab.addItem("Time Constant", tfTimeConstant);
        mainTab.addItem("Add noise", tsNoise);
        tabbedPane.add(mainTab, "Main");
        tabbedPane.add(randTab, "Noise");
    }
    
    public void fillFieldValues() {
        NakaRushtonNeuron neuron_ref = (NakaRushtonNeuron)neuron_list.get(0);
        
        tfMaxSpikeRate.setText(Double.toString(neuron_ref.getMaximumSpikeRate()));
        tfSemiSaturation.setText(Double.toString(neuron_ref.getSemiSaturationConstant()));
        tfSteepness.setText(Double.toString(neuron_ref.getSteepness()));
        tfTimeConstant.setText(Double.toString(neuron_ref.getTimeConstant()));
        tsNoise.setSelected(neuron_ref.getAddNoise());

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
        if(!NetworkUtils.isConsistent(neuron_list, NakaRushtonNeuron.class, "getAddNoise")) {
            tsNoise.setNull();
        }
        randTab.fillFieldValues(getRandomizers());
    }

    private ArrayList getRandomizers() {
        ArrayList ret = new ArrayList();
        for (int i = 0; i < neuron_list.size(); i++) {
            ret.add(((NakaRushtonNeuron)neuron_list.get(i)).getNoiseGenerator());
        }
        return ret;
    }
    
    public void fillDefaultValues() {
        NakaRushtonNeuron neuron_ref = new NakaRushtonNeuron();
        tfMaxSpikeRate.setText(Double.toString(neuron_ref.getMaximumSpikeRate()));
        tfSemiSaturation.setText(Double.toString(neuron_ref.getSemiSaturationConstant()));
        tfSteepness.setText(Double.toString(neuron_ref.getSteepness()));
        tfTimeConstant.setText(Double.toString(neuron_ref.getTimeConstant()));
        tsNoise.setSelected(neuron_ref.getAddNoise());
        randTab.fillDefaultValues();
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
            if (tsNoise.isNull() == false) {
                neuron_ref.setAddNoise(tsNoise.isSelected());
            }
            randTab.commitRandom(neuron_ref.getNoiseGenerator());
        }

    }

}
