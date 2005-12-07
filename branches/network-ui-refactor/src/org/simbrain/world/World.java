/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world;

import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenu;

import org.simbrain.network.NetworkPanel;

/**
 * <b>World</b>.
 */
public interface World {

    /**
     * Return the type of this world.
     *
     * @return the type of this world
     */
    String getType();

    /**
     * Return the name of this world.
     *
     * @return the name of this world
     */
    String getName();

    /**
     * Return the list of agents for this world.
     *
     * @return the list of agents for this world
     */
    ArrayList getAgentList();

    /**
     * Return a menu of motor commands for this world and
     * register the specified action listener as an action
     * listener for the menu items in the returned menu.
     *
     * @param actionListener action listener to register
     * @return a menu of motor command for this world
     */
    JMenu getMotorCommandMenu(ActionListener actionListener);

    /**
     * Return a menu of sensor ids for this world and register
     * the specified action listener as an action listener for
     * the menu items in the returned menu.
     *
     * @param actionListener action listener to register
     * @return a menu of sensor ids for this world
     */
    JMenu getSensorIdMenu(ActionListener actionListener);

    //    TODO: Is this the right design?
    //        worlds have lists of targets that, when they are
    //        updated, they update

    /**
     * Add the specified network panel to this world's list
     * of command targets.
     *
     * @param networkPanel network panel to add
     */
    void addCommandTarget(NetworkPanel networkPanel);

    /**
     * Remove the specified network panel from this world's
     * list of command targets.
     *
     * @param networkPanel network panel to remove
     */
    void removeCommandTarget(NetworkPanel networkPanel);

    /**
     * Return the list of command targets for this world.
     *
     * @return the list of command targets for this world
     */
    ArrayList getCommandTargets();
}
