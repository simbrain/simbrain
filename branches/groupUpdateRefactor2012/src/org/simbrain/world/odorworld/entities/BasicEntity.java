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

import org.simbrain.util.propertyeditor.ComboBoxWrapper;
import org.simbrain.world.odorworld.OdorWorld;

/**
 * <b>BasicEntity</b> represents a static entity in the Odor World environment, with a static
 * image file.  (TODO: Improve doc)
 */
public class BasicEntity extends OdorWorldEntity {

    /** Static image location. */
    private static final String STATIC_IMAGE_DIR = "static/";

    /** Default image. */
    private static final String DEFAULT_IMAGE = STATIC_IMAGE_DIR  + "Swiss.gif";

    /**
     * Construct a basic entity with a specified animation.
     *
     * @param animation animation associated with this entity.
     */
    public BasicEntity(final Animation anim, final OdorWorld world) {
        super(anim, world);
        //behavior = new StationaryBehavior();
    }

    /**
     * Construct a default entity.
     */
    public BasicEntity(final OdorWorld world) {
        super(DEFAULT_IMAGE, world);
    }

    /**
     * Construct a basic entity with a single image location. 
     *
     * @param imageLocation image location
     */
    public BasicEntity(final String imageLocation, final OdorWorld world) {
        super(STATIC_IMAGE_DIR + imageLocation, world);
    }

    /**
     * Updates this OdorWorldEntity's Animation and its position based on the
     * velocity.
     */
    public void update(final long elapsedTime) {
        behavior.apply(elapsedTime);
        getAnimation().update(elapsedTime);
    }

    /**
     * Getter which returns data that can be used in a combo box (the property
     * editor dialog).
     *
     * @return the image data
     */
    public ComboBoxWrapper getType() {
        return new ComboBoxWrapper() {
            public Object getCurrentObject() {
                String imageName = getAnimation().getImageLocations()[0];
                String[] truncatedName = imageName.split("/");
                return truncatedName[truncatedName.length - 1];
            }

            public Object[] getObjects() {
                return new Object[] { "Bell.gif", "Fish.gif", "Gouda.gif",
                        "Poison.gif", "Bluecheese.gif", "Flax.gif",
                        "Pansy.gif", "Swiss.gif", "Candle.png", "Flower.gif",
                        "PinkFlower.gif", "Tulip.gif" };
            }
        };
    }

    /**
     * Setter which takes the data from a combo box as an argument.
     *
     * @param imageData the data from the combo box
     */
    public void setType(ComboBoxWrapper imageData) {
        setImage( (String) imageData.getCurrentObject());
    }

    /**
     * Set type of object.
     *
     * @param fileName file name.  //TODO!  Better doc
     */
    public void setImage(String fileName) {
        this.setAnimation(new Animation(STATIC_IMAGE_DIR + fileName));
    }

}
