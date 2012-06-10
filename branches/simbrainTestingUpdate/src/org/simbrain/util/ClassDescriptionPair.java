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

/**
 * A simple pairing of a class and a short description. Used in combo boxes to
 * associate short descriptions (in the combo box) with classes used to
 * instantiate objects.
 */
public class ClassDescriptionPair {

    /** The class. */
    private final Class<?> theClass;

    /** A description of the class. */
    private final String description;

    /**
     * Construct a class / description pair.
     *
     * @param theClass the class
     * @param description the description
     */
    public ClassDescriptionPair(Class<?> theClass, String description) {
        this.theClass = theClass;
        this.description = description;
    }

    /**
     * @return the theClass
     */
    public Class<?> getTheClass() {
        return theClass;
    }

    @Override
    public String toString() {
        return description;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the simple name of the class part of this pair.
     *
     * @return the simple name;
     */
    public String getSimpleName() {
        return theClass.getSimpleName();
    }

}
