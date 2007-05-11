package org.simnet.groups;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Group;
import org.simnet.interfaces.RootNetwork;

/**
 * Will implement the Leabra / GeneRec algorith.  Currently for testing.
 *
 */
public class GeneRec extends Group {

    /** @see Group. */
    public GeneRec(final RootNetwork net) {
        super(net);
        // TODO Auto-generated constructor stub
    }

    /** @Override. */
    public void update() {
        for (Neuron n : getFlatNeuronList()) {
            if (n != null) {
                n.randomize(); // update neuron buffers
            }
        }
    }
}
