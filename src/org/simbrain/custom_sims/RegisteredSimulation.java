/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.custom_sims;

import org.simbrain.custom_sims.helper_classes.Simulation;
import org.simbrain.custom_sims.simulations.actor_critic.ActorCritic;
import org.simbrain.custom_sims.simulations.agent_trails.AgentTrails;
import org.simbrain.custom_sims.simulations.behaviorism.Behaviorism;
import org.simbrain.custom_sims.simulations.behaviorism.Behaviorism2;
import org.simbrain.custom_sims.simulations.cortex_simple.CortexSimple;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Super class for all custom simulations. Also has code to manage custom
 * simulations.
 * 
 * All registered simulations must be added to REGISTERED_SIMS to
 * show up in Simbrain's menu.
 *
 * @author ztosi
 * @author jyoshimi
 *
 */
public abstract class RegisteredSimulation {

    /**
     * The list used by calling classes to determine what custom simulations are
     * available. Any custom simulations added to simbrain must be added to this
     * list in order to show up in the gui menu. All items are sorted
     * alphabetically by name.
     */
    public static final List<RegisteredSimulation> REGISTERED_SIMS = new ArrayList<>();

    /** The main simulation object. */
    protected final Simulation sim;

    static {
        // TODO: Commented out items are not ready for prime time
        // REGISTERED_SIMS.add(new EdgeOfChaos());
        // REGISTERED_SIMS.add(new EdgeOfChaosBitStream());
        // REGISTERED_SIMS.add(new Hippocampus());
        // REGISTERED_SIMS.add(new RL_Sim_Main());
        // REGISTERED_SIMS.add(new Cerebellum());
        REGISTERED_SIMS.add(new AgentTrails());
        REGISTERED_SIMS.add(new ActorCritic());
        REGISTERED_SIMS.add(new CortexSimple());
        // REGISTERED_SIMS.add(new CreaturesSim());
        // REGISTERED_SIMS.add(new MpfsSOM());
        // REGISTERED_SIMS.add(new SimpleNeuroevolution());
        REGISTERED_SIMS.add(new Behaviorism());
        REGISTERED_SIMS.add(new Behaviorism2());

        // Alphabetize
        REGISTERED_SIMS
                .sort(Comparator.comparing(RegisteredSimulation::getName));
    }

    /**
     * No argument constructor used for registering the simulation.
     */
    protected RegisteredSimulation() {
        sim = null;
    }

    /**
     * This constructor is called when the menu items are created in the
     * Simbrain desktop. Called by instantiate.
     *
     * @param desk the simbrain desktop where this will be instantiated
     */
    protected RegisteredSimulation(final SimbrainDesktop desk) {
        sim = new Simulation(desk);
    }

    /**
     * A method for ensuring that a caller can assign a name to a custom
     * simulations for display purposes.
     *
     * @return the name of the custom simulation
     */
    public abstract String getName();

    /**
     * Run the simulation.
     */
    public abstract void run();

    /**
     * Instantiates a registered simulation class the same as the caller.
     *
     * @param desktop the simbrain desktop object where the instantiated sim
     *            will exist
     * @return An instance of this registered simulation (ideally of the same
     *         type) that has its Simbrain desktop parameter initialized to a
     *         non null
     */
    public abstract RegisteredSimulation instantiate(SimbrainDesktop desktop);

    /**
     * Add a registered simulation to this list of available registered
     * simulations.
     *
     * @param rs the registered simulation to add to the directory of registered
     *            sims
     */
    public static void register(final RegisteredSimulation rs) {
        if (!REGISTERED_SIMS.contains(rs)) {
            REGISTERED_SIMS.add(rs);
        }
    }

}
