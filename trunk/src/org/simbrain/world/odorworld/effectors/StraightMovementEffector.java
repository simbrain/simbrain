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
 * Move the agent in a straight line relative to its current heading. Movement
 * amount is value * movementAmont * scaleFactor is the amount the agent moves
 * forward.
 */
public class StraightMovementEffector implements Effector {

    /** Translation. */
    private double movementAmount = 10;

    /** ScaleFactor. */
    private double scaleFactor = .1;

    /** Reference to parent object. */
    private RotatingEntity parentObject;

    /** Value of straight movement which can vary in real time. */
    private double currentValue;

    /**
     * Construct the straight movement effector.
     *
     * @param parentObject
     */
    public StraightMovementEffector(RotatingEntity parentObject) {
        this.parentObject = parentObject;
    }

    /**
     * {@inheritDoc}
     */
    public void activate() {

        if (currentValue == 0) {
            return;
        }
        if(parentObject.getParentWorld().isObjectsBlockMovement()) {
            if (parentObject.hasCollided()) {
                return;
            }
        }

        double offset = (currentValue * movementAmount) * scaleFactor;
        double heading = parentObject.getHeadingRadians();
        parentObject.setX(parentObject.getX()
                + (float) (offset * Math.cos(heading)));
        parentObject.setY(parentObject.getY()
                - (float) (offset * Math.sin(heading)));
    }

    /**
     * @return the parentObject
     */
    public RotatingEntity getParent() {
        return parentObject;
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
     * @param currentValue the currentValue to set
     */
    public void setCurrentValue(double currentValue) {
        this.currentValue = currentValue;
    }
    
    

}
