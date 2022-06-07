/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
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

    /**
     * Construct the straight movement effector.
     *
     * @param parent parent entity.
     * @param label  descriptive label
     */
    public StraightMovement(OdorWorldEntity parent, String label) {
        super(parent, label);
    }

    /**
     * Construct the straight movement effector with default values.
     *
     * @param parent parent entity.
     */
    public StraightMovement(OdorWorldEntity parent) {
        super(parent);
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

    /**
     * Default constructor for {@link org.simbrain.util.propertyeditor.AnnotatedPropertyEditor}.
     *
     * NOTE:
     * {@link org.simbrain.world.odorworld.dialogs.AddEffectorDialog} handles the set up of {@link #parent}.
     * When calling this directly, remember to set up the required field {@link #parent} accordingly.
     */
    public StraightMovement() {
        super();
    }

    @Override
    public void update() {
        parent.setSpeed(amount * scalingFactor);
        this.amount = 0;
    }

    @Override
    public void setParent(OdorWorldEntity parent) {
        this.parent = parent;
    }

    public double getAmount() {
        return amount;
    }

    @Consumable(customDescriptionMethod = "getAttributeDescription")
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