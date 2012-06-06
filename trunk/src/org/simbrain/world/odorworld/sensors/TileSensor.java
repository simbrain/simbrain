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
package org.simbrain.world.odorworld.sensors;

import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * A sensor which detects whether the entity is in a given sector or "tile" of
 * the world. Broadly inspired by "place cells".
 *
 * @author jyoshimi
 */
public class TileSensor extends Sensor {

    /**
     * Current value of the sensor; activationAmount if "active", 0 otherwise.
     */
    private double value = 0;

    /** Value to return when the tile sensor is activated. */
    private double activationAmount = 1;

    /** Upper left corner. */
    private int x;

    /** Upper left corner. */
    private int y;

    /** Upper left corner. */
    private int width;

    /** Upper left corner. */
    private int height;

    /**
     * Construct a tile sensor.
     *
     * @param entity parent entity
     * @param x upper left
     * @param y upper right
     * @param width width in pixels
     * @param height height
     */
    public TileSensor(OdorWorldEntity entity, int x, int y, int width,
            int height) {
        this.parent = entity;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        setLabel("Tile (" + x + "," + y + "):" + width + "x" + height);
    }

    /**
     * {@inheritDoc}
     */
    public void update() {
        double entityX = parent.getCenterLocation()[0];
        double entityY = parent.getCenterLocation()[1];
        boolean xone = entityX > x;
        boolean xtwo = entityX < (x + width);
        boolean yone = entityY > y;
        boolean ytwo = entityY < (y + height);

        // System.out.println(xone + " " + xtwo + " " + yone + " " + ytwo);
        if (xone && xtwo && yone && ytwo) {
            value = activationAmount;
        } else {
            value = 0;
        }
    }

    /**
     * @return value associated with this sensor, 0 if occupied,
     */
    public double getValue() {
        return value;
    }

    /**
     * @return the activationAmount
     */
    public double getActivationAmount() {
        return activationAmount;
    }

    /**
     * @return the x
     */
    public int getX() {
        return x;
    }

    /**
     * @return the y
     */
    public int getY() {
        return y;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

}
