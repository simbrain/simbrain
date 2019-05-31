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
package org.simbrain.custom_sims.resources;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

/**
 * Resource manager for custom sims.  Ensures resources like images and html docs
 * can be used when simulations are deployed in the Simbrain jar.
 */
public class CustomSimResourceManager {

    /**
     * Retrieve a string based file (e.g. html) based on its file name.
     *
     * @param name name of the file to retrieve
     *
     * @return string associated with this file, or null if file not found
     */
    public static String getDocString(final String name) {

        URL url;
        url = ClassLoader.getSystemClassLoader().getResource(name);

        try {
            String string = new Scanner(url.openStream(), "UTF-8").useDelimiter("\\A").next();
            return string;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Retrieve an Image based on its file name.
     *
     * @param name name of the image file to retrieve
     *
     * @return the Image which can be used with Swing components, etc
     */
    public static Image getImage(final String name) {
        URL url;

        url = CustomSimResourceManager.class.getResource(name);

        java.awt.Toolkit toolKit = java.awt.Toolkit.getDefaultToolkit();

        return toolKit.getImage(url);
    }
}
