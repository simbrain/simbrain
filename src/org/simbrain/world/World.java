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
 * <b>World</b>
 */
public interface World {
    public String getType();

    public String getName();

    public ArrayList getAgentList();

    public JMenu getMotorCommandMenu(ActionListener al);

    public JMenu getSensorIdMenu(ActionListener al);

    //    TODO: Is this the right design?
    //        worlds have lists of targets that, when they are
    //        updated, they update
    public void addCommandTarget(NetworkPanel net);

    public void removeCommandTarget(NetworkPanel net);

    public ArrayList getCommandTargets();
}
