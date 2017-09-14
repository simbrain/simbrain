package org.simbrain.custom_sims.simulations.creatures;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.custom_sims.helper_classes.NetBuilder;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;

/**
 * A helper class of Creatures for filling in networks, from either a base
 * template or from genetic code. (Better here than cluttering up the main
 * class)
 * 
 * @author Sharai
 *
 */
public class CreaturesBrain {
    
    //TODO: General OOP principle.  Anything common across all creature brains
    //  should be here.Also any methods for easily customizing
    // creature brains should be here. Then at Creatures.java level
    // individual brains can be further customized.

    // TODO: Make private then add accessor
    public List<NeuronGroup> lobes = new ArrayList();
    public NetBuilder builder;

    public CreaturesBrain(NetworkComponent component) {
        this.builder = new NetBuilder(component);
    }

    /**
     * Sets up a "brain" network from a template.
     * 
     * @param brain The network to set up the brain in
     */
    public void setUp() {
        System.out.println("One empty thinkpan, coming up!");

        // Set up Lobe #1: Drive Lobe
        NeuronGroup driveLobe = builder.addNeuronGroup(0, 0, 12, "grid",
                new CreaturesNeuronRule());
        driveLobe.setLabel("Drive Lobe");
        lobes.add(driveLobe);

        // Label drive lobe neurons
        driveLobe.getNeuronList().get(0).setLabel("Myself");

        // Set up Lobe #2: Stimulus Source Lobe
        NeuronGroup stimulusLobe = builder.addNeuronGroup(150, 0, 10, "grid",
                new CreaturesNeuronRule());
        stimulusLobe.setLabel("Object Lobe");
        lobes.add(stimulusLobe);
        
        // set up standard node labels

    }

    public Network getNetwork() {
        return builder.getNetwork();
    }

}
