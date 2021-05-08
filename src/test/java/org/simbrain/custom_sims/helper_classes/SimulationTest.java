package org.simbrain.custom_sims.helper_classes;

import org.junit.jupiter.api.Test;
import org.simbrain.workspace.Workspace;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SimulationTest {

    @Test
    public void basicTest() {
        Workspace workspace = new Workspace();
        Simulation sim = new Simulation(workspace);
        sim.addOdorWorld(0,0,100,100, "Test");

        assertTrue(sim.getWorkspace().getComponentList().size() == 1);

    }
}