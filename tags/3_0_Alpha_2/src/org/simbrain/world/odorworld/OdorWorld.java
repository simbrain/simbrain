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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.simbrain.util.SimbrainMath;
import org.simbrain.util.SimpleId;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.Animation;
import org.simbrain.world.odorworld.entities.BasicEntity;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.entities.RotatingEntity;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Core model class of Odor World, which contains a list of entities in the
 * world. Some code from Developing Games in Java, by David Brackeen.
 */
public class OdorWorld {

    /** List of odor world entities. */
    private List<OdorWorldEntity> entityList = new CopyOnWriteArrayList<OdorWorldEntity>();

    /** Listeners on this odor world. */
    private List<WorldListener> listenerList = new ArrayList<WorldListener>();

    /** Sum of lengths of smell vectors for all smelly objects in the world. */
    private double totalSmellVectorLength;

    /** Whether or not sprites wrap around or are halted at the borders */
    private boolean wrapAround = true;

    /**
     * If true, then objects block movements; otherwise agents can walk through
     * objects.
     */
    private boolean objectsBlockMovement = true;

    /** Height of world. */
    private int height = 450;

    /** Width of world. */
    private int width = 450;

    /** Entity Id generator. */
    private SimpleId entityIDGenerator = new SimpleId("Entity", 1);

    /** Agent Name generator. */
    private SimpleId agentNameGenerator = new SimpleId("Agent", 1);

    /**
     * Default constructor.
     */
    OdorWorld() {
    }

    /**
     * Update world.
     */
    public void update() {
        for (OdorWorldEntity object : entityList) {
            object.updateSmellSource();
            object.updateSensors();
            object.applyEffectors();
            updateEntity(object, 1); // time defaults to 1 now
        }
        fireUpdateEvent();
    }

    /**
     * Add an Odor World Entity.
     *
     * @param entity the entity to add
     */
    public void addEntity(final OdorWorldEntity entity) {

       // Set the entity's id
        entity.setId(entityIDGenerator.getId());

        // Add entity to the map
        // map.addSprite(entity);
        entityList.add(entity);

        // Fire entity added event
        fireEntityAdded(entity);

        // Recompute max stimulus length
        recomputeMaxStimulusLength();

    }
    
    /**
     * Does the world contain this entity? 
     *
     * @param entity the entity to check for
     * @return whether it is in this world or not.
     */
    public boolean containsEntity(final OdorWorldEntity entity) {
        return entityList.contains(entity);
    }

    /**
     * Adds an agent and by default adds several sensors and effectors to it.
     *
     * @param entity the entity  corresponding to the agent
     */
    public void addAgent(final OdorWorldEntity entity) {

        entity.setName(agentNameGenerator.getId());

        if (entity instanceof RotatingEntity) {

            // Add effectors (currently none)

            // Add sensors
            entity.addSensor(new SmellSensor(entity, "Smell-Left", Math.PI / 8, 50));
            entity.addSensor(new SmellSensor(entity, "Smell-Center", 0, 0));
            entity.addSensor(new SmellSensor(entity, "Smell-Right", -Math.PI / 8, 50));
        }
        addEntity(entity);
    }

    /**
     * Returns the entity with the given id, or, failing that, a given name.  If
     * no entity is found return null.
     *
     * @param id name of entity
     * @return matching entity, if any
     */
    public OdorWorldEntity getEntity(final String id) {
        // Search by id
        for (OdorWorldEntity entity : entityList) {
            if (entity.getId().equalsIgnoreCase(id)) {
                return entity;
            }
        }
        // Search for label if no matching id found
        for (OdorWorldEntity entity : entityList) {
            if (entity.getName().equalsIgnoreCase(id)) {
                return entity;
            }
        }
        // Matching entity not found
        return null;
    }

    /**
     * Returns the sensor with the given id, or null if none is found.
     *
     * @param entityId entity id
     * @param sensorId sensor id
     * @return sensor if found
     */
    public Sensor getSensor(final String entityId, final String sensorId) {
        OdorWorldEntity entity = getEntity(entityId);
        if (entity == null) {
            return null;
        }
        for (Sensor sensor : entity.getSensors()) {
            if (sensor.getId().equalsIgnoreCase(sensorId)) {
                return sensor;
            }
        }
        return null;
    }

    /**
     * Returns the effector with the given id, or null if none is found.
     *
     * @param entityId entity id
     * @param effectorId sensor id
     * @return effector if found
     */
    public Effector getEffector(final String entityId, final String effectorId) {
        OdorWorldEntity entity = getEntity(entityId);
        if (entity == null) {
            return null;
        }
        for (Effector effector : entity.getEffectors()) {
            if (effector.getId().equalsIgnoreCase(effectorId)) {
                return effector;
            }
        }
        return null;

    }

