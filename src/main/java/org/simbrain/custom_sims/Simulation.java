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

import org.simbrain.custom_sims.helper_classes.SimulationUtils;
import org.simbrain.workspace.gui.SimbrainDesktop;

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
public abstract class Simulation {

    /**
     * The main simulation object.
     */
    protected final SimulationUtils sim;

    /**
     * No argument constructor used for registering the simulation.
     */
    protected Simulation() {
        sim = null;
    }

    /**
     * This constructor is called when the menu items are created in the
     * Simbrain desktop. Called by instantiate.
     *
     * @param desk the simbrain desktop where this will be instantiated
     */
    protected Simulation(final SimbrainDesktop desk) {
        if (desk == null) {
            sim = null;
        } else {
            sim = new SimulationUtils(desk);
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
     * Run the simulation.
     */
    public abstract void run();

    /**
     * Instantiates a registered simulation class the same as the caller.
     *
     * @param desktop the simbrain desktop object where the instantiated sim
     *                will exist
     * @return An instance of this registered simulation (ideally of the same
     * type) that has its Simbrain desktop parameter initialized to a
     * non null
     */
    public abstract Simulation instantiate(SimbrainDesktop desktop);


}
