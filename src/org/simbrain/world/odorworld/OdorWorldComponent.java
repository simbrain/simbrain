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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;

/**
 * <b>WorldPanel</b> is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in
 * {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent {

    /** Reference to model world. */
    private OdorWorld world;

    /**
     * Default constructor.
     * 
     * @param name
     */
    public OdorWorldComponent(String name) {
        super(name);
        world = new OdorWorld();
        addListener();
    }

    /**
     * Constructor used in deserializing.
     *
     * @param name name of world
     * @param world model world
     */
    public OdorWorldComponent(String name, OdorWorld world) {
        super(name);
        this.world = world;
        addListener();
    }

    /**
     * Initialize this component.
     */
    private void addListener() {
        world.addListener(new WorldListener() {

            public void updated() {
                fireUpdateEvent();
            }

            public void effectorAdded(Effector effector) {
                setChangedSinceLastSave(true);
            }

            public void effectorRemoved(Effector effector) {
                setChangedSinceLastSave(true);
            }

            public void entityAdded(OdorWorldEntity entity) {
                setChangedSinceLastSave(true);
            }

            public void entityRemoved(OdorWorldEntity entity) {
                setChangedSinceLastSave(true);
            }

            public void sensorAdded(Sensor sensor) {
                setChangedSinceLastSave(true);
            }

            public void sensorRemoved(Sensor sensor) {
                setChangedSinceLastSave(true);
            }

            public void entityChanged(OdorWorldEntity entity) {
                setChangedSinceLastSave(true);
            }

            public void propertyChanged() {
                setChangedSinceLastSave(true);
            }
        });
    }

    /**
     * Recreates an instance of this class from a saved component.
     *
     * @param input
     * @param name
     * @param format
     * @return
     */
    public static OdorWorldComponent open(InputStream input, String name, String format) {
        OdorWorld newWorld = (OdorWorld) OdorWorld.getXStream().fromXML(input);
        return new OdorWorldComponent(name, newWorld);
    }

    @Override
    public String getXML() {
        return OdorWorld.getXStream().toXML(world);
    }

    @Override
    public void save(OutputStream output, String format) {
        OdorWorld.getXStream().toXML(world, output);
    }

    @Override
    public String getKeyFromObject(Object object) {
        if (object instanceof OdorWorldEntity) {
            return ((OdorWorldEntity) object).getId();
        } else if (object instanceof Sensor) {
            String entityName = ((Sensor) object).getParent().getName();
            String sensorName = ((Sensor) object).getId();
            return entityName + ":sensor:" + sensorName;
        } else if (object instanceof Effector) {
            String entityName = ((Effector) object).getParent().getName();
            String effectorName = ((Effector) object).getId();
            return entityName + ":effector:" + effectorName;
        }
        return null;
    }

    @Override
    public Object getObjectFromKey(String objectKey) {
        String[] parsedKey = objectKey.split(":");
        String entityName = parsedKey[0];
        if (parsedKey.length == 1) {
            return getWorld().getEntity(entityName);
        } else {
            String secondString = parsedKey[1];
            if (secondString.equalsIgnoreCase("sensor")) {
                return getWorld().getSensor(entityName, parsedKey[2]);
            } else if (secondString.equalsIgnoreCase("effector")) {
                return getWorld().getEffector(entityName, parsedKey[2]);
            } else if (secondString.equalsIgnoreCase("smellSensorGetter")) {
                // Needed to read simulations created before 2/11; remove before
                // beta release
                int index = Integer.parseInt(parsedKey[3]);
                return getWorld().getSensor(entityName, parsedKey[2]);
            } else if (secondString.equalsIgnoreCase("smeller")) {
                int index = Integer.parseInt(parsedKey[3]);
                return getWorld().getSensor(entityName, parsedKey[2]);
            }
        }
        return null;
    }

    @Override
    public void closing() {}

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
        for (OdorWorldEntity entity : world.getObjectList()) {
            models.add(entity);
            models.addAll(entity.getSensors());
            models.addAll(entity.getEffectors());
        }
        return models;
    }
}
