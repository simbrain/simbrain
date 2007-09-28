package org.simbrain.world.threedee;

import org.apache.log4j.Logger;

import com.jme.math.Vector3f;

/**
 * An implementation of Moveable that provides simple collision
 * handling and hugs the terrain of the environment provided
 * 
 * @author Matt Watson
 */
public class Agent extends Moveable {
    private static final float COLLISION_RADIUS = 0.5f;
    private static final float HOVER_HEIGHT = 0.5f;
    
    /** a logger based on this class and the agent name */
    private final Logger logger;
    
    /** the environment this agent lives in */
    private Environment environment;
    
    /** the current location */
    private volatile Vector3f direction;
    /** the current direction */
    private volatile Vector3f location;
    
    /** tentative location */
    private volatile Vector3f tenativeLocation;
    /** tentative direction */
    private volatile Vector3f tenativeDirection;

    /**
     * create a new Agent with the given name
     * 
     * @param name the agents name
     */
    public Agent(String name) {
        logger = Logger.getLogger("" + Agent.class + '.' + name);
        
        logger.debug("created new Agent: " + name);
    }
    
    /**
     * returns the current direction
     */
    @Override
    protected Vector3f getDirection() {
        return direction;
    }

    /**
     * returns the current location
     */
    @Override
    protected Vector3f getLocation() {
        return location;
    }

    /**
     * sets the current and tentative locations and directions
     */
    @Override
    public void init(Vector3f direction, Vector3f location) {
        this.direction = direction;
        this.location = location;
        tenativeDirection = direction;
        tenativeLocation = location;
    }

    /**
     * updates the tenative direction
     */
    @Override
    protected void updateDirection(Vector3f direction) {
        tenativeDirection = direction;
    }

    /**
     * updates the tenative location
     */
    @Override
    protected void updateLocation(Vector3f location) {
        tenativeLocation = location;
    }
    
    /**
     * calls the Moveable version and sets the height
     */
    @Override
    protected void doUpdates() {
        super.doUpdates();
        
        setHeight();
    }
    
    /**
     * updates the height based on the environment's terrain
     */
    private void setHeight()
    {
        float height = environment.getFloorHeight(tenativeLocation);
        
        if (!Float.isNaN(height)) tenativeLocation.setY(height + HOVER_HEIGHT);
    }
    
    /**
     * implements the logic for collisions
     * 
     * @param collision
     */
    public void collision(Collision collision)
    {
        float speed = getSpeed();
      
        if (speed == 0) return;      
        
        tenativeDirection = (Vector3f) direction.clone();
        tenativeLocation = (Vector3f) location.clone();
    }

    /**
     * returns the tentative spatial data for this agent.  This is 
     * used after an update but before a commit.
     * 
     * @return the tentative spatial data
     */
    public SpatialData getTenative() {
        return new SpatialData(tenativeLocation, COLLISION_RADIUS);
    }

    /**
     * sets the location and direction to the tentative values
     */
    public void commit() {
        direction = (Vector3f) tenativeDirection.clone();
        location = (Vector3f) tenativeLocation.clone();
    }
    
    /**
     * sets the environment for the agent
     * 
     * @param environment the agent's environment
     */
    void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}