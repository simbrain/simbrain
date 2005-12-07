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
    private int x;
    private int y;
    private int height;
    private int width;
    private OdorWorld parent;
    private Stimulus theStimulus = new Stimulus();
    private boolean edible;
    private final int initBites = 30;
    private int bitesToDie = initBites;
    private int bites;
    private double resurrectionProb = 0;

    public double getResurrectionProb() {
        return resurrectionProb;
    }

    public void setResurrectionProb(final double resurrectionProb) {
        this.resurrectionProb = resurrectionProb;
    }

    public Wall() {
    }

    public Wall(final OdorWorld parentWorld) {
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

    public void setHeight(final int height) {
        this.height = height;
    }

    public void setWidth(final int width) {
        this.width = width;
    }

    public void setX(final int x) {
        this.x = x;
    }

    public void setY(final int y) {
        this.y = y;
    }

    public Rectangle getRectangle() {
        return new Rectangle(x, y, width, height);
    }

    public OdorWorld getParent() {
        return parent;
    }

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

    public Rectangle getRectangle(final Point p) {
        return new Rectangle(p.x, p.y, width, height);
    }

    public Stimulus getStimulus() {
        return theStimulus;
    }

    public void setStimulus(final Stimulus theStimulus) {
        this.theStimulus = theStimulus;
    }

    public Point getLocation() {
        return new Point(getX() + (getWidth() / 2), getY() + (getHeight() / 2));
    }

    public int getBitesToDie() {
        return bitesToDie;
    }

    public void setBitesToDie(final int bitesToDie) {
        this.bitesToDie = bitesToDie;
    }

    public boolean getEdible() {
        return edible;
    }

    public void setEdible(final boolean edible) {
        this.edible = edible;
    }

    public int getBites() {
        return bites;
    }

    public void setBites(final int bites) {
        this.bites = bites;
    }

    public void terminate() {
        parent.getAbstractEntityList().remove(this);
        parent.getDeadEntityList().add(this);
    }
}
