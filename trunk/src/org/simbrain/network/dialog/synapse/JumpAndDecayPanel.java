package org.simbrain.network.dialog.synapse;

import javax.swing.JTextField;

import org.simnet.synapses.spikeresponders.JumpAndDecay;
import org.simnet.synapses.spikeresponders.Step;

public class JumpAndDecayPanel extends AbstractSpikeResponsePanel {
    
    private JTextField tfJumpHeight = new JTextField();
    private JTextField tfBaseLine = new JTextField();
    private JTextField tfDecayRate = new JTextField();
    
    private JumpAndDecayPanel(){
        this.addItem("Jump height", tfJumpHeight);
        this.addItem("Base-line", tfBaseLine);
        this.addItem("Decay rate", tfDecayRate);
    }

    public void fillFieldValues() {
        // TODO Auto-generated method stub

    }

    public void fillDefaultValues() {
        JumpAndDecay spiker_ref = new JumpAndDecay();
        tfJumpHeight.setText(Double.toString(spiker_ref.getJumpHeight()));
        tfBaseLine.setText(Double.toString(spiker_ref.getBaseLine()));
        tfDecayRate.setText(Double.toString(spiker_ref.getDecayRate()));
    }

    public void commitChanges() {
        // TODO Auto-generated method stub

    }

}
