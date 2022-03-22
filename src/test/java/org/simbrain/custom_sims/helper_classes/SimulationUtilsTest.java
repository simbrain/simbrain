package org.simbrain.custom_sims.helper_classes;

import org.junit.jupiter.api.Test;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimulationUtilsTest {

    @Test
    public void testAddWorld() {
        SimbrainDesktop desktop = new SimbrainDesktop(new Workspace());
        SimulationUtils sim = new SimulationUtils(desktop);
        sim.addOdorWorld(0,0,100,100, "Test");

        assertEquals(1, sim.getWorkspace().getComponentList().size());

    }
}