package org.simbrain.network.dialog.neuron;

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.neurons.BinaryNeuron;
import org.simnet.neurons.IzhikevichNeuron;

public class IzhikevichNeuronPanel extends AbstractNeuronPanel {

    private JTextField tfA = new JTextField();
    private JTextField tfB = new JTextField();
    private JTextField tfC = new JTextField();
    private JTextField tfD = new JTextField();
    
    public IzhikevichNeuronPanel(){
        this.addItem("A", tfA);
        this.addItem("B", tfB);
        this.addItem("C", tfC);
        this.addItem("D", tfD);
    }
    
    public void fillFieldValues() {
        IzhikevichNeuron neuron_ref = (IzhikevichNeuron)neuron_list.get(0);
        
        tfA.setText(Double.toString(neuron_ref.getA()));
        tfB.setText(Double.toString(neuron_ref.getB()));
        tfC.setText(Double.toString(neuron_ref.getC()));
        tfD.setText(Double.toString(neuron_ref.getD()));

        //Handle consistency of multiple selections
        if(!NetworkUtils.isConsistent(neuron_list, IzhikevichNeuron.class, "getA")) {
            tfA.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, IzhikevichNeuron.class, "getB")) {
            tfB.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, IzhikevichNeuron.class, "getC")) {
            tfC.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(neuron_list, IzhikevichNeuron.class, "getD")) {
            tfD.setText(NULL_STRING);
        }

    }

    public void fillDefaultValues() {
        IzhikevichNeuron neuron_ref = new IzhikevichNeuron();
        tfA.setText(Double.toString(neuron_ref.getA()));
        tfB.setText(Double.toString(neuron_ref.getB()));
        tfC.setText(Double.toString(neuron_ref.getC()));
        tfD.setText(Double.toString(neuron_ref.getD()));

    }

    public void commitChanges() {
        for (int i = 0; i < neuron_list.size(); i++) {
            IzhikevichNeuron neuron_ref = (IzhikevichNeuron) neuron_list.get(i);

            if (tfA.getText().equals(NULL_STRING) == false) {
                neuron_ref.setA(Double.parseDouble(tfA
                        .getText()));
            }
            if (tfB.getText().equals(NULL_STRING) == false) {
                neuron_ref.setB(Double.parseDouble(tfB
                        .getText()));
            }
            if (tfC.getText().equals(NULL_STRING) == false) {
                neuron_ref.setC(Double.parseDouble(tfC
                        .getText()));
            }
            if (tfD.getText().equals(NULL_STRING) == false) {
                neuron_ref.setD(Double.parseDouble(tfD
                        .getText()));
            }
        }

    }

}
