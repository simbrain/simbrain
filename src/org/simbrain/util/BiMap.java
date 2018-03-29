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

import java.util.HashMap;
import java.util.Set;

/**
 * Two directional map, that allows inverses.
 *
 * https://stackoverflow.com/questions/9783020/bidirectional-map
 *
 * @param <K>
 * @param <V>
 */
public class BiMap<K,V> {

    /**
     * The main map.
     */
    HashMap<K,V> map = new HashMap<K, V>();

    /**
     * The inverse map.
     */
    HashMap<V,K> inversedMap = new HashMap<V, K>();

    /**
     * Standard put operation.
     *
     * @param k key
     * @param v value
     */
    public void put(K k, V v) {
        map.put(k, v);
        inversedMap.put(v, k);
    }

    /**
     * Standard get operation
     *
     * @param k key
     * @return associated value
     */
    public V get(K k) {
        return map.get(k);
    }

    /**
     * What makes me special: an inverse!
     *
     * @param v value
     * @return associated key
     */
    public K getInverse(V v) {
        return inversedMap.get(v);
    }

    public Set<K> keySet() {
        return map.keySet();
    }
}
