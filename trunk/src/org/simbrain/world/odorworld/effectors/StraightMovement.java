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

import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Effector for straight ahead movement.
 *
 * @author Jeff Yoshimi
 */
public class StraightMovement extends Effector {

    /** Amount by which to move ahead. */
    private double amount = 0;

    //TODO: Add a settable scaling factor?

    /**
     * Construct the straight movement effector.
     *
     * @param parent parent entity.
     * @param label descriptive label
     */
    public StraightMovement(RotatingEntity parent, String label) {
        super(parent, label);
    }

    @Override
    public void activate() {
        ((RotatingEntity) parent).goStraight(amount);
    }

    /**
     * @return the amount
     */
    public double getAmount() {
        return amount;
    }

    /**
     * @param amount the amount to set
     */
    public void setAmount(double amount) {
        this.amount = amount;
    }

}
