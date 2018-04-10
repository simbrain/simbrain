package org.simbrain.custom_sims.simulations.neat;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.workspace.gui.SimbrainDesktop;

public class NEAT extends RegisteredSimulation {

    public NEAT() {
        super();
    }

    public NEAT(SimbrainDesktop desktop) {
        super(desktop);
    }

    @Override
    public String getName() {
        return "NEAT";
    }

    @Override
    public void run() {
        // TODO Auto-generated method stub

    }

    @Override
    public RegisteredSimulation instantiate(SimbrainDesktop desktop) {
        return new NEAT(desktop);
    }

}
