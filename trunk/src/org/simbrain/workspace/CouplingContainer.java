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

import java.util.List;

/**
 * Classes which implements this interface contain objects which can act as couplings to communicate in networks transactions.
 *
 */
public interface CouplingContainer {

    /**
     * Return an unmodifiable list of producers for this component.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of producers for this component
     */
    public List<Producer> getProducers();

    /**
     * Return an unmodifiable list of consumers for this component.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of consumers for this component
     */
    public List<Consumer> getConsumers();

    /**
     * Return an unmodifiable list of couplings for this component.
     * The list may be empty but may not be null.
     *
     * @return an unmodifiable list of couplings for this component
     */
    public List<Coupling> getCouplings();

}
