package org.simbrain.custom_sims.simulations.creatures;

import org.simbrain.custom_sims.RegisteredSimulation;
import org.simbrain.network.core.NetworkUpdateAction;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.gui.SimbrainDesktop;
import org.simbrain.workspace.updater.UpdateActionAdapter;

/**
 * A simulation of an A-Life agent based off of the Creatures entertainment
 * software by Cyberlife Technology & Steve Grand, as seen in "Creatures:
 * Entertainment software agents with artificial life" (D. Cliff & S. Grand,
 * 1998)
 * 
 * @author Sharai
 *
 */
public class Creatures extends RegisteredSimulation {

    /**
     * This is a constructor.
     * 
     * @param desktop The Simbrain application
     */
    public Creatures(SimbrainDesktop desktop) {
        super(desktop);
    }

    /**
     * This, too, is a constructor.
     */
    public Creatures() {
        super();
    }

    @Override
    public void run() {
        // Clear workspace
        sim.getWorkspace().clearWorkspace();

        // Add docviewer
        sim.addDocViewer(0, 0, 450, 600, "Doc",
                "src/org/simbrain/custom_sims/simulations/creatures/CreaturesMain.html");

        sim.getWorkspace().addUpdateAction(
                new UpdateActionAdapter("Update Creatures Sim") {
                    @Override
                    public void invoke() {
                        updateCreaturesSim();
                    }
                });

    }
    
    void updateCreaturesSim() {
        System.out.println("Here I am!");
    }

    /**
     * Runs the constructor for the simulation.
     */
    @Override
    public Creatures instantiate(SimbrainDesktop desktop) {
        return new Creatures(desktop);
    }

    /**
     * Supplies the name of the simulation.
     */
    @Override
    public String getName() {
        return "Creatures";
    }

}
