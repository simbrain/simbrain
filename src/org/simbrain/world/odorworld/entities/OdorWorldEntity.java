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
package org.simbrain.world.odorworld.entities;

import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.util.SimpleId;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.behaviors.Behavior;
import org.simbrain.world.odorworld.behaviors.StationaryBehavior;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.TileSensor;

/**
 * Parent class for all Odor World objects. Adapted and extended from From
 * Developing Games in Java, by David Brackeen.
 */
public abstract class OdorWorldEntity {

    /**
     * Animation used to depict this object. If the animation has one frame this
     * is equivalent to just using a single image to represent it.
     */
    private Animation animation;

    /** Name of this entity. */
    private String name;

    /** Id of this entity. */
    private String id;

    /** X Position. */
    protected float x;

    /** Y Position. */
    protected float y;

    /** X Velocity. */
    protected float dx;

    /** Y Velocity. */
    protected float dy;

    /** Back reference to parent parentWorld. */
    private OdorWorld parentWorld;

    /** Sensors. */
    private List<Sensor> sensors = new ArrayList<Sensor>();

    /** Effectors. */
    private List<Effector> effectors = new ArrayList<Effector>();

    /** Behavior. */
    protected Behavior behavior = new StationaryBehavior();

    /** Smell Source (if any). */
    private SmellSource smellSource;

    /** True if a collision occurred in the last time step. */
    private boolean collision;

    /** Enable sensors. If not the agent is "blind." */
    private boolean sensorsEnabled = true;

    /** Enable effectors. If not the agent is "paralyzed. */
    private boolean effectorsEnabled = true;

    /** If true, show sensors. */
    private boolean showSensors = true;

    /** Entity Id generator. */
    private SimpleId sensorIDGenerator = new SimpleId("Sensor", 1);

    /** Entity Id generator. */
    private SimpleId effectorIDGenerator = new SimpleId("Effector", 1);

    /**
     * Updates this OdorWorldEntity's Animation and its position based on the
     * velocity.
     */
    public abstract void update(final long elapsedTime);

    /**
     * Called before update() if the creature collided with a tile horizontally.
     */
    public void collideHorizontal() {
        behavior.collisionX();
        collision = true;
    }

    /**
     * Called before update() if the creature collided with a tile vertically.
     */
    public void collideVertical() {
        behavior.collissionY();
        collision = true;
    }

    /**
     * Construct an entity from an animation.
     *
     * @param animation animation to use.
     */
    public OdorWorldEntity(final Animation anim, OdorWorld world) {
        this.animation = anim;
        this.parentWorld = world;
        anim.start();
    }

    /**
     * Construct an odor world entity from a single image location.
     *
     * @param imageLocation the image location
     */
    public OdorWorldEntity(final String imageLocation, OdorWorld world) {
        this.animation = new Animation(imageLocation);
        this.parentWorld = world;
        animation.start();
    }

    /**
     * Construct an entity.
     *
     * @param world parent world of entity
     */
    public OdorWorldEntity(OdorWorld world) {
        this.parentWorld = world;
    }

    // TODO: Say in docs if this is upper right or not (but not center).
    /**
     * Gets this OdorWorldEntity's current x position.
     */
    public float getX() {
        return x;
    }

    /**
     * Gets this OdorWorldEntity's current y position.
     */
    public float getY() {
        return y;
    }

    /**
     * Sets this OdorWorldEntity's current x position.
     */
    public void setX(final float newx) {
        //System.out.println("x:" + newx);
        if (parentWorld.getWrapAround()) {
            if (newx <= 0) {
                this.x = parentWorld.getWidth()
                        - (Math.abs(newx) % parentWorld.getWidth());
            } else if (newx >  parentWorld.getWidth()) {
                this.x = newx % parentWorld.getWidth();
            } else {
                this.x = newx;
            }
        } else {
            if (isInBoundsX(newx)) {
                this.x = newx;
            }
        }
    }

    /**
     * Sets this OdorWorldEntity's current y position.
     */
    public void setY(final float newy) {
        //System.out.println("y:" + newy);
        if (parentWorld.getWrapAround()) {
            if (newy <= 0) {
                this.y = parentWorld.getHeight()
                        - (Math.abs(newy) % parentWorld.getHeight());
            } else if (newy >  parentWorld.getHeight()) {
                this.y = newy % parentWorld.getHeight();
            } else {
                this.y = newy;
            }
        } else {
            if (isInBoundsY(newy)) {
                this.y = newy;
            }
        }
    }

    /**
     * Check whether, if the provided point is used to set the x (upper left)
     * coordinate of the entity, the bounds of the object will be in
     * bounds.
     *
     * @param x the point to check
     * @return whether the point is in bounds or not.
     */
    public boolean isInBoundsX(float x) {
        if ((x < 0) || ((x + getWidth()) > getParentWorld().getWidth())) {
            return false;
        }
        return true;
    }

    /**
     * Check whether, if the provided point is used to set the y (upper left)
     * coordinate of the entity, the bounds of the object will be in
     * bounds.
     *
     * @param y the point to check
     * @return whether the point is in bounds or not.
     */
    public boolean isInBoundsY(float y) {
        if ((y < 0) || ((y + getHeight()) > getParentWorld().getHeight())) {
            return false;
        }
        return true;
    }


