package org.simbrain.network.dialog.synapse;

import javax.swing.JTextField;

import org.simnet.synapses.spikeresponders.JumpAndDecay;
import org.simnet.synapses.spikeresponders.RiseAndDecay;

public class RiseAndDecayPanel extends AbstractSpikeResponsePanel {

    private JTextField tfMaximumResponse = new JTextField();
    private JTextField tfBaseLineResponse = new JTextField();
    private JTextField tfDecayRate = new JTextField();
    
    public RiseAndDecayPanel(){
        this.addItem("Maximum response", tfMaximumResponse);
        this.addItem("Base-line response", tfBaseLineResponse);
        this.addItem("Decay rate", tfDecayRate);
    }
    
    public void fillFieldValues() {
        // TODO Auto-generated method stub

    }

    public void fillDefaultValues() {
        RiseAndDecay spiker_ref = new RiseAndDecay();
        tfMaximumResponse.setText(Double.toString(spiker_ref.getMaximumResponse()));
        tfBaseLineResponse.setText(Double.toString(spiker_ref.getBaseLineResponse()));
        tfDecayRate.setText(Double.toString(spiker_ref.getDecayRate()));
    }

    public void commitChanges() {
        // TODO Auto-generated method stub

    }

}
