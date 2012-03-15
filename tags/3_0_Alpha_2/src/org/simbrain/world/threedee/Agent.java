package org.simbrain.world.threedee;

import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.simbrain.world.threedee.environment.Environment;

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
    private Logger logger;
    /** the environment this agent lives in. */
    private Environment environment;
    /** the current location. */
    private volatile Vector direction;
    /** the current direction. */
    private volatile Point location;
    /** tentative direction. */
    private volatile Vector tenativeDirection;
    /** tentative location. */
    private volatile Point tenativeLocation;
    /** determines the limits (x, z) of the world. */
    private int limit;
    /** */
    //private AgentBindings bindings;

    /**
     * Create a new Agent with the given name.
     *
     * @param name the agents name.
     * @param component the parent component.
     */
    public Agent(final String name, final ThreeDeeComponent component) {
        logger = Logger.getLogger("" + Agent.class + '.' + name);

        this.name = name;
        //this.bindings = new AgentBindings(this, component);

        logger.debug("created new Agent: " + name);

        direction = new Vector(0, 0, 10).normalize();
        location = new Point(0, 0, 0);
        tenativeDirection = direction;
        tenativeLocation = location;
    }

    protected Object readResolve() {
        super.readResolve();

        logger = Logger.getLogger("" + Agent.class + '.' + name);

        //bindings.setInputs();

        return this;
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
    public Vector getDirection() {
        return direction;
    }
    
    /**
     * Sets the current direction.
     *
     * @param v the new direction.
     */
    public void setDirection(final Vector v) {        
        this.direction = v;
        this.tenativeDirection = v;
    }

    /**
     * returns the current location.
     *
     * @return the current location.
     */
    @Override
    public Point getLocation() {
        return location;
    }

    /**
     * Sets the current direction.
     *
     * @param p The new location.
     */
    public void setTentativeLocation(final Point p) {
        this.tenativeLocation = p;
    }
    
    /**
     * Sets the current and tentative locations and directions.
     *
     * @param direction the direction vector that controls the view (updateable.)
     * @param location the location of the view (updateable.)
     */
//    public void init(Renderer renderer, Camera cam, int width, int height) {//final Vector direction, final Point location) {
////        cam.setDirection(direction.toVector3f());
////        cam.setLocation(location.toVector3f());
//        
////        System.out.println("setting w, h: " + width + ", " + height);
//        
////        this.width = width;
////        this.height = height;
//    }

    /**
     * Updates the tentative direction.
     *
     * @param direction the new tentative direction.
     */
    @Override
    protected void updateDirection(final Vector direction) {
        tenativeDirection = direction;
    }

    /**
     * Updates the tentative location.
     *
     * @param location the new tentative location.
     */
    @Override
    protected void updateLocation(final Point location) {
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

        if (Math.abs(x) > limit) { x = (-1 * x); }
        if (Math.abs(z) > limit) { z = (-1 * z); }

        tenativeLocation = new Point(x, tenativeLocation.getY(), z);
        
        setFloor(environment.getFloorHeight(tenativeLocation));
    }

    /**
     * Updates the height based on the environment's terrain.
     */
    public void setHeight() {
        final float height = environment.getFloorHeight(tenativeLocation);

        if (!Float.isNaN(height)) { tenativeLocation = new Point(tenativeLocation.getX(),
           height + HOVER_HEIGHT, tenativeLocation.getZ()); }
    }

    /**
     * Updates the height based on the environment's terrain.
     */
    public void setFloor(final float height) {
        if (!Float.isNaN(height)) { tenativeLocation = new Point(tenativeLocation.getX(),
           height + HOVER_HEIGHT, tenativeLocation.getZ()); }
    }
    
    /**
     * Implements the logic for collisions.
     *
     * @param collision the collision detail.
     */
    public void collision(final Collision collision) {
        final float speed = getSpeed();

        if (speed == 0) { return; }

        tenativeDirection = getDirection();
        tenativeLocation = getLocation();
    }

    /**
     * Returns the tentative spatial data for this agent. This is used after an
     * update but before a commit.
     *
     * @return the tentative spatial data
     */
    public SpatialData getTentative() {
        return new SpatialData(tenativeLocation, COLLISION_RADIUS);
    }

    /**
     * Sets the location and direction to the tentative values.
     */
    public void commit() {
        direction = tenativeDirection;
        location = tenativeLocation;
    }
    
    /**
     * Sets the environment for the agent.
     *
     * @param environment the agent's environment
     */
    public void setEnvironment(final Environment environment) {
        this.environment = environment;
    }
    
    /**
     * Returns this agent's environment.
     * 
     * @return This agent's environment.
     */
    public Environment getEnvironment() {
        return environment;
    }

    /**
     * {@inheritDoc}
     */
    public List<Odor> getOdors() {
        return Collections.singletonList(new Odor("red", this));
    }
    
//    /**
//     * Returns the bindings for this agent.
//     * 
//     * @return The bindings for this agent.
//     */
//    public AgentBindings getBindings() {
//        return bindings;
//    }
    
    public String toString() {
        return "agent[" + name + "]";
    }
}
