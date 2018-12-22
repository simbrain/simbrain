package org.simbrain.workspace;

import java.util.*;

/**
 * Maintains a list of {@link Coupling}'s, and of potential {@link Producer} and
 * {@link Consumer} objects.  Supports creation of couplings, setting of producer
 * and consumer visibility, and filtering of all three types of object, e.g.
 *
 * This is a transient field of {@link Workspace} which is thus not persisted.
 *
 * Coupling creation should rely on the factory methods here
 * rather than by invoking constructors on Coupling directly so that couplings will
 * be properly managed and serialized.
 */
public class CouplingManager {

    /**
     * All couplings for the workspace.
     */
    private final List<Coupling<?>> couplings = new ArrayList<Coupling<?>>();

    /**
     * List of listeners to fire updates when couplings are changed.
     */
    private transient List<CouplingListener> couplingListeners = new ArrayList<CouplingListener>();

    /**
     * Construct a new coupling manager.
     *
     * @param workspace reference to parent workspace
     */
    CouplingManager(Workspace workspace) {

        // Update coupling list as components are added or removed
        workspace.addListener(new WorkspaceListener() {

            @Override
            public void workspaceCleared() {
                // This should clear all couplings, producers, and consumers
                // This is happening now in different ways
                //   Couplings are cleared by directly getting the list from here and clearning it
                //   Producers, consumers is not as clear. But see AttributePanel#Refresh
            }

            @Override
            public void newWorkspaceOpened() {
            }

            @Override
            public void componentAdded(WorkspaceComponent component) {

                // Any new producers or consumers should be added
                // Example, when a new odor world is added, all the producers and consumers
                // corresponding to the default objects should be added

                // This might be enough, may not need anything above.
                component.addListener(new WorkspaceComponentAdapter() {

                    @Override
                    public void attributeContainerAdded(AttributeContainer addedModel) {
                        // Update producer/consumer list
                        // Example; when a neuron is added, producers must be added
                    }

                    @Override
                    public void attributeContainerRemoved(AttributeContainer removedModel) {
                        // Update producer / consumer list
                        removeDeadCouplings(removedModel);
                        // Example; when a neuron is removed, producers must be removed
                    }
                });
            }

            @Override
            public void componentRemoved(WorkspaceComponent component) {
                // Any producers, consumers, or couplings containing them
                // that have been removed should also be removed

                // May not be needed. If the attributeContainerRemoved events are fired
            }
        });
    }

    /**
     * Create a coupling from a producer and consumer of the same type.
     *
     * @param producer producer part of the coupling
     * @param consumer consumer part of the coupling
     * @param <T> type of the coupling
     * @return the newly creating coupling
     * @throws MismatchedAttributesException exception if type of producer and
     *  consumer don't match
     */
    private <T> Coupling<T> createCoupling(
            Producer<T> producer,
            Consumer<T> consumer)
        throws MismatchedAttributesException {
        Coupling<T> coupling = Coupling.create(producer, consumer);
        addCoupling(coupling);
        return coupling;
    }

    //TODO: Consider removing this. It seems to just be a convenience method to avoid dealing with exceptions.
    /**
     * Try to create a coupling from a producer and consumer of the same type, but
     * do nothing if the types do not match and return null.
     */
    @SuppressWarnings("unchecked")
    public Coupling tryCoupling(Producer producer, Consumer consumer) {
        if (producer.getType() == consumer.getType()) {
            try {
                return createCoupling(producer, consumer);
            } catch (MismatchedAttributesException ex) {
                // Should never happen
                throw new AssertionError(ex);
            }
        } else {
            return null;
        }
    }

    /**
     * Create a coupling from each producer to every consumer of the same type.
     * Will throw an exception if any of the types do not match.
     *
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     * @throws MismatchedAttributesException An exception indicating that a pair of attributes did not match
     *                                       types. This will be thrown for the first such pair encountered.
     */
    public void createOneToManyCouplings(Collection<Producer<?>> producers, Collection<Consumer<?>> consumers) throws MismatchedAttributesException {
        for (Producer producer : producers) {
            for (Consumer consumer : consumers) {
                createCoupling(producer, consumer);
            }
        }
    }

    /**
     * Create a coupling from each attribute in the smaller collection to a corresponding attribute.
     * Will throw an exception if any of the types do not match.
     *
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     * @throws MismatchedAttributesException An exception indicating that a pair of attributes did not match
     *                                       types. This will be thrown for the first such pair encountered.
     */
    public void createOneToOneCouplings(Collection<Producer<?>> producers, Collection<Consumer<?>> consumers) throws MismatchedAttributesException {
        Iterator<Consumer<?>> consumerIterator = consumers.iterator();
        for (Producer<?> producer : producers) {
            if (consumerIterator.hasNext()) {
                Consumer consumer = consumerIterator.next();
                createCoupling(producer, consumer);
            } else {
                break;
            }
        }
    }

    /**
     * Try to create a coupling from each attribute in the smaller collection, but ignore any mismatched types.
     *
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     */
    public void tryOneToOneCouplings(Collection<Producer<?>> producers, Collection<Consumer<?>> consumers) {
        Iterator<Consumer<?>> consumerIterator = consumers.iterator();
        for (Producer producer : producers) {
            if (consumerIterator.hasNext()) {
                Consumer consumer = consumerIterator.next();
                tryCoupling(producer, consumer);
            } else {
                break;
            }
        }
    }

    public List<Coupling<?>> getCouplings() {
        return couplings;
    }

    /**
     * Adds a new listener to be updated when changes are made.
     *
     * @param listener to be updated of changes
     */
    public void addCouplingListener(CouplingListener listener) {
        couplingListeners.add(listener);
    }

    /**
     * Removes the listener from the list.
     *
     * @param listener to be removed
     */
    public void removeCouplingListener(CouplingListener listener) {
        couplingListeners.remove(listener);
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

    private void fireCouplingsRemoved(List<Coupling<?>> couplings) {
        for (CouplingListener listeners : couplingListeners) {
            listeners.couplingsRemoved(couplings);
        }
    }

    public void removeCouplings(List<Coupling<?>> couplings) {
        getCouplings().removeAll(couplings);
        // What to do here?
        this.fireCouplingsRemoved(couplings);
    }

    /**
     * Return a coupling in the workspace by the coupling id.
     */
    public Coupling<?> getCoupling(String id) {
        return getCouplings().stream().filter(c -> c.getId().equalsIgnoreCase(id)).findFirst().get();
    }

    /**
     * Convenience method for updating a set of couplings.
     *
     * @param couplingList the list of couplings to be updated
     */
    public void updateCouplings(List<Coupling<?>> couplingList) {
        for (Coupling<?> coupling : couplingList) {
            coupling.update();
        }
    }

    public void addCoupling(Coupling<?> coupling) {
        getCouplings().add(coupling);
        fireCouplingAdded(coupling);
    }

    /**
     * Remove any couplings associated with the "dead" object.
     *
     * @param object the object that has been removed
     */
    private void removeDeadCouplings(AttributeContainer object) {
        List<Coupling<?>> toRemove = new ArrayList<>();
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

    public void updateCouplings() {
        for (Coupling<?> coupling : getCouplings()) {
            coupling.update();
        }
    }

    public void removeCoupling(Coupling<?> coupling) {
        getCouplings().remove(coupling);
        fireCouplingRemoved(coupling);
    }

}
