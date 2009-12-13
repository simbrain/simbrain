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

    /** File separator. */
    private static String FS = System.getProperty("file.separator");

    /**
     * Tree map for mouse. Images by David Fleishman.
     *
     * @return mouse tree map
     */
    public static TreeMap<Double, Animation> getMouse() {
        TreeMap<Double, Animation> mouseMap = new TreeMap<Double, Animation>();
        mouseMap.put(7.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_0.gif"));
        mouseMap.put(22.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_15.gif"));
        mouseMap.put(37.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_30.gif"));
        mouseMap.put(52.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_45.gif"));
        mouseMap.put(67.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_60.gif"));
        mouseMap.put(82.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_75.gif"));
        mouseMap.put(97.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_90.gif"));
        mouseMap.put(112.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_105.gif"));
        mouseMap.put(127.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_120.gif"));
        mouseMap.put(142.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_135.gif"));
        mouseMap.put(157.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_150.gif"));
        mouseMap.put(172.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_165.gif"));
        mouseMap.put(187.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_180.gif"));
        mouseMap.put(202.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_195.gif"));
        mouseMap.put(217.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_210.gif"));
        mouseMap.put(232.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_225.gif"));
        mouseMap.put(247.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_240.gif"));
        mouseMap.put(262.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_255.gif"));
        mouseMap.put(277.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_270.gif"));
        mouseMap.put(292.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_285.gif"));
        mouseMap.put(307.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_300.gif"));
        mouseMap.put(322.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_315.gif"));
        mouseMap.put(337.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_330.gif"));
        mouseMap.put(352.5, new Animation("rotating" + FS + "mouse" + FS
                + "Mouse_345.gif"));
        return mouseMap;
    }

    /**
     * Tree map for cow.
     * 
     * Courtesy of Reiner Prokein,
     * http://reinerstileset.4players.de/englisch.html
     * 
     * @return horse tree map
     */
    public static TreeMap<Double, Animation> getCow() {
        TreeMap<Double, Animation> cowMap = new TreeMap<Double, Animation>();

        double angle = 7.5;
        int duration = 25;
        cowMap.put(angle, new Animation(new String[] {
                "rotating" + FS + "cow" + FS + "e0001.png",
                "rotating" + FS + "cow" + FS + "e0002.png",
                "rotating" + FS + "cow" + FS + "e0003.png",
                "rotating" + FS + "cow" + FS + "e0004.png",
                "rotating" + FS + "cow" + FS + "e0005.png",
                "rotating" + FS + "cow" + FS + "e0006.png",
                "rotating" + FS + "cow" + FS + "e0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                "rotating" + FS + "cow" + FS + "ne0000.png",
                "rotating" + FS + "cow" + FS + "ne0001.png",
                "rotating" + FS + "cow" + FS + "ne0002.png",
                "rotating" + FS + "cow" + FS + "ne0003.png",
                "rotating" + FS + "cow" + FS + "ne0004.png",
                "rotating" + FS + "cow" + FS + "ne0005.png",
                "rotating" + FS + "cow" + FS + "ne0006.png",
                "rotating" + FS + "cow" + FS + "ne0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                "rotating" + FS + "cow" + FS + "n0000.png",
                "rotating" + FS + "cow" + FS + "n0001.png",
                "rotating" + FS + "cow" + FS + "n0002.png",
                "rotating" + FS + "cow" + FS + "n0003.png",
                "rotating" + FS + "cow" + FS + "n0004.png",
                "rotating" + FS + "cow" + FS + "n0005.png",
                "rotating" + FS + "cow" + FS + "n0006.png",
                "rotating" + FS + "cow" + FS + "n0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                "rotating" + FS + "cow" + FS + "nw0000.png",
                "rotating" + FS + "cow" + FS + "nw0001.png",
                "rotating" + FS + "cow" + FS + "nw0002.png",
                "rotating" + FS + "cow" + FS + "nw0003.png",
                "rotating" + FS + "cow" + FS + "nw0004.png",
                "rotating" + FS + "cow" + FS + "nw0005.png",
                "rotating" + FS + "cow" + FS + "nw0006.png",
                "rotating" + FS + "cow" + FS + "nw0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                "rotating" + FS + "cow" + FS + "w0000.png",
                "rotating" + FS + "cow" + FS + "w0001.png",
                "rotating" + FS + "cow" + FS + "w0002.png",
                "rotating" + FS + "cow" + FS + "w0003.png",
                "rotating" + FS + "cow" + FS + "w0004.png",
                "rotating" + FS + "cow" + FS + "w0005.png",
                "rotating" + FS + "cow" + FS + "w0006.png",
                "rotating" + FS + "cow" + FS + "w0007.png" }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                "rotating" + FS + "cow" + FS + "sw0000.png",
                "rotating" + FS + "cow" + FS + "sw0001.png",
                "rotating" + FS + "cow" + FS + "sw0002.png",
                "rotating" + FS + "cow" + FS + "sw0003.png",
                "rotating" + FS + "cow" + FS + "sw0004.png",
                "rotating" + FS + "cow" + FS + "sw0005.png",
                "rotating" + FS + "cow" + FS + "sw0006.png",
                "rotating" + FS + "cow" + FS + "sw0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                "rotating" + FS + "cow" + FS + "s0000.png",
                "rotating" + FS + "cow" + FS + "s0001.png",
                "rotating" + FS + "cow" + FS + "s0002.png",
                "rotating" + FS + "cow" + FS + "s0003.png",
                "rotating" + FS + "cow" + FS + "s0004.png",
                "rotating" + FS + "cow" + FS + "s0005.png",
                "rotating" + FS + "cow" + FS + "s0006.png",
                "rotating" + FS + "cow" + FS + "s0007.png", }, duration));

        angle += 45.0;
        cowMap.put(angle, new Animation(new String[] {
                "rotating" + FS + "cow" + FS + "se0000.png",
                "rotating" + FS + "cow" + FS + "se0001.png",
                "rotating" + FS + "cow" + FS + "se0002.png",
                "rotating" + FS + "cow" + FS + "se0003.png",
                "rotating" + FS + "cow" + FS + "se0004.png",
                "rotating" + FS + "cow" + FS + "se0005.png",
                "rotating" + FS + "cow" + FS + "se0006.png",
                "rotating" + FS + "cow" + FS + "se0007.png", }, duration));

        return cowMap;
    }

}
