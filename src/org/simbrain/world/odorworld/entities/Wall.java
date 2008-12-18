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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;

import org.simbrain.util.environment.SmellSource;
import org.simbrain.util.environment.TwoDEnvironment;
import org.simbrain.world.odorworld.LifeCycle;
import org.simbrain.world.odorworld.OdorWorld;


/**
 * <b>Wall</b> represents a wall in the world.
 *
 * @author Ryan Bartley
 */
public class Wall implements OdorWorldEntity {
    
	/** X value. */
    private int x;
    
    /** Y value. */
    private int y;
    
    /** Height value. */
    private int height;
    
    /** Width value. */
    private int width;
    
    /** Parent world. */
    private OdorWorld parent;
    
    /** Wall color. */
    Color wallColor = Color.black;

	/** Smell of this entity, if any. */
	private SmellSource smellSource;
	
	/** Life cycle of this entity, if it can be eaten. */
	private LifeCycle lifeCycle;

    /** Whether this entity inhibits movement. */
	private boolean inhibitsMovement;

	/**
     * Creates a wall in OdorWorld.
     *
     * @param parentWorld Current world
     */
    public Wall(final OdorWorld parentWorld) {
        parent = parentWorld;
    }

    /**
     * Return the height.
     *
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    /**
     * Return the width.
     *
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * Return the x.
     *
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * Return the y.
     *
     * @return the y
     */
    public int getY() {
        return y;
    }

    /**
     * Sets the height.
     *
     * @param height The height
     */
    public void setHeight(final int height) {
        this.height = height;
    }

    /**
     * Sets the width.
     *
     * @param width The width
     */
    public void setWidth(final int width) {
        this.width = width;
    }

    /**
     * Sets the x.
     *
     * @param x The x
     */
    public void setX(final int x) {
        this.x = x;
    }

    /**
     * Sets the y.
     *
     * @param y The y
     */
    public void setY(final int y) {
        this.y = y;
    }

    /**
     * Return the rectangle.
     *
     * @return the rectangle
     */
    public Rectangle getRectangle() {
        return new Rectangle(x, y, width, height);
    }

    /**
     * Return the parent world.
     *
     * @return the parent world
     */
    public OdorWorld getParent() {
        return parent;
    }

    /**
     * Sets the parent world.
     *
     * @param world The parent world
     */
    public void setParent(final OdorWorld world) {
        parent = world;
    }

    /**
     * Implements abstract paintThis() from AbstractEntity.
     *
     * @param g the world graphics object
     */
    public void paintThis(final Graphics g) {
    	//g.setColor(parent.getWa());
        g.fillRect(getX(), getY(), getWidth(), getHeight());
    }
    
    /**
     * Return the rectangle at the point.
     *
     * @return the rectangle at the point
     * @param p Point
     */
    public Rectangle getRectangle(final Point p) {
        return new Rectangle(p.x, p.y, width, height);
    }

    /**
     * {@inheritDoc}
     */
	public void update() {
		// No implementation
	}

    /**
     * {@inheritDoc}
     */
	public TwoDEnvironment getEnvironment() {
		return parent;
	}

	/**
	 * Suggested = actual for static entities.
	 *
	 * @return location
	 */
	public double[] getLocation() {
		return new double[]{x,y};
	}


    /**
     * {@inheritDoc}
     */
	public void setLocation(double[] location) {
		x = (int) location[0];
		y = (int) location[0];
	}

    /**
     * {@inheritDoc}
     */
	public double[] getSuggestedLocation() {
		return getLocation();
	}

    /**
     * {@inheritDoc}
     */
	public void setSuggestedLocation(double[] location) {
		setLocation(location);
	}

    /**
     * {@inheritDoc}
     */
	public Rectangle getBounds() {
		return null; // TODO
	}


    /**
     * {@inheritDoc}
     */
	public void paintEntity(Component component, Graphics g) {
        g.setColor(wallColor);
        Rectangle rect = getBounds();
        g.fillRect(rect.x, rect.y, rect.width, rect.height);
	}

    /**
     * {@inheritDoc}
     */
	public void postSerializationInit() {
		// TODO Auto-generated method stub
	}

    /**
     * {@inheritDoc}
     */
	public boolean inhibitsMovement() {
		return inhibitsMovement;
	}

    /**
     * {@inheritDoc}
     */
	public void setInhibitsMovement(boolean inhibitsMovement) {
		this.inhibitsMovement = inhibitsMovement;
	}

    /**
     * {@inheritDoc}
     */
	public void setSmellSource(SmellSource source) {
		smellSource = source;
	}

    /**
     * {@inheritDoc}
     */
	public SmellSource getSmellSource() {
		return smellSource;
	}

    /**
     * {@inheritDoc}
     */
	public LifeCycle getLifeCycleObject() {
		return lifeCycle;
	}

}
