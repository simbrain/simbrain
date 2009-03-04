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
package org.simbrain.network.gui.filters;

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
        // if you don't want the filter to traverse the specified
        // node's children
        return true;
    }
}