/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2003 Jeff Yoshimi <www.jeffyoshimi.net>
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
 * <b>AbstractEntity</b> is an abstract class providing a basic 
 * structure for all items in the world.
 * 
 * @author RJB
 *
 */
public abstract class AbstractEntity {
	

	public abstract int getX();
	
	public abstract int getY();
	
	public abstract void setX(int x);
	
	public abstract void setY(int y);
	
	public abstract int getWidth();
	
	public abstract int getHeight();
	
	public abstract boolean isEdible();
	
	public abstract int getBitesToDie();
	
	public abstract int getBites();
	
	public abstract void setBites(int bites);
	
	public abstract void terminate();
	
	/**
	 * Returns a Rectangle describing the loacation of the Entity
	 * (For Calculating Collisions)
	 * @return
	 */
	public abstract Rectangle getRectangle();
	
	public abstract Rectangle getRectangle(Point p);
	
	public abstract OdorWorld getParent();

	public abstract Stimulus getStimulus();
	
	public abstract void setParent(OdorWorld world);
	
	/**
	 * Causes the item to paint itself to the world
	 * @param g
	 */
	public abstract void paintThis(Graphics g);
	
	public abstract Point getLocation();

}