    /**
     * Gets this OdorWorldEntity's width, based on the size of the current
     * image.
     */
    public int getWidth() {
        while (getImage().getWidth(null) < 0) {
            ;
        }
        return animation.getImage().getWidth(null);
    }

    /**
     * Gets this OdorWorldEntity's height, based on the size of the current
     * image.
     */
    public int getHeight() {
        while (getImage().getHeight(null) < 0) {
            ;
        }
        return animation.getImage().getHeight(null);
    }

    /**
     * Gets the horizontal velocity of this OdorWorldEntity in pixels per
     * millisecond.
     */
    public float getVelocityX() {
        return dx;
    }

    /**
     * Gets the vertical velocity of this OdorWorldEntity in pixels per
     * millisecond.
     */
    public float getVelocityY() {
        return dy;
    }

    /**
     * Sets the horizontal velocity of this OdorWorldEntity in pixels per
     * millisecond.
     */
    public void setVelocityX(final float dx) {
        this.dx = dx;
    }

    /**
     * Sets the vertical velocity of this OdorWorldEntity in pixels per
     * millisecond.
     */
    public void setVelocityY(final float dy) {
        this.dy = dy;
    }

    /**
     * Get the entity's name.
     *
     * @return entity's name.
     */
    public String getName() {
        if (name == null) {
            return id; // For backwards compatibility as of 2/11
        } else {
            return name;
        }
    }

