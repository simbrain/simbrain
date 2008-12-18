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
package org.simbrain.world.odorworld.entities;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.environment.TwoDEntity;
import org.simbrain.world.odorworld.LifeCycle;
import org.simbrain.world.odorworld.OdorWorld;

/**
 * <b>OdorWorldEntity</b> is a base implementation for painting objects in the Odor World.
 */
public interface OdorWorldEntity extends TwoDEntity {

    /**
     * Returns the bounds of the entity.
     *
     * @return Rectangle that represents the current boundaries of this entity
     */
    public Rectangle getBounds();

    /**
     * @return the parent OdorWorld
     */
    public OdorWorld getParent();
    
    /**
     * @param world parent world
     */
    public void setParent(OdorWorld world);
    
    /**
     * Update the entity.
     */
    public void update();
    
    /**
     * Method for painting entity.
     */
    public void paintEntity(Component component, final Graphics g);
    
    /**
     * Handle any initialization needed after deserializing.
     */
    public void postSerializationInit();
    
    /**
     * @return true if the object stops agents, false otherwise.
     */
    public boolean inhibitsMovement();
    
    /** 
     * @param inhibitsMovement
     */
    public void setInhibitsMovement(boolean inhibitsMovement);

    /**
     * Smell source, if any.  Null if none.
     */
    public SmellSource getSmellSource();
    
    /**
     * Attach a smell source, or set to null via null.
     */
    public void setSmellSource(SmellSource source);

    /**
     * Lifecycle object, if this object can be "eaten."  Null if not.
     */
    public LifeCycle getLifeCycleObject();

    /**
     * Location of object.
     *
     * @return location of object.
     */
	public double[] getLocation();

	/**
	 * Set location of object.
	 * 
	 * @param location
	 */
	public void setLocation(double[] location);
}
