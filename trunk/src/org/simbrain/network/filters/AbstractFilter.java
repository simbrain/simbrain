
package org.simbrain.network.filters;

import edu.umd.cs.piccolo.PNode;

import edu.umd.cs.piccolo.util.PNodeFilter;

import org.apache.commons.collections.Predicate;

/**
 * Abstract filter, implements both <code>PNodeFilter</code>
 * and <code>Predicate</code> interfaces.
 */
public abstract class AbstractFilter
    implements PNodeFilter, Predicate {

    /** @see Predicate */
    public boolean evaluate(final Object object) {
        // recast in terms of PNodeFilter
        if (object instanceof PNode) {
            PNode node = (PNode) object;
            return accept(node);
        } else {
            return false;
        }
    }

    /** @see PNodeFilter */
    public abstract boolean accept(final PNode node);

    /** @see PNodeFilter */
    public boolean acceptChildrenOf(final PNode node) {
        // always returns true, override for performance reasons
        return true;
    }
}