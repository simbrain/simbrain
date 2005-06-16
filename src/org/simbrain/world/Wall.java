/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world;


import java.awt.Point;

import javax.swing.JPanel;

/**
 * <b>Wall</b> represents a wall in the world.
 * 
 * @author Ryan Bartley
 */
public class Wall {
	
	private int upperLeftX;
	private int upperLeftY;
	private int width;
	private int height;
	
	public Wall(){		
	}

	/**
	 * @return Returns the height.
	 */
	public int getHeight() {
		return height;
	}
	/**
	 * @param height The height to set.
	 */
	public void setHeight(int height) {
		this.height = height;
	}
	/**
	 * @return Returns the upperLeftX.
	 */
	public int getUpperLeftX() {
		return upperLeftX;
	}
	/**
	 * @param upperLeftX The upperLeftX to set.
	 */
	public void setUpperLeftX(int upperLeftX) {
		this.upperLeftX = upperLeftX;
	}
	/**
	 * @return Returns the upperLeftY.
	 */
	public int getUpperLeftY() {
		return upperLeftY;
	}
	/**
	 * @param upperLeftY The upperLeftY to set.
	 */
	public void setUpperLeftY(int upperLeftY) {
		this.upperLeftY = upperLeftY;
	}
	/**
	 * @return Returns the width.
	 */
	public int getWidth() {
		return width;
	}
	/**
	 * @param width The width to set.
	 */
	public void setWidth(int width) {
		this.width = width;
	}

}
