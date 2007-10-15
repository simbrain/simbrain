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
package org.simbrain.workspace.couplingmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.CouplingContainer;
import org.simbrain.workspace.Producer;

/**
 * Graphical element for managing coupling of objects.
 */
public class CouplingManager {

    /** List of consumers. */
    private final List<Consumer> consumers = new ArrayList<Consumer>();

    /** List of producers. */
    private final List<Producer> producers = new ArrayList<Producer>();

    /**
     * Default constructor. Creates and displays the coupling manager.
     * @param frame parent of panel.
     */
    public CouplingManager() {
        
    }
    
    /**
     * temporary method until refactoring is complete
     * @param container
     */
    public void refreshConsumers(CouplingContainer container) {
        consumers.clear();
        consumers.addAll(container.getConsumers());
    }
    
    public void refreshProducers(CouplingContainer container) {
        producers.clear();
        producers.addAll(container.getProducers());
    }
    
    public List<? extends Consumer> getConsumers() {
        return Collections.unmodifiableList(consumers);
    }
    
    public List<? extends Producer> getProducers() {
        return Collections.unmodifiableList(producers);
    }
}
