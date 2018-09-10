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
package org.simbrain.world.odorworld;

import org.simbrain.util.Utils;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.gui.EntityNode;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * <b>WorldPanel</b> is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in
 * {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent {
    /**
     * Recreates an instance of this class from a saved component.
     *
     * @param input
     * @param name
     * @param format
     * @return
     */
    public static OdorWorldComponent open(InputStream input, String name, String format) {
        OdorWorld newWorld = (OdorWorld) Utils.getSimbrainXStream().fromXML(input);
        return new OdorWorldComponent(name, newWorld);
    }

    /**
     * Reference to model world.
     */
    private OdorWorld world;

    /**
     * Timer to update entity animations.
     */
    private Timer animationTimer;

    /**
     * Default constructor.
     *
     * @param name
     */
    public OdorWorldComponent(String name) {
        super(name);
        world = new OdorWorld();
        init();
    }

    /**
     * Constructor used in deserializing.
     *
     * @param name  name of world
     * @param world model world
     */
    public OdorWorldComponent(String name, OdorWorld world) {
        super(name);
        this.world = world;
        init();
    }

    private void init() {
        world.addPropertyChangeListener(evt -> {
            if ("entityAdded".equals(evt.getPropertyName())) {
                fireModelAdded(evt.getNewValue());
            }
            if ("entityDeleted".equals(evt.getPropertyName())) {
                fireModelRemoved(evt.getNewValue());
            }
        });
    }

    @Override
    public String getXML() {
        return Utils.getSimbrainXStream().toXML(world);
    }

    @Override
    public void save(OutputStream output, String format) {
        Utils.getSimbrainXStream().toXML(world, output);
    }

    @Override
    public Object getObjectFromKey(String objectKey) {

        //System.out.println("-->" + objectKey);
        if (objectKey.startsWith("Entity")) {
            return getWorld().getEntity(objectKey);
        } else if (objectKey.startsWith("Sensor")) {
            return getWorld().getSensor(objectKey);
        } else if (objectKey.startsWith("Effector")) {
            return getWorld().getEffector(objectKey);
        }
        return null;
    }

    @Override
    public void closing() {
    }

    @Override
    public void update() {
        world.update(this.getWorkspace().getTime());
    }

    /**
     * Returns a reference to the odor world.
     *
     * @return the odor world object.
     */
    public OdorWorld getWorld() {
        return world;
    }

    @Override
    public List getModels() {
        List<Object> models = new ArrayList<Object>();
        for (OdorWorldEntity entity : world.getEntityList()) {
            models.add(entity);
            models.addAll(entity.getSensors());
            models.addAll(entity.getEffectors());
        }
        return models;
    }

    @Override
    public void start() {
        animationTimer = new Timer();
        animationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                world.advance();
            }
        },50, 50);
    }

    @Override
    public void stop() {
        animationTimer.cancel();
        world.stopAnimation();
    }
}
