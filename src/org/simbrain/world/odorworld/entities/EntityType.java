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

/**
 * The type of an odor world entity (swiss, candle, etc).
 */
public enum EntityType {

    // TODO: Put in all missing static objects

    SWISS("Swiss", false, false, false, 32, 32),
    CANDLE("Candle", false, false, false, 32, 32),
    FISH("Fish", false, false, false, 32, 32),
    FLOWER("Flower", false, false, false, 32, 32),
    MOUSE("Mouse", true, true, true, 40, 40),
    AMY("Amy", true, true, true, 96, 96),
    ARNO("Arno", true, true, true, 96, 96),
    BOY("Boy", true, true, true, 96, 96),
    COW("Cow", true, true, true, 96, 96),
    GIRL("Girl", true, true, true, 96, 96),
    JAKE("Jake", true, true, true, 96, 96),
    LION("Lion", true, true, true, 96, 96),
    STEVE("Steve", true, true, true, 96, 96),
    SUSI("Susi", true, true, true, 96, 96);

    /**
     * String description that shows up in dialog boxes.
     */
    private final String description;

    /**
     * Whether the sprite representing this entity is based on heading.
     */
    private boolean isRotating;

    /**
     * Whether this entity type uses sensors by default.
     */
    private boolean useSensors;

    /**
     * Whether this entity type uses effectors by default.
     */
    private boolean useEffectors;

    private int imageWidth;

    private int imageHeight;

    /**
     * Create the entity
     */
    EntityType(
        String description,
        boolean isRotating,
        boolean sensors,
        boolean effectors,
        int imageWidth,
        int imageHeight
    ) {
        this.description = description;
        this.isRotating = isRotating;
        this.useSensors = sensors;
        this.useEffectors = effectors;
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
    }

    @Override
    public String toString() {
        return description;
    }

    public double getImageWidth() {
        return imageWidth;
    }

    public double getImageHeight() {
        return imageHeight;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRotating() {
        return isRotating;
    }

    public boolean isUseSensors() {
        return useSensors;
    }

    public boolean isUseEffectors() {
        return useEffectors;
    }
}
