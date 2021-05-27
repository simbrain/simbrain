package org.simbrain.network.util;

import org.simbrain.network.core.Synapse;
import org.simbrain.network.groups.SynapseGroup;

import java.util.HashSet;

/**
 * A hashset with a reference to a parent synapse group to support xstream serialization
 * via {@link org.simbrain.network.groups.SynapseGroupConverter}
 */
public class SynapseSet extends HashSet<Synapse> {

    /**
     * Parent synapse group
     */
    private final SynapseGroup sg;

    public SynapseSet(SynapseGroup sg, int numSynapses) {
        super(numSynapses);
        this.sg = sg;
    }

    public SynapseSet(SynapseGroup sg, HashSet<Synapse> newSynapses) {
        super();
        this.sg = sg;
        if (newSynapses != null) {
            addAll(newSynapses);
        }
    }

    public SynapseGroup getParent() {
        return sg;
    }
}
