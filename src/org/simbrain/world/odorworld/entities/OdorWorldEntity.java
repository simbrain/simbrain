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

import org.simbrain.util.UserParameter;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.AttributeContainer;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.RectangleCollisionBound;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.effectors.StraightMovement;
import org.simbrain.world.odorworld.effectors.Turning;
import org.simbrain.world.odorworld.sensors.ObjectSensor;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;
import org.simbrain.world.odorworld.sensors.LocationSensor;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Parent class for all Odor World objects.
 */
public class OdorWorldEntity implements EditableObject, AttributeContainer {

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
     * Actual collision bound.
     */
    private RectangleCollisionBound collisionBound;

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
    @UserParameter(label = "Straight movement", order = 10)
    protected double manualStraightMovementIncrement = 1;

    /**
     * The velocity vector used to update the entity's position when it is
     * manually moved using the keyboard commands.  This should not be set by
     * the user.  It is computed from the entity's {@link
     * #manualStraightMovementIncrement} and {@link #heading}.
     */
    private Point2D.Double manualMovementVelocity = new Point2D.Double();

    /**
     * Set to true when keyboard is being used for movement (instead of couplings, etc.).
     */
    private boolean manualMode = false;

    /**
     * Current heading / orientation.
     */
    @UserParameter(label = "heading", description = "heading", order = 2)
    private double heading = DEFAULT_HEADING;

    /**
     * Change in current heading.
     */
    private double dtheta;

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
     * Smell Source (if any). Initialize to random smell source with 10
     * components.
     */
    private SmellSource smellSource = new SmellSource(10);

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
     * If true, show peripheral attributes.
     */
    @UserParameter(label = "Show Attributes", description = "Show Attributes (Sensors and Effectors)", order = 30)
    private boolean showSensors = true;

    /**
     * Things currently being said by talking entities.
     */
    private List<String> currentlyHeardPhrases = new ArrayList<String>();

    /**
     * If true, the agent's heading is always updated based on its velocity.
     */
    @UserParameter(label = "Heading based on velocity", description = "If true, the agent's heading is updated at each iteration based on its velocity.", order = 100)
    private boolean updateHeadingBasedOnVelocity = false;

    /**
     * Construct an entity.
     *
     * @param world parent world of entity
     */
    public OdorWorldEntity(OdorWorld world) {
        this.parentWorld = world;
        collisionBound = new RectangleCollisionBound(
            new Rectangle2D.Double(
                0,
                0,
                getEntityType().getImageWidth(),
                getEntityType().getImageHeight()
            ));
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
        sensorsEnabled = type.isUseSensors();
        effectorsEnabled = type.isUseEffectors();
    }

    /**
     * Updates this OdorWorldEntity's Animation and its position based on the
     * velocity.
     */
    public void update() {
        simpleMotion();
        updateCollisionBound();

        updateSensors();
        updateEffectors();

        // For Backwards compatibility
        if (currentlyHeardPhrases != null) {
            currentlyHeardPhrases.clear();
        }

        if(updateHeadingBasedOnVelocity) {
            updateHeadingBasedOnVelocity();
        }

        changeSupport.firePropertyChange("updated", null, this);

    }

    public void manualMovementUpdate() {
        if (manualMode) {
            updateCollisionBound();
            simpleMotion();
            changeSupport.firePropertyChange("updated", null, this);
            changeSupport.firePropertyChange("manuallyUpdated", null, this);
        }
    }

    public void resetManualVelocity() {
        manualMovementVelocity.setLocation(0.0, 0.0);
    }

    /**
     * Simple motion control.
     */
    private void simpleMotion() {
        double dx, dy;
        if (manualMode) {
            dx = manualMovementVelocity.getX();
            dy = manualMovementVelocity.getY();
        } else {
            dx = this.dx;
            dy = this.dy;
        }
        if (dx != 0) {
            if (!collideOn("x")) {
                setX(x + dx);
            }
        }
        if (dy != 0) {
            if (!collideOn("y")) {
                setY(y + dy);
            }
        }
        if (dtheta != 0) {
            setHeading(heading + dtheta);
            double dthetaRad = Math.toRadians(-dtheta);
            double dx2 = Math.cos(dthetaRad) * dx - Math.sin(dthetaRad) * dy;
            double dy2 = Math.sin(dthetaRad) * dx + Math.cos(dthetaRad) * dy;
            if (manualMode) {
                manualMovementVelocity.setLocation(dx2, dy2);
            } else {
                this.dx = dx2;
                this.dy = dy2;
            }
        }
    }

