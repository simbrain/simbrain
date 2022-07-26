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

import org.simbrain.util.piccolo.Animation;
import org.simbrain.util.piccolo.Animations;
import org.simbrain.world.odorworld.OdorWorldResourceManager;

import java.awt.*;
import java.util.ArrayList;

/**
 * Manages the creation of treemaps for rotating entities.
 *
 * @author jyoshimi
 */
public class RotatingEntityManager {

    /**
     * Rotating image base directory.
     */
    private static final String ROTATING_IMAGE_DIR = "rotating/";

    /**
     * Tree map for mouse. Images by David Fleishman.
     *
     * @return mouse tree map
     */
    public static ArrayList<Animation> getMouse() {

        ArrayList<Animation> mouseMap = new ArrayList<>();
        int degree = 15;
        int count = 360 / degree; // each image is 15 degree apart
        for (int i = 0; i < count; i++) {
            mouseMap.add(
                    Animations.createAnimation(
                            OdorWorldResourceManager.getRotatingImage(
                                    "mouse/Mouse_" + i * degree + ".gif"
                            )
                    )
            );
        }

        return mouseMap;
    }

    public static Animation getMouseByDegree(double degree) {
        return getAnimationByHeading("mouse", degree);
    }

    public static Animation getAnimationByHeading(String animationName, double degree) {
        ArrayList<Animation> animation;
        if (animationName.equals("mouse")) {
            animation = getMouse();
        } else {
            animation = getRotatingTileset(animationName, 8);
        }

        return getAnimationByHeading(animation, degree);
    }

    public static Animation getAnimationByHeading(ArrayList<Animation> animations, double degree) {
        int degreeApart = 360 / animations.size();
        degree = (degree + degreeApart / 2) % 360;
        int index = (int)(degree / degreeApart);
        return animations.get(index);
    }

    /**
     * Tree map for a rotating image from Reiner Prokein's collection.
     * <p>
     * Courtesy of http://reinerstileset.4players.de/englisch.html
     *
     * @param tileBaseName base name used to access the relevant set of image,
     *                     which are named in a standard way
     * @param numFrames how many frames there are per direction
     * @return horse tree map
     */
    public static ArrayList<Animation> getRotatingTileset(String tileBaseName, int numFrames) {

        // The folders are lower cased
        tileBaseName = tileBaseName.toLowerCase();

        ArrayList<Animation> rotatingTileset = new ArrayList<>();

        String[] fileNameInitials = {"e000", "ne000", "n000", "nw000", "w000", "sw000", "s000", "se000"};
        for (String fni : fileNameInitials) {
            ArrayList<Image> frames = new ArrayList<>();
            for (int i = 0; i < numFrames; i++) {
                frames.add(OdorWorldResourceManager.getRotatingImage(tileBaseName + "/" + fni + i + ".png"));
            }
            rotatingTileset.add(Animations.createLoopedAnimation(frames));
        }

        return rotatingTileset;
    }

}
