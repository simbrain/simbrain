package org.simbrain.network.gui.dialogs.synapse;

import javax.swing.JTextField;

import org.simbrain.network.gui.NetworkUtils;
import org.simbrain.network.synapses.TDSynapse;

public class TDSynapsePanel extends AbstractSynapsePanel{
    /** Learning rate field. */
    private JTextField tfLearningRate = new JTextField();
    
    /** Discount factor field. */
    private JTextField tfGamma = new JTextField();

    
    /** Synapse reference. */
    private TDSynapse synapseRef;

    /**
     * This method is the default constructor.
     */
    public TDSynapsePanel() {
        this.addItem("Learning rate", tfLearningRate);
        this.addItem("Discount factor", tfGamma);
    }

    /**
     * Populate fields with current data.
     */
    public void fillFieldValues() {
        synapseRef = (TDSynapse) synapseList.get(0);

        tfLearningRate.setText(Double.toString(synapseRef.getLearningRate()));
        tfGamma.setText(Double.toString(synapseRef.getGamma()));

        //Handle consistency of multiply selections
        if (!NetworkUtils.isConsistent(synapseList, TDSynapse.class, "getLearningRate")) {
            tfLearningRate.setText(NULL_STRING);
        }
        if (!NetworkUtils.isConsistent(synapseList, TDSynapse.class, "getGamma")) {
            tfGamma.setText(NULL_STRING);
        }
        
    }

    /**
     * Fill field values to default values for this synapse type.
     */
    public void fillDefaultValues() {
//	      TDSynapse synapseRef = new TDSynapse();
        tfLearningRate.setText(Double.toString(TDSynapse.DEFAULT_LEARNING_RATE));
        tfGamma.setText(Double.toString(TDSynapse.DEFAULT_GAMMA));
    }

    /**
     * Called externally when the dialog is closed, to commit any changes made.
     */
    public void commitChanges() {
        for (int i = 0; i < synapseList.size(); i++) {
            TDSynapse synapseRef = (TDSynapse) synapseList.get(i);

            if (!tfLearningRate.getText().equals(NULL_STRING)) {
                synapseRef.setLearningRate(Double.parseDouble(tfLearningRate.getText()));
            }
            if (!tfGamma.getText().equals(NULL_STRING)) {
                synapseRef.setGamma(Double.parseDouble(tfGamma.getText()));
            }            
        }
    }
}
