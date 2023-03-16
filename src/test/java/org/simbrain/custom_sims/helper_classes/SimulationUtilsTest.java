package org.simbrain.custom_sims.helper_classes;

import org.junit.jupiter.api.Test;
import org.simbrain.workspace.gui.SimbrainDesktop;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimulationUtilsTest {

    @Test
    public void testAddWorld() {
        SimulationUtils sim = new SimulationUtils(SimbrainDesktop.INSTANCE);
        sim.addOdorWorld(0,0,100,100, "Test");

        assertEquals(1, sim.getWorkspace().getComponentList().size());

    }
}