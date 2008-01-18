/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2006 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simbrain.world.visionworld.filter;

import java.awt.image.BufferedImage;

import org.simbrain.world.visionworld.Filter;

/**
 * Pixel accumulator.
 */
public final class PixelAccumulator
    implements Filter {

    /** Display name. */
    private static final String DISPLAY_NAME = "Pixel accumulator";

    /** {@inheritDoc} */
    public double filter(final BufferedImage image) {
        int pixels = 0;
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                int rgb = image.getRGB(x, y);
                // hack!
                if (rgb == -16777216) {
                    pixels++;
                }
                // ...because this isn't giving the right value
                /*
                int r = (rgb >> 16) & 255;
                int g = (rgb >> 8) & 255;
                int b = rgb & 255;
                if ((r == 0) && (g == 0) && (b == 0)) {
                    pixels++;
                }
                */
            }
        }
        return pixels;
    }

    public String getDescription() {
        return DISPLAY_NAME;
    }
    
}