    /**
     * Delete entity.
     *
     * @param entity the entity to delete
     */
    public void deleteEntity(OdorWorldEntity entity) {
        // map.removeSprite(entity);
        if (entityList.contains(entity)) {
            entityList.remove(entity);
            for(Sensor sensor : entity.getSensors()) {
                fireSensorRemoved(sensor);
            }
            for(Effector effector : entity.getEffectors()) {
                fireEffectorRemoved(effector);
            }
            recomputeMaxStimulusLength();
            fireEntityRemoved(entity);
        }
    }
    
    /**
     * Delete all entities.
     */
    public void deleteAllEntities() {
    	for (OdorWorldEntity entity : entityList) {
    		deleteEntity(entity);
    	}
    }

    /**
     * Computes maximum stimulus length. This is used for scaling the color in
     * the graphical display of the agent sensors.
     */
    private void recomputeMaxStimulusLength() {
        totalSmellVectorLength = 0;
        for (OdorWorldEntity entity : entityList) {
            if (entity.getSmellSource() != null) {
                totalSmellVectorLength += SimbrainMath.getVectorNorm(entity
                        .getSmellSource().getStimulusVector());
            }
        }
    }

    /**
     * Returns a properly initialized xstream object.
     *
     * @return the XStream object TODO: There is more to remove!
     */
    static XStream getXStream() {
        XStream xstream = new XStream(new DomDriver());
        xstream.omitField(OdorWorld.class, "listenerList");
        xstream.omitField(Animation.class, "frames");
        xstream.omitField(Animation.class, "currFrameIndex");
        xstream.omitField(BasicEntity.class, "images");
        xstream.omitField(OdorWorldEntity.class, "images");
        xstream.omitField(RotatingEntity.class, "imageMap");
        return xstream;
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        listenerList = new ArrayList<WorldListener>();
        if (agentNameGenerator == null) {
            agentNameGenerator = new SimpleId("Agent" , 1);
        }

        for (OdorWorldEntity entity : entityList) {
            entity.postSerializationInit();
        }
        recomputeMaxStimulusLength();
        return this;
    }

    /**
     * Updates all entities.
     */
    private void updateEntity(final OdorWorldEntity entity,
            final long elapsedTime) {

        // Collision detection
        float dx = entity.getVelocityX();
        float oldX = entity.getX();
        float newX = oldX + dx * elapsedTime;
        float dy = entity.getVelocityY();
        float oldY = entity.getY();
        float newY = oldY + dy * elapsedTime;

        // Very simple motion
        if (dx != 0) {
            entity.setX(entity.getX() + dx);
        }
        if (dy != 0) {
            entity.setY(entity.getY() + dy);
        }

        // Handle sprite collisions
        entity.setHasCollided(false);
        for (OdorWorldEntity otherEntity : entityList) {
            if (entity == otherEntity) {
                continue;
            }
            if (otherEntity.getReducedBounds().intersects(entity.getReducedBounds())) {
                otherEntity.setHasCollided(true);
            }
        }

        // TODO: Refactor below! Needed for behaviors...
        // Handle sprite collisions
        // if (xCollission(sprite, newX)) {
        // sprite.collideHorizontal();
        // } else {
        // // sprite.setX(newX);
        // }
        // if (yCollission(sprite, newY)) {
        // sprite.collideVertical();
        // } else {
        // // sprite.setY(newY);
        // }

        // Update creature
        entity.update(elapsedTime);

        //System.out.println(sprite.getId() + " new - x: " + sprite.getX() + " y:" + sprite.getY());
    }

