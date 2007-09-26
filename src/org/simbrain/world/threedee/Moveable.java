package org.simbrain.world.threedee;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.jme.math.FastMath;
import com.jme.math.Quaternion;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;

/**
 * Implements the basic functionality of a moveable view.
 * 
 * @author Matt Watson
 */
public abstract class Moveable implements Viewable {
    /**
     * all the inputs for this view sorted by priority.  Only one
     * input will be processed in an update.  That is the input with updates
     * with the highest priority will blck events on a lower priority input
     */
    private SortedMap<Integer, Input> inputs = Collections.synchronizedSortedMap(
            new TreeMap<Integer, Input>());
    
    /** the number of degrees each turn event rotates the view */
    private final float rotationSpeed = 2.5f;
    /** how fast the view moves in a linear direction */
    private final float movementSpeed = .1f;
    
    /** current angle in the y/x plane */
    private float upDownRot = 0;
    /** current angle in the x/z plane */
    private float leftRightRot = 0;
    
    /** x axis of the world */
    private static final Vector3f X_AXIS = new Vector3f(1f, 0f, 0f);
    /** y axis of the world */
    private static final Vector3f Y_AXIS = new Vector3f(0f, 1f, 0f);
    
    /** current forward speed (may be negative) */
    private float speed = 0f;
    /** current up speed (may be negative) */
    private float upSpeed = 0f;
    
    /**
     * adds an input with the given priority (lower has more priority)
     * 
     * @param priority the priority of the input provided
     * @param input the input for this view
     */
    public void addInput(int priority, Input input) {
        inputs.put(priority, input);
    }

    /**
     * initializes the implementation with the given direction
     * and location.  This is essentially a suggestion. Implementations 
     * can use these objects and modify them or ignore them.
     * 
     * @param direction
     * @param location
     */
    public abstract void init(Vector3f direction, Vector3f location);

    /**
     * updates the camera direction and location based on
     * getDirection and getLocation.  Sets the camera up and
     * left axis for proper culling
     */
    public void render(Camera camera) {
        Vector3f direction = getDirection();
        
        camera.setDirection(direction);
        camera.setLocation(getLocation());
        
        Vector3f left = direction.cross(Y_AXIS).normalizeLocal();
        Vector3f up = left.cross(direction).normalizeLocal();
        
        camera.setLeft(left);
        camera.setUp(up);
    }

    /**
     * called on a regular basis by a top level class such as 
     * Environment to update the view.  Checks for inputs events
     * and handles any on the highest priority input with events
     */
    public void updateView() {
        speed = 0f;
        upSpeed = 0f;
        
        /* input is synchronized but we need to lock over the iterator */
        synchronized(inputs) {
            for (Input input : inputs.values()) {
                /* if there are events on this input process them and then return */
                if (input.actions.size() > 0) {;
                    input.doActions(this);
                    doUpdates();
                    
                    return;
                }
            }
        }
    }
    
    /**
     * does the necessary processing for any changes to the view
     */
    protected void doUpdates() {
        /* these are for doing proper rotations */
        Quaternion leftRightQuat = new Quaternion();
        Quaternion upDownQuat = new Quaternion();
        
        /* normalize the left/right angle and then use it to set the left/right quat */
        leftRightRot = (leftRightRot + 3600) % 360;
        leftRightQuat.fromAngleNormalAxis(leftRightRot * FastMath.DEG_TO_RAD, Y_AXIS);
        
        /* normalize the up/down angle and then use it to set the up/down quat */
        upDownRot = (upDownRot + 3600) % 360;
        upDownQuat.fromAngleAxis(upDownRot * FastMath.DEG_TO_RAD, X_AXIS);
      
        /* get copies of the current direction and location */
        Vector3f direction = (Vector3f) getDirection().clone();
        Vector3f location = (Vector3f) getLocation().clone();
        
        /* combine the two quaternions */
        Quaternion sumQuat = leftRightQuat.mult(upDownQuat);
        
        /* set the new direction */
        direction.addLocal(sumQuat.getRotationColumn(2)).normalizeLocal();        
        
        /* 
         * update the location by adding a vector that is defined
         * by the current direction multiplied by the current speed
         */
        location.addLocal(direction.mult(speed));
        location.setY(location.getY() + upSpeed);
        
        /* update with the new values */
        updateLocation(location);
        updateDirection(direction);
    }
    
