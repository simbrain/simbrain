package org.simbrain.custom_sims.simulations.rl_sim;

import java.util.ArrayList;
import java.util.List;

import org.simbrain.custom_sims.helper_classes.ControlPanel;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Superclass for particular world configuration, which sets the mouse and
 * object positions and smell properties, as well as setting the goal objects.
 */
// CHECKSTYLE:OFF
public abstract class RL_Sim {

    /** Back reference to main simulation object. */
    RL_Sim_Main sim;

    /** Sub-control panel for this RL_Sim. */
    ControlPanel controls = new ControlPanel();

    /**
     * Initial mouse location in this world. Every time a new trial is run the
     * mouse is set to these values.
     */
    int mouse_x;
    int mouse_y;
    int mouse_heading;

    /** Goal entities.  When the agent reaches one of these a trial ends.  */
    List<OdorWorldEntity> goalEntities = new ArrayList<OdorWorldEntity>();

    /**
     * Construct an RL sim with a reference to the main simulation object.
     *
     * @param mainSim the main rl sim, with reference to relevant objects.
     */
    public RL_Sim(RL_Sim_Main mainSim) {
        this.sim = mainSim;
    }

    /**
     * Load the simulation.
     */
    public abstract void load();

}
