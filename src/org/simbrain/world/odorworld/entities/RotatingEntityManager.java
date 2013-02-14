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

import java.util.TreeMap;

/**
 * Manages the creation of treemaps for rotating entities.
 *
 * @author jyoshimi
 */
public class RotatingEntityManager {

    /** Rotating image base directory. */
    private static final String ROTATING_IMAGE_DIR = "rotating/";

    /**
     * Tree map for mouse. Images by David Fleishman.
     *
     * @return mouse tree map
     */
    public static TreeMap<Double, Animation> getMouse() {
        TreeMap<Double, Animation> mouseMap = new TreeMap<Double, Animation>();
        mouseMap.put(7.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_0.gif"));
        mouseMap.put(22.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_15.gif"));
        mouseMap.put(37.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_30.gif"));
        mouseMap.put(52.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_45.gif"));
        mouseMap.put(67.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_60.gif"));
        mouseMap.put(82.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_75.gif"));
        mouseMap.put(97.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_90.gif"));
        mouseMap.put(112.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_105.gif"));
        mouseMap.put(127.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_120.gif"));
        mouseMap.put(142.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_135.gif"));
        mouseMap.put(157.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_150.gif"));
        mouseMap.put(172.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_165.gif"));
        mouseMap.put(187.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_180.gif"));
        mouseMap.put(202.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_195.gif"));
        mouseMap.put(217.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_210.gif"));
        mouseMap.put(232.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_225.gif"));
        mouseMap.put(247.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_240.gif"));
        mouseMap.put(262.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_255.gif"));
        mouseMap.put(277.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_270.gif"));
        mouseMap.put(292.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_285.gif"));
        mouseMap.put(307.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_300.gif"));
        mouseMap.put(322.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_315.gif"));
        mouseMap.put(337.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_330.gif"));
        mouseMap.put(352.5, new Animation(ROTATING_IMAGE_DIR
                + "mouse/Mouse_345.gif"));
        return mouseMap;
    }

    /**
     * Tree map for a rotating image from Reiner Prokein's collection.
     *
     * Courtesy of http://reinerstileset.4players.de/englisch.html
     *
     * @param tileBaseName base name used to access the relevant set of image,
     *            which are named in a standard way
     * @return horse tree map
     */
    public static TreeMap<Double, Animation> getRotatingTileset(
            String tileBaseName, int duration) {
        TreeMap<Double, Animation> cowMap = new TreeMap<Double, Animation>();

        double angle = 7.5;
        cowMap.put(angle, new Animation(new String[] {
                ROTATING_IMAGE_DIR + tileBaseName + "/e0000.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/e0001.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/e0002.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/e0003.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/e0004.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/e0005.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/e0006.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/e0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                ROTATING_IMAGE_DIR + tileBaseName + "/ne0000.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/ne0001.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/ne0002.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/ne0003.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/ne0004.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/ne0005.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/ne0006.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/ne0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                ROTATING_IMAGE_DIR + tileBaseName + "/n0000.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/n0001.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/n0002.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/n0003.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/n0004.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/n0005.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/n0006.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/n0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                ROTATING_IMAGE_DIR + tileBaseName + "/nw0000.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/nw0001.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/nw0002.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/nw0003.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/nw0004.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/nw0005.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/nw0006.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/nw0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                ROTATING_IMAGE_DIR + tileBaseName + "/w0000.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/w0001.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/w0002.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/w0003.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/w0004.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/w0005.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/w0006.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/w0007.png" }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                ROTATING_IMAGE_DIR + tileBaseName + "/sw0000.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/sw0001.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/sw0002.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/sw0003.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/sw0004.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/sw0005.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/sw0006.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/sw0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                ROTATING_IMAGE_DIR + tileBaseName + "/s0000.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/s0001.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/s0002.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/s0003.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/s0004.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/s0005.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/s0006.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/s0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                ROTATING_IMAGE_DIR + tileBaseName + "/se0000.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/se0001.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/se0002.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/se0003.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/se0004.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/se0005.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/se0006.png",
                ROTATING_IMAGE_DIR + tileBaseName + "/se0007.png", }, duration));

        return cowMap;
    }

}
