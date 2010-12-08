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

import java.util.ArrayList;
import java.util.List;

import org.simbrain.world.odorworld.entities.RotatingEntity;

/**
 * Effector which rotates agent by a specified amount.
 */
public class RotationEffector implements Effector {

    /** Reference to parent object. */
    private RotatingEntity parentObject;

    /** Amount to turn on next update. */
    private double turnAmount;

    /** Makes the difference between Right and Left and how much. */
    private double scaleFactor = 1;

    /**
     * Constructor.
     */
    public RotationEffector(final RotatingEntity agent) {
        parentObject = agent;
    }

    /**
     * {@inheritDoc}
     */
    public void activate() {
        if (turnAmount == 0) {
            return;
        }
        if (!parentObject.isBlocked()) {
            // double offset =  turnIncrement * scaleFactor;
            parentObject.setHeading(parentObject.getHeading()
                    + turnAmount);
            turnAmount = 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<Class> getApplicableTypes() {
        ArrayList<Class> list = new ArrayList<Class>();
        list.add(RotatingEntity.class);
        return list;
    }

    /**
     * @return the parentObject
     */
    public RotatingEntity getParent() {
        return parentObject;
    }

    /**
     * @return the scaleFactor
     */
    public double getScaleFactor() {
        return scaleFactor;
    }

    /**
     * @param scaleFactor the scaleFactor to set
     */
    public void setScaleFactor(final double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    /**
     * Update the turn amount. Note that this additive, since multiple
     * attributes can use the same effector. Not sure about this design. (TODO).
     *
     * @param turnAmount the turnAmount to set
     */
    public void setTurnAmount(double value) {
        if (!parentObject.isBlocked()) {
            turnAmount += (value * scaleFactor);
        }
    }

}