    /**
     * Set the entity's name.
     *
     * @param string name for entity.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the id
     */
    public String getId() {
        if (id == null) {
            return name; // For backwards compatibility as of 2/11
        } else {
            return id;
        }
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets this OdorWorldEntity's current image.
     */
    public Image getImage() {
        return animation.getImage();
    }

    /**
     * Get bounds, based on current image.
     *
     * @return bounds of this entity.
     */
    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, getWidth(), getHeight());
    }

    /**
     * Reduced bounds used for some entities, to improve the look of collisions
     * and blocking. TODO: This may not work well when shapes (not pixel images)
     * are used.
     *
     * @return reduced bounds.
     */
    public Rectangle getReducedBounds() {
        Rectangle ret = getBounds();
        ret.grow(-getHeight() / 5, -getWidth() / 5);
        return ret;
    }

    /**
     * Add an effector.
     *
     * @param effector effector to add
     */
    public void addEffector(final Effector effector) {
        // if (sensor.getApplicableTypes().contains(this.getClass()))...
        effectors.add(effector);
        effector.setId(effectorIDGenerator.getId());
        parentWorld.fireEffectorAdded(effector);
    }

    /**
     * Add a sensor.
     *
     * @param sensor sensor to add
     */
    public void addSensor(final Sensor sensor) {
        // if (sensor.getApplicableTypes().contains(this.getClass()))...
        sensors.add(sensor);

        // Assign an id unless it already has one
        if (sensor.getId() == null) {
            sensor.setId(sensorIDGenerator.getId());
        }

        parentWorld.fireSensorAdded(sensor);
    }

    /**
     * Apply impact of all effectors.
     */
    public void applyEffectors() {
        if (effectorsEnabled) {
            for (Effector effector : effectors) {
                effector.activate();
            }
        }
    }

    /**
     * Update all sensors.
     */
    public void updateSensors() {
        if (sensorsEnabled) {
            for (Sensor sensor : sensors) {
                sensor.update();
            }
        }
    }

    /**
     * Add a grid of tile sensors.
     *
     * @param numTilesX number of rows in grid
     * @param numTilesY number of columns in grid
     */
    public void addTileSensors(final int numTilesX, final int numTilesY) {
        addTileSensors(numTilesX, numTilesY, 1);
    }

    /**
     * Add a grid of tile sensors, offset by some fraction of a tile's length.
     *
     * @param numTilesX number of rows in grid
     * @param numTilesY number of columns in grid
     * @param offset offset amount in pixels
     */
    public void addTileSensors(final int numTilesX, final int numTilesY,
            final int offset) {
        int tileWidth = parentWorld.getWidth() / numTilesX;
        int tileHeight = parentWorld.getHeight() / numTilesY;
        for (int i = 0; i < numTilesX; i++) {
            for (int j = 0; j < numTilesY; j++) {
                addSensor(new TileSensor(this, ((i * tileWidth) + offset), 
                        ((j * tileHeight) + offset), tileWidth, tileHeight));
            }
        }
    }

    /**
     * @return the smellSource
     */
    public SmellSource getSmellSource() {
        return smellSource;
    }

    /**
     * @param smellSource the smellSource to set
     */
    public void setSmellSource(final SmellSource smellSource) {
        this.smellSource = smellSource;
        smellSource.setLocation(this.getLocation());
    }

    /**
     * @return the parentWorld
     */
    public OdorWorld getParentWorld() {
        return parentWorld;
    }

    /**
     * Returns the location of the center of this entity as a double array.
     *
     * @return center location of the entity.
     */
    public double[] getCenterLocation() {
        return new double[] { getCenterX(), getCenterY() };
    }

    /**
     * Returns the center x position of this entity.
     *
     * @return center x coordinate.
     */
    public double getCenterX() {
        return x + (getWidth() / 2);
    }

    /**
     * Returns the center y position of this entity.
     *
     * @return center y coordinate.
     */
    public double getCenterY() {
        return y + (getHeight() / 2);
    }
    
    

    /**
     * Set the location of this entity.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void setCenterLocation(float x, float y) {
        setX(x - (getWidth() / 2));
        setY(y - (getHeight() / 2));
    }
    /**
     * Returns the location of the entity as a double array.
     *
     * @return location of the entity.
     */
    public double[] getLocation() {
        return new double[] { x, y };
    }

    /**
     * Set the location of this entity.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void setLocation(float x, float y) {
        setX(x);
        setY(y);
    }

    /**
     * @return the animation associated with this entity
     */
    public Animation getAnimation() {
        return animation;
    }

    /**
     * @param animation the animation to set
     */
    public void setAnimation(final Animation animation) {
        this.animation = animation;
        parentWorld.fireEntityChanged(this);
    }

    /**
     * Initialize the animation from stored image location(s).
     */
    public void postSerializationInit() {
        getAnimation().initializeImages(); // TODO
    }

    /**
     * @return the sensors
     */
    public List<Sensor> getSensors() {
        return sensors;
    }

    /**
     * @param sensors the sensors to set
     */
    public void setSensors(List<Sensor> sensors) {
        this.sensors = sensors;
    }

    /**
     * @return the effectors
     */
    public List<Effector> getEffectors() {
        return effectors;
    }

    /**
     * @param effectors the effectors to set
     */
    public void setEffectors(List<Effector> effectors) {
        this.effectors = effectors;
    }

    /**
     * @return true if a collision occurred
     */
    public boolean hasCollided() {
        return collision;
    }

    /**
     * @param collission the collision to set
     */
    public void setHasCollided(boolean collission) {
        this.collision = collission;
    }

    /**
     * @return the sensorsEnabled
     */
    public boolean isSensorsEnabled() {
        return sensorsEnabled;
    }

    /**
     * @param sensorsEnabled the sensorsEnabled to set
     */
    public void setSensorsEnabled(boolean sensorsEnabled) {
        this.sensorsEnabled = sensorsEnabled;
    }

    /**
     * @return the effectorsEnabled
     */
    public boolean isEffectorsEnabled() {
        return effectorsEnabled;
    }

    /**
     * @param effectorsEnabled the effectorsEnabled to set
     */
    public void setEffectorsEnabled(boolean effectorsEnabled) {
        this.effectorsEnabled = effectorsEnabled;
    }

    /**
     * @return the showSensors
     */
    public boolean isShowSensors() {
        return showSensors;
    }

    /**
     * @param showSensors the showSensors to set
     */
    public void setShowSensors(boolean showSensors) {
        this.showSensors = showSensors;
    }

    /**
     * Returns true if the entity is blocked from moving.
     *
     * @return true if blocked, false otherwise.
     */
    public boolean isBlocked() {
        if (getParentWorld().isObjectsBlockMovement()) {
            if (hasCollided()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return the behavior
     */
    public Behavior getBehavior() {
        return behavior;
    }

    /**
     * @param behavior the behavior to set
     */
    public void setBehavior(Behavior behavior) {
        this.behavior = behavior;
    }

    // TODO: the methods below need not be double, but are double to accommodate
    // the
    // coupling framework, which does not currently handle casts between data
    // types.

    /**
     * Move the object north by the specified amount in pixels.
     *
     * @param amount amount to move by
     */
    public void moveNorth(double amount) {
        if (!isBlocked()) {
            setY(getY() - (float) amount);
        }
    }

    /**
     * Move the object south by the specified amount in pixels.
     *
     * @param amount amount to move by
     */
    public void moveSouth(double amount) {
        if (!isBlocked()) {
            setY(getY() + (float) amount);
        }
    }

    /**
     * Move the object east by the specified amount in pixels.
     *
     * @param amount amount to move by
     */
    public void moveEast(double amount) {
        if (!isBlocked()) {
            setX(getX() + (float) amount);
        }
    }

    /**
     * Move the object west by the specified amount in pixels.
     *
     * @param amount amount to move by
     */
    public void moveWest(double amount) {
        if (!isBlocked()) {
            setX(getX() - (float) amount);
        }
    }

    /**
     * Get the X position as a double.
     *
     * @return the x position as a double.
     */
    public double getDoubleX() {
        return (double) x;
    }

    /**
     * Get the Y position as a double.
     *
     * @return the y position as a double.
     */
    public double getDoubleY() {
        return (double) y;
    }

    /**
     * @param x the x to set
     */
    public void setX(double x) {
        setX((float) x);
    }

    /**
     * @param y the y to set
     */
    public void setY(double y) {
        setY((float) y);
    }

    /**
     * Update this object's smell source, if any.
     */
    public void updateSmellSource() {
        if (smellSource != null) {
            smellSource.update();
        }
    }

}
