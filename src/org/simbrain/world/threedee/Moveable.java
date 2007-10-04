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
     * All the inputs for this view sorted by priority. Only one input will be
     * processed in an update. That is the input with updates with the highest
     * priority will block events on a lower priority input.
     */
    private final SortedMap<Integer, Input> inputs = Collections
            .synchronizedSortedMap(new TreeMap<Integer, Input>());

    /** The number of degrees each turn event rotates the view. */
    private final float rotationSpeed = 2.5f;

    /** How fast the view moves in a linear direction. */
    private final float movementSpeed = .1f;

    /** Current angle in the y/x plane. */
    private float upDownRot = 0;

    /** Current angle in the x/z plane. */
    private float leftRightRot = 0;

    /** X axis of the world. */
    private static final Vector3f X_AXIS = new Vector3f(1f, 0f, 0f);

    /** Y axis of the world. */
    private static final Vector3f Y_AXIS = new Vector3f(0f, 1f, 0f);

    /** Current forward speed (may be negative). */
    private float speed = 0f;

    /** Current up speed (may be negative). */
    private float upSpeed = 0f;

    /**
     * Adds an input with the given priority (lower has more priority).
     * 
     * @param priority the priority of the input provided
     * @param input the input for this view
     */
    public void addInput(final int priority, final Input input) {
        inputs.put(priority, input);
    }

    /**
     * Initializes the implementation with the given direction and location.
     * This is essentially a suggestion. Implementations can use these objects
     * and modify them or ignore them.
     * 
     * @param direction the direction
     * @param location the location
     */
    public abstract void init(Vector3f direction, Vector3f location);

    /**
     * Updates the camera direction and location based on getDirection and
     * getLocation. Sets the camera up and left axis for proper culling.
     */
    public void render(final Camera camera) {
        final Vector3f direction = getDirection();

        camera.setDirection(direction);
        camera.setLocation(getLocation().add(0, .5f, 0));

        final Vector3f left = direction.cross(Y_AXIS).normalizeLocal();
        final Vector3f up = left.cross(direction).normalizeLocal();

        camera.setLeft(left);
        camera.setUp(up);
    }

    /**
     * Called on a regular basis by a top level class such as Environment to
     * update the view. Checks for inputs events and handles any on the highest
     * priority input with events.
     */
    public void updateView() {
        speed = 0f;
        upSpeed = 0f;

        /* input is synchronized but we need to lock over the iterator */
        synchronized (inputs) {
            for (final Input input : inputs.values()) {
                /*
                 * if there are events on this input process them and then
                 * return
                 */
                if (input.actions.size() > 0) {
                    input.doActions(this);
                    doUpdates();

                    return;
                }
            }
        }
    }

    /**
     * Does the necessary processing for any changes to the view.
     */
    protected void doUpdates() {
        /* these are for doing proper rotations */
        final Quaternion leftRightQuat = new Quaternion();
        final Quaternion upDownQuat = new Quaternion();

        /*
         * normalize the left/right angle and then use it to set the left/right
         * quaternion
         */
        leftRightRot = (leftRightRot + 3600) % 360;
        leftRightQuat.fromAngleNormalAxis(leftRightRot * FastMath.DEG_TO_RAD, Y_AXIS);

        /* normalize the up/down angle and then use it to set the up/down quat */
        upDownRot = (upDownRot + 3600) % 360;
        upDownQuat.fromAngleAxis(upDownRot * FastMath.DEG_TO_RAD, X_AXIS);

        /* get copies of the current direction and location */
        final Vector3f direction = (Vector3f) getDirection().clone();
        final Vector3f location = (Vector3f) getLocation().clone();

        /* combine the two quaternions */
        final Quaternion sumQuat = leftRightQuat.mult(upDownQuat);

        /* set the new direction */
        direction.addLocal(sumQuat.getRotationColumn(2)).normalizeLocal();

        /*
         * update the location by adding a vector that is defined by the current
         * direction multiplied by the current speed
         */
        location.addLocal(direction.mult(speed));
        location.setY(location.getY() + upSpeed);

        /* update with the new values */
        updateLocation(location);
        updateDirection(direction);
    }

    /**
     * Return the current committed location.
     * 
     * @return the current location
     */
    protected abstract Vector3f getLocation();

    /**
     * Return the current committed direction.
     * 
     * @return the current direction
     */
    protected abstract Vector3f getDirection();

    /**
     * Update the location tentatively.
     * 
     * @param location the new location
     */
    protected abstract void updateLocation(Vector3f location);

    /**
     * Update the direction tentatively.
     * 
     * @param direction the new direction
     */
    protected abstract void updateDirection(Vector3f direction);

    /**
     * Sets the current speed.
     * 
     * @param speed the new speed
     */
    protected void setSpeed(final float speed) {
        this.speed = speed;
    }

    /**
     * Returns the current speed.
     * 
     * @return the current speed
     */
    public float getSpeed() {
        return speed;
    }

    /**
     * Returns the maximum movement speed.
     * 
     * @return the maximum movement speed
     */
    public float getMovementSpeed() {
        return movementSpeed;
    }

    /**
     * An input instance holds all the action that have occurred and to update
     * the view.
     * 
     * @author Matt Watson
     */
    public static class Input {
        /**
         * The current set of actions.
         */
        private final Set<Action> actions = new HashSet<Action>();

        /**
         * Sets a current action.
         * 
         * @param action the action to set
         */
        public void set(final Action action) {
            actions.add(action);
        }

        /**
         * Removes a current action.
         * 
         * @param action the action to remove
         */
        public void clear(final Action action) {
            actions.remove(action);
        }

        /**
         * Executes the current actions against the provided Moveable.
         * 
         * @param moveable the view to apply the actions to
         */
        private void doActions(final Moveable moveable) {
            for (final Action action : actions) {
                if (action != null) {
                    action.doAction(moveable);
                }
            }
        }

        /**
         * Presents a debug string with the number of current actions.
         */
        @Override
        public String toString() {
            return "actions: " + actions.size();
        }
    }

    /**
     * Enum of actions that can be applied to a Moveable.
     * 
     * @author Matt Watson
     */
    public enum Action {
        /** Turn left. */
        LEFT {
            @Override
            void doAction(final Moveable agent) {
                agent.leftRightRot += agent.rotationSpeed;
            }
        },

        /** Turn right. */
        RIGHT {
            @Override
            void doAction(final Moveable agent) {
                agent.leftRightRot -= agent.rotationSpeed;
            }
        },

        /** Move forwards. */
        FORWARD {
            @Override
            void doAction(final Moveable agent) {
                agent.speed = agent.movementSpeed;
            }
        },

        /** Move backwards. */
        BACKWARD {
            @Override
            void doAction(final Moveable agent) {
                agent.speed = 0f - agent.movementSpeed;
            }
        },

        /** Rise straight up regardless of orientation. */
        RISE {
            @Override
            void doAction(final Moveable agent) {
                agent.upSpeed = agent.movementSpeed;
            }
        },

        /** Fall straight down regardless of orientation. */
        FALL {
            @Override
            void doAction(final Moveable agent) {
                agent.upSpeed = 0 - agent.movementSpeed;
            }
        },

        /** Nose down. */
        DOWN {
            @Override
            void doAction(final Moveable agent) {
                agent.upDownRot += agent.rotationSpeed;
            }
        },

        /** Nose up. */
        UP {
            @Override
            void doAction(final Moveable agent) {
                agent.upDownRot -= agent.rotationSpeed;
            }
        };

        /**
         * Method all action instances use. Not meant to be called this from
         * outside this class.
         */
        abstract void doAction(Moveable agent);
    }
}
