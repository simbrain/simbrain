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
package org.simbrain.resource;

import java.awt.Image;
import java.net.URL;

import javax.swing.ImageIcon;

/**
 * <b>ResourceManager</b> provides resources (stored in the same directory) to
 * the rest of the program.
 */
public class ResourceManager {
    private static int smallIconSize = 18;

    /**
     * @return Returns the size in pixels to which small icons will be scaled.
     */
    public static int getSmallIconSize() {
        return smallIconSize;
    }

    /**
     * @param value Assigns the size in pixels to which small icons will be scaled.
     */
    public static void setSmallIconSize(int value) {
        smallIconSize = value;
    }

    /**
     * Retrieve an ImageIcon based on its file name.
     *
     * @param name name of the image file to retrieve
     *
     * @return the ImageIcon which can be used with Swing components, etc
     */
    public static ImageIcon getImageIcon(final String name) {
        // TODO: Replace usage of this method with get<SIZE>Icon and create a user
        // preference for changing the sizes.
        URL url = ResourceManager.class.getResource(name);
        return new ImageIcon(url);
    }

    /**
     * Load an ImageIcon from the resources directory and scale it if necessary.
     * @param name The name of the icon to load within the resources directory.
     * @return Returns a scaled ImageIcon.
     */
    public static ImageIcon getSmallIcon(final String name) {
        URL url = ResourceManager.class.getResource(name);
        ImageIcon imageIcon = new ImageIcon(url);
        Image image = imageIcon.getImage().getScaledInstance(
                smallIconSize, smallIconSize, Image.SCALE_AREA_AVERAGING);
        imageIcon.setImage(image);
        return imageIcon;
    }

    /**
     * Retrieve an Image based on its file name.
     *
     * @param name name of the image file to retrieve
     *
     * @return the Image which can be used with Swing components, etc
     */
    public static Image getImage(final String name) {
        URL url = ResourceManager.class.getResource(name);
        java.awt.Toolkit toolKit = java.awt.Toolkit.getDefaultToolkit();
        return toolKit.getImage(url);
    }
}
