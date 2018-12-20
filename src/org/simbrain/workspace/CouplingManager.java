package org.simbrain.workspace;

import org.simbrain.util.Pair;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
     * List of potential producers to be used in couplings.
     */
    private Map<Pair<AttributeContainer, Method>, Producer> potentialProducers = new HashMap<>();

    /**
     * List of potential consumers to be updated in couplings.
     */
    private Map<Pair<AttributeContainer, Method>, Consumer> potentialConsumers = new HashMap<>();

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

    /**
     * Get all visible producers on a specified component.
     *
     * @param component component to scan for visible producers
     * @return the visible producers
     */
    public List<Producer<?>> getVisibleProducers(WorkspaceComponent component) {
        return getProducers(component).stream().filter(Attribute::isVisible).collect(Collectors.toList());
    }

    /**
     * Get all visible consumers on a specified component.
     *
     * @param component component to scan for visible consumers
     * @return the visible consumers
     */
    public List<Consumer<?>> getVisibleConsumers(WorkspaceComponent component) {
        return getConsumers(component).stream().filter(Attribute::isVisible).collect(Collectors.toList());
    }

    /**
     * Get all the potential producers for a given WorkspaceComponent.
     *
     * @param component The component to generate producers from.
     * @return A list of potential producers.
     */
    public List<Producer<?>> getProducers(WorkspaceComponent component) {
        List<Producer<?>> producers = new ArrayList<>();
        // TODO: Note this is not using the internal map of producers
        for (AttributeContainer container : component.getAttributeContainers()) {
            producers.addAll(getProducersFromContainers(container));
        }
        return producers;
    }

    /**
     * Get all the potential consumers for a given WorkspaceComponent.
     *
     * @param component The component to generate consumers from.
     * @return A list of potential consumers.
     */
    public List<Consumer<?>> getConsumers(WorkspaceComponent component) {
        List<Consumer<?>> consumers = new ArrayList<>();
        for (AttributeContainer container : component.getAttributeContainers()) {
            consumers.addAll(getConsumersFromContainer(container));
        }
        return consumers;
    }


    /**
     * Get all the potential producers from an {@link AttributeContainer}.
     *
     * @param container The object to check for Producibles.
     * @return A list of producers.
     */
    public List<Producer<?>> getProducersFromContainers(AttributeContainer container) {
        List<Producer<?>> producers = new ArrayList<>();
        for (Method method : container.getClass().getMethods()) {
            if (isProducible(method)) {
                producers.add(getProducer(container, method));
            }
        }
        return producers;
    }

    /**
     * Get all the potential consumers from an attribute container.
     *
     * @param container The object to check for Consumables.
     * @return A list of consumers.
     */
    public List<Consumer<?>> getConsumersFromContainer(AttributeContainer container) {
        List<Consumer<?>> consumers = new ArrayList<>();
        for (Method method : container.getClass().getMethods()) {
            if (isConsumable(method)) {
                consumers.add(getConsumer(container, method));
            }
        }
        return consumers;
    }

    /**
     * Get a specific consumer from a specified {@link AttributeContainer}.
     *
     * @param container The object in which to find the consumable.
     * @param methodName The name of the consumable method.
     * @return The consumer
     */
    public Consumer<?> getConsumer(AttributeContainer container, String methodName) {
        Stream<Method> stream = Arrays.stream(container.getClass().getMethods());
        Optional<Method> method = stream.filter(m -> m.getName().equals(methodName)).findFirst();
        if (method.isPresent()) {
            return getConsumer(container, method.get());
        } else {
            throw new IllegalArgumentException(String.format("No consumable method with name %s was found in class %s.", methodName, container.getClass().getSimpleName()));
        }
    }

    /**
     * Get a specific consumer from the {@link AttributeContainer} and throw
     * an exception if it is not of the specified type.
     *
     * @param container The object in which to find the consumable.
     * @param methodName The name of the consumable method.
     * @return The consumer.
     */
    @SuppressWarnings("unchecked")
    public <T> Consumer<T> getConsumer(AttributeContainer container, String methodName, Class<T> type) throws NoSuchMethodException {
        Consumer<?> consumer = getConsumer(container, methodName);
        if (consumer.getType() == type) {
            return (Consumer<T>) consumer;
        } else {
            throw new NoSuchMethodException(String.format("Consumer type %s does not match method value type %s.", consumer.getType(), type));
        }
    }

    /**
     * Get a specific producer from the {@link AttributeContainer} object.
     *
     * @param container      The container in which to find the producible.
     * @param methodName The name of the producible method.
     * @return The producer.
     */
    public Producer<?> getProducer(AttributeContainer container, String methodName) {
        try {
            Method method = container.getClass().getMethod(methodName);
            return getProducer(container, method);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Get a specific producer from the {@link AttributeContainer}
     * and throw an exception if it is not of the specified type.
     *
     * @param container The object in which to find the producible.
     * @param methodName The name of the producible method.
     * @return The producer
     */
    @SuppressWarnings("unchecked")
    public <T> Producer<T> getProducer(AttributeContainer container, String methodName, Class<T> type) throws NoSuchMethodException {
        Producer<?> producer = getProducer(container, methodName);
        if (producer.getType().equals(type)) {
            return (Producer<T>) producer;
        } else {
            throw new NoSuchMethodException(String.format("Producer with type %s does not match method return type %s.", producer.getType(), type));
        }
    }

    /**
     * Return whether the specified method is producible.
     */
    private boolean isProducible(Method method) {
        return method.getAnnotation(Producible.class) != null;
    }

    /**
     * Return whether the specified method is consumable.
     */
    private boolean isConsumable(Method method) {
        return method.getAnnotation(Consumable.class) != null;
    }

    /**
     * Create a producer from the specified method on the {@link AttributeContainer}.
     */
    private Producer<?> getProducer(AttributeContainer container, Method method) {
        Pair key = new Pair(container, method);
        if(potentialProducers.containsKey(key)) {
            return potentialProducers.get(key);
        }
        Producible annotation = method.getAnnotation(Producible.class);
        if (annotation == null) {
            throw new IllegalArgumentException(String.format("Method %s is not producible.", method.getName()));
        }
        Method idMethod = getMethod(container, annotation.idMethod());
        Method customDescriptionMethod = getMethod(container, annotation.customDescriptionMethod());
        Method arrayDescriptionMethod = getMethod(container, annotation.arrayDescriptionMethod());

        String description = annotation.description();
        boolean visibility = annotation.defaultVisibility();
        Producer newProducer = new Producer(container, method, description, idMethod, customDescriptionMethod, arrayDescriptionMethod, visibility );
        potentialProducers.put(key, newProducer);
        return newProducer;
    }

    /**
     * Create a consumer from the specified method on the 
     * {@link AttributeContainer}.
     */
    private Consumer<?> getConsumer(AttributeContainer container, Method method) {
        Pair key = new Pair(container, method);
        if(potentialConsumers.containsKey(key)) {
            return potentialConsumers.get(key);
        }
        Consumable annotation = method.getAnnotation(Consumable.class);
        if (annotation == null) {
            throw new IllegalArgumentException(String.format("Method %s is not consumable.", method.getName()));
        }
        Method idMethod = getMethod(container, annotation.idMethod());
        Method customDescriptionMethod = getMethod(container, annotation.customDescriptionMethod());
        String description = annotation.description();
        boolean visibility = annotation.defaultVisibility();
        Consumer newConsumer = new Consumer(container, method, description, idMethod, customDescriptionMethod, visibility);
        potentialConsumers.put(key, newConsumer);
        return newConsumer;

    }

    /**
     * Helper to get a method object given the object and methodname
     */
    private Method getMethod(AttributeContainer container, String methodName) {
        try {
            return container.getClass().getMethod(methodName);
        } catch (NoSuchMethodException ex) {
            return null;
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
