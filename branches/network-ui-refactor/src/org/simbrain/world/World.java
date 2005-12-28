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

import java.awt.LayoutManager;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.event.EventListenerList;

import org.simbrain.network.NetworkPanel;
import org.simnet.interfaces.NetworkEvent;
import org.simnet.interfaces.NetworkListener;
import org.simnet.interfaces.Neuron;

/**
 * <b>World</b> is the abstract superclass of all worlds, which are components which interact
 * with neural networks.
 */
public abstract class World extends JPanel {

    /** List of components which listen for changes to this network. */
    private EventListenerList listenerList = new EventListenerList();

    /**
     * Return the type of this world.
     *
     * @return the type of this world
     */
    public abstract String getType();

    /**
     * Return the name of this world.
     *
     * @return the name of this world
     */
    public abstract String getWorldName();

    /**
     * Return the list of agents for this world.
     *
     * @return the list of agents for this world
     */
    public abstract ArrayList getAgentList();

    /**
     * Return a menu of motor commands for this world and
     * register the specified action listener as an action
     * listener for the menu items in the returned menu.
     *
     * @param actionListener action listener to register
     * @return a menu of motor command for this world
     */
    public abstract JMenu getMotorCommandMenu(ActionListener actionListener);

    /**
     * Return a menu of sensor ids for this world and register
     * the specified action listener as an action listener for
     * the menu items in the returned menu.
     *
     * @param actionListener action listener to register
     * @return a menu of sensor ids for this world
     */
    public abstract JMenu getSensorIdMenu(ActionListener actionListener);

    public World() {
        super();
    }

    public World(final LayoutManager layout) {
        super(layout);
    }

    /**
     * Notify all world listeners that this world has changed.
     */
    public void fireWorldChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i-= 2) {
            if (listeners[i]==WorldListener.class) {
                // Lazily create the event:
                ((WorldListener)listeners[i+1]).worldChanged();
            }
        }
    }

    /**
     * Add the specified world listener.
     *
     * @param wl listener to add
     */
    public void addWorldListener(final WorldListener wl) {
        listenerList.add(WorldListener.class, wl);
    }

    /**
     * Remove the specified world listener.
     *
     * @param wl listener to remove
     */
    public void removeWorldListener(final WorldListener wl) {
        listenerList.remove(WorldListener.class, wl);
    }

}
