package org.simbrain.custom_sims;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.custom_sims.simulations.actor_critic.ActorCritic;
import org.simbrain.custom_sims.simulations.agent_trails.AgentTrails;
import org.simbrain.custom_sims.simulations.cerebellum.Cerebellum;
import org.simbrain.custom_sims.simulations.hippocampus.Hippocampus;
import org.simbrain.custom_sims.simulations.rl_sim.RL_Sim_Main;
import org.simbrain.custom_sims.simulations.test.TestSim;
import org.simbrain.workspace.gui.SimbrainDesktop;

/**
 *
 * An abstract class enforcing a small set of functions on any custom
 * simulations that inherit from it (they all should). These functions have to
 * do with expected behavior and information that a GUI would need to display
 * them as a menu option. All registered simulations muss be added to
 * REGISTERED_SIMS to show up in Simbrain's menu.
 *
 * @author ztosi
 *
 */
public abstract class RegisteredSimulation {

    /**
     * The list used by calling classes to determine what custom simulations are
     * available. Any custom simulations added to simbrain must be added to this
     * list in order to show up in the gui menu. All items are sorted
     * alphabetically by name.
     */
    public static final List<RegisteredSimulation> REGISTERED_SIMS =
            new ArrayList<>();
    static {
        REGISTERED_SIMS.add(new TestSim());
        REGISTERED_SIMS.add(new Hippocampus());
        REGISTERED_SIMS.add(new RL_Sim_Main());
        REGISTERED_SIMS.add(new Cerebellum());
        REGISTERED_SIMS.add(new AgentTrails());
        REGISTERED_SIMS.add(new ActorCritic());
        // Alphabetize
        REGISTERED_SIMS
                .sort(Comparator.comparing(RegisteredSimulation::getName));

    }

    /** The main simulation object. */
    protected final Simulation sim;

    /**
     *
     */
    protected RegisteredSimulation() {
        sim = null;
    }

    /**
     *
     * @param desk
     *            the simbrain desktop where this will be intstantiated
     */
    protected RegisteredSimulation(SimbrainDesktop desk) {
        sim = new Simulation(desk);
    }

    /**
     * A method for ensuring that a caller can assign a name to your (and all
     * other) custom simulations for display purposes.
     *
     * @return the name of the custom simulation
     */
    public abstract String getName();

    /**
     * TODO: rename "build" ?
     */
    public abstract void run();

    /**
     * Instantiates a registered simulation class the same as the caller.
     *
     * @param desktop
     *            the simbrain desktop object where the instantiated sim will
     *            exist
     * @return An instance of this registered simulation (ideally of the same
     *         type) that has its Simbrain desktop parameter initialized to a
     *         non null
     */
    public abstract RegisteredSimulation instantiate(SimbrainDesktop desktop);


    /**
     * Add a registered simulation to this list of available registered
     * simulations.
     *
     * @param rs
     *            the registered simulation to add to the directory of
     *            registered sims
     */
    public static void register(RegisteredSimulation rs) {
        if (!REGISTERED_SIMS.contains(rs)) {
            REGISTERED_SIMS.add(rs);

            // Alphabetize
            REGISTERED_SIMS
                    .sort(Comparator.comparing(RegisteredSimulation::getName));

        }
    }

}
