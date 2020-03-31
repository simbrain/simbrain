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

import org.simbrain.util.SimpleId;
import org.simbrain.util.UserParameter;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.piccolo.TMXUtils;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.events.OdorWorldEvents;
import org.simbrain.world.odorworld.sensors.Sensor;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Core model class of Odor World, which contains a list of entities in the
 * world. Some code from Developing Games in Java, by David Brackeen.
 */
public class OdorWorld implements EditableObject {

    /**
     * List of odor world entities.
     */
    private List<OdorWorldEntity> entityList = new CopyOnWriteArrayList<OdorWorldEntity>();

    /**
     * Basic tilemap that determines the size and basic features of the world.
     */
    private TileMap tileMap = TMXUtils.loadTileMap("empty.tmx");

    /**
     * Sum of lengths of smell vectors for all smelly objects in the world.
     */
    private transient double maxVectorNorm;

    /**
     * Whether or not sprites wrap around or are halted at the borders
     */
    @UserParameter(label = "Map boundary wraps around", description = "Whether or not entities wrap around or are halted at the borders of the map", order = 6)
    private boolean wrapAround = true;

    /**
     * If true, then objects block movements; otherwise agents can walk through
     * objects.
     */
    @UserParameter(label = "Objects block movement", description = "If true, then objects block movements; otherwise agents can walk through objects", order = 10)
    private boolean objectsBlockMovement = true;

    /**
     * Entity Id generator.
     */
    private SimpleId entityIDGenerator = new SimpleId("Entity", 1);

    /**
     * Sensor Id generator.
     */
    private SimpleId sensorIDGenerator = new SimpleId("Sensor", 1);

    /**
     * Effector Id generator.
     */
    private SimpleId effectorIDGenerator = new SimpleId("Effector", 1);

    /**
     * Agent Name generator.
     */
    private SimpleId agentIdGenerator = new SimpleId("Agent", 1);

    /**
     * Event support
     */
    protected transient OdorWorldEvents events = new OdorWorldEvents(this);

    /**
     * Last clicked position.
     */
    private Point2D lastClickedPosition = new Point2D.Double(50,50);

    private RectangleCollisionBound worldBoundary = new RectangleCollisionBound(new Rectangle2D.Double(
            0, 0, tileMap.getMapWidth(), tileMap.getMapHeight()
    ));

    /**
     * Default constructor.
     */
    public OdorWorld() {
    }

    /**
     * Update world.
     */
    public void update() {
        for (OdorWorldEntity entity : entityList) {
            entity.updateSmellSource();
            entity.update();
        }
        events.fireUpdated();
    }

    /**
     * Stop animation.
     */
    public void stopAnimation() {
        events.fireAnimationStopped();
        events.fireWorldStopped();
    }

    /**
     * Add an Odor World Entity.
     *
     * @param entity the entity to add
     */
    public void addEntity(final OdorWorldEntity entity) {

        if(entityList.contains(entity)) {
            return;
        }

        // Set the entity's id
        entity.setId(entityIDGenerator.getId());
        entity.setName(entity.getId());

        // Add entity to the map
        // map.addSprite(entity);
        entityList.add(entity);

        entity.setParentWorld(this);

        events.fireEntityAdded(entity);

        // Recompute max stimulus length
        recomputeMaxVectorNorm();
    }

    /**
     * Add new entity at last clicked position with default properties.
     */
    public OdorWorldEntity addEntity() {
        OdorWorldEntity entity = new OdorWorldEntity(this);
        entity.setLocation(lastClickedPosition.getX(), lastClickedPosition.getY());
        addEntity(entity);
        return entity;
    }

    /**
     * Add new "agent" (rotating with some default) sensors and effectors at last clicked position.
     */
    public OdorWorldEntity addAgent() {

        OdorWorldEntity entity = new OdorWorldEntity(this, EntityType.MOUSE);
        addEntity(entity);
        entity.setEntityType(EntityType.MOUSE);
        double x = lastClickedPosition.getX();
        if (x > tileMap.getMapWidth() - EntityType.MOUSE.getImageWidth()) {
            x = tileMap.getMapWidth() - EntityType.MOUSE.getImageWidth();
        }
        double y = lastClickedPosition.getY();
        if (y > tileMap.getMapHeight() - EntityType.MOUSE.getImageHeight()) {
            y = tileMap.getMapHeight() - EntityType.MOUSE.getImageHeight();
        }
        entity.setLocation(x, y);
        entity.addDefaultSensorsEffectors();
        return entity;
    }