    /**
     * Sets this OdorWorldEntity's current x position.
     *
     * @param newx
     */
    @Consumable(idMethod = "getId")
    public void setX(final double newx) {
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
        updateCollisionBound();
        changeSupport.firePropertyChange("moved", null, null);

    }

    /**
     * Sets this OdorWorldEntity's current y position.
     *
     * @param newy
     */
    @Consumable(idMethod = "getId")
    public void setY(final double newy) {
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
        updateCollisionBound();
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
        updateCollisionBound();
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
        updateCollisionBound();
    }

    public Point2D.Double getManualMovementVelocity() {
        return manualMovementVelocity;
    }

    public RectangleCollisionBound getCollisionBound() {
        return collisionBound;
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

    public void updateCollisionBound() {
        if (manualMode) {
            collisionBound.setVelocity(manualMovementVelocity.getX(), manualMovementVelocity.getY());
        } else {
            collisionBound.setVelocity(dx, dy);
        }
        collisionBound.setLocation(x, y);
        collisionBound.setSize(entityType.getImageWidth(), entityType.getImageHeight()); // TODO: optimize
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
                addSensor(new LocationSensor(this, ((i * tileWidth) + offset), ((j * tileHeight) + offset), tileWidth, tileHeight));
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
        return x + entityType.getImageWidth() / 2;
    }

    /**
     * Returns the center y position of this entity.
     *
     * @return center y coordinate.
     */
    @Producible(idMethod = "getId")
    public double getCenterY() {
        return y + entityType.getImageHeight() / 2;
    }

    /**
     * Set the location of this entity.
     *
     * @param x x coordinate
     * @param y y coordinate
     */
    public void setCenterLocation(double x, double y) {
        this.x = x - entityType.getImageWidth() / 2;
        this.y = y - entityType.getImageHeight() / 2;
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
     * Perform initialization of objects after de-serializing.
     */
    public void postSerializationInit() {
        changeSupport = new PropertyChangeSupport(this);
        currentlyHeardPhrases = new ArrayList<String>();
        for(Sensor sensor : sensors) {
            sensor.postSerializationInit();
        }
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

    /**
     * Move the object north by the specified amount in pixels.
     *
     * @param amount amount to move by
     */
    @Consumable(idMethod = "getId")
    public void moveNorth(double amount) {
        if (!collideOn("y") && (amount > 0)) {
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
        if (!collideOn("y") && (amount > 0)) {
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
        if (!collideOn("x") && (amount > 0)) {
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
        if (!collideOn("left") && (amount > 0)) {
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

    public void speakToEntity(String phrase) {
        currentlyHeardPhrases.add(phrase);
    }

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
        collisionBound = new RectangleCollisionBound(
            new Rectangle2D.Double(
                0,
                0,
                getEntityType().getImageWidth(),
                getEntityType().getImageHeight()
            ));
        updateCollisionBound();
    }

    public EntityType getEntityType() {
        return entityType;
    }

    // TODO: Make finer grained. entityTypeChange?
    public void commitEditorChanges() {
        changeSupport.firePropertyChange("propertiesChanged", null, this);
    }

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
        setHeading(heading + amount);
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
        double radians = getHeadingRadians();
        dx = amount * Math.cos(radians);
        dy = -amount * Math.sin(radians);
        changeSupport.firePropertyChange("moved", null, null);
    }

    public void goStraight() {
        double radians = getHeadingRadians();
        double dx = manualStraightMovementIncrement * Math.cos(radians);
        double dy = -manualStraightMovementIncrement * Math.sin(radians);
        if (manualMode) {
            manualMovementVelocity.setLocation(dx, dy);
        } else {
            this.dx = dx;
            this.dy = dy;
        }
    }

    public void goBackwards() {
        double radians = getHeadingRadians();
        double dx = -manualStraightMovementIncrement * Math.cos(radians);
        double dy = manualStraightMovementIncrement * Math.sin(radians);
        if (manualMode) {
            manualMovementVelocity.setLocation(dx, dy);
        } else {
            this.dx = dx;
            this.dy = dy;
        }
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
        return entityType.isRotating();
    }

    /**
     * Set the heading to be in the direction of current velocity.
     */
    public void updateHeadingBasedOnVelocity() {
        boolean velocityIsNonZero = !((dx == 0) && (dy == 0));
        if (velocityIsNonZero) {
            setHeading(Math.toDegrees(Math.atan2(getVelocityX(), getVelocityY())) - 90);
        }
    }

    /**
     * Add some default sensors and effectors.
     */
    public void addDefaultSensorsEffectors() {

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

        if(parentWorld.getWrapAround()) {
            double [] locO = getCenterLocation();
            double[] dxdy = new double[2];
            dxdy[0] = sensorLocation[0] - locO[0];
            dxdy[1] = sensorLocation[1] - locO[1];
            double dxWrap = parentWorld.getWidth() - Math.abs(dxdy[0]);
            double dyWrap = parentWorld.getHeight() - Math.abs(dxdy[1]);

            double [] dists = new double[4];
            dists[0] = Math.sqrt(dxdy[0]*dxdy[0] + dxdy[1]*dxdy[1]);
            dists[1] = Math.sqrt(dxWrap*dxWrap + dxdy[1]*dxdy[1]);
            dists[2] = Math.sqrt(dxdy[0]*dxdy[0] + dyWrap*dyWrap);
            dists[3] = Math.sqrt(dxWrap*dxWrap + dyWrap*dyWrap);

            double [] stimulus = new double[smellSource.getStimulusDimension()];
            int obCount = 0;
            for(int ii=0; ii<4; ++ii) {
                if(dists[ii] > smellSource.getDispersion()) {
                    obCount++;
                    continue;
                }
                stimulus = SimbrainMath.addVector(stimulus, smellSource.getStimulus(dists[ii]));
            }
            if(obCount == 4) {
                return  null;
            } else {
                return stimulus;
            }
        } else {
            double distanceToSensor = SimbrainMath.distance(getCenterLocation(), sensorLocation);
            double [] stimulus = smellSource.getStimulus(distanceToSensor);
            if (distanceToSensor < smellSource.getDispersion()) {
                return stimulus;
            } else {
                return null;
            }
        }

    }

    /**
     * Check if this entity is colliding with other entity in a given
     * direction.
     *
     * @param direction direction can be "x", "y", or "xy".
     * @param other     the other entity
     * @return true if collided
     */
    public boolean collideOn(String direction, OdorWorldEntity other) {
        return collisionBound.collide(direction, other.collisionBound);
    }

    /**
     * Check if this entity is colliding with any entity in the world.
     *
     * @param direction direction can be "x", "y".
     * @return true if collided
     */
    public boolean collideOn(String direction) {
        if (!getParentWorld().isObjectsBlockMovement()) {
            return false;
        }
        for (OdorWorldEntity i : getEntitiesInCollisionRadius()) {
            if (i != this) {
                if (collideOn(direction, i)) {
                    return true;
                }
            }
        }
        return !parentWorld.getWrapAround() && collisionBound.collide(direction, parentWorld.getWorldBoundary());
    }

    /**
     * Get all entities that are in the collision bound of this entity.
     *
     * @return a list of entities in the collision bound.
     */
    public List<OdorWorldEntity> getEntitiesInCollisionRadius() {
        return parentWorld.getEntityList().stream()
            .filter(i -> collisionBound.isInCollisionRadius(i.collisionBound))
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
        double dx = getCenterX() - other.getCenterX();
        double dy = getCenterY() - other.getCenterY();
        return radius * radius > dx * dx + dy * dy;
    }

    public boolean isManualMode() {
        return manualMode;
    }

    public void setManualMode(boolean manualMode) {
        this.manualMode = manualMode;
    }

    /**
     * Returns the name of the closest nearby object, if any, in a fixed radius.
     *
     * @return the name of the nearby object or an empty string if there is none
     */
    @Producible(idMethod = "getId")
    public String getNearbyObjects() {
        List<OdorWorldEntity> entities = this.getEntitiesInRadius(7);
        //TODO: Need them ordered by distance
        return entities.isEmpty() ? "" : entities.get(0).getEntityType().getDescription();
    }

    public void setManualMotionTurnIncrement(double manualMotionTurnIncrement) {
        this.manualMotionTurnIncrement = manualMotionTurnIncrement;
    }

    public void setManualStraightMovementIncrement(double manualStraightMovementIncrement) {
        this.manualStraightMovementIncrement = manualStraightMovementIncrement;
    }

    public boolean isUpdateHeadingBasedOnVelocity() {
        return updateHeadingBasedOnVelocity;
    }

    public void setUpdateHeadingBasedOnVelocity(boolean updateHeadingBasedOnVelocity) {
        this.updateHeadingBasedOnVelocity = updateHeadingBasedOnVelocity;
    }
}