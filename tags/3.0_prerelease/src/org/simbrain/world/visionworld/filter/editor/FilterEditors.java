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
package org.simbrain.world.visionworld.filter.editor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Filter editors.
 */
public final class FilterEditors {

    /** Random filter editor. */
    public static final FilterEditor RANDOM = new RandomFilterEditor();

    /** Uniform filter editor. */
    public static final FilterEditor UNIFORM = new UniformFilterEditor();

    /** Pixel accumulator editor. */
    public static final FilterEditor PIXEL_ACCUMULATOR = new PixelAccumulatorEditor();

    /** Pixel accumulator editor. */
    public static final FilterEditor RGB = new RgbFilterEditor();

    /** Private array of filter editors. */
    private static final FilterEditor[] values = new FilterEditor[] {
            PIXEL_ACCUMULATOR, RANDOM, UNIFORM, RGB };

    /** Public list of filter editors. */
    public static final List<FilterEditor> VALUES = Collections
            .unmodifiableList(Arrays.asList(values));
}