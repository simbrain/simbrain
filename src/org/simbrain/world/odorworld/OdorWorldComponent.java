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

import org.simbrain.workspace.AttributeType;
import org.simbrain.workspace.PotentialConsumer;
import org.simbrain.workspace.PotentialProducer;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.effectors.RotationEffector;
import org.simbrain.world.odorworld.effectors.StraightMovementEffector;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;
import org.simbrain.world.odorworld.sensors.SmellSensor.SmellSensorGetter;

/**
 * <b>WorldPanel</b> is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in
 * {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent {

    /** Reference to model world. */
    private OdorWorld world = new OdorWorld();

    /** Attribute types. */
    AttributeType xLocationType = (new AttributeType(this, "Location", "X", float.class, false));
    AttributeType yLocationType = (new AttributeType(this, "Location", "Y", float.class, false));
    AttributeType leftRotationType = (new AttributeType(this, "Left", "TurnAmount", double.class, true));
    AttributeType rightRotationType = (new AttributeType(this, "Right", "TurnAmount", double.class, true));
    AttributeType straightMovementType = (new AttributeType(this, "Straight", "MovementAmount", double.class, true));
    AttributeType smellSensorType = (new AttributeType(this, "Smell-Sensor", "Value", double.class, true));

    /**
     * Default constructor.
     */
    public OdorWorldComponent(final String name) {
        super(name);
        initializeAttributes();
        addListener();
    }

    /**
     * Constructor used in deserializing.
     *
     * @param name name of world
     * @param world model world
     */
    public OdorWorldComponent(final String name, final OdorWorld world) {
        super(name);
        this.world = world;
        initializeAttributes();
        addListener();
    }

    /**
     * Initialize odor world attributes.
     */
    private void initializeAttributes() {

        addConsumerType(xLocationType);
        addConsumerType(yLocationType);
        addConsumerType(leftRotationType);
        addConsumerType(rightRotationType);
        addConsumerType(straightMovementType);

        addProducerType(xLocationType);
        addProducerType(yLocationType);
        addProducerType(smellSensorType);
    }

    @Override
    public List<PotentialConsumer> getPotentialConsumers() {

        List<PotentialConsumer> returnList = new ArrayList<PotentialConsumer>();

        for (OdorWorldEntity entity : world.getObjectList()) {

            // X, Y Locations
            if (xLocationType.isVisible()) {
                String description = entity.getName() + ":" + xLocationType.getDescription();
                returnList.add(getAttributeManager().createPotentialConsumer(entity, xLocationType, description));
            }
            if (yLocationType.isVisible()) {
                String description = entity.getName() + ":" + yLocationType.getDescription();
                returnList.add(getAttributeManager().createPotentialConsumer(entity, yLocationType, description));
            }

            // Turning and Going Straight
            for (Effector effector : entity.getEffectors()) {
                if (effector instanceof RotationEffector) {
                    RotationEffector rotator = (RotationEffector) effector;
                    if (rotator.getScaleFactor() < 0) {
                        if (leftRotationType.isVisible()) {
                            String description = entity.getName() + ":" + leftRotationType.getSimpleDescription();
                            returnList.add(getAttributeManager().createPotentialConsumer(effector, leftRotationType, description));
                        }
                    }
                    if (rotator.getScaleFactor() > 0) {
                        if (rightRotationType.isVisible()) {
                            String description = entity.getName() + ":" + rightRotationType.getSimpleDescription();
                            returnList.add(getAttributeManager().createPotentialConsumer(effector, rightRotationType, description));
                        }
                    }
                } else if (effector instanceof StraightMovementEffector) {
                    if (straightMovementType.isVisible()) {
                        String description = entity.getName() + ":" + straightMovementType.getSimpleDescription();
                        returnList.add(getAttributeManager().createPotentialConsumer(effector, straightMovementType, description));
                    }
                }
            }
        }
        return returnList;
    }

    @Override
    public List<PotentialProducer> getPotentialProducers() {

        List<PotentialProducer> returnList = new ArrayList<PotentialProducer>();

        for (OdorWorldEntity entity : world.getObjectList()) {

            // X, Y Location of entities
            if (xLocationType.isVisible()) {
                String description = entity.getName() + ":" + xLocationType.getDescription();
                returnList.add(getAttributeManager().createPotentialProducer(entity, xLocationType, description));
            }
            if (yLocationType.isVisible()) {
                String description = entity.getName() + ":" + yLocationType.getDescription();
                returnList.add(getAttributeManager().createPotentialProducer(entity, yLocationType, description));
            }

            // Smell sensor
            if (smellSensorType.isVisible()) {
                for (Sensor sensor : entity.getSensors()) {
                    if (sensor instanceof SmellSensor) {
                        SmellSensor smell = (SmellSensor) sensor;
                        for (int i = 0; i < smell.getCurrentValue().length; i++) {
                            SmellSensorGetter getter =  smell.createGetter(i);
                            String description = smellSensorType.getSimpleDescription(entity
                                    .getName() + ":" + smell.getName() + "[" + i + "]");
                            returnList.add(getAttributeManager().createPotentialProducer(getter, smellSensorType, description));
                        }
                        // TODO: A way of indicating sensor location (relative
                        // location in polar coordinates)
                    }
                }
            }
        }
        return returnList;
    }


    /**
     * Initialize this component.
     */
    private void addListener() {
        world.addListener(new WorldListener() {

            public void updated() {
                fireUpdateEvent();
            }
            public void effectorAdded(final Effector effector) {
                firePotentialAttributesChanged();
            }

            public void effectorRemoved(final Effector effector) {
                fireAttributeObjectRemoved(effector);
                firePotentialAttributesChanged();
            }

            public void entityAdded(final OdorWorldEntity entity) {
                firePotentialAttributesChanged();
            }

            public void entityRemoved(final OdorWorldEntity entity) {
                fireAttributeObjectRemoved(entity);
                firePotentialAttributesChanged();
            }

            public void sensorAdded(final Sensor sensor) {
                firePotentialAttributesChanged();
            }

            public void sensorRemoved(Sensor sensor) {
                // TODO: Go through all smell sensor getters, and if any refer to this, 
                //  then remove that getter
                fireAttributeObjectRemoved(sensor); 
                firePotentialAttributesChanged();
            }
        });
    }

    /**
     * Return a smell sensor getter, or null if there is no matching object.
     * Helper method for scripts.
     *
     * @param rotatingEntity entity to match
     * @param name name of sensor (left, right, center) to match
     * @param i index of smell vector
     *
     * @return smell sensor getter wrapping this sensor
     */
    public SmellSensorGetter createSmellSensor(RotatingEntity rotatingEntity, String name, int i) {
        for (OdorWorldEntity entity : world.getObjectList()) {
            if (entity == rotatingEntity) {
                for (Sensor sensor : rotatingEntity.getSensors()) {
                    if (sensor instanceof SmellSensor) {
                        SmellSensor smellSensor = (SmellSensor) sensor;
                        if (smellSensor.getName().equalsIgnoreCase(name)) {
                            return smellSensor.createGetter(i);
                        }
                    }
                }
            }
        }
        return null;
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