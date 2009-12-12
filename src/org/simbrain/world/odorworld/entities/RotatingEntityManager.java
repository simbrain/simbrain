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

    /**
     * Tree map for mouse.
     *
     * @return mouse tree map
     */
    public static TreeMap<Double, Animation> getMouse() {
        TreeMap<Double, Animation> mouseMap = new TreeMap<Double, Animation>();
        mouseMap.put(7.5, new Animation("Mouse_0.gif"));
        mouseMap.put(22.5, new Animation("Mouse_15.gif"));
        mouseMap.put(37.5, new Animation("Mouse_30.gif"));
        mouseMap.put(52.5, new Animation("Mouse_45.gif"));
        mouseMap.put(67.5, new Animation("Mouse_60.gif"));
        mouseMap.put(82.5, new Animation("Mouse_75.gif"));
        mouseMap.put(97.5, new Animation("Mouse_90.gif"));
        mouseMap.put(112.5, new Animation("Mouse_105.gif"));
        mouseMap.put(127.5, new Animation("Mouse_120.gif"));
        mouseMap.put(142.5, new Animation("Mouse_135.gif"));
        mouseMap.put(157.5, new Animation("Mouse_150.gif"));
        mouseMap.put(172.5, new Animation("Mouse_165.gif"));
        mouseMap.put(187.5, new Animation("Mouse_180.gif"));
        mouseMap.put(202.5, new Animation("Mouse_195.gif"));
        mouseMap.put(217.5, new Animation("Mouse_210.gif"));
        mouseMap.put(232.5, new Animation("Mouse_225.gif"));
        mouseMap.put(247.5, new Animation("Mouse_240.gif"));
        mouseMap.put(262.5, new Animation("Mouse_255.gif"));
        mouseMap.put(277.5, new Animation("Mouse_270.gif"));
        mouseMap.put(292.5, new Animation("Mouse_285.gif"));
        mouseMap.put(307.5, new Animation("Mouse_300.gif"));
        mouseMap.put(322.5, new Animation("Mouse_315.gif"));
        mouseMap.put(337.5, new Animation("Mouse_330.gif"));
        mouseMap.put(352.5, new Animation("Mouse_345.gif"));
        return mouseMap;
    }

    /**
     * Tree map for horse.
     * 
     * Courtesy of Reiner Prokein,
     * http://reinerstileset.4players.de/englisch.html
     * 
     * @return horse tree map
     */
    public static TreeMap<Double, Animation> getHorse() {
        TreeMap<Double, Animation> horseMap = new TreeMap<Double, Animation>();

        double angle = 7.5;
        int duration = 25;
        horseMap.put(angle, new Animation(new String[] { "walking/e0001.png",
                "walking/e0002.png", "walking/e0003.png",
                "walking/e0004.png", "walking/e0005.png",
                "walking/e0006.png", "walking/e0007.png", }, duration));

        angle += 45.0;
        horseMap.put(angle, new Animation(new String[] { "./walking/ne0000.png",
                "./walking/ne0001.png", "./walking/ne0002.png",
                "./walking/ne0003.png", "./walking/ne0004.png",
                "./walking/ne0005.png", "./walking/ne0006.png",
                "./walking/ne0007.png", }, duration));

        angle += 45.0;
        horseMap.put(angle, new Animation(new String[] { "./walking/n0000.png",
                "./walking/n0001.png", "./walking/n0002.png",
                "./walking/n0003.png", "./walking/n0004.png",
                "./walking/n0005.png", "./walking/n0006.png",
                "./walking/n0007.png", }, duration));

        angle += 45.0;
        horseMap.put(angle, new Animation(new String[] { "./walking/nw0000.png",
                "./walking/nw0001.png", "./walking/nw0002.png",
                "./walking/nw0003.png", "./walking/nw0004.png",
                "./walking/nw0005.png", "./walking/nw0006.png",
                "./walking/nw0007.png", }, duration));

        angle += 45.0;
        horseMap.put(angle, new Animation(new String[] { "./walking/w0000.png",
                "./walking/w0001.png", "./walking/w0002.png",
                "./walking/w0003.png", "./walking/w0004.png",
                "./walking/w0005.png", "./walking/w0006.png",
                "./walking/w0007.png" }, duration));

        angle += 45.0;
        horseMap.put(angle, new Animation(new String[] { "./walking/sw0000.png",
                "./walking/sw0001.png", "./walking/sw0002.png",
                "./walking/sw0003.png", "./walking/sw0004.png",
                "./walking/sw0005.png", "./walking/sw0006.png",
                "./walking/sw0007.png", }, duration));

        angle += 45.0;
        horseMap.put(angle, new Animation(new String[] { "./walking/s0000.png",
                "./walking/s0001.png", "./walking/s0002.png",
                "./walking/s0003.png", "./walking/s0004.png",
                "./walking/s0005.png", "./walking/s0006.png",
                "./walking/s0007.png", }, duration));

        angle += 45.0;
        horseMap.put(angle, new Animation(new String[] { "./walking/se0000.png",
                "./walking/se0001.png", "./walking/se0002.png",
                "./walking/se0003.png", "./walking/se0004.png",
                "./walking/se0005.png", "./walking/se0006.png",
                "./walking/se0007.png", }, duration));

        return horseMap;
    }

}
