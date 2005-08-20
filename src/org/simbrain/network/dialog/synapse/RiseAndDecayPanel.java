package org.simbrain.network.dialog.synapse;

import javax.swing.JTextField;

import org.simbrain.network.NetworkUtils;
import org.simnet.synapses.spikeresponders.JumpAndDecay;
import org.simnet.synapses.spikeresponders.RiseAndDecay;

public class RiseAndDecayPanel extends AbstractSpikeResponsePanel {

    private JTextField tfMaximumResponse = new JTextField();
    private JTextField tfBaseLineResponse = new JTextField();
    private JTextField tfDecayRate = new JTextField();
    
    public RiseAndDecayPanel(){
        tfMaximumResponse.setColumns(6);
        this.addItem("Maximum response", tfMaximumResponse);
        this.addItem("Base-line response", tfBaseLineResponse);
        this.addItem("Decay rate", tfDecayRate);
    }
    
    public void fillFieldValues() {
        RiseAndDecay spikeResponder = (RiseAndDecay)spikeResponderList.get(0);
        
        tfMaximumResponse.setText(Double.toString(spikeResponder.getMaximumResponse()));
        tfBaseLineResponse.setText(Double.toString(spikeResponder.getBaseLineResponse()));
        tfDecayRate.setText(Double.toString(spikeResponder.getDecayRate()));
        
        //Handle consistency of multiply selections
        if(!NetworkUtils.isConsistent(spikeResponderList, RiseAndDecay.class, "getMaximumResponse")) {
            tfMaximumResponse.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(spikeResponderList, RiseAndDecay.class, "getBaseLineResponse")) {
            tfBaseLineResponse.setText(NULL_STRING);
        }
        if(!NetworkUtils.isConsistent(spikeResponderList, RiseAndDecay.class, "getDecayRate")) {
            tfDecayRate.setText(NULL_STRING);
        }


    }

    public void fillDefaultValues() {
        RiseAndDecay spiker_ref = new RiseAndDecay();
        tfMaximumResponse.setText(Double.toString(spiker_ref.getMaximumResponse()));
        tfBaseLineResponse.setText(Double.toString(spiker_ref.getBaseLineResponse()));
        tfDecayRate.setText(Double.toString(spiker_ref.getDecayRate()));
    }

    public void commitChanges() {
        
        for (int i = 0; i < spikeResponderList.size(); i++) {
            RiseAndDecay spiker_ref = (RiseAndDecay) spikeResponderList.get(i);
            if (tfMaximumResponse.getText().equals(NULL_STRING) == false) {
                spiker_ref.setMaximumResponse(Double.parseDouble(tfMaximumResponse
                        .getText()));
            }
            if (tfBaseLineResponse.getText().equals(NULL_STRING) == false) {
                spiker_ref.setBaseLineResponse(Double.parseDouble(tfBaseLineResponse
                        .getText()));
            }
            if (tfDecayRate.getText().equals(NULL_STRING) == false) {
                spiker_ref.setDecayRate(Double.parseDouble(tfDecayRate
                        .getText()));
            }
        }
    }

}
