package org.simbrain.world.threedee;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

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
    /** the static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Moveable.class);

    /** used to put the camera out in front of the moveable so it it's not in the view. */
    private static final float VIEW_LEAD = 0.5f;
    /** arbitrary number used to calculate the current angle. */
    private static final int ROT_CALC_BUFFER = 3600;
    /** the number of degrees in a circle. */
    private static final int DEGREES_IN_A_CIRCLE = 360;

    /**
     * All the inputs for this view sorted by priority. Only one input will be
     * processed in an update. That is the input with updates with the highest
     * priority will block events on a lower priority input.
     */
    private final SortedMap<Integer, Collection<? extends Action>> inputs = Collections
      .synchronizedSortedMap(new TreeMap<Integer, Collection<? extends Action>>());

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
    public void addInput(final int priority, final Collection<Action> input) {
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
     *
     * @param camera determines the perspective of the view
     */
    public void render(final Camera camera) {
        final Vector3f direction = getDirection();

        camera.setDirection(direction);

        Vector3f location = getLocation();

        /*
         * move the view up a little and out in front
         * to improve the view
         */
        location = location.add(0, VIEW_LEAD, 0);
        location = location.add(direction);
        camera.setLocation(location);

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
        for (final Collection<? extends Action> input : inputs.values()) {
            /*
             * if there are events on this input process them and then
             * return
             */
            synchronized (input) {
                if (input.size() > 0) {
                    for (final Action action : input) {
                        if (action.parent != this) {
                            throw new IllegalArgumentException(
                                "actions can only be handled by parent");
                        }

                        action.doAction();
                    }

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
        leftRightRot = (leftRightRot + ROT_CALC_BUFFER) % DEGREES_IN_A_CIRCLE;
        leftRightQuat.fromAngleNormalAxis(leftRightRot * FastMath.DEG_TO_RAD, Y_AXIS);

        /* normalize the up/down angle and then use it to set the up/down quaternion */
        upDownRot = (upDownRot + ROT_CALC_BUFFER) % DEGREES_IN_A_CIRCLE;
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
        LOGGER.trace("location: " + location);
        updateLocation(location);
        LOGGER.trace("direction: " + direction);
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
     * returns an action for turning left.
     *
     * @return an action for turning left.
     */
    public Action left() {
        return new Action() {
            @Override
            void doAction() {
                LOGGER.trace("left: " + super.value);
                leftRightRot += getValue() * rotationSpeed;
            }
        };
    }

    /**
     * returns an action for turning right.
     *
     * @return an action for turning right.
     */
    public Action right() {
        return new Action() {
            @Override
            void doAction() {
                LOGGER.trace("right");
                leftRightRot -= getValue() * rotationSpeed;
            }
        };
    }

    /**
     * returns an action for moving forwards.
     *
     * @return an action for moving forwards.
     */
    public Action forward() {
        return new Action() {
            @Override
            void doAction() {
                LOGGER.trace("forward");
                speed = getValue() * movementSpeed;
            }
        };
    }

    /**
     * returns an action for moving backwards.
     *
     * @return an action for moving backwards.
     */
    public final Action backward() {
        return new Action() {
            @Override
            void doAction() {
                LOGGER.trace("backward");
                speed = 0f - (getValue() * movementSpeed);
            }
        };
    }

    /**
     * returns an action for rising straight up regardless of orientation.
     *
     * @return an action for rising straight up regardless of orientation.
     */
    public final Action rise() {
        return new Action() {
            @Override
            void doAction() {
                LOGGER.trace("rise");
                upSpeed = getValue() * movementSpeed;
            }
        };
    }

    /**
     * returns an action for falling straight down regardless of orientation.
     *
     * @return an action for falling straight down regardless of orientation.
     */
    public final Action fall() {
        return new Action() {
            @Override
            void doAction() {
                LOGGER.trace("fall");
                upSpeed = 0 - (getValue() * movementSpeed);
            }
        };
    }

    /**
     * returns an action for nosing down.
     *
     * @return an action for nosing down.
     */
    public final Action down() {
        return new Action() {
            @Override
            void doAction() {
                LOGGER.trace("down");
                upDownRot += getValue() * rotationSpeed;
            }
        };
    }

    /**
     * returns an action for nosing up.
     *
     * @return an action for nosing up.
     */
    public final Action up() {
        return new Action() {
            @Override
            void doAction() {
                LOGGER.trace("up");
                upDownRot -= getValue() * rotationSpeed;
            }
        };
    }

    /**
     * Enum of actions that can be applied to a Moveable.
     *
     * @author Matt Watson
     */
    public abstract class Action {
        /**
         * Method all action instances use. Not meant to be called this from
         * outside this class.
         */
        abstract void doAction();

        /** used to make sure this Action is not passed to a different agent. */
        private final Moveable parent = Moveable.this;

        /**
         * the value used to determine the amount of the actions change.
         * 1 is the default and the normal full amount. Larger values
         * and negative values are allowed.
         */
        private float value = 1f;

        /**
         * Sets the degree of the action.  0 will result in no change.
         *
         * @param amount the amount to move.
         */
        public void setValue(final float amount) {
            this.value = amount;
        }

        /**
         * Retrieves the movement value for this action.
         *
         * @return the movement value.
         */
        public float getValue() {
            return value;
        }
    }
}
