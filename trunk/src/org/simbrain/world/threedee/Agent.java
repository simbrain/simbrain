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
    /** a logger based on this class and the agent name */
    private final Logger logger;
    
    /** the environment this agent lives in */
    private Environment environment;
    
    /** the current location */
    private volatile Vector3f direction;
    /** the current direction */
    private volatile Vector3f location;
    
    /** tenative location */
    private volatile Vector3f tenativeLocation;
    /** tenative direction */
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
     * sets the current and tenative locations and directions
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
        
        if (!Float.isNaN(height)) tenativeLocation.setY(height + 2f);
    }
    
    /**
     * does special updates after a collision
     */
    private void doCollisionUpdate() {       
        tenativeLocation.addLocal(tenativeDirection.mult(getSpeed()));
        setHeight();
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
      
        Vector3f colVector = collision.point().subtract(tenativeLocation).normalizeLocal();
        float initialLength = tenativeDirection.length();
      
        tenativeDirection.subtractLocal(colVector);
              
        float finalLength = tenativeDirection.length();
        float newSpeed = (finalLength / initialLength) * speed;
        float movementSpeed = getMovementSpeed();
      
        if (speed > 0) {
            if (newSpeed > movementSpeed) {
                speed = movementSpeed;
            } else {
                speed = newSpeed;
            }
        } else {
            if (newSpeed < -movementSpeed) {
                speed = -movementSpeed;
            } else {
                speed = newSpeed;
            }
        }
        
        setSpeed(speed);
        
        tenativeDirection.setY(0);
        tenativeDirection.normalizeLocal();
      
        doCollisionUpdate();
    }

    /**
     * returns the tenative spatial data for this agent.  This is 
     * used after an update but before a commit.
     * 
     * @return the tenative spatial data
     */
    public SpatialData getTenative() {
        return new SpatialData(tenativeLocation, 1.0f);
    }

    /**
     * sets the location and direction to the tenative values
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