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
import org.simbrain.custom_sims.simulations.*;
import org.simbrain.custom_sims.simulations.actor_critic.ActorCritic;
import org.simbrain.custom_sims.simulations.agent_trails.AgentTrails;
import org.simbrain.custom_sims.simulations.agent_trails.RandomizedPursuer;
import org.simbrain.custom_sims.simulations.behaviorism.ClassicalConditioning;
import org.simbrain.custom_sims.simulations.behaviorism.OperantConditioning;
import org.simbrain.custom_sims.simulations.behaviorism.OperantWithEnvironment;
import org.simbrain.custom_sims.simulations.behaviorism.SimpleOperant;
import org.simbrain.custom_sims.simulations.braitenberg.Braitenberg;
import org.simbrain.custom_sims.simulations.cerebellum.Cerebellum;
import org.simbrain.custom_sims.simulations.cortex.CortexSimple;
import org.simbrain.custom_sims.simulations.cortex.CorticalBranching;
import org.simbrain.custom_sims.simulations.creatures.CreaturesSim;
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaos;
import org.simbrain.custom_sims.simulations.edge_of_chaos.EdgeOfChaosBitStream;
import org.simbrain.custom_sims.simulations.hippocampus.Hippocampus;
import org.simbrain.custom_sims.simulations.patterns_of_activity.KuramotoOscillators;
import org.simbrain.custom_sims.simulations.patterns_of_activity.ModularOscillatoryNetwork;
import org.simbrain.custom_sims.simulations.patterns_of_activity.PatternsOfActivity;
import org.simbrain.custom_sims.simulations.rl_sim.RL_Sim_Main;
import org.simbrain.custom_sims.simulations.sorn.SORN;
import org.simbrain.custom_sims.simulations.test.*;
import org.simbrain.workspace.Workspace;
import org.simbrain.workspace.gui.SimbrainDesktop;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Super class for all custom simulations. Also has code to manage custom
 * simulations.
 * <p>
 * All registered simulations must be added to REGISTERED_SIMS to
 * show up in Simbrain's menu.
 *
 * @author ztosi
 * @author jyoshimi
 */
public abstract class RegisteredSimulation {

    /**
     * The list used by calling classes to determine what custom simulations are
     * available. Any custom simulations added to simbrain must be added to this
     * list in order to show up in the gui menu. All items are sorted
     * alphabetically by name.
     */
    private static final List<RegisteredSimulation> REGISTERED_SIMS = new ArrayList<>();

    /**
     * The main simulation object.
     */
    protected final Simulation sim;

    /**
     * Handle simulation completion events. See {@link #simulationCompleted()}.
     */
    private Runnable completionHandler;

    static {
        // TODO: Commented out items are not ready for prime time
        REGISTERED_SIMS.add(new EdgeOfChaos());
        REGISTERED_SIMS.add(new EdgeOfChaosBitStream());
        REGISTERED_SIMS.add(new Hippocampus());
        REGISTERED_SIMS.add(new RL_Sim_Main());
        REGISTERED_SIMS.add(new Cerebellum());
        REGISTERED_SIMS.add(new CreaturesSim());
        REGISTERED_SIMS.add(new AgentTrails());
        REGISTERED_SIMS.add(new ActorCritic());
        REGISTERED_SIMS.add(new OperantWithEnvironment());
        REGISTERED_SIMS.add(new ClassicalConditioning());
        REGISTERED_SIMS.add(new OperantConditioning());
        REGISTERED_SIMS.add(new SimpleOperant());
        REGISTERED_SIMS.add(new CorticalBranching());
        REGISTERED_SIMS.add(new CortexSimple());
        REGISTERED_SIMS.add(new ConvertSim());
        REGISTERED_SIMS.add(new ReadSim());
        REGISTERED_SIMS.add(new ModularOscillatoryNetwork());
        REGISTERED_SIMS.add(new RandomizedPursuer());
        REGISTERED_SIMS.add(new PatternsOfActivity());
        REGISTERED_SIMS.add(new KuramotoOscillators());
        REGISTERED_SIMS.add(new SORN());
        REGISTERED_SIMS.add(new TestSim());
        REGISTERED_SIMS.add(new lstmBlock());
        REGISTERED_SIMS.add(new Braitenberg());
        REGISTERED_SIMS.add(new EvolveAutoEncoder(null));
        REGISTERED_SIMS.add(new EvolveXor(null));
        REGISTERED_SIMS.add(new EvolveMouse(null));
        REGISTERED_SIMS.add(new EvolveAvoider(null));
        REGISTERED_SIMS.add(new EvolveNetwork(null));

        // Alphabetize
        // TODO: Find a way to sort by submenu name as well.
        REGISTERED_SIMS.sort(Comparator.comparing(RegisteredSimulation::getSubmenuName));
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
        if (desk == null) {
            sim = null;
        } else {
            sim = new Simulation(desk);
        }
    }

    /**
     * A method for ensuring that a caller can assign a name to a custom
     * simulations for display purposes.
     *
     * @return the name of the custom simulation
     */
    public abstract String getName();

    /**
     * Override to return a submenu name. If it is set, the simulation will be placed in a menu of that name.
     *
     * @return the name of the submenu used, if any
     */
    public String getSubmenuName() {
        return null;
    };

    /**
     * Run the simulation.
     */
    public abstract void run();

    /**
     * See {@link #simulationCompleted()}.
     */
    public void onCompleted(Runnable handler) {
        completionHandler = handler;
    }

    /**
     * Call when the simulation is completed for proper termination when run outside of GUI.
     */
    public void simulationCompleted() {
        if (completionHandler != null) {
            completionHandler.run();
        }
    };

    /**
     * Instantiates a registered simulation class the same as the caller.
     *
     * @param desktop the simbrain desktop object where the instantiated sim
     *                will exist
     * @return An instance of this registered simulation (ideally of the same
     * type) that has its Simbrain desktop parameter initialized to a
     * non null
     */
    public abstract RegisteredSimulation instantiate(SimbrainDesktop desktop);

    /**
     * Add a registered simulation to this list of available registered
     * simulations.
     *
     * @param rs the registered simulation to add to the directory of registered
     *           sims
     */
    public static void register(final RegisteredSimulation rs) {
        if (!REGISTERED_SIMS.contains(rs)) {
            REGISTERED_SIMS.add(rs);
        }
    }

    public static List<RegisteredSimulation> getRegisteredSims() {
        return REGISTERED_SIMS;
    }

    /**
     * This can be called from gradle using "runSim"
     */
    public static void main(String[] args) {
       run(args[0]);
    }

    /**
     * Run a simulation with the given name, outside the GUI.  Used, for example, to run
     * evolution scripts on a server.
     *
     * @param simName the name of the simulation to run
     */
    public static void run(String simName) {
        RegisteredSimulation sim = REGISTERED_SIMS.stream()
                .filter(s -> s.getName().equals(simName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Simulation named " + simName + " does not exist"));

        sim = sim.instantiate(new SimbrainDesktop(new Workspace()));
        sim.onCompleted(() -> {
            System.exit(0);
        });
        sim.run();

    }
}
