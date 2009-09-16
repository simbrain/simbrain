package org.simbrain.world.odorworld.entities;

import java.awt.Image;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.behaviors.Behavior;
import org.simbrain.world.odorworld.behaviors.StationaryBehavior;
import org.simbrain.world.odorworld.effectors.Effector;
import org.simbrain.world.odorworld.sensors.Sensor;
import org.simbrain.world.odorworld.sensors.SmellSensor;

/**
 * Adapted and extended from From Developing Games in Java, by David Brackeen.
 */
public abstract class OdorWorldEntity {

    /**
     * Animation used to depict this object. If the animation has one frame this
     * is equivalent to just using a single image to represent it.
     */
    private Animation animation;  

    /** X Position. */
    protected float x;

    /** Y Position. */
    protected float y;

    /** X Velocity. */
    protected float dx = .05f;

    /** Y Velocity. */
    protected float dy = .05f;

    /** Back reference to parent parentWorld. */
    private OdorWorld parentWorld;

    /** Name of this entity. */
    private String name;

    /** Sensors. */
    private List<Sensor> sensors= new ArrayList<Sensor>();

    /** Effectors. */
    private List<Effector> effectors = new ArrayList<Effector>();

    /** Behavior. */
    protected Behavior behavior = new StationaryBehavior();

    /** Smell Source (if any). */
    private SmellSource smellSource = null;

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
    }

    /**
     * Called before update() if the creature collided with a tile vertically.
     */
    public void collideVertical() {
        behavior.collissionY();
    }

    /**
     * Construct an entity from an animoation.
     *
     * @param animation animation to use.
     */
    public OdorWorldEntity(final Animation anim, OdorWorld world) {
        this.animation = anim;
        this.parentWorld = world;
        anim.start();
    }

    /**
     * Construct an odor worl dentity from a single image location.
     *
     * @param imageLocation the image location 
     */
    public OdorWorldEntity(final String imageLocation, OdorWorld world) {
        this.animation = new Animation(imageLocation);
        this.parentWorld = world;
        animation.start();
    }

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
    public void setX(final float x) {
        this.x = x;
    }

    /**
     * Sets this OdorWorldEntity's current y position.
     */
    public void setY(final float y) {
        this.y = y;
    }

    /**
     * Gets this OdorWorldEntity's width, based on the size of the current
     * image.
     */
    public int getWidth() {
        return animation.getImage().getWidth(null);
    }

    /**
     * Gets this OdorWorldEntity's height, based on the size of the current
     * image.
     */
    public int getHeight() {
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
        return name;
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
     * Add an effector.
     *
     * @param effector effector to add
     */
    public void addEffector(final Effector effector) {
        parentWorld.fireEffectorAdded(effector);
    }

    /**
     * Add a sensor.
     *
     * @param sensor sensor to add
     */
    public void addSensor(final Sensor sensor) {
        sensors.add(sensor);
        parentWorld.fireSensorAdded(sensor);
    }

    /**
     * Apply impact of all effectors.
     */
    public void applyEffectors() {
        for (Effector effector : effectors) {
            effector.activate();
        }
    }

    /**
     * Update all sensors.
     */
    public void updateSensors() {
        for (Sensor sensor : sensors) {
            sensor.update();
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
        this.x = x;
        this.y = y;
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
    }

    /**
     * Initialize the animation from stored image location(s). 
     */
    public void postSerializationInit() {
        getAnimation().initializeImages(); //TODO
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
}
