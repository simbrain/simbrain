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

import javafx.util.Pair;
import org.simbrain.util.UserParameter;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.behaviors.Behavior;
import org.simbrain.world.odorworld.behaviors.StationaryBehavior;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;
import org.simbrain.world.odorworld.sensors.TileSensor;

import java.awt.geom.Line2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parent class for all Odor World objects.
 */
public class OdorWorldEntity implements EditableObject {

    /**
     * Support for property change events.
     */
    protected transient PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);

    @UserParameter(label = "Type", order = 2)
    private EntityType entityType = EntityType.SWISS;

    /**
     * Name of this entity.
     */
    @UserParameter(label = "Name", order = 1)
    private String name;

    /**
     * Id of this entity.
     */
    private String id;

    /**
     * X Position.
     */
    @UserParameter(label = "X", description = "X Position", order = 3)
    protected double x;

    /**
     * Y Position.
     */
    @UserParameter(label = "Y", description = "Y Position", order = 4)
    protected double y;

    /**
     * Approximated collision bound.
     */
    private double collisionRadius;

    /**
     * Actual collision bound.
     */
    private HashMap<String, Line2D.Double> collisionBounds = new HashMap<>();

    /**
     * Collision bound handling conditions
     */
    private HashMap<String, List<Pair<String, String>>> collisionConditions = new HashMap<>();

    /**
     * X Velocity.
     */
    @UserParameter(label = "dx", description = "amount to move in x-direction each update", order = 5)
    protected double dx;

    /**
     * Y Velocity.
     */
    @UserParameter(label = "dy", description = "amount to move in y-direction each update", order = 6)
    protected double dy;

    /**
     * Amount to manually move forward or in cardinal directions.
     */
    @UserParameter(label = "Straigh movement", order = 10)
    protected double manualStraightMovementIncrement = 1;

    /**
     * Current heading / orientation.
     */
    private double heading = DEFAULT_HEADING;

    /**
     * Change in current heading.
     */
    private double dtheta;

    //TODO
    private boolean manualControl;

    /**
     * Initial heading of agent.
     */
    private final static double DEFAULT_HEADING = 0;

    /**
     * Default location for sensors relative to agent.
     */
    private static double WHISKER_ANGLE = Math.PI / 4;

    /**
     * Amount to manually rotate.
     */
    @UserParameter(label = "Turn amount", order = 10)
    private double manualMotionTurnIncrement = 1;

    /**
     * Back reference to parent parentWorld.
     */
    private OdorWorld parentWorld;

    /**
     * Sensors.
     */
    private List<Sensor> sensors = new ArrayList<Sensor>();

    /**
     * Effectors.
     */
    private List<Effector> effectors = new ArrayList<Effector>();

    /**
     * Behavior.
     */
    protected Behavior behavior = new StationaryBehavior();

    /**
     * Smell Source (if any).
     */
    private SmellSource smellSource;

    /**
     * Enable sensors. If not the agent is "blind."
     */
    @UserParameter(label = "Enable Sensors", order = 5)
    private boolean sensorsEnabled = false;

    /**
     * Enable effectors. If not the agent is "paralyzed.
     */
    @UserParameter(label = "Enable Effectors", order = 6)
    private boolean effectorsEnabled = false;

    /**
     * If true, show sensors.
     */
    @UserParameter(label = "Show sensors", order = 30)
    private boolean showSensors = true;

    /**
     * Things currently being said by talking entities.
     */
    private List<String> currentlyHeardPhrases = new ArrayList<String>();

    /**
     * Construct an entity.
     *
     * @param world parent world of entity
     */
    public OdorWorldEntity(OdorWorld world) {
        this.parentWorld = world;
        updateCollisionRadius();
        updateCollisionBound();
    }

    /**
     * Construct a basic entity with a single image location.
     *
     * @param type  image location
     * @param world parent world
     */
    public OdorWorldEntity(final OdorWorld world, final EntityType type) {
        this.parentWorld = world;
        setEntityType(type);
        sensorsEnabled = type.useSensors;
        effectorsEnabled = type.useEffectors;
    }

    /**
     * Updates this OdorWorldEntity's Animation and its position based on the
     * velocity.
     */
    public void update() {
        updateCollisionBound();
        if (!manualControl) {
            simpleMotion();
        }

        updateSensors();
        updateEffectors();

        // For Backwards compatibility
        if (currentlyHeardPhrases != null) {
            currentlyHeardPhrases.clear();
        }

        changeSupport.firePropertyChange("updated", null, this);

    }

    /**
     * Simple motion control.
     */
    private void simpleMotion() {
        if (dx != 0) {
            boolean collided = false;
            if (dx > 0) {
                collided = collideOn("right");
            } else {
                collided = collideOn("left");
            }

            if (!collided) {
                setX(x + dx);
            }
        }
        if (dy != 0) {
            boolean collided = false;
            if (dy > 0) {
                collided = collideOn("down");
            } else {
                collided = collideOn("up");
            }
            if (!collided) {
                setY(y + dy);
            }
        }
        if (dtheta != 0) {
            setHeading(heading + dtheta);
            double dthetaRad = Math.toRadians(-dtheta);
            double dx2 = Math.cos(dthetaRad) * dx - Math.sin(dthetaRad) * dy;
            double dy2 = Math.sin(dthetaRad) * dx + Math.cos(dthetaRad) * dy;
            dx = dx2;
            dy = dy2;
        }
    }

    /**
     * Sets this OdorWorldEntity's current x position.
     *
     * @param newx
     */
    @Consumable(idMethod = "getId")
    public void setX(final double newx) {
        updateCollisionBound();
        // System.out.println("x:" + newx);
        if (parentWorld.getWrapAround()) {
            if (newx <= 0) {
                this.x = parentWorld.getWidth() - (Math.abs(newx) % parentWorld.getWidth());
            } else if (newx > parentWorld.getWidth()) {
                this.x = newx % parentWorld.getWidth();
            } else {
                this.x = newx;
            }
        } else {
            this.x = newx;
        }
        changeSupport.firePropertyChange("moved", null, null);

    }

    /**
     * Sets this OdorWorldEntity's current y position.
     *
     * @param newy
     */
    @Consumable(idMethod = "getId")
    public void setY(final double newy) {
        updateCollisionBound();
        // System.out.println("y:" + newy);
        if (parentWorld.getWrapAround()) {
            if (newy <= 0) {
                this.y = parentWorld.getHeight() - (Math.abs(newy) % parentWorld.getHeight());
            } else if (newy > parentWorld.getHeight()) {
                this.y = newy % parentWorld.getHeight();
            } else {
                this.y = newy;
            }
        } else {
            this.y = newy;
        }
        changeSupport.firePropertyChange("moved", null, null);
    }

    /**
     * Gets the horizontal velocity of this OdorWorldEntity in pixels per
     * millisecond.
     *
     * @return
     */
    @Producible(idMethod = "getId")
    public double getVelocityX() {
        return dx;
    }

    /**
     * Gets the vertical velocity of this OdorWorldEntity in pixels per
     * millisecond.
     *
     * @return
     */
    @Producible(idMethod = "getId")
    public double getVelocityY() {
        return dy;
    }

    /**
     * Sets the horizontal velocity of this OdorWorldEntity in pixels per
     * millisecond.
     *
     * @param dx
     */
    @Consumable(idMethod = "getId")
    public void setVelocityX(final double dx) {
        this.dx = dx;
    }

    /**
     * Sets the vertical velocity of this OdorWorldEntity in pixels per
     * millisecond.
     *
     * @param dy
     */
    @Consumable(idMethod = "getId")
    public void setVelocityY(final double dy) {
        this.dy = dy;
    }

    /**
     * Get the entity's name.
     *
     * @return entity's name.
     */
    public String getName() {
        if (name == null) {
            return id;
        } else {
            return name;
        }
    }

    /**
     * Set the entity's name.
     *
     * @param name string name for entity.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
        if (name == null) {
            name = id;
        }
    }

    /**
     * Add an effector.
     *
     * @param effector effector to add
     */
    public void addEffector(final Effector effector) {
        // if (effector.getApplicableTypes().contains(this.getClass()))...
        effectors.add(effector);
        effector.setId(parentWorld.getEffectorIDGenerator().getId());
        changeSupport.firePropertyChange("effectorAdded", null, effector);
    }

    /**
     * Removes an effector.
     *
     * @param effector effector to remove
     */
    public void removeEffector(final Effector effector) {
        effectors.remove(effector);
        changeSupport.firePropertyChange("effectorRemoved", null, effector);

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
            sensor.setId(parentWorld.getSensorIDGenerator().getId());
        }

        changeSupport.firePropertyChange("sensorAdded", null, sensor);
    }

    /**
     * Get the sensor with the specified label, or null if none found.
     * <p>
     * Some common choices: "Smell-Left", "Smell-Center", and "Smell-Right"
     *
     * @param label label to search for
     * @return the associated sensor
     */
    public final Sensor getSensor(final String label) {
        for (Sensor sensor : this.getSensors()) {
            if (sensor.getLabel().equalsIgnoreCase(label)) {
                return sensor;
            }
        }
        return null;
    }

    /**
     * Get the effector with the specified label, or null if none found.
     *
     * @param label label to search for
     * @return the associated sensor
     */
    public final Effector getEffector(final String label) {
        for (Effector effector : this.getEffectors()) {
            if (effector.getLabel().equalsIgnoreCase(label)) {
                return effector;
            }
        }
        return null;
    }

    /**
     * Removes a sensor.
     *
     * @param sensor sensor to remove
     */
    public void removeSensor(final Sensor sensor) {
        sensors.remove(sensor);
        changeSupport.firePropertyChange("sensorRemoved", null, sensor);
    }

    /**
     * Apply impact of all effectors.
     */
    public void updateEffectors() {
        if (effectorsEnabled) {
            for (Effector effector : effectors) {
                effector.update();
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
     * @param offset    offset amount in pixels
     */
    public void addTileSensors(final int numTilesX, final int numTilesY, final int offset) {
        int tileWidth = parentWorld.getWidth() / numTilesX;
        int tileHeight = parentWorld.getHeight() / numTilesY;
        for (int i = 0; i < numTilesX; i++) {
            for (int j = 0; j < numTilesY; j++) {
                addSensor(new TileSensor(this, ((i * tileWidth) + offset), ((j * tileHeight) + offset), tileWidth, tileHeight));
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
    @Producible(idMethod = "getId")
    public double[] getCenterLocation() {
        return new double[] {getCenterX(), getCenterY()};
    }

    /**
     * Returns the center x position of this entity.
     *
     * @return center x coordinate.
     */
    @Producible(idMethod = "getId")
    public double getCenterX() {
        return x;
    }

    /**
     * Returns the center y position of this entity.
     *
     * @return center y coordinate.
     */
    @Producible(idMethod = "getId")
    public double getCenterY() {
        return y;
    }

    /**
     * Set the location of this entity.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void setCenterLocation(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the location of the entity as a double array.
     *
     * @return location of the entity.
     */
    //TODO: Remove
    @Producible(idMethod = "getId")
    public double[] getLocation() {
        return new double[] {x, y};
    }

    /**
     * Set the location of this entity.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    //TODO: Remove
    public void setLocation(double x, double y) {
        setX(x);
        setY(y);
    }

    /**
     * Initialize the animation from stored image location(s).
     */
    public void postSerializationInit() {
        changeSupport = new PropertyChangeSupport(this);
        currentlyHeardPhrases = new ArrayList<String>();
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

    public void setShowSensors(boolean showSensors) {
        this.showSensors = showSensors;
    }

    // Todo: move collisions, blocks etc. to piccolo

    public boolean isBlocked() {
        if (getParentWorld().isObjectsBlockMovement()) {
//            if (hasCollided()) {
//                return true;
//            }
        }
        return false;
    }

    public Behavior getBehavior() {
        return behavior;
    }

    /**
     * Move the object north by the specified amount in pixels.
     *
     * @param amount amount to move by
     */
    @Consumable(idMethod = "getId")
    public void moveNorth(double amount) {
        if (!isBlocked() && (amount != 0)) {
            if (this.isRotating()) {
                setHeading(90);
            }
            setY(y - amount);
        }
    }

    /**
     * Move the object south by the specified amount in pixels.
     *
     * @param amount amount to move by
     */
    @Consumable(idMethod = "getId")
    public void moveSouth(double amount) {
        if (!isBlocked() && (amount != 0)) {
            if (this.isRotating()) {
                setHeading(270);
            }
            setY(y + amount);
        }
    }

    /**
     * Move the object east by the specified amount in pixels.
     *
     * @param amount amount to move by
     */
    @Consumable(idMethod = "getId")
    public void moveEast(double amount) {
        if (!isBlocked() && (amount != 0)) {
            if (this.isRotating()) {
                this.setHeading(0);
            }
            setX(x + amount);
        }
    }

    public void goNorth() {
        moveNorth(manualStraightMovementIncrement);
    }

    public void goSouth() {
        moveSouth(manualStraightMovementIncrement);
    }

    public void goEast() {
        moveEast(manualStraightMovementIncrement);
    }

    public void goWest() {
        moveWest(manualStraightMovementIncrement);
    }

    /**
     * Move the object west by the specified amount in pixels.
     *
     * @param amount amount to move by
     */
    @Consumable(idMethod = "getId")
    public void moveWest(double amount) {
        if (!isBlocked() && (amount != 0)) {
            if (this.isRotating()) {
                setHeading(180);
            }
            setX(x - amount);
        }
    }

    /**
     * Update this object's smell source, if any.
     */
    public void updateSmellSource() {
        if (smellSource != null) {
            smellSource.update();
        }
    }

    /**
     * Add a phrase to the list of things currently being said.
     *
     * @param phrase the phrase to add
     */
    public void speakToEntity(String phrase) {
        currentlyHeardPhrases.add(phrase);
    }

    /**
     * @return the currentlyHeardPhrases
     */
    public List<String> getCurrentlyHeardPhrases() {
        return currentlyHeardPhrases;
    }

    /**
     * Update the entity type of this entity.
     *
     * @param entityType the entity type
     */
    public void setEntityType(EntityType entityType) {
        this.entityType = entityType;
        updateCollisionRadius();
        updateCollisionBound();
    }

    public EntityType getEntityType() {
        return entityType;
    }

    private void updateCollisionRadius() {
        // the collision radius is approximately the circle enclosing the rectangular bound of this object.
        // 1.5 is about sqrt(2), which is the ratio of length of the diagonal line to the side of a square,
        // and the radius is half of that.
        collisionRadius = Math.max(entityType.imageWidth, entityType.imageHeight) * 0.75;
    }

    /**
     * Set collision bound based on the entity type information.
     */
    private void updateCollisionBound() {
        if (collisionBounds.isEmpty()) {
            collisionBounds.put("up", new Line2D.Double());
            collisionBounds.put("down", new Line2D.Double());
            collisionBounds.put("left", new Line2D.Double());
            collisionBounds.put("right", new Line2D.Double());
        }
        collisionBounds.get("up").setLine(
            x + dx,
            y,
            x + entityType.imageWidth + dx,
            y
        );
        collisionBounds.get("down").setLine(
            x + dx,
            y + entityType.imageHeight,
            x + entityType.imageWidth + dx,
            y + entityType.imageHeight
        );
        collisionBounds.get("left").setLine(
            x,
            y + dy,
            x,
            y + entityType.imageHeight + dy
        );
        collisionBounds.get("right").setLine(
            x + entityType.imageWidth,
            y + dy,
            x + entityType.imageWidth,
            y + entityType.imageHeight + dy
        );
    }

    // TODO: Make finer grained. entityTypeChange?
    public void commitEditorChanges() {
        changeSupport.firePropertyChange("propertiesChanged", null, this);
    }

    ;

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * Remove this entity. Assumes it's been removed from parent world already.
     */
    public void delete() {
        parentWorld.deleteEntity(this);
        changeSupport.firePropertyChange("deleted", null, this);
    }

    /**
     * Returns the heading in radians.
     *
     * @return orientation in degrees
     */
    public double getHeadingRadians() {
        return (heading * Math.PI) / 180;
    }

    /**
     * Set the orientation of the creature.
     *
     * @param d the orientation, in degrees
     */
    public void setHeading(final double d) {

        // TOOD: Exception if isRotating is false

        double newHeading = d;
        if (newHeading >= 360) {
            newHeading -= 360;
        }
        if (newHeading < 0) {
            newHeading += 360;
        }
        heading = newHeading;
        changeSupport.firePropertyChange("moved", null, null);
    }


    /**
     * Returns the current heading, in degrees.
     *
     * @return current heading.
     */
    public double getHeading() {
        return heading;
    }


    public double getManualStraightMovementIncrement() {
        return manualStraightMovementIncrement;
    }

    public double getManualMotionTurnIncrement() {
        return manualMotionTurnIncrement;
    }

    /**
     * Rotate left by the specified amount.
     *
     * @param amount amount to turn left. Assumes a positive number.
     */
    //@Consumible(customDescriptionMethod="getId")
    public void turnLeft(double amount) {
        turn(amount);
    }

    /**
     * Turn by the specified amount, positive or negative.
     *
     * @param amount
     */
    //@Consumible(customDescriptionMethod="getId")
    public void turn(double amount) {
        if (amount == 0) {
            return;
        }
        if (!isBlocked()) {
            setHeading(heading + amount);
        }
        changeSupport.firePropertyChange("moved", null, null);

    }

    /**
     * Rotate right by the specified amount.
     *
     * @param amount amount to turn right. Assumes a positive number.
     */
    //@Consumible(customDescriptionMethod="getId")
    public void turnRight(double amount) {
        turn(-amount);
    }

    /**
     * Move the entity in a straight line relative to its current heading.
     *
     * @param amount
     */
    //@Consumible(customDescriptionMethod="getId")
    public void goStraight(double amount) {
        if (amount == 0) {
            return;
        }
        if (!isBlocked()) {
            double radians = getHeadingRadians();
            setX(getX() + (float) (amount * Math.cos(radians)));
            setY(getY() - (float) (amount * Math.sin(radians)));
        }
        changeSupport.firePropertyChange("moved", null, null);
    }

    public void goStraight() {
        double radians = getHeadingRadians();
        dx = manualStraightMovementIncrement * Math.cos(radians);
        dy = -manualStraightMovementIncrement * Math.sin(radians);
    }

    public void goBackwards() {
        double radians = getHeadingRadians();
        dx = -manualStraightMovementIncrement * Math.cos(radians);
        dy = manualStraightMovementIncrement * Math.sin(radians);

    }

    public void turnLeft() {
        dtheta = manualMotionTurnIncrement;
    }

    public void turnRight() {
        dtheta = -manualMotionTurnIncrement;
    }

    public void stopTurning() {
        dtheta = 0;
    }

    public boolean isRotating() {
        return entityType.isRotating;
    }

    /**
     * Add some default sensors and effectors.
     */
    public void addDefaultSensorsEffectors() {

        // Rotating entities are currently a proxy for "agents", though any object
        // can have sensors and effectors
        if (entityType.isRotating) {
            // Add default effectors
            addEffector(new StraightMovement(this,
                "Go-straight"));
            addEffector(new Turning(this, "Go-left",
                Turning.LEFT));
            addEffector(new Turning(this, "Go-right",
                Turning.RIGHT));

            // Add default sensors
            addSensor(new SmellSensor(this, "Smell-Left", Math.PI / 8,
                50));
            addSensor(new SmellSensor(this, "Smell-Center", 0, 0));
            addSensor(new SmellSensor(this, "Smell-Right",
                -Math.PI / 8, 50));

            // Add an object sensor
            addSensor(new ObjectSensor(this, EntityType.SWISS));
        }
    }

    /**
     * Returns the smell, if any, associated with this object.
     *
     * @param sensorLocation location of the sensor detecting the smell of this
     *                       object
     * @return the smell vector, or null if the object is out of range or has no
     * smell
     */
    public double[] getSmellVector(double[] sensorLocation) {
        if (smellSource == null) {
            return null;
        }
        double distanceToSensor = SimbrainMath.distance(getCenterLocation(), sensorLocation);
        if (distanceToSensor < smellSource.getDispersion()) {
            return smellSource.getStimulus(distanceToSensor);
        } else {
            return null;
        }
    }

    /**
     * Check if this entity is colliding with other entity in a given direction.
     *
     * @param direction direction can be "up", "down", "left", "right.
     * @param other     the other entity
     * @return true if collided
     */
    public boolean collideOn(String direction, OdorWorldEntity other) {
        for (Pair p : collisionConditions.get(direction)) {
            if (collisionBounds.get(p.getKey()).intersectsLine(other.collisionBounds.get(p.getValue()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if this entity is colliding with any entity in the world.
     *
     * @param direction direction can be "up", "down", "left", "right.
     * @return true if collided
     */
    public boolean collideOn(String direction) {
        for (OdorWorldEntity i : getEntitiesInCollisionRadius()) {
            if (i != this) {
                if (collideOn(direction, i)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get all entities that are in the collision bound of this entity.
     *
     * @return a list of entities in the collision bound.
     */
    public List<OdorWorldEntity> getEntitiesInCollisionRadius() {
        return parentWorld.getEntityList().stream()
            .filter(i -> isInRadius(i, collisionRadius + i.collisionRadius))
            .collect(Collectors.toList());
    }

    /**
     * Get all entities that are in the given radius.
     *
     * @param radius the radius bound
     * @return a list of entities in the given radius
     */
    public List<OdorWorldEntity> getEntitiesInRadius(double radius) {
        return parentWorld.getEntityList().stream()
            .filter(i -> isInRadius(i, radius))
            .collect(Collectors.toList());
    }

    /**
     * Check if a given entity is in a radius of this entity.
     *
     * @param other  the entity to check
     * @param radius the radius
     * @return true if the given entity is in radius
     */
    public boolean isInRadius(OdorWorldEntity other, double radius) {
        if (other == this) {
            return false;
        }
        double dx = x - other.x;
        double dy = y - other.y;
        return radius * radius > dx * dx + dy * dy;
    }


    // TODO: Put in all missing static objects
    // TODO: Move to separate class?
    /**
     * Type of this object.  These are mapped to images, etc.
     */
    public enum EntityType {
        SWISS("Swiss", false, false, false, 32, 32),
        FLOWER("Flower", false, false, false, 32, 32),
        MOUSE("Mouse", true, true, true, 40, 40),
        AMY("Amy", true, true, true, 96, 96),
        ARNO("Arno", true, true, true, 96, 96),
        BOY("Boy", true, true, true, 96, 96),
        COW("Cow", true, true, true, 96, 96),
        GIRL("Girl", true, true, true, 96, 96),
        JAKE("Jake", true, true, true, 96, 96),
        LION("Lion", true, true, true, 96, 96),
        STEVE("Steve", true, true, true, 96, 96),
        SUSI("Susi", true, true, true, 96, 96);

        /**
         * String description that shows up in dialog boxes.
         */
        private final String description;

        /**
         * Whether the sprite representing this entity is based on heading.
         */
        private boolean isRotating;

        /**
         * Whether this entity type uses sensors by default.
         */
        private boolean useSensors;

        /**
         * Whether this entity type uses effectors by default.
         */
        private boolean useEffectors;

        private int imageWidth;

        private int imageHeight;

        /**
         * Create the entity
         */
        EntityType(
            String description,
            boolean isRotating,
            boolean sensors,
            boolean effectors,
            int imageWidth,
            int imageHeight
        ) {
            this.description = description;
            this.isRotating = isRotating;
            this.useSensors = sensors;
            this.useEffectors = effectors;
            this.imageWidth = imageWidth;
            this.imageHeight = imageHeight;
        }

        @Override
        public String toString() {
            return description;
        }

    }


    // Intializer
    {
        // When checking collision on the right of this entity
        collisionConditions.put("right",
            List.of(
                // if the up bound of this entity is collided with the left of other entity,
                // it is a right collision
                new Pair<>("up", "left"),
                // if down bound of this and left bound of other
                new Pair<>("down", "left"),
                // if right bound of this and up bound of other
                new Pair<>("right", "up"),
                // etc.
                new Pair<>("right", "down")
            )
        );
        collisionConditions.put("left",
            List.of(
                new Pair<>("up", "right"),
                new Pair<>("down", "right"),
                new Pair<>("left", "up"),
                new Pair<>("left", "up")
            )
        );
        collisionConditions.put("down",
            List.of(
                new Pair<>("left", "up"),
                new Pair<>("right", "up"),
                new Pair<>("down", "left"),
                new Pair<>("down", "right")
            )
        );
        collisionConditions.put("up",
            List.of(
                new Pair<>("left", "down"),
                new Pair<>("right", "down"),
                new Pair<>("up", "left"),
                new Pair<>("up", "right")
            )
        );

    }

}