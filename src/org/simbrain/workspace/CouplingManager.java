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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Manages all the couplings for a Workspace instance.
 * 
 * @author Matt Watson
 */
public class CouplingManager {
    /** The static logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CouplingManager.class);
    /** All couplings for the workspace. */
    private List<Coupling<?>> all = new ArrayList<Coupling<?>>();
    /** The couplings indexed by the source and target combination. */
    private Map<SourceTarget, List<Coupling<?>>> sourceTargetCouplings
        = new HashMap<SourceTarget, List<Coupling<?>>>();
    /** The couplings indexed by source. */
    private Map<WorkspaceComponent<?>, List<Coupling<?>>> sourceCouplings
        = new HashMap<WorkspaceComponent<?>, List<Coupling<?>>>();
    /** The couplings indexed by target. */
    private Map<WorkspaceComponent<?>, Coupling<?>> targetCouplings
        = new HashMap<WorkspaceComponent<?>, Coupling<?>>();
    
    /**
     * Returns an unmodifiable list of all the couplings.
     * 
     * @return An unmodifiable list of all the couplings.
     */
    public Collection<? extends Coupling<?>> getCouplings() {
        return Collections.unmodifiableList(all);
    }

    /**
     * Returns all couplings from the given source to the given target.
     * 
     * @param source The source to use in the search.
     * @param target The target to use in the search.
     * @return A list of the couplings between the provided source and target.
     */
    public Collection<? extends Coupling<?>> getCouplings(
            final WorkspaceComponent<?> source, final WorkspaceComponent<?> target) {
        Collection<Coupling<?>> couplings = sourceTargetCouplings.get(
                new SourceTarget(source, target));
        
        if (couplings == null) {
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableCollection(couplings);
        }
    }

    /**
     * Updates all couplings in the workspace.
     */
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

    /**
     * Finds a coupling for the provided ids.
     * 
     * @param sourceId The source ID.
     * @param targetId The target ID.
     * @return The coupling associated with the ids.
     */
    //TODO implement findCoupling
    Coupling<?> findCoupling(final String sourceId, final String targetId) {
        return null;
    }

    /**
     * returns whether the coupling is referenced by this manager.
     * 
     * @param coupling The coupling to search for.
     * @return whether the coupling is referenced by this manager.
     */
    public boolean containsCoupling(final Coupling<?> coupling) {
       return all.contains(coupling);
    }
    
    /**
     * Adds a coupling to this instance.
     * 
     * @param coupling The coupling to add.
     */
    public void addCoupling(final Coupling<?> coupling) {
        System.out.println("coupling added: " + coupling);
        
        all.add(coupling);
        
        WorkspaceComponent<?> source = coupling.getProducingAttribute()
            .getParent().getParentComponent();
        WorkspaceComponent<?> target = coupling.getConsumingAttribute()
            .getParent().getParentComponent();
        
        SourceTarget sourceTarget = new SourceTarget(source, target);

//        List<Coupling<?>> couplings = sourceTargetCouplings.get(sourceTarget);
        
        sourceTargetCouplings.put(sourceTarget, addCouplingToList(
            sourceTargetCouplings.get(sourceTarget), coupling));
        sourceCouplings.put(source, addCouplingToList(
            sourceCouplings.get(source), coupling));
        // TODO is this the way to do this?
        targetCouplings.put(target, coupling);
        
//        couplings.add(coupling);
    }
    
    /**
     * Adds a coupling to the provided list.  If the list is null, a new list
     * is created.  The list that the coupling is added to is returned.
     * 
     * @param list The list to add to.
     * @param coupling The coupling to add.
     * @return The passed in list or a new list if null was provided.
     */
    private List<Coupling<?>> addCouplingToList(final List<Coupling<?>> list,
            final Coupling<?> coupling) {
        List<Coupling<?>> local = list;
        
        if (local == null) {
            local = new ArrayList<Coupling<?>>();
        }
        
        local.add(coupling);
        
        return local;
    }
    
    /**
     * Removes a coupling from the manager.
     * 
     * @param coupling The coupling to remove.
     */
    void removeCoupling(final Coupling<?> coupling) {
        WorkspaceComponent<?> source = coupling.getProducingAttribute()
            .getParent().getParentComponent();
        WorkspaceComponent<?> target = coupling.getConsumingAttribute()
            .getParent().getParentComponent();
        
        SourceTarget sourceTarget = new SourceTarget(source, target);
        
        all.remove(coupling);
        
        removeCouplingFromList(sourceTargetCouplings.get(sourceTarget), coupling);
        removeCouplingFromList(sourceCouplings.get(source), coupling);
        targetCouplings.remove(target);
        
        source.couplingRemoved(coupling);
        
        if (target != source) { target.couplingRemoved(coupling); }
    }
    
    /**
     * Removes a coupling from the provided list.  If the list is null nothing is done.
     * 
     * @param list The list to remove from.
     * @param coupling The coupling to remove.
     */
    private void removeCouplingFromList(final List<Coupling<?>> list,
            final Coupling<?> coupling) {
        if (list != null) {
            list.remove(coupling);
        }
    }
    
//    public void removeCoupling(WorkspaceComponent<?> target) {
//        List<Coupling<?>> couplings = targetCouplings.get(target);
//
//        if (couplings != null) { couplings.remove(index)
//    }
    
    /**
     * A Simple holder for linking a source and a target.
     * 
     * @author Matt Watson
     */
    private static class SourceTarget {
        /** An arbitrary prime used to improve hashing distribution. */
        private static final int ARBITRARY_PRIME = 57;
        
        /** The source component. */
        private final WorkspaceComponent<?> source;
        /** The target component. */
        private final WorkspaceComponent<?> target;
        
        /**
         * Creates an instance.
         * 
         * @param source The source.
         * @param target The target.
         */
        SourceTarget(final WorkspaceComponent<?> source, final WorkspaceComponent<?> target) {
            this.source = source;
            this.target = target;
            
            System.out.println("created source-target: " + source + ", " + target);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(final Object o) {
            if (o instanceof SourceTarget) {
                SourceTarget other = (SourceTarget) o;
                
                return other.source == source && other.target == target;
            } else {
                return false;
            }
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return source.hashCode() + (ARBITRARY_PRIME * target.hashCode());
        }
    }
}