    /**
     * Handle collisions in x directions.
     *
     * @param entityToCheck
     * @param xCheck position to check
     * @return whether or not a collision occurred.
     */
    private boolean xCollission(OdorWorldEntity entityToCheck, float xCheck) {

        // Hit a wall
        // if ((entityToCheck.getX() < 0) || (entityToCheck.getX() >
        // getWidth())) {
        // return true;
        // }

        // Check for collisions with sprites
        for (OdorWorldEntity entity : entityList) {
            if (entity == entityToCheck) {
                continue;
            }
            if ((entityToCheck.getX() > entity.getX())
                    && (entityToCheck.getX() < (entity.getX() + entity
                            .getWidth()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Handle collisions in y directions.
     *
     * @param entityToCheck
     * @param yCheck position to check
     * @return whether or not a collision occurred.
     */
    private boolean yCollission(OdorWorldEntity entityToCheck, float yCheck) {
        // Hit a wall
        // if ((entityToCheck.getY() < 0) || (entityToCheck.getY() >
        // getHeight())) {
        // return true;
        // }

        // Check for collisions with sprites
        for (OdorWorldEntity sprite : entityList) {

            if (sprite == entityToCheck) {
                continue;
            }

            if ((entityToCheck.getY() > sprite.getY())
                    && (entityToCheck.getY() < (sprite.getY() + sprite
                            .getHeight()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a world listener.
     *
     * @param listener listener to add.
     */
    public void addListener(WorldListener listener) {
        listenerList.add(listener);
    }

    /**
     * Returns the list of entities.
     *
     * @return the entity list
     */
    public List<OdorWorldEntity> getObjectList() {
        return entityList;
    }

    /**
     * Fire entity added event.
     *
     * @param entity entity that was added
     */
    public void fireEntityAdded(final OdorWorldEntity entity) {
        for (WorldListener listener : listenerList) {
            listener.entityAdded(entity);
        }
    }

    /**
     * Fire entity removed event.
     *
     * @param entity entity that was removed
     */
    public void fireEntityRemoved(final OdorWorldEntity entity) {
        for (WorldListener listener : listenerList) {
            listener.entityRemoved(entity);
        }
    }

    /**
     * Fire entity changed event.
     *
     * @param entity entity that was changed
     */
    public void fireEntityChanged(final OdorWorldEntity entity) {
        for (WorldListener listener : listenerList) {
            listener.entityChanged(entity);
        }
    }

    /***
     * Fire sensor added event.
     *
     * @param sensor sensor that was added
     */
    public void fireSensorAdded(final Sensor sensor) {
        for (WorldListener listener : listenerList) {
            listener.sensorAdded(sensor);
        }
    }

    /**
     * Fire sensor removed event.
     *
     * @param sensor sensor that was removed
     */
    public void fireSensorRemoved(final Sensor sensor) {
        for (WorldListener listener : listenerList) {
            listener.sensorRemoved(sensor);
        }
    }

    /**
     * Fire effector added event.
     *
     * @param effector effector that was added
     */
    public void fireEffectorAdded(final Effector effector) {
        for (WorldListener listener : listenerList) {
            listener.effectorAdded(effector);
        }
    }

    /**
     * Fire effector removed event.
     *
     * @param effector effector that was removed
     */
    public void fireEffectorRemoved(final Effector effector) {
        for (WorldListener listener : listenerList) {
            listener.effectorRemoved(effector);
        }
    }

    /**
     * Fire an update event.
     */
    public void fireUpdateEvent() {
        for (WorldListener listener : listenerList) {
            listener.updated();
        }
    }

    /**
     * Fire a property changed event.
     */
    public void firePropertyChangedEvent() {
        for (WorldListener listener : listenerList) {
            listener.propertyChanged();
        }
    }

    /**
     * @return the wrapAround
     */
    public boolean getWrapAround() {
        return wrapAround;
    }

    /**
     * @param wrapAround the wrapAround to set
     */
    public void setWrapAround(boolean wrapAround) {
        this.wrapAround = wrapAround;
    }

    /**
     * Set width.
     *
     * @param newWidth new width
     * @param fireEvent whether to fire a property changed event
     */
    public void setWidth(int newWidth, boolean fireEvent) {
        this.width = newWidth;
        if (fireEvent) {
            firePropertyChangedEvent();
        }
    }

    /**
     * Set height.
     *
     * @param newHeight new height
     * @param fireEvent whether to fire a property changed event
     */
    public void setHeight(int newHeight, boolean fireEvent) {
        this.height = newHeight;
        if (fireEvent) {
            firePropertyChangedEvent();
        }
    }
    /**
     * @param height the height to set
     */
    public void setHeight(int height) {
        setHeight(height, true);
    }

    /**
     * @param width the width to set
     */
    public void setWidth(int width) {
        setWidth(width, true);
    }

    /**
     * Returns width of world in pixels.
     *
     * @return width in pixels.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns height of world in pixels.
     *
     * @return height of world
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return the objectsBlockMovement
     */
    public boolean isObjectsBlockMovement() {
        return objectsBlockMovement;
    }

    /**
     * @param objectsBlockMovement the objectsBlockMovement to set
     */
    public void setObjectsBlockMovement(boolean objectsBlockMovement) {
        this.objectsBlockMovement = objectsBlockMovement;
    }

    /**
     * @return the maxSmellVectorLength
     */
    public double getTotalSmellVectorLength() {
        return totalSmellVectorLength;
    }

}
