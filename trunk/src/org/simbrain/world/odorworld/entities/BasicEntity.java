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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.simbrain.util.propertyeditor.ComboBoxable;
import org.simbrain.world.odorworld.OdorWorld;


/**
 * <b>BasicEntity</b> represents a basic entity in the Odor World environment.
 */
public class BasicEntity extends OdorWorldEntity {

    /** Default image. */
    private static final String DEFAULT_IMAGE = "Swiss.gif";

    /** List of available objects (for property editor).*/
    private List<String> images = new ArrayList<String>();

    /** Current image name (for property editor).*/
    private String currentImage = DEFAULT_IMAGE;

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
        super(imageLocation, world);
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
     * Initialize the image name list.
     */
    private void initImageList() {
        String baseDir = System.getProperty("user.dir");
        final String FS = System.getProperty("file.separator");
        File dir = new File(baseDir + FS + "src" + FS + "org" + FS + "simbrain"
                + FS + "world" + FS + "odorworld" + FS + "images" + FS
                + "static");
        //TODO: Create file filter for jpg, etc.
        //TODO: Use File separator
        images.addAll(Arrays.asList(dir.list()));
    }

    /**
     * @return the imageBox
     */
    public ComboBoxable getImages() {
        initImageList();
        return new ComboBoxable() {
            public Object getCurrentObject() {
                return currentImage;
            }

            public Object[] getObjects() {
                return images.toArray();
            }
        };
    }

    /**
     * @param imageBox the imageBox to set
     */
    public void setImages(ComboBoxable imageBox) {
        currentImage = (String) imageBox.getCurrentObject();
        this.setAnimation(new Animation(currentImage));
    }

}
