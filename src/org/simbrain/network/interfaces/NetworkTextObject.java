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
package org.simbrain.network.interfaces;

/**
 * <b>NetworkTextObject</b> is a string of text in a neural network, typically
 * used to label elements of a neural network simulation.
 */
public class NetworkTextObject {

    /** Reference to parent root network of this text object. */
    private final RootNetwork parent;

    /** x-coordinate of this object in 2-space. */
    private double x;

    /** y-coordinate of this object in 2-space. */
    private double y;

    /** The main data */
    private String text = "";

    //TODO: Add formatting info

    /**
     * Construct the text object.
     *
     * @param parent root network
     * @param x x position
     * @param y y position
     */
    public NetworkTextObject(RootNetwork parent, double x, double y) {
        this.parent = parent;
        this.x = x;
        this.y = y;
    }

    /**
     * @return the x
     */
    public double getX() {
        return x;
    }

    /**
     * @param x the x to set
     */
    public void setX(double x) {
        this.x = x;
    }

    /**
     * @return the y
     */
    public double getY() {
        return y;
    }

    /**
     * @param y the y to set
     */
    public void setY(double y) {
        this.y = y;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "(" + Math.round(x) + "," + Math.round(y) + "):" + text;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }


}
