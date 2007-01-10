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

import java.util.Arrays;
import java.util.List;

import org.simbrain.world.visionworld.Filter;

/**
 * Typesafe enumeration of filter types.
 */
public final class FilterType {

    /** Display name. */
    private final String displayName;

    /** Implementation class. */
    private final Class implementationClass;

    /**
     * Create a new filter type with the specified display name
     * and implementation class.
     *
     * @param displayName display name
     * @param implementationClass implementation class
     */
    private FilterType(final String displayName, final Class implementationClass) {
        this.displayName = displayName;
        this.implementationClass = implementationClass;
    }


    /**
     * Return the display name for this filter type.
     *
     * @return the display name for this filter type
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Return the implementation class for this filter type.
     *
     * @return the implementation class for this filter type
     */
    public Class getImplementationClass() {
        return implementationClass;
    }

    /** {@inheritDoc} */
    public String toString() {
        return displayName;
    }

    /** Uniform filter type. */
    public static final FilterType UNIFORM = new FilterType("Uniform", UniformFilter.class);

    /** Private array of filter types. */
    private static final FilterType[] values = new FilterType[] { UNIFORM };

    /** Public list of filter types. */
    public static final List<FilterType> VALUES = Arrays.asList(values);
}
