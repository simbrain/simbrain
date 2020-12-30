package org.simbrain.workspace;

import org.junit.Before;
import org.junit.Test;
import org.simbrain.network.NetworkComponent;
import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.workspace.serialization.WorkspaceSerializer;
import org.simbrain.world.odorworld.OdorWorldComponent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class WorkspaceTest {

    Workspace workspace;

    Neuron n1, n2;

    @Before
    public void setUpTestWorkspace() {
        workspace = new Workspace();
        Network net1 = new Network();
        NetworkComponent nc1 = new NetworkComponent("Net1", net1);
        Network net2 = new Network();
        NetworkComponent nc2 = new NetworkComponent("Net2", net2);
        workspace.addWorkspaceComponent(nc1);
        workspace.addWorkspaceComponent(nc2);
        workspace.addWorkspaceComponent(new OdorWorldComponent("odorworld"));
        workspace.addWorkspaceComponent(new ProjectionComponent("projection"));

        // Add a neuron to network 1
        n1 = new Neuron(net1);
        net1.addLooseNeuron(n1);

        // Add a neuron to network 2
        n2 = new Neuron(net2);
        net2.addLooseNeuron(n2);

        // Couple them
        workspace.getCouplingManager().createCoupling(
                workspace.getCouplingManager().getProducer(n1, "getActivation"),
                workspace.getCouplingManager().getConsumer(n2, "setInputValue"));

    }

    @Test
    public void testComponents() {
        assertEquals(4, workspace.getComponentList().size());
    }

    @Test
    public void testCouplings() {
        assertEquals(1, workspace.getCouplingManager().getCouplings().size());
        n1.forceSetActivation(.8);
        workspace.simpleIterate();
        assertEquals(.8, n2.getActivation(), .0001);

    }

    @Test
    public void testSerialization() throws IOException {

        WorkspaceSerializer serializer = new WorkspaceSerializer(workspace);

        // "Save" to output stream
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        serializer.serialize(bas);
        bas.close();

        // Clear workspace
        workspace.clearWorkspace();

        // Create an input stream from the output stream
        ByteArrayInputStream bis = new ByteArrayInputStream(bas.toByteArray());

        // "Open" from the input stream
        serializer.deserialize(bis);
        bis.close();

        // Check everything is as expected in the deserialized net
        assertEquals(4, workspace.getComponentList().size());
        assertEquals(1, workspace.getCouplingManager().getCouplings().size());

        // Can't reuse n1 and n2 because it's been deserialized
        Neuron newN1 = ((NetworkComponent)workspace.getComponent("Net1")).getNetwork().getLooseNeuron(0);
        Neuron newN2 = ((NetworkComponent)workspace.getComponent("Net2")).getNetwork().getLooseNeuron(0);
        newN1.forceSetActivation(.8);
        workspace.simpleIterate();
        assertEquals(.8, newN2.getActivation(), .0001);

    }

    @Test
    public void testZipMethods() throws IOException {

        byte[] byteArray = workspace.getZipData();
        workspace.openFromZipData(byteArray);

        // Check everything is as expected in the deserialized net
        assertEquals(4, workspace.getComponentList().size());
        assertEquals(1, workspace.getCouplingManager().getCouplings().size());

        // Can't reuse n1 and n2 because it's been deserialized
        Neuron newN1 = ((NetworkComponent)workspace.getComponent("Net1")).getNetwork().getLooseNeuron(0);
        Neuron newN2 = ((NetworkComponent)workspace.getComponent("Net2")).getNetwork().getLooseNeuron(0);
        newN1.forceSetActivation(.8);
        workspace.simpleIterate();
        assertEquals(.8, newN2.getActivation(), .0001);

    }


}