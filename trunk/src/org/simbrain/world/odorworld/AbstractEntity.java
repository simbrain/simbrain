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
package org.simbrain.world.odorworld;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;


/**
 * <b>AbstractEntity</b> is an abstract class providing a basic  structure for all items in the world.
 *
 * @author RJB
 */
public abstract class AbstractEntity {
    /**
     * @return the x position of the entity
     */
    public abstract int getX();

    /**
     * @return the y position
     */
    public abstract int getY();

    /**
     * @param x the x to set
     */
    public abstract void setX(int x);

    /**
     * @param y the y to set
     */
    public abstract void setY(int y);

    /**
     * @return the width
     */
    public abstract int getWidth();

    /**
     * @return the height
     */
    public abstract int getHeight();

    /**
     * @return the edibility
     */
    public abstract boolean getEdible();

    /**
     * @param edible the boolean value for edibility
     */
    public abstract void setEdible(boolean edible);

    /**
     * @return the number of bites to die
     */
    public abstract int getBitesToDie();

    /**
     * @return the number of bites
     */
    public abstract int getBites();

    /**
     * @param bites the bites to set
     */
    public abstract void setBites(int bites);

    /**
     * @return the probability of resurrection
     */
    public abstract double getResurrectionProb();

    /**
     * @param bites the resurrection prob to set
     */
    public abstract void setResurrectionProb(double bites);

    /**
     */
    public abstract void terminate();

    /**
     * Returns a Rectangle describing the location of the Entity (For Calculating Collisions).
     *
     * @return Rectangle that represents the current boundaries of this entity
     */
    public abstract Rectangle getRectangle();

    /**
     * @param p the point around which to build the testing bounds
     * @return the testing bounds
     */
    public abstract Rectangle getRectangle(Point p);

    /**
     * @return the parent OdorWorld
     */
    public abstract OdorWorld getParent();

    /**
     * @return the stimulus produced by this entity
     */
    public abstract Stimulus getStimulus();

    /**
     * @param world the parent OdorWorld to set
     */
    public abstract void setParent(OdorWorld world);

    /**
     * @return the location of this entity
     */
    public abstract Point getLocation();
}
