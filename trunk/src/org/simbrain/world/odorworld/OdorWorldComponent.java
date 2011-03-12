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
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;
import org.simbrain.world.odorworld.sensors.TileSensor;

/**
 * <b>WorldPanel</b> is the container for the world component. Handles toolbar
 * buttons, and serializing of world data. The main environment codes is in
 * {@link OdorWorldPanel}.
 */
public class OdorWorldComponent extends WorkspaceComponent {

    /** Reference to model world. */
    private OdorWorld world = new OdorWorld();

    /** Attribute types. */
    AttributeType xLocationType = (new AttributeType(this, "Location", "X", double.class, false));
    AttributeType yLocationType = (new AttributeType(this, "Location", "Y", double.class, false));
    AttributeType leftRotationType = (new AttributeType(this, "Left", double.class, true));
    AttributeType rightRotationType = (new AttributeType(this, "Right", double.class, true));
    AttributeType straightMovementType = (new AttributeType(this, "Straight", double.class, true));
    AttributeType absoluteMovementType = (new AttributeType(this, "Absolute-movement", double.class, false));
    AttributeType smellSensorType = (new AttributeType(this, "Smell", double.class, true));
    AttributeType tileSensorType = (new AttributeType(this, "Tile", double.class, false));

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
        addConsumerType(absoluteMovementType);

        addProducerType(xLocationType);
        addProducerType(yLocationType);
        addProducerType(smellSensorType);
        addProducerType(tileSensorType);
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

            // Absolute movement
            if (absoluteMovementType.isVisible()) {
                returnList.add(getAttributeManager().createPotentialConsumer(entity, "moveNorth", double.class, entity.getName() + ":goNorth"));
                returnList.add(getAttributeManager().createPotentialConsumer(entity, "moveSouth", double.class, entity.getName() + ":goSouth"));
                returnList.add(getAttributeManager().createPotentialConsumer(entity, "moveEast", double.class, entity.getName() + ":goEast"));
                returnList.add(getAttributeManager().createPotentialConsumer(entity, "moveWest", double.class, entity.getName() + ":goWest"));
            }

            // Turning and Going Straight
            if (entity instanceof RotatingEntity) {
                returnList.add(getAttributeManager().createPotentialConsumer(entity, "turnLeft", double.class, entity.getName() + ":turnLeft"));
                returnList.add(getAttributeManager().createPotentialConsumer(entity, "turnRight", double.class, entity.getName() + ":turnRight"));
                returnList.add(getAttributeManager().createPotentialConsumer(entity, "goStraight", double.class, entity.getName() + ":goStraight"));
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
                returnList.add(getAttributeManager().createPotentialProducer(entity, "DoubleX", double.class, description));
            }
            if (yLocationType.isVisible()) {
                String description = entity.getName() + ":" + yLocationType.getDescription();
                returnList.add(getAttributeManager().createPotentialProducer(entity, "DoubleY", double.class, description));
            }

            // Smell sensor
            if (smellSensorType.isVisible()) {
                for (Sensor sensor : entity.getSensors()) {
                    if (sensor instanceof SmellSensor) {
                        SmellSensor smell = (SmellSensor) sensor;
                        for (int i = 0; i < smell.getCurrentValue().length; i++) {
                            returnList.add(new PotentialProducer(this, smell, "getCurrentValue",
                                            double.class, int.class, i, sensor.getLabel() + "-" + (i + 1)));
                        }
                        // TODO: A way of indicating sensor location (relative
                        // location in polar coordinates)
                    }
                }
            }

            // Tile sensor
            if (tileSensorType.isVisible()) {
                for (Sensor sensor : entity.getSensors()) {
                    if (sensor instanceof TileSensor) {
                        returnList.add(getAttributeManager()
                                .createPotentialProducer(sensor, "isActivated",
                                        double.class,
                                        ((TileSensor) sensor).getLabel()));
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
                setChangedSinceLastSave(true);
                firePotentialAttributesChanged();
            }

            public void effectorRemoved(final Effector effector) {
                setChangedSinceLastSave(true);
                fireAttributeObjectRemoved(effector);
                firePotentialAttributesChanged();
            }

            public void entityAdded(final OdorWorldEntity entity) {
                setChangedSinceLastSave(true);
                firePotentialAttributesChanged();
            }

            public void entityRemoved(final OdorWorldEntity entity) {
                setChangedSinceLastSave(true);
                fireAttributeObjectRemoved(entity);
                firePotentialAttributesChanged();
            }

            public void sensorAdded(final Sensor sensor) {
                setChangedSinceLastSave(true);
                firePotentialAttributesChanged();
            }

            public void sensorRemoved(Sensor sensor) {
                setChangedSinceLastSave(true);
                fireAttributeObjectRemoved(sensor);
                firePotentialAttributesChanged();
            }
            public void entityChanged(OdorWorldEntity entity) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void save(final OutputStream output, final String format) {
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
//        } else if (object instanceof Smeller) {
//            // Need to handle smell sensors in a special way, since they are not directly
//            //  used as producers, but rather specific objects they contain are used.
//            String entityName = ((Smeller) object).getParent().getParent().getName();
//            String sensorName = ((Smeller) object).getParent().getId();
//            String index = "" + ((Smeller) object).getIndex();
//            return entityName + ":smeller:" + sensorName + ":" + index;
//        }

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
            } 
//            else if (secondString.equalsIgnoreCase("smellSensorGetter")) {
//                // Needed to read simulations created before 2/11; remove before beta release
//                int index = Integer.parseInt(parsedKey[3]);
//                return getWorld().getSmeller(entityName,
//                        parsedKey[2], index);
//            } else if (secondString.equalsIgnoreCase("smeller")) {
//                int index = Integer.parseInt(parsedKey[3]);
//                return getWorld().getSmeller(entityName,
//                        parsedKey[2], index);
//            }
        }
        return null;
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