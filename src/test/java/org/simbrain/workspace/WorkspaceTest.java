package org.simbrain.workspace;

import org.junit.Before;
import org.junit.Test;
import org.simbrain.network.NetworkComponent;
import org.simbrain.plot.projection.ProjectionComponent;
import org.simbrain.workspace.serialization.WorkspaceSerializer;
import org.simbrain.world.odorworld.OdorWorldComponent;

import java.io.IOException;

import static org.junit.Assert.*;

public class WorkspaceTest {

    Workspace workspace;

    @Before
    public void setUpTestWorkspace() {
        workspace = new Workspace();
        workspace.addWorkspaceComponent(new NetworkComponent("network"));
        workspace.addWorkspaceComponent(new OdorWorldComponent("odorworld"));
        workspace.addWorkspaceComponent(new ProjectionComponent("projection"));

        // TODO: Set up a coupling and test it

    }

    @Test
    public void testComponents() {
        assertEquals(3, workspace.getComponentList().size());
    }

    @Test
    public void testSerialization() {
        WorkspaceSerializer serializer = new WorkspaceSerializer(workspace);
        try {
            serializer.serialize(System.out);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // TODO: Improve the serialization api a bit to make it easier to test
        // (I also always thought it was kind of hard to use)
    }




}