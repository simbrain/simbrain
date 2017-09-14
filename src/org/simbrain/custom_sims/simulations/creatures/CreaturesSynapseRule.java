package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.synapse_update_rules.StaticSynapseRule;

public class CreaturesSynapseRule extends StaticSynapseRule {


    @Override
    public String getName() {
        return "Creatures Synapse";
    }

    @Override
    public void update(Synapse synapse) {
        super.update(synapse);
        System.out.println("updating synapse: " + synapse.getId());
    }

}
