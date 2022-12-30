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

import org.jetbrains.annotations.NotNull;
import org.simbrain.util.SimpleIdManager;
import org.simbrain.util.UserParameter;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.piccolo.TMXUtils;
import org.simbrain.util.piccolo.TileMap;
import org.simbrain.util.propertyeditor.EditableObject;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.entities.Bounded;
import org.simbrain.world.odorworld.entities.EntityType;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
import org.simbrain.world.odorworld.events.OdorWorldEvents;
import org.simbrain.world.odorworld.sensors.Sensor;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A 2d environment. Contains a list of {@link OdorWorldEntity}s, which can either be agents or static objects.
 *
 * Agents have sensors for detecting objects, and effectors for moving.
 *
 * Contains a {@link TileMap} which is a grid of tiles. This tilemap determines the size of the world.
 * The world's size cannot be set directly.
 */
public class OdorWorld implements EditableObject, Bounded {

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

    @UserParameter(label = "Use camera centering", description = "For large worlds centers the camera on the current " +
            "agent. Turn off in particular when not using tilemaps.",
            order = 20)
    private boolean useCameraCentering = true;

    /**
     * Entity Id generator.
     */
    private SimpleIdManager.SimpleId entityIDGenerator = new SimpleIdManager.SimpleId("Entity", 1);

    /**
     * Sensor Id generator.
     */
    private SimpleIdManager.SimpleId sensorIDGenerator = new SimpleIdManager.SimpleId("Sensor", 1);

    /**
     * Effector Id generator.
     */
    private SimpleIdManager.SimpleId effectorIDGenerator = new SimpleIdManager.SimpleId("Effector", 1);

    /**
     * Agent Name generator.
     */
    private SimpleIdManager.SimpleId agentIdGenerator = new SimpleIdManager.SimpleId("Agent", 1);

    /**
     * Event support
     */
    protected transient OdorWorldEvents events = new OdorWorldEvents(this);

    /**
     * Last clicked position.
     */
    private Point2D lastClickedPosition = new Point2D.Double(50,50);

    /**
     * Default constructor.
     */
    public OdorWorld() {
    }

    /**
     * Update world.
     */
    public void update() {
        entityList.forEach(OdorWorldEntity::update);
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
        entity.setId(entityIDGenerator.getAndIncrement());
        entity.setName(entity.getId());

        // Add entity to the map
        entityList.add(entity);

        events.fireEntityAdded(entity);

        // Recompute max stimulus length
        recomputeMaxVectorNorm();
    }

    /**
     * Add new entity at last clicked position with default properties.
     */
    public OdorWorldEntity addEntity() {
        OdorWorldEntity entity = new OdorWorldEntity(this);
        entity.setLocation(new Point2D.Double(lastClickedPosition.getX(), lastClickedPosition.getY()));
        addEntity(entity);
        return entity;
    }

    /**
     * Add entity of a specified type.
     */
    public OdorWorldEntity addEntity(EntityType type) {
        final var entity = new OdorWorldEntity(this, type);
        addEntity(entity);
        return entity;
    }

    /**
     * Add entity with a stimulus value.
     */
    public OdorWorldEntity addEntity(int x, int y, EntityType type, double[] stimulus) {
        OdorWorldEntity entity = addEntity(x,y,type);
        entity.setSmellSource(new SmellSource(stimulus));
        addEntity(entity);
        return entity;
    }

    public OdorWorldEntity addEntity(int x, int y, EntityType type) {
        OdorWorldEntity entity = new OdorWorldEntity(this, type);
        entity.setLocation(x, y);
        entity.setSmellSource(new SmellSource(6));
        entity.setEntityType(type);
        addEntity(entity);
        return entity;
    }

    public OdorWorldEntity addEntity(double x, double y, EntityType type) {
        return addEntity((int) x, (int) y, type);
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
        tileMap.setTile(
                (int) lastClickedPosition.getX() / tileMap.getTileWidth(),
                (int) lastClickedPosition.getY() / tileMap.getTileHeight(),
                61
        );
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
                entity.getEvents().fireSensorRemoved(sensor);
            }
            for (Effector effector : entity.getEffectors()) {
                entity.getEvents().fireEffectorRemoved(effector);
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
     * See {@link org.simbrain.workspace.serialization.WorkspaceComponentDeserializer}
     */
    private Object readResolve() {

        events = new OdorWorldEvents(this);

        for (OdorWorldEntity entity : entityList) {
//            entity.postSerializationInit();
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
    public double getWidth() {
        return tileMap.getMapWidth();
    }

    /**
     * Returns height of world in pixels.
     *
     * @return height of world
     */
    public double getHeight() {
        return tileMap.getMapHeight();
    }

    public boolean isObjectsBlockMovement() {
        return objectsBlockMovement;
    }

    public void setObjectsBlockMovement(boolean objectsBlockMovement) {
        this.objectsBlockMovement = objectsBlockMovement;
    }

    public List<Bounded> getCollidableObjects() {
        var bounds = new ArrayList<Bounded>();

        if (isObjectsBlockMovement()) {
            bounds.addAll(entityList);
        }

        if (!wrapAround) {
            bounds.add(this);
        }

        return bounds;
    }

    public double getMaxVectorNorm() {
        return maxVectorNorm;
    }

    public SimpleIdManager.SimpleId getSensorIDGenerator() {
        return sensorIDGenerator;
    }

    public SimpleIdManager.SimpleId getEffectorIDGenerator() {
        return effectorIDGenerator;
    }

    public TileMap getTileMap() {
        return tileMap;
    }

    public void setTileMap(TileMap tileMap) {
        this.tileMap = tileMap;
        getEvents().fireTileMapChanged();
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

    @Override
    public String getName() {
        return "Odor World";
    }

    public OdorWorldEvents getEvents() {
        return events;
    }

    public void setUseCameraCentering(boolean useCameraCentering) {
        this.useCameraCentering = useCameraCentering;
    }

    public boolean isUseCameraCentering() {
        return useCameraCentering;
    }

    @Override
    public double getX() {
        return getWidth() / 2;
    }


    @Override
    public double getY() {
        return getHeight() / 2;
    }

    /**
     * Center location by default.
     */
    @NotNull
    @Override
    public Point2D getLocation() {
        return new Point2D.Double(getX(), getY());
    }

    /**
     * Forwards to [getLocation] but makes clear that location is centerlocation.
     */
    @NotNull
    public Point2D getCenterLocation() {
        return getLocation();
    }

}
