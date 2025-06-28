package org.simbrain.util;

import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Set;

/**
 * Two directional map, that allows inverses.
 * <p>
 * https://stackoverflow.com/questions/9783020/bidirectional-map
 *
 * @param <K>
 * @param <V>
 */
public class BiMap<K, V> extends AbstractMap<K, V> {

    /**
     * The main map.
     */
    HashMap<K, V> map = new HashMap<K, V>();

    /**
     * The inverse map.
     */
    HashMap<V, K> inversedMap = new HashMap<V, K>();

    /**
     * Standard put operation.
     *  @param k key
     * @param v value
     * @return
     */
    public V put(K k, V v) {
        map.put(k, v);
        inversedMap.put(v, k);
        return v;
    }

    /**
     * Standard get operation
     *
     * @param k key
     * @return associated value
     */
    public V get(Object k) {
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

    @NotNull
    public Set<K> keySet() {
        return map.keySet();
    }

    @NotNull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return map.entrySet();
    }
}
