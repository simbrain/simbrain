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
import java.util.Arrays;
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
 *
 * @see Coupling
 */
public class CouplingManager {

    /** The static logger for this class. */
    private static final Logger LOGGER = Logger
            .getLogger(CouplingManager.class);

    /** Parent workspace. */
    private final Workspace workspace;

    /** All couplings for the workspace. */
    private List<Coupling<?>> couplingList = new CopyOnWriteArrayList<Coupling<?>>();

    /** The couplings indexed by the source and target combination. */
    private Map<SourceTarget, List<Coupling<?>>> sourceTargetCouplings = newMap();

    /** The couplings indexed by source component. */
    private Map<WorkspaceComponent, List<Coupling<?>>> sourceComponentCouplings = newMap();

    /** The couplings indexed by target component. */
    private Map<WorkspaceComponent, List<Coupling<?>>> targetComponentCouplings = newMap();

    /** Associates workspace components with attribute listeners. */
    private Map<WorkspaceComponent, AttributeListener> listenerMap = newMap();

    /**
     * The couplings indexed by consuming attribute, which is unique. Used to
     * check for (and remove) couplings with a shared consumer.
     */
    private Map<Consumer<?>, Coupling<?>> consumers = newMap();

    /** Default priority. */
    private static final int DEFAULT_PRIORITY = 0;

    /** Priority of this component; used in priority based workspace update. */
    private int priority = DEFAULT_PRIORITY;

    /** List of listeners to fire updates when couplings are changed. */
    private List<CouplingListener> couplingListeners = new ArrayList<CouplingListener>();

    /**
     * Construct a coupling manager.
     *
     * @param workspace workspace reference
     */
    public CouplingManager(final Workspace workspace) {
        this.workspace = workspace;

        // When workspace components are added, add listeners to them,
        // so that when relevant objects are removed the corresponding couplings
        // can be cleaned up
        workspace.addListener(new WorkspaceListener() {

            /**
             * {@ineritDoc}
             */
            public void componentAdded(WorkspaceComponent component) {

                AttributeListener listener = new AttributeListener() {

                    /**
                     * {@ineritDoc}
                     */
                    public void attributeObjectRemoved(Object object) {
                        removeDeadCouplings(object);
                    }

                    /**
                     * {@ineritDoc}
                     */
                    public void attributeTypeVisibilityChanged(
                            AttributeType type) {
                    }

                    /**
                     * {@ineritDoc}
                     */
                    public void potentialAttributesChanged() {
                    }

                };

                component.addAttributeListener(listener);
                listenerMap.put(component, listener);
            }

            /**
             * {@ineritDoc}
             */
            public void componentRemoved(WorkspaceComponent component) {
                AttributeListener listener = listenerMap.get(component);
                component.removeAttributeListener(listener);
                listenerMap.remove(component);
            }

            /**
             * {@ineritDoc}
             */
            public void workspaceCleared() {
            }

            /**
             * {@ineritDoc}
             */
            public void newWorkspaceOpened() {
            }

        });
    }

    /**
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
        return Collections.unmodifiableList(couplingList);
    }

    /**
     * Clear all couplings.
     */
    public void clearCouplings() {
        couplingList.clear();
    }

    /**
     * Returns all couplings from the given source component to the given target
     * component.
     *
     * @param sourceComponent The source component to use in the search.
     * @param targetComponent The target component to use in the search.
     * @return A list of the couplings between the provided source and target.
     */
    public Collection<? extends Coupling<?>> getCouplings(
            final WorkspaceComponent sourceComponent,
            final WorkspaceComponent targetComponent) {

        Collection<Coupling<?>> couplings = sourceTargetCouplings
                .get(new SourceTarget(sourceComponent, targetComponent));

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
     * Removes all couplings associated with a producer or consumer.
     *
     * @param attribute consumer or producer.
     */
    public void removeAttachedCouplings(final Attribute attribute) {
        for (Coupling<?> coupling : getCouplings()) {
            if (attribute instanceof Consumer<?>) {
                if (coupling.getConsumer() == attribute) {
                    removeCoupling(coupling);
                }
            }
            if (attribute instanceof Producer<?>) {
                if (coupling.getProducer() == attribute) {
                    removeCoupling(coupling);
                }
            }
        }
    }

    /**
     * Returns whether the coupling is referenced by this manager.
     *
     * @param toCheck The coupling to search for.
     * @return whether the coupling is referenced by this manager.
     */
    public boolean containseEquivalentCoupling(final Coupling<?> toCheck) {
        for (Coupling<?> coupling : getCouplings()) {
            boolean consumersMatch = attributesMatch(coupling.getConsumer(),
                    toCheck.getConsumer());
            boolean producersMatch = attributesMatch(coupling.getProducer(),
                    toCheck.getProducer());
            if (consumersMatch && producersMatch) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether two attributes are the same
     *
     * @param attribute1 first attribute to compare
     * @param attribute2 second attribute to compare
     * @return whether they match
     */
    private boolean attributesMatch(Attribute attribute1, Attribute attribute2) {
        boolean baseObjectMatches = (attribute1.getBaseObject() == attribute2
                .getBaseObject());
        boolean methodNameMatches = (attribute1.getMethodName()
                .equalsIgnoreCase(attribute2.getMethodName()));
        boolean typeMatches = (attribute1.getDataType() == attribute2
                .getDataType());
        boolean argTypesMatch = Arrays.deepEquals(
                attribute1.getArgumentDataTypes(), attribute2.getArgumentDataTypes());
        boolean argValuesMatch = Arrays.deepEquals(
                attribute1.getArgumentValues(), attribute2.getArgumentValues());
        return (baseObjectMatches && methodNameMatches && typeMatches
                && argValuesMatch && argTypesMatch);
    }

    /**
     * Remove coupling (if any) that is essentially a copy of the supplied
     * coupling.
     *
     * @param toRemove the coupling type to remove
     */
    public void removeMatchingCoupling(Coupling<?> toRemove) {
        for (Coupling<?> coupling : getCouplings()) {
            boolean consumersMatch = attributesMatch(coupling.getConsumer(),
                    toRemove.getConsumer());
            boolean producersMatch = attributesMatch(coupling.getProducer(),
                    toRemove.getProducer());
            if (consumersMatch && producersMatch) {
                removeCoupling(coupling);
            }
        }
    }

    /**
     * Adds a coupling to this instance.
     *
     * @param coupling The coupling to add.
     * @throws UmatchedAttributesException thrown if the attributes in this
     *             coupling have mismatched data types
     */
    public void addCoupling(final Coupling<?> coupling)
            throws UmatchedAttributesException {

        // If there is already a coupling with the same consumer, remove it,
        // because it does not make sense for one attribute to have multiple
        // sources.
        Coupling<?> old = consumers.get(coupling.getConsumer());
        if (old != null) {
            System.out.println("removing old coupling: " + old);
            removeCoupling(old);
        }
        consumers.put(coupling.getConsumer(), coupling);

        // Throw exception if datatypes are unmatched
        if (coupling.getConsumer().getDataType() != coupling.getProducer()
                .getDataType()) {
            String warning = "Producer type ("
                    + coupling.getProducer().getDataType().getCanonicalName()
                    + ") does not match consumer type ("
                    + coupling.getConsumer().getDataType().getCanonicalName()
                    + ")";
            throw new UmatchedAttributesException(warning);
        }
        couplingList.add(coupling);

        // Populate source / target maps (used to return lists of couplings
        // connecting particular workspace components together)
        WorkspaceComponent source = coupling.getProducer().getParentComponent();
        WorkspaceComponent target = coupling.getConsumer().getParentComponent();
        SourceTarget sourceTarget = new SourceTarget(source, target);
        sourceTargetCouplings.put(
                sourceTarget,
                addCouplingToList(sourceTargetCouplings.get(sourceTarget),
                        coupling));
        sourceComponentCouplings.put(
                source,
                addCouplingToList(sourceComponentCouplings.get(source),
                        coupling));
        targetComponentCouplings.put(
                target,
                addCouplingToList(targetComponentCouplings.get(source),
                        coupling));

        // Fire coupling added event
        fireCouplingAdded(coupling);
    }

    /**
     * Remove any couplings associated with the "dead" object.
     *
     * @param object the object that has been removed
     */
    private void removeDeadCouplings(Object object) {
        List<Coupling<?>> toRemove = new ArrayList<Coupling<?>>();
        for (Coupling<?> coupling : getCouplings()) {
            if (coupling.getConsumer().getBaseObject() == object) {
                toRemove.add(coupling);
            }
            if (coupling.getProducer().getBaseObject() == object) {
                toRemove.add(coupling);
            }
        }
        for (Coupling<?> coupling : toRemove) {
            removeCoupling(coupling);
        }
    }

    /**
     * Replaces any couplings where the old attribute is the source or target
     * with a new coupling with the new attribute in the source and/or target.
     *
     * @param oldAttr the attribute to be replaced.
     * @param newAttr the attribute to replace it with.
     */
    @SuppressWarnings("unchecked")
    public void replaceCouplings(final Attribute oldAttr,
            final Attribute newAttr) {

        for (Coupling<?> coupling : new ArrayList<Coupling<?>>(couplingList)) {
            boolean replace = false;
            Producer<?> producer = coupling.getProducer();
            Consumer<?> consumer = coupling.getConsumer();

            if (consumer == oldAttr) {
                replace = true;
                consumer = (Consumer) newAttr;
            }

            if (producer == oldAttr) {
                replace = true;
                producer = (Producer) newAttr;
            }

            if (replace) {
                removeCoupling(coupling);
                try {
                    addCoupling(new Coupling(producer, consumer));
                } catch (UmatchedAttributesException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Adds a coupling to the provided list. If the list is null, a new list is
     * created. The list that the coupling is added to is returned.
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

        // consumingAttributes.remove(coupling.getConsumingAttribute());

        couplingList.remove(coupling);

        removeCouplingFromList(sourceTargetCouplings.get(sourceTarget),
                coupling);
        removeCouplingFromList(sourceComponentCouplings.get(source), coupling);
        removeCouplingFromList(targetComponentCouplings.get(target), coupling);

        source.couplingRemoved(coupling);

        if (target != source) {
            target.couplingRemoved(coupling);
        }

        fireCouplingRemoved(coupling);
    }

    /**
     * Removes a coupling from the provided list. If the list is null nothing is
     * done.
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
        SourceTarget(final WorkspaceComponent source,
                final WorkspaceComponent target) {
            if (source == null) {
                throw new IllegalArgumentException("source cannot be null");
            }
            if (target == null) {
                throw new IllegalArgumentException("target cannot be null");
            }

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
     * Get the priority. Used to set updating of couplings to a specific
     * priority when managing custom workspace update.
     *
     * @return priority for the coupling manager.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set the priority.
     *
     * @param value the priority to set
     */
    public void setPriority(final int value) {
        priority = value;
    }

    /**
     * Coupling added.
     *
     * @param coupling coupling that was added
     */
    private void fireCouplingAdded(Coupling<?> coupling) {

        for (CouplingListener listeners : couplingListeners) {
            listeners.couplingAdded(coupling);
        }
    }

    /**
     * Coupling removed.
     *
     * @param coupling coupling that was removed
     */
    private void fireCouplingRemoved(Coupling<?> coupling) {

        for (CouplingListener listeners : couplingListeners) {
            listeners.couplingRemoved(coupling);
        }
    }

    /**
     * Adds a new listener to be updated when changes are made.
     *
     * @param listener to be updated of changes
     */
    public void addCouplingListener(final CouplingListener listener) {
        couplingListeners.add(listener);
    }

    /**
     * Removes the listener from the list.
     *
     * @param listener to be removed
     */
    public void removeCouplingListener(final CouplingListener listener) {
        couplingListeners.remove(listener);
    }

    /**
     * Convenience method for updating a set of couplings.
     *
     * @param couplingList the list of couplings to be updated
     */
    public void updateCouplings(List<Coupling<?>> couplingList) {
        for (Coupling<?> coupling : couplingList) {
            coupling.setBuffer();
        }

        for (Coupling<?> coupling : couplingList) {
            coupling.update();
        }
    }

}