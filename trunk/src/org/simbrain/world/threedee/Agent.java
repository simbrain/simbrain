package org.simbrain.world.threedee;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.workspace.WorkspaceComponent;
import org.simbrain.world.threedee.environment.Environment;

import com.jme.math.Vector3f;

/**
 * An implementation of Moveable that provides simple collision handling and
 * hugs the terrain of the environment provided.
 *
 * @author Matt Watson
 */
public class Agent extends Moveable implements Entity {
    /** the radius used to determine whether this agent has collided with another. */
    private static final float COLLISION_RADIUS = 0.5f;
    /** the height that the agent 'hovers' above the terrain. */
    private static final float HOVER_HEIGHT = 0.5f;

    /** the name of this agent. */
    private final String name;
    /** a logger based on this class and the agent name. */
    private final Logger logger;
    /** the environment this agent lives in. */
    private Environment environment;
    /** the current location. */
    private volatile Vector3f direction;
    /** the current direction. */
    private volatile Vector3f location;
    /** tentative location. */
    private volatile Vector3f tenativeLocation;
    /** tentative direction. */
    private volatile Vector3f tenativeDirection;
    /** determines the limits (x, z) of the world. */
    private int limit;
    /** */
    private Bindings bindings;
    
    /**
     * Create a new Agent with the given name.
     *
     * @param name the agents name
     */
    public Agent(final String name, WorkspaceComponent<?> component) {
        logger = Logger.getLogger("" + Agent.class + '.' + name);

        this.name = name;
        this.bindings = new AgentBindings(this, component);
        
        logger.debug("created new Agent: " + name);

        direction = new Vector3f(0, 0, 0);
        location = new Vector3f(0, 0, 0);
        tenativeLocation = new Vector3f(0, 0, 0);
        tenativeDirection = new Vector3f(0, 0, 0);
    }

    /**
     * returns the name of the agent.
     *
     * @return the name of the agent.
     */
    public String getName() {
        return name;
    }

    /**
     * sets the limit (x, z) for the world.
     *
     * @param limit the limit (x, z) for the world.
     */
    public void setLimit(final int limit) {
        this.limit = limit;
    }

    /**
     * returns the current direction.
     *
     * @return the current direction.
     */
    @Override
    protected Vector3f getDirection() {
        return direction;
    }

    /**
     * returns the current location.
     *
     * @return the current location.
     */
    @Override
    public Vector3f getLocation() {
        return location;
    }

    /**
     * Sets the current and tentative locations and directions.
     *
     * @param direction the direction vector that controls the view (updateable.)
     * @param location the location of the view (updateable.)
     */
    @Override
    public void init(final Vector3f direction, final Vector3f location) {
        this.direction = direction;
        this.location = location;
        tenativeDirection = direction;
        tenativeLocation = location;
        setHeight();
    }

    /**
     * Updates the tentative direction.
     *
     * @param direction the new tentative direction.
     */
    @Override
    protected void updateDirection(final Vector3f direction) {
        tenativeDirection = direction;
    }

    /**
     * Updates the tentative location.
     *
     * @param location the new tentative location.
     */
    @Override
    protected void updateLocation(final Vector3f location) {
        tenativeLocation = location;
    }

    /**
     * Calls the Moveable version and sets the height.
     */
    @Override
    protected void doUpdates() {
        super.doUpdates();

        /*
         * if the agent has gone beyond it's limit
         * move it to the other side the environment
         */
        float x = tenativeLocation.getX();
        float z = tenativeLocation.getZ();

        if (Math.abs(x) > limit) { tenativeLocation.setX(-1 * x); }
        if (Math.abs(z) > limit) { tenativeLocation.setZ(-1 * z); }

        setHeight();
    }

    /**
     * Updates the height based on the environment's terrain.
     */
    private void setHeight() {
        final float height = environment.getFloorHeight(tenativeLocation);

        if (!Float.isNaN(height)) { tenativeLocation.setY(height + HOVER_HEIGHT); }
    }

    /**
     * Implements the logic for collisions.
     *
     * @param collision the collision detail.
     */
    public void collision(final Collision collision) {
        final float speed = getSpeed();

        if (speed == 0) { return; }

        tenativeDirection = (Vector3f) direction.clone();
        tenativeLocation = (Vector3f) location.clone();
    }

    /**
     * Returns the tentative spatial data for this agent. This is used after an
     * update but before a commit.
     *
     * @return the tentative spatial data
     */
    public SpatialData getTenative() {
        return new SpatialData(tenativeLocation, COLLISION_RADIUS);
    }

    /**
     * Sets the location and direction to the tentative values.
     */
    public void commit() {
        direction = (Vector3f) tenativeDirection.clone();
        location = (Vector3f) tenativeLocation.clone();
    }

    /**
     * Sets the environment for the agent.
     *
     * @param environment the agent's environment
     */
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }
    
    public Environment getEnvironment() {
        return environment;
    }

    public List<Odor> getOdors() {
        return Collections.singletonList(new Odor("red", 10, this));
    }
    
    Bindings getBindings() {
        return bindings;
    }
}
