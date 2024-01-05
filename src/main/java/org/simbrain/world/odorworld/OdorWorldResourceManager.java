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
package org.simbrain.world.odorworld;

import org.simbrain.util.Utils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.simbrain.util.ResourceManager.assertForwardSlash;

/**
 * <b>OdorWorldResourceManager</b> provides resources used by odor world.
 */
public class OdorWorldResourceManager {

    // TODO: Buffered images have to be retrieved in a special way; but we can't remember the details...
    //  If it's resolved get rid of this class and just use ResourceManager.java

    /**
     * Return an image for a "rotating" entity.
     *
     * @param name name of the image
     * @return the image
     */
    public static Image getRotatingImage(final String name) {
        return getBufferedImage("rotating" + Utils.FS + name);
    }

    /**
     * Return an image for an unmoving "static entity.
     *
     * @param name name of the image
     * @return the image
     */
    public static Image getStaticImage(final String name) {
        return getBufferedImage("static" + Utils.FS + name);
    }

    /**
     * Retrieve and load an Image into buffer based on its file name.
     *
     * @param name name of the image file to retrieve
     * @return the Image which can be used with Swing components, etc
     */
    public static BufferedImage getBufferedImage(final String name) {
        /*
         * There is currently some performance issue with this method,
         * but this the image retrieve from the other getImage() method
         * does not seem to allow immediate retrieval of the image size
         * so this method is needed to correctly set the bound for the PNodes.
         */
        URL url = ClassLoader.getSystemClassLoader().getResource(assertForwardSlash("odorworld" + File.separator + name));
        BufferedImage image = null;
        try {
            // source: https://stackoverflow.com/a/44170254
            // with disk caching ImageIO.read() is really slow...
            // even when disabled, the performance is still bad
            // TODO: improve performance on image loading?
            ImageIO.setUseCache(false);
            image = ImageIO.read(url);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    public static URL getFileURL(String path) {
        path = assertForwardSlash("odorworld/" + path);
        return ClassLoader.getSystemClassLoader().getResource(path);
    }

}