    public void addTile() {
        tileMap.editTile(
                "program_layer", 61,
                (int) lastClickedPosition.getX() / tileMap.getTileWidth(),
                (int) lastClickedPosition.getY() / tileMap.getTileHeight());
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
     * Returns the entity with the given id, or, failing that, a given name. If
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
     * Return sensor with matching id or null if none found.
     */
    public Sensor getSensor(String id) {
        for (OdorWorldEntity entity : entityList) {
            for (Sensor sensor : entity.getSensors()) {
                if (sensor.getId().equalsIgnoreCase(id)) {
                    return sensor;
                }
            }
        }
        return null;
    }

    /**
     * Return effector with matching id or null if none found.
     */
    public Effector getEffector(String id) {
        for (OdorWorldEntity entity : entityList) {
            for (Effector effector : entity.getEffectors()) {
                if (effector.getId().equalsIgnoreCase(id)) {
                    return effector;
                }
            }
        }
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
     * @param entityId   entity id
     * @param effectorId sensor id
     * @return effector if found
     */
    public Effector getEffector(final String entityId,
                                final String effectorId) {
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
            entity.delete();
            for (Sensor sensor : entity.getSensors()) {
                //fireSensorRemoved(sensor);
            }
            for (Effector effector : entity.getEffectors()) {
                //fireEffectorRemoved(effector);
            }
            recomputeMaxVectorNorm();
            events.fireEntityRemoved(entity);
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
     * Caches the maximum vector norm, which is used for scaling the color smell sensors.
     */
    private void recomputeMaxVectorNorm() {
        maxVectorNorm = entityList.stream()
                .filter(Objects::nonNull)
                .map(e -> SimbrainMath.getVectorNorm(e.getSmellSource().getStimulusVector()))
                .max(Double::compareTo)
                .orElse(0.0);
    }

    /**
     * Standard method call made to objects after they are deserialized. See:
     * http://java.sun.com/developer/JDCTechTips/2002/tt0205.html#tip2
     * http://xstream.codehaus.org/faq.html
     *
     * @return Initialized object.
     */
    private Object readResolve() {
        if (agentIdGenerator == null) {
            agentIdGenerator = new SimpleId("Agent", 1);
        }

        events = new OdorWorldEvents(this);

        for (OdorWorldEntity entity : entityList) {
            entity.postSerializationInit();
        }
        recomputeMaxVectorNorm();
        return this;
    }

    /**
     * Handle collisions in x directions.
     *
     * @param entityToCheck
     * @param xCheck        position to check
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
            //if ((entityToCheck.getX() > entity.getX()) && (entityToCheck.getX() < (entity.getX() + entity.getWidth()))) {
            //    return true;
            //}
        }
        return false;
    }

    /**
     * Handle collisions in y directions.
     *
     * @param entityToCheck
     * @param yCheck        position to check
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

            //if ((entityToCheck.getY() > sprite.getY()) && (entityToCheck.getY() < (sprite.getY() + sprite.getHeight()))) {
            //    return true;
            //}
        }
        return false;
    }

    /**
     * Returns the list of entities.
     *
     * @return the entity list
     */
    public List<OdorWorldEntity> getEntityList() {
        return entityList;
    }

    public boolean getWrapAround() {
        return wrapAround;
    }

    public void setWrapAround(boolean wrapAround) {
        this.wrapAround = wrapAround;
    }

    /**
     * Returns width of world in pixels.
     *
     * @return width in pixels.
     */
    public int getWidth() {
        return tileMap.getMapWidth();
    }

    /**
     * Returns height of world in pixels.
     *
     * @return height of world
     */
    public int getHeight() {
        return tileMap.getMapHeight();
    }

    public boolean isObjectsBlockMovement() {
        return objectsBlockMovement;
    }

    public void setObjectsBlockMovement(boolean objectsBlockMovement) {
        this.objectsBlockMovement = objectsBlockMovement;
    }

    public double getMaxVectorNorm() {
        return maxVectorNorm;
    }

    /**
     * Use the provided set of vectors (stored as a 2-d array of doubles, one
     * vector per row) to set the stimulus vectors on all odor world entities,
     * in the order in which they are stored in the internal list (which should
     * match the order in which they were added to the world).
     *
     * @param stimulusVecs the 2d matrix of stimulus vectors
     */
    public void loadStimulusVectors(double[][] stimulusVecs) {
        Iterator<OdorWorldEntity> entityIterator = getEntityList().iterator();
        for (int i = 0; i < stimulusVecs.length; i++) {
            if (entityIterator.hasNext()) {
                OdorWorldEntity entity = entityIterator.next();
                if (entity.getSmellSource() != null) {
                    //System.out.println(entity);
                    entity.getSmellSource().setStimulusVector(stimulusVecs[i]);
                }
            }
        }
    }

    public SimpleId getSensorIDGenerator() {
        return sensorIDGenerator;
    }

    public SimpleId getEffectorIDGenerator() {
        return effectorIDGenerator;
    }

    public TileMap getTileMap() {
        return tileMap;
    }

    public void setTileMap(TileMap tileMap) {
        this.tileMap = tileMap;
        worldBoundary = new RectangleCollisionBound(new Rectangle2D.Double(
                0, 0, tileMap.getMapWidth(), tileMap.getMapHeight()
        ));
        events.fireTileMapChanged();
    }

    public void start() {
        events.fireWorldStarted();
    }

    public Point2D getLastClickedPosition() {
        return lastClickedPosition;
    }

    public void setLastClickedPosition(Point2D position) {
        lastClickedPosition = position;
    }

    public RectangleCollisionBound getWorldBoundary() {
        return worldBoundary;
    }

    @Override
    public String getName() {
        return "Odor World";
    }

    public OdorWorldEvents getEvents() {
        return events;
    }
}
