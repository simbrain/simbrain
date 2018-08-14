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
package org.simbrain.world.odorworld.resources;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

/**
 * <b>OdorWorldResourceManager</b> provides resources (stored in the same
 * directory) to the rest of the program.
 */
public class OdorWorldResourceManager {

    /**
     * Return an image for a "rotating" entity.
     *
     * @param name name of the image
     * @return the image
     */
    public static Image getRotatingImage(final String name) {
        return getImage("rotating/" + name);
    }

    /**
     * Return an image for an unmoving "static entity.
     *
     * @param name name of the image
     * @return the image
     */
    public static Image getStaticImage(final String name) {
        return getImage("static/" + name);
    }

    /**
     * Retrieve an Image based on its file name.
     *
     * @param name name of the image file to retrieve
     * @return the Image which can be used with Swing components, etc
     */
    private static Image getImage(final String name) {
        URL url = OdorWorldResourceManager.class.getResource(name);
        java.awt.Toolkit toolKit = java.awt.Toolkit.getDefaultToolkit();
        return toolKit.getImage(url);
    }
}
