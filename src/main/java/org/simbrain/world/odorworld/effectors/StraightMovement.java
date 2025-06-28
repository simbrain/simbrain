package org.simbrain.world.odorworld.effectors;

import org.simbrain.util.UserParameter;
import org.simbrain.workspace.Consumable;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Effector for straight ahead movement.
 *
 * @author Jeff Yoshimi
 */
public class StraightMovement extends Effector {

    /**
     * Amount by which to move ahead. Set externally.
     */
    private double amount = 0;

    /**
     * Default scaling factor.
     */
    public static final double DEFAULT_SCALING_FACTOR = 1;

    /**
     * Effector moves agent ahead by scaling factor times amount.
     */
    @UserParameter(label = "Base Movement Amount",
            description = "Effector moves agent ahead by scaling factor times amount.",
            order = 4)
    private double scalingFactor = DEFAULT_SCALING_FACTOR;

    public StraightMovement() {
        super();
    }

    /**
     * Construct a copy of a straight movement effector.
     *
     * @param straightMovement the straight movement effector to copy
     */
    public StraightMovement(StraightMovement straightMovement) {
        super(straightMovement);
        this.amount = straightMovement.amount;
    }

    @Override
    public void update(OdorWorldEntity parent) {
        parent.setSpeed(amount * scalingFactor);
        this.amount = 0;
    }

    public double getAmount() {
        return amount;
    }

    @Consumable(customDescriptionMethod = "getAttributeDescription", priority = 1)
    public void setAmount(double amount) {
        this.amount = amount;
    }

    /**
     * Add an amount to go straight. Allows for multiple "moves" to be
     * aggregated.
     *
     * @param amount amount to turn.
     */
    @Consumable(customDescriptionMethod = "getAddAmountDescription",
        defaultVisibility = false)
    public void addAmount(double amount) {
        this.amount += amount;
    }

    public String getAddAmountDescription() {
        return getAttributeDescription() + " (Add)";
    }

    public double getScalingFactor() {
        return scalingFactor;
    }

    public void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
    }

    @Override
    public String getLabel() {
        if (super.getLabel().isEmpty()) {
            return "Move Straight";
        } else {
            return super.getLabel();
        }
    }

    @Override
    public String getName() {
        return "Straight Movement";
    }

    @Override
    public StraightMovement copy() {
        return new StraightMovement(this);
    }
}