    /**
     * return the current committed location
     * 
     * @return the current location
     */
    protected abstract Vector3f getLocation();
    
    /**
     * return the current committed direction
     * 
     * @return the current direction
     */
    protected abstract Vector3f getDirection();
    
    /**
     * update the location tenatively
     * 
     * @param location the new location
     */
    protected abstract void updateLocation(Vector3f location);
    
    /**
     * update the direction tenatively
     * 
     * @param direction the new direction
     */
    protected abstract void updateDirection(Vector3f direction);
    
    /**
     * sets the current speed
     * 
     * @param speed the new speed
     */
    protected void setSpeed(float speed) {
        this.speed = speed;
    }
    
    /**
     * returns the current speed
     * 
     * @return the current speed
     */
    public float getSpeed() {
        return speed;
    }
    
    /**
     * returns the maximum movement speed
     * 
     * @return the maximum movement speed
     */
    public float getMovementSpeed() {
        return movementSpeed;
    }
    
    /**
     * An input instance holds all the action that have occurred
     * and to update the view
     * 
     * @author Matt Watson
     */
    public static class Input {
        /**
         * the current set of actions
         */
        private Set<Action> actions = new HashSet<Action>();
        
        /**
         * sets a current action
         * 
         * @param action
         */
        public void set(Action action) {
            actions.add(action);
        }
        
        /**
         * removes a current action
         * 
         * @param action the 
         */
        public void clear(Action action) {
            actions.remove(action);
        }
        
        /**
         * executes the current actions against the provided Moveable
         * 
         * @param moveable the view to apply the actions to
         */
        private void doActions(Moveable moveable) {
            for (Action action : actions) {
                action.doAction(moveable);
            }
        }
        
        /**
         * presents a debug string with the number of current actions
         */
        public String toString() {
            return "actions: " + actions.size();
        }
    }
    
    /**
     * enum of actions that can be applied to a Moveable
     * 
     * @author Matt Watson
     */
    public enum Action {
        /** turn left */
        LEFT {
            void doAction(Moveable agent) {
                agent.leftRightRot += agent.rotationSpeed;
            }
        }, 
        
        /** turn right */
        RIGHT {
            void doAction(Moveable agent) {
                agent.leftRightRot -= agent.rotationSpeed;
            }
        }, 
        
        /** move forwards */
        FORWARD {
            void doAction(Moveable agent) {
                agent.speed = agent.movementSpeed;
            }
        },
        
        /** move backwards */
        BACKWARD {
            void doAction(Moveable agent) {
                agent.speed = 0f - agent.movementSpeed;
            }
        },
        
        /** rise straight up regardless of orientation */
        RISE {
            void doAction(Moveable agent) {
                agent.upSpeed = agent.movementSpeed;
            }
        },
        
        /** fall straight down regardless of orientation */
        FALL {
            void doAction(Moveable agent) {
                agent.upSpeed = 0 - agent.movementSpeed;
            }
        },
        
        /** nose down */
        DOWN {
            void doAction(Moveable agent) {
                agent.upDownRot += agent.rotationSpeed;
            }
        },
        
        /** nose up */
        UP {
            void doAction(Moveable agent) {
                agent.upDownRot -= agent.rotationSpeed;
            }
        };
                
        /**
         * method all action instances use. not meant to be 
         * called this from outside this class
         */
        abstract void doAction(Moveable agent);
    }
}
