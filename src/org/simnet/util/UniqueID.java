/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/Documentation/docs/SimbrainDocs.html#Credits
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
package org.simnet.util;

/**
 * <b>UniqueID</b> provides a unique id which is used to name, for example,
 * neurons.
 */
public final class UniqueID {

    /** The current time in milliseconds; used to generate a unique id. */
    private static long current = System.currentTimeMillis();


    /**
     * Private default constructor.
     */
    private UniqueID() {
        // empty
    }


    /**
     * Returns a unique identifier.
     *
     * @return a unique identification
     */
    public static synchronized String get() {
        return "" + current++;
    }
}
