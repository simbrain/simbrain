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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class CouplingManager {
    private static final Logger LOGGER = Logger.getLogger(CouplingManager.class);
    
    private List<Coupling<?>> all = new ArrayList<Coupling<?>>();
    private Map<SourceTarget, List<Coupling<?>>> sourceTargetCouplings = new HashMap<SourceTarget, List<Coupling<?>>>();
    
    List<? extends Coupling<?>> getCouplings() {
        return Collections.unmodifiableList(all);
    }
    
    List<? extends Coupling<?>> getCouplings(WorkspaceComponent source, WorkspaceComponent target) {
        return Collections.unmodifiableList(sourceTargetCouplings.get(new SourceTarget(source, target)));
    }
    
    void updateAllCouplings() {
        LOGGER.debug("updating all couplings");
        for (Coupling<?> coupling : getCouplings()) {
            LOGGER.trace(coupling.getClass());
            coupling.setBuffer();
        }
        
        for (Coupling<?> coupling : getCouplings()) {
            coupling.update();
        }
    }
    
    //TODO implement findCoupling
    Coupling<?> findCoupling(String sourceId, String targetId) {
        return null;
    }
    
    void addCoupling(Coupling<?> coupling) {
        all.add(coupling);
        
        SourceTarget sourceTarget = new SourceTarget(
            coupling.getConsumingAttribute().getParent().getParentComponent(),
            coupling.getProducingAttribute().getParent().getParentComponent());
                
        List<Coupling<?>> couplings = sourceTargetCouplings.get(sourceTarget);
        
        if (couplings == null) {
            couplings = new ArrayList<Coupling<?>>();
            sourceTargetCouplings.put(sourceTarget, couplings);
        }
        
        couplings.add(coupling);
    }
    
    void removeCoupling(Coupling<?> coupling) {
        all.remove(coupling);
        
        SourceTarget sourceTarget = new SourceTarget(
            coupling.getConsumingAttribute().getParent().getParentComponent(),
            coupling.getProducingAttribute().getParent().getParentComponent());
            
        List<Coupling<?>> couplings = sourceTargetCouplings.get(sourceTarget);
        
        if (couplings != null) couplings.remove(coupling);
    }
    
    private class SourceTarget {
        WorkspaceComponent source;
        WorkspaceComponent target;
        
        SourceTarget(WorkspaceComponent source, WorkspaceComponent target) {
            this.source = source;
            this.target = target;
        }
    }
}
