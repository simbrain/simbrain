package org.simnet.groups;

import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Group;

/**
 * Will implement the Leabra / GeneRec algorith.  Currently for testing.
 *
 */
public class GeneRec extends Group {

    /** @Override. */
    public void update() {
        for (Neuron n : getNeuronList()) {
                n.randomize(); // update neuron buffers
        }
    }

}
