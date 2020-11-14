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
package org.simbrain.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.net.URL;

/**
 * <b>ResourceManager</b> provides convenient access to resource files. Pass in paths relative to the resource
 * directory as root.
 */
public class ResourceManager {

    private static int smallIconSize = 18;

    /**
     * Retrieve an ImageIcon based on its file name.
     *
     * @param path name of the image file to retrieve
     * @return the ImageIcon which can be used with Swing components, etc
     */
    public static ImageIcon getImageIcon(final String path) {
        URL url = ClassLoader.getSystemClassLoader().getResource(path);
        return new ImageIcon(url);
    }

    /**
     * Retrieve an Image based on its file name.
     *
     * @param path name of the image file to retrieve
     * @return the Image which can be used with Swing components, etc
     */
    public static Image getImage(final String path) {
        URL url = ClassLoader.getSystemClassLoader().getResource(path);
        java.awt.Toolkit toolKit = java.awt.Toolkit.getDefaultToolkit();
        return toolKit.getImage(url);
    }

    /**
     * Load an ImageIcon from the resources directory and scale it if necessary.
     *
     * @param path The path of the icon to load within the resources directory.
     * @return Returns a scaled ImageIcon.
     */
    public static ImageIcon getSmallIcon(final String path) {
        URL url = ClassLoader.getSystemClassLoader().getResource(path);
        ImageIcon imageIcon = new ImageIcon(url);
        Image image = imageIcon.getImage().getScaledInstance(smallIconSize, smallIconSize, Image.SCALE_AREA_AVERAGING);
        imageIcon.setImage(image);
        return imageIcon;
    }

    /**
     * Returns a string (e.g. an html string) from a specified path.
     */
    public static String getString(String name) {
        return Utils.readFileContents(new File(ClassLoader.getSystemClassLoader().getResource(name).getPath()));
    }

}
