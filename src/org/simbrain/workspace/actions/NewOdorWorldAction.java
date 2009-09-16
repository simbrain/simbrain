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
package org.simbrain.workspace.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.simbrain.util.SimbrainMath;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.workspace.Workspace;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.OdorWorldComponent;
import org.simbrain.world.odorworld.entities.BasicEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Add odor world to workspace.
 */
public final class NewOdorWorldAction extends WorkspaceAction {

    private static final long serialVersionUID = 1L;

    /**
     * Create a new odor world action with the specified
     * workspace.
     */
    public NewOdorWorldAction(Workspace workspace) {
        super("2D World", workspace);
    }

    /** @see AbstractAction */
    public void actionPerformed(final ActionEvent event) {
        OdorWorldComponent worldComponent = new OdorWorldComponent("");
        workspace.addWorkspaceComponent(worldComponent);
        OdorWorld world = worldComponent.getWorld();
        createDefaultWorld(world);
    }

    /**
     * Create a default odor world.
     */
    private void createDefaultWorld(OdorWorld world) {
        // Add agent to environment
        RotatingEntity mouse = new RotatingEntity(world);
        mouse.setLocation(162, 200);
        world.addAgent(mouse);
        // TODO: Above is repeated code. It should be replaced by "addAgent" or
        // some such...

        // Add objects
        BasicEntity object1 = new BasicEntity("Swiss.gif", world);
        object1.setLocation(36, 107);
        object1.setSmellSource(new SmellSource(SimbrainMath.multVector(
                new double[] { 0.7, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0 }, 100),
                SmellSource.DecayFunction.GAUSSIAN, object1.getLocation()));
        world.addEntity(object1);

        BasicEntity object2 = new BasicEntity("Gouda.gif", world);
        object2.setLocation(169, 32);
        object2.setSmellSource(new SmellSource(SimbrainMath.multVector(
                new double[] { 0.7, 0.0, 0.3, 0.0, 0.0, 0.0, 0.0, 0.0 }, 100),
                SmellSource.DecayFunction.GAUSSIAN, object1.getLocation()));
        world.addEntity(object2);

        BasicEntity object3 = new BasicEntity("Bluecheese.gif", world);
        object3.setLocation(304, 87);
        object3.setSmellSource(new SmellSource(SimbrainMath.multVector(
                new double[] { 0.7, 0.0, 0.0, 0.0, 0.3, 0.0, 0.0, 0.0 }, 100),
                SmellSource.DecayFunction.GAUSSIAN, object1.getLocation()));
        world.addEntity(object3);

        BasicEntity object4 = new BasicEntity("Tulip.gif", world);
        object4.setLocation(80, 351);
        object4.setSmellSource(new SmellSource(SimbrainMath.multVector(
                new double[] { 0.0, 0.3, 0.0, 0.7, 0.0, 0.0, 0.0, 0.0 }, 100),
                SmellSource.DecayFunction.GAUSSIAN, object1.getLocation()));
        world.addEntity(object4);

        BasicEntity object5 = new BasicEntity("Pansy.gif", world);
        object5.setLocation(251, 370);
        object5.setSmellSource(new SmellSource(SimbrainMath.multVector(
                new double[] { 0.0, 0.0, 0.3, 0.7, 0.0, 0.0, 0.0, 0.0 }, 100),
                SmellSource.DecayFunction.GAUSSIAN, object1.getLocation()));
        world.addEntity(object5);
    }

}