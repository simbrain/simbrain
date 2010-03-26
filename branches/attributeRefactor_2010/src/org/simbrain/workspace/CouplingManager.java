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
import java.util.concurrent.CopyOnWriteArrayList;

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
    private List<Coupling<?>> all = new CopyOnWriteArrayList<Coupling<?>>();

    /** The couplings indexed by the source and target combination. */
    private Map<SourceTarget, List<Coupling<?>>> sourceTargetCouplings = newMap();

    /** The couplings indexed by source. */
    private Map<WorkspaceComponent, List<Coupling<?>>> sourceCouplings = newMap();

    /** The couplings indexed by target. */
    private Map<WorkspaceComponent, List<Coupling<?>>> targetCouplings = newMap();

    /** The couplings indexed by consuming attribute, which is unique. */
//    private Map<ConsumingAttribute<?>, Coupling<?>> consumingAttributes = newMap(); //TODO: What was this for?

    /** Default priority. */
    private static final int DEFAULT_PRIORITY = 0;

    /** Priority of this component; used in priorty based workspace update. */
    private int priority = DEFAULT_PRIORITY;

    /** List of listeners to fire updates when couplings are changed. */
    private ArrayList<CouplingComponentListener> couplingManagerListeners
      = new ArrayList<CouplingComponentListener>();
    
    /**
     * 
     * Helper method to cleanup nasty generics declarations.
     * 
     * @param <K> The key type.
     * @param <V> The value type.
     * @return A new HashMap.
     */
    private static <K, V> Map<K, V> newMap() {
        return new HashMap<K, V>();
    }
    
    /**
     * Returns an unmodifiable list of all the couplings.
     * 
     * @return An unmodifiable list of all the couplings.
     */
    public Collection<? extends Coupling<?>> getCouplings() {
        return Collections.unmodifiableList(all);
    }
    
    /**
     * Clear all couplings.
     */
    public void clearCouplings() {
        all.clear();
    }

    /**
     * Returns all couplings from the given source to the given target.
     * 
     * @param source The source to use in the search.
     * @param target The target to use in the search.
     * @return A list of the couplings between the provided source and target.
     */
    public Collection<? extends Coupling<?>> getCouplings(
            final WorkspaceComponent source, final WorkspaceComponent target) {
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
    public void updateAllCouplings() {
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
    public Coupling<?> findCoupling(final String sourceId, final String targetId) {
        return null;
    }
    
    /**
     * Removes all couplings associated with a producer or consumer.
     *
     * @param attribute consumer or producer.
     */
    public void removeAttachedCouplings(final Attribute attribute) {
        for (Coupling<?> coupling : getCouplings()) {
            if (attribute instanceof Consumer) {
                if (coupling.getConsumer() == attribute) {
                    removeCoupling(coupling);
                }
            }
            if (attribute instanceof Producer) {
                if (coupling.getProducer() == attribute) {
                    removeCoupling(coupling);
                }
            }
        }
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
//        Coupling<?> old = consumingAttributes.get(coupling.getConsumingAttribute());
//        
//        // TODO warning that old was deleted
//        
//        if (old != null) {
//            System.out.println("removing old coupling: " + old);
////            removeCoupling(old);
//        }
//
//        consumingAttributes.put(coupling.getConsumingAttribute(), coupling);

        all.add(coupling);

        WorkspaceComponent source = coupling.getProducer().getParentComponent();
        WorkspaceComponent target = coupling.getConsumer().getParentComponent();

        SourceTarget sourceTarget = new SourceTarget(source, target);

        sourceTargetCouplings.put(sourceTarget, addCouplingToList(
            sourceTargetCouplings.get(sourceTarget), coupling));
        sourceCouplings.put(source, addCouplingToList(
            sourceCouplings.get(source), coupling));
        // TODO is this the way to do this?
        targetCouplings.put(target, addCouplingToList(
            targetCouplings.get(source), coupling));
        fireCouplingListUpdated();
    }

    /**
     * Replaces any couplings where the old attribute is the source or
     * target with a new coupling with the new attribute in the source
     * and/or target.
     *
     * @param oldAttr the attribute to be replaced.
     * @param newAttr the attribute to replace it with.
     */
    @SuppressWarnings("unchecked")
    public void replaceCouplings(final Attribute oldAttr, final Attribute newAttr) {
        
        //TODO: Think
        
//        for (Coupling<?> coupling : new ArrayList<Coupling<?>>(all)) {
//            boolean replace = false;
//            ProducingAttribute producer = coupling.getProducingAttribute();
//            ConsumingAttribute consumer = coupling.getConsumingAttribute();
//
//            if (consumer == oldAttr) {
//                replace = true;
//                consumer = (ConsumingAttribute) newAttr;
//            }
//
//            if (producer == oldAttr) {
//                replace = true;
//                producer = (ProducingAttribute) newAttr;
//            }
//
//            if (replace) {
//                removeCoupling(coupling);
//                addCoupling(new Coupling(producer, consumer));
//            }
//        }
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
     * Remove all couplings associated with a WorkspaceComponent.
     *
     * @param component component to check.
     */
    public void removeCouplings(final WorkspaceComponent component) {
        ArrayList<Coupling<?>> toRemove = new ArrayList<Coupling<?>>();
        for (Coupling<?> coupling : getCouplings()) {
            if (coupling.getConsumer().getParentComponent() == component) {
                toRemove.add(coupling);
            }
            if (coupling.getProducer().getParentComponent() == component) {
                toRemove.add(coupling);
            }
        }
        removeCouplings(toRemove);
    }

    /**
     * Remove a specified list of couplings.
     *
     * @param couplings list of couplings to remove
     */
    public void removeCouplings(final ArrayList<Coupling<?>> couplings) {
        for (Coupling<?> coupling : couplings) {
            removeCoupling(coupling);
        }
    }

    /**
     * Removes a coupling from the manager.
     * 
     * @param coupling The coupling to remove.
     */
    public void removeCoupling(final Coupling<?> coupling) {
        WorkspaceComponent source = coupling.getProducer().getParentComponent();
        WorkspaceComponent target = coupling.getConsumer().getParentComponent();

        SourceTarget sourceTarget = new SourceTarget(source, target);

        //consumingAttributes.remove(coupling.getConsumingAttribute());

        all.remove(coupling);

        removeCouplingFromList(sourceTargetCouplings.get(sourceTarget), coupling);
        removeCouplingFromList(sourceCouplings.get(source), coupling);
        removeCouplingFromList(targetCouplings.get(target), coupling);

        source.couplingRemoved(coupling);

        if (target != source) { target.couplingRemoved(coupling); }
        fireCouplingListUpdated();
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
    
    /**
     * A Simple holder for linking a source and a target.
     * 
     * @author Matt Watson
     */
    private static class SourceTarget {
        /** An arbitrary prime used to improve hashing distribution. */
        private static final int ARBITRARY_PRIME = 57;
        
        /** The source component. */
        private final WorkspaceComponent source;
        /** The target component. */
        private final WorkspaceComponent target;
        
        /**
         * Creates an instance.
         * 
         * @param source The source.
         * @param target The target.
         */
        SourceTarget(final WorkspaceComponent source, final WorkspaceComponent target) {
            if (source == null) throw new IllegalArgumentException("source cannot be null");
            if (target == null) throw new IllegalArgumentException("target cannot be null");
            
            this.source = source;
            this.target = target;
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
    
    /**
     * {@inheritDoc}
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * {@inheritDoc}
     */
    public void setPriority(final int value) {
        priority = value;
    }


    /**
     * Sends updates to all listeners when changes are made.
     */
    private void fireCouplingListUpdated() {

        for (CouplingComponentListener listeners : couplingManagerListeners) {
            listeners.couplingListUpdated();
        }
    }

    /**
     * Adds a new listener to be updated when changes are made.
     * @param listener to be updated of changes
     */
    public void addCouplingListener(final CouplingComponentListener listener) {
        couplingManagerListeners.add(listener);
    }

    /**
     * Removes the listener from the list.
     * @param listener to be removed
     */
    public void removeCouplingListener(final CouplingComponentListener listener) {
        couplingManagerListeners.remove(listener);
    }
}
