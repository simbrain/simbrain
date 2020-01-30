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
package org.simbrain.workspace;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Designates an object as one that contains {@link Consumable}
 * or {@link Producible} annotations, that can be linked together in
 * {@link Coupling}s.
 */
public interface AttributeContainer {

    /**
     * Returns an attribute id that can be used to identify this container. Used in persistence
     * (see {@link org.simbrain.workspace.serialization.ArchivedAttribute} and in displaying
     * Producers and Consumers. Attributes are persisted as a triple <component id, attribute id, methodname>.
     * If there is only a single attribute in component, the id can be simple.  Compare "neuron_1" in networkcomponent
     * with "pie chart" in PieChartCompnent.
     *
     * @return the String id.
     */
    default String getId() {
        // TODO: Remove default.
        return "TODO";
    }

}
