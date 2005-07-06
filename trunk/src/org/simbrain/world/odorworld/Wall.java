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
package org.simbrain.world.odorworld;


import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * <b>Wall</b> represents a wall in the world.
 * 
 * @author Ryan Bartley
 */
public class Wall extends AbstractEntity {
	
	private int x;
	private int y;
	private int height;
	private int width;
	private OdorWorld parent;
	
	public Wall(){
	}
	
	public Wall(OdorWorld parentWorld){
		parent = parentWorld;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}
	
	public Rectangle getRectangle(){
		return new Rectangle(x,y,width,height);
	}

	public OdorWorld getParent() {
		return parent;
	}

	public void setParent(OdorWorld world) {
		parent = world;
	}

	/**
	 * @param theWall
	 * @param g
	 */
	public void paintThis(Graphics g) {
		g.setColor(getParent().getWallColor());
		g.fillRect(getX(),getY(),getWidth(),getHeight());
	}

}
