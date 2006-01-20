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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;


/**
 * <b>Wall</b> represents a wall in the world.
 *
 * @author Ryan Bartley
 */
public class Wall extends AbstractEntity {
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
    /** Stimulus. */
    private Stimulus theStimulus = new Stimulus();
    /** Object edible boolean. */
    private boolean edible;
    /** Initial bites value. */
    private final int initBites = 30;
    /** Bites to die value. */
    private int bitesToDie = initBites;
    /** Number of bites value. */
    private int bites;
    /** Resurrection probability value. */
    private double resurrectionProb = 0;

    /**
     * Return the resurrectin probability.
     *
     * @return the resurrection probability
     */
    public double getResurrectionProb() {
        return resurrectionProb;
    }

    /**
     * Sets the resurrection probability.
     *
     * @param resurrectionProb Resurrection probability value
     */
    public void setResurrectionProb(final double resurrectionProb) {
        this.resurrectionProb = resurrectionProb;
    }

    /**
     * Default wall constructor.
     */
    public Wall() {
    }

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
     * Sets the widht.
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
     * @param theWall
     * @param g the world graphics object
     */
    public void paintThis(final Graphics g) {
        g.setColor(new Color(getParent().getWallColor()));
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
     * Return the stimulus.
     *
     * @return the stimulus
     */
    public Stimulus getStimulus() {
        return theStimulus;
    }

    /**
     * Sets the stimulus.
     *
     * @param theStimulus The stimulus
     */
    public void setStimulus(final Stimulus theStimulus) {
        this.theStimulus = theStimulus;
    }

    /**
     * Return the location.
     *
     * @return the location
     */
    public Point getLocation() {
        return new Point(getX() + (getWidth() / 2), getY() + (getHeight() / 2));
    }

    /**
     * Return the bites to die.
     *
     * @return the bites to die
     */
    public int getBitesToDie() {
        return bitesToDie;
    }

    /**
     * Sets the bites to die.
     *
     * @param bitesToDie The bies to die
     */
    public void setBitesToDie(final int bitesToDie) {
        this.bitesToDie = bitesToDie;
    }

    /**
     * Return the edible.
     *
     * @return the edible
     */
    public boolean getEdible() {
        return edible;
    }

    /**
     * Sets the edible.
     *
     * @param edible The edible
     */
    public void setEdible(final boolean edible) {
        this.edible = edible;
    }

    /**
     * Return the bites.
     *
     * @return the bites
     */
    public int getBites() {
        return bites;
    }

    /**
     * Sets the bites.
     *
     * @param bites The bites
     */
    public void setBites(final int bites) {
        this.bites = bites;
    }

    /**
     * Terminates an object that is edible.
     */
    public void terminate() {
        parent.getAbstractEntityList().remove(this);
        parent.getDeadEntityList().add(this);
    }
}
