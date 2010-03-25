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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Utility classes for Simbrain.
 */
public class SimbrainUtils {

    /**
     * <p>Decides if the operating system matches.</p>
     * 
     * Source adapted from org.apache.commons.lang.SystemUtils
     * 
     * @param osNamePrefix  the prefix for the os name
     * @return true if matches, or false if not or can't determine
     */
    private static boolean getOSMatches(String osNamePrefix) {
        final String OS_NAME = System.getProperty("os.name");
        if (OS_NAME == null) {
            return false;
        }
        return OS_NAME.startsWith(osNamePrefix);
    }

    /**
     * Determines whether the system is a Mac os x.
     * 
     * @return whether the system is a Mac os x.
     */
    public static boolean isMacOSX() {
        return getOSMatches("Mac OS X");
    }
    
    /**
     * Reimplementation of same method from org.apache.commons.collections.CollectionUtils.
     * 
     * @param selection the collection to filter
     * @param filter the predicate to be used in filtering.
     * @return those members of the selection to which the predicate applies
     */
    public static Collection select(final Collection selection, final Predicate filter) {
        Collection ret = new ArrayList();
        for(Object object: selection) {
            if (filter.evaluate(object)) {
                ret.add(object);
            }
        }
        return ret;
    }

    /**
     * Re-implementation of same method from org.apache.commons.collections.CollectionUtils.
     *
     * @param a Collection
     * @param b Collection
     * @return union of two collections
     */
    public static Collection union(final Collection a, final Collection b) {
        Collection ret = new ArrayList();
        ret.addAll(a);
        ret.addAll(b);
        return ret;
    }

    /**
     * Re-implementation of same method from org.apache.commons.collections.CollectionUtils.
     *
     * @param a Collection
     * @param b Collection
     * @return intersection of two collections
     */
    public static Collection intersection(final Collection a, final Collection b) {
        Collection ret = new ArrayList();
        for (Object object : a) {
            if (b.contains(object)) {
                ret.add(object);
            }
        }
        return ret;
    }
}
