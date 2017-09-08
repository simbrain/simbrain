package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.network.core.Network.TimeType;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.NeuronUpdateRule;
import org.simbrain.network.neuron_update_rules.LinearRule;

// Can override integrate and fire or something else too
public class CreaturesNeuronRule extends LinearRule {


    public CreaturesNeuronRule() {
        System.out.println("here");
    }

    @Override
    public String getName() {
        return "Creatures Neuron";
    }

    @Override
    public void update(Neuron neuron) {
        super.update(neuron);
        // Do custom stuff here.
        System.out.println("updating neuron: " + neuron.getLabel());
    }
//
//    @Override
//    public CreaturesNeuronRule deepCopy() {
//        
//        // Copy any custom properties here 
//        return (CreaturesNeuronRule) super.deepCopy();
//    }

}
