package org.simbrain.network.update_actions;

import org.junit.jupiter.api.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PriorityUpdateTest {

    Network net;
    Neuron n1, n2, n3;
    Synapse s1, s2;

    @Test
    void testUpdate() {
        net = new Network();
        net.getUpdateManager().clear();
        net.getUpdateManager().addAction(new PriorityUpdate(net));

        n1 = new Neuron();
        n1.setUpdatePriority(1);
        n1.setClamped(true);
        n1.setActivation(.5);

        n2 = new Neuron();
        n2.setUpdatePriority(2);

        n3 = new Neuron();
        n3.setUpdatePriority(3);

        s1 = new Synapse(n1, n2);
        s2 = new Synapse(n2, n3);
        net.addNetworkModels(List.of(n1,n2,n3,s1,s2));

        net.update();

        assertEquals(.5, n3.getActivation(), .001);

        // Comparison case: buffered update. .5 activation won't propagate to end after one iteration
        net.clearActivations();
        net.getUpdateManager().clear();
        net.getUpdateManager().addAction(new BufferedUpdate(net));
        assertNotEquals(.5, n3.getActivation(), .001);


    }
}