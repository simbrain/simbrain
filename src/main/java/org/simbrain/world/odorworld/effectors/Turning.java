package org.simbrain.world.odorworld.effectors;

import org.simbrain.util.UserParameter;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.couplings.CouplingManagerKt;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Effector for turning.
 *
 * @author Jeff Yoshimi
 */
public class Turning extends Effector {

    /**
     * Default direction.
     */
    public static final double DEFAULT_DIRECTION = 1;

    /**
     * Direction value to turn left.
     */
    public static final double LEFT = 1;

    /**
     * Direction value to turn right.
     */
    public static final double RIGHT = -1;

    /**
     * Turn by amount times direction, which encodes direction: 1 for left, -1
     * for right.
     */
    @UserParameter(label = "Turning Direction and Weight",
            description = "Turn by amount times direction, which encodes direction: 1 for left, -1 for right.",
            order = 4)
    private double direction = DEFAULT_DIRECTION;

    /**
     * Default amount.
     */
    public static final double DEFAULT_AMOUNT = 0;

    /**
     * Amount to turn in radians.
     */
    @UserParameter(label = "Turning Amount",
            description = "Amount to turn in radians.",
            order = 5)
    private double amount = DEFAULT_AMOUNT;

    /**
     * Default label.
     */
    public static final String DEFAULT_LABEL = "Turn";

    /**
     * Construct a turning effector.
     *
     * @param direction amount turn in radians.
     */
    public Turning(double direction) {
        this.direction = direction;
    }

    /**
     * Construct a copy of a turning effector.
     *
     * @param turning the turning effector to copy
     */
    public Turning(Turning turning) {
        super(turning);
        this.direction = turning.direction;
        this.amount = turning.amount;
    }

    /**
     * Default constructor for {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     *
     * NOTE:
     * {@link org.simbrain.world.odorworld.dialogs.AddEffectorDialog} handles the set up of {@link #parent}.
     * When calling this directly, remember to set up the required field {@link #parent} accordingly.
     */
    public Turning() {
        super();
    }

    @Override
    public void update(OdorWorldEntity parent) {
        parent.setHeading(parent.getHeading() + direction * amount);
        this.amount = 0;
    }

    public double getAmount() {
        return amount;
    }

    @Consumable(customDescriptionMethod = "getAttributeDescription", priority = CouplingManagerKt.HIGH_PRIORITY)
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * Add an amount to turning.  Allows for multiple "turns" to be aggregated.
     *
     * @param amount amount to turn.
     */
    @Consumable(customDescriptionMethod = "getAddAmountDescription")
    public void addAmount(double amount) {
        this.amount += amount;
    }

    public String getAddAmountDescription() {
        return getAttributeDescription() + " (Add)";
    }

    /**
     * Return a string indicating the direction of turning motion
     */
    public String getDirectionString() {
        if (direction < 0) {
            return "Right";
        } else if (direction > 0) {
            return "Left";
        } else {
            return "Nowhere";
        }
    }

    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction;
    }

    @Override
    public String getLabel() {
        if (super.getLabel().isEmpty()) {
            return "Turn " + getDirectionString();
        } else {
            return super.getLabel();
        }
    }

    @Override
    public String getName() {
        return "Turning Effector";
    }

    @Override
    public Turning copy() {
        return new Turning(this);
    }
}
