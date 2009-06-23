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

import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.attributes.EntityWrapper;
import org.simbrain.world.odorworld.attributes.RotationConsumer;
import org.simbrain.world.odorworld.attributes.SmellProducer;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.effectors.RotationEffector;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * <b>WorldPanel</b> is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in
 * {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent {

    /** Reference to model world. */
    private OdorWorld world = new OdorWorld();
    
    /**
     * Default constructor.
     */
    public OdorWorldComponent(final String name) {
        super(name);
        init();
    }
    
    @SuppressWarnings("unchecked")
    private OdorWorldComponent(final String name, final OdorWorld world) {
        super(name);
        this.world = world;
        init();
    }

    /**
     * Initialize this component.
     */
    private void init() {
        this.setAttributeListingStyle(AttributeListingStyle.TOTAL);
        world.addListener(new WorldListener() {
            public void updated() {
                fireUpdateEvent();
            }

            public void effectorAdded(final Effector effector) {
                if (effector instanceof RotationEffector) {
                    addConsumer(new RotationConsumer(OdorWorldComponent.this,
                            (RotationEffector) effector));
                }
            }

            public void effectorRemoved(final Effector effector) {
                // TODO Auto-generated method stub
                
            }

            public void entityAdded(final OdorWorldEntity entity) {
                addConsumer(new EntityWrapper(OdorWorldComponent.this, entity));
                addProducer(new EntityWrapper(OdorWorldComponent.this, entity));
            }

            public void entityRemoved(final OdorWorldEntity entity) {
                // TODO Auto-generated method stub
            }

            public void sensorAdded(final Sensor sensor) {
                if (sensor instanceof SmellSensor) {
                    addProducer(new SmellProducer(OdorWorldComponent.this, (SmellSensor) sensor));
                }
            }

            public void sensorRemoved(Sensor sensor) {
                // TODO Auto-generated method stub
                
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
        OdorWorld.getXStream().toXML(world, output);
    }
    
    @Override
    public void closing() {
        // TODO Auto-generated method stub
    }
    
    @Override
    public void update() {
        world.update();
    }
    
    @Override
    public void setCurrentDirectory(final String currentDirectory) { 
        super.setCurrentDirectory(currentDirectory);
        OdorWorldPreferences.setCurrentDirectory(currentDirectory);
    }
    
    @Override
    public String getCurrentDirectory() {
       return OdorWorldPreferences.getCurrentDirectory();
    }
    
    /**
     * Returns a reference to the odor world.
     *
     * @return the odor world object.
     */
    public OdorWorld getWorld() {
        return world;
    }
}