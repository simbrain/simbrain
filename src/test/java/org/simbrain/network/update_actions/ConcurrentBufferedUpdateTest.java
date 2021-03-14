package org.simbrain.network.update_actions;

import org.junit.Test;
import org.simbrain.network.core.Network;
import org.simbrain.network.groups.NeuronGroup;

import static org.junit.Assert.*;

public class ConcurrentBufferedUpdateTest

{

    @Test
    public void simpleTest() {

        // Not an actual test yet...

        Network net = new Network();
        net.setTimeStep(0.1);
        NeuronGroup ng = new NeuronGroup(net, 40_000);
        net.addNetworkModel(ng);
        net.getUpdateManager().clear();
        ConcurrentBufferedUpdate cbu = ConcurrentBufferedUpdate.createConcurrentBufferedUpdate(net);
        net.getUpdateManager().addAction(cbu);
        net.update();

        System.out.println(cbu);


        // TODO: Try with normal run to see if same results
    }
}