package org.simbrain.workspace;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * CouplingFactory provides methods for instantiating couplings between components in a workspace.
 * Simulation and GUI code should rely on the factory methods here rather than invoking constructors
 * on Coupling directly so that couplings will be properly managed and serialized.
 */
public class CouplingFactory {
    /** The workspace in which the couplings created by this factory will be managed. */
    private Workspace workspace;

    /** Create a new CouplingFactory in the specified workspace. */
    public CouplingFactory(Workspace workspace) {
        this.workspace = workspace;
    }

    /** Create a coupling from a producer and consumer of the same type. */
    public <T> Coupling<T> createCoupling(Producer<T> producer, Consumer<T> consumer)
            throws MismatchedAttributesException {
        Coupling<T> coupling = Coupling.create(producer, consumer);
        workspace.addCoupling(coupling);
        return coupling;
    }

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
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     * @exception MismatchedAttributesException An exception indicating that a pair of attributes did not match
     *     types. This will be thrown for the first such pair encountered.
     */
     public void createOneToManyCouplings(Collection<Producer<?>> producers, Collection<Consumer<?>> consumers)
            throws MismatchedAttributesException {
         for (Producer producer : producers) {
             for (Consumer consumer : consumers) {
                 createCoupling(producer, consumer);
             }
         }
     }

    /**
     * Try to create a coupling from each producer to every consumer, but ignore any mismatched types.
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     */
    public void tryOneToManyCouplings(Collection<Producer<?>> producers, Collection<Consumer<?>> consumers) {
        for (Producer producer : producers) {
            for (Consumer consumer : consumers) {
                tryCoupling(producer, consumer);
            }
        }
    }

    /**
     * Create a coupling from each attribute in the smaller collection to a corresponding attribute.
     * Will throw an exception if any of the types do not match.
     * @param producers A collection of producers to couple
     * @param consumers A collection of consumers to couple
     * @exception MismatchedAttributesException An exception indicating that a pair of attributes did not match
     *     types. This will be thrown for the first such pair encountered.
     */
    public void createOneToOneCouplings(Collection<Producer<?>> producers, Collection<Consumer<?>> consumers)
            throws MismatchedAttributesException {
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
     * Get all the potential producers for a given WorkspaceComponent.
     * @param component The component to generate producers from.
     * @return A list of potential producers.
     */
    public List<Producer<?>> getAllProducers(WorkspaceComponent component) {
        return getProducersFromModels(component.getModels());
    }

    /**
     * Get all the potential consumers for a given WorkspaceComponent.
     * @param component The component to generate consumers from.
     * @return A list of potential consumers.
     */
    public List<Consumer<?>> getAllConsumers(WorkspaceComponent component) {
        return getConsumersFromModels(component.getModels());
    }

    /**
     * Get all the potential producers from a list of model objects.
     * @param listOfModels A list of models to check for Producibles.
     * @return A list of producers.
     */
    public List<Producer<?>> getProducersFromModels(List listOfModels) {
        List<Producer<?>> producers = new ArrayList<>();
        for (Object model : listOfModels) {
            producers.addAll(getProducersFromModel(model));
        }
        return producers;
    }

    /**
     * Get all the potential consumers from a list of model objects.
     * @param listOfModels A list of models to check for Consumables.
     * @return A list of consumers.
     */
    public List<Consumer<?>> getConsumersFromModels(List listOfModels) {
        List<Consumer<?>> consumers = new ArrayList<>();
        for (Object model : listOfModels) {
            consumers.addAll(getConsumersFromModel(model));
        }
        return consumers;
    }

    /**
     * Get all the potential producers from a model object.
     * @param model The object to check for Producibles.
     * @return A list of producers.
     */
    public List<Producer<?>> getProducersFromModel(Object model) {
        List<Producer<?>> producers = new ArrayList<Producer<?>>();
        for (Method method : model.getClass().getMethods()) {
            Producible annotation = method.getAnnotation(Producible.class);
            if (annotation != null) {
                if (!annotation.indexListMethod().isEmpty()) {
                    // A custom keyed annotation is being used
                    try {
                        Method indexListMethod = model.getClass().getMethod(annotation.indexListMethod(), null);
                        List keys = (List) indexListMethod.invoke(model, null);
                        for (Object key : keys) {
                            Producer<?> consumer = new Producer(model, method);
                            consumer.key = key;
                            producers.add(consumer);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }  else {
                    // Annotation has no key
                    Producer<?> producer = new Producer(model, method);
                    producers.add(producer);
                }
            }
        }
        return producers;
    }

    /**
     * Get all the potential consumers from a model object.
     * @param model The object to check for Consumables.
     * @return A list of consumers.
     */
    public List<Consumer<?>> getConsumersFromModel(Object model) {
        List<Consumer<?>> consumers = new ArrayList<>();
        for (Method method : model.getClass().getMethods()) {
            Consumable annotation = method.getAnnotation(Consumable.class);
            if (annotation != null) {
                if (!annotation.indexListMethod().isEmpty()) {
                    // A custom keyed annotation is being used
                    try {
                        Method indexListMethod = model.getClass().getMethod(annotation.indexListMethod(), null);
                        List keys = (List) indexListMethod.invoke(model, null);
                        for (Object key: keys) {
                            Consumer<?> consumer = new Consumer(model, method);
                            consumer.key = key;
                            consumers.add(consumer);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // Annotation has no key
                    Consumer<?> consumer = new Consumer(model, method);
                    consumers.add(consumer);
                }
            }
        }
        return consumers;
    }

    /**
     * Get a specific consumer from the model object.
     * @param model The object in which to find the consumable.
     * @param methodName The name of the consumable method.
     * @return The consumer.
     */
    public Consumer<?> getConsumer(Object model, String methodName) {
        return getConsumersFromModel(model).stream().filter(
                c -> c.getMethod().getName().equalsIgnoreCase(methodName))
                .findFirst().get();
    }

    /**
     * Get a specific consumer from the model object and throw an exception if it is not of the specified type.
     * @param model The object in which to find the consumable.
     * @param methodName The name of the consumable method.
     * @return The consumer.
     */
    @SuppressWarnings("unchecked")
    public <T> Consumer<T> getConsumer(Object model, String methodName, Class<T> type)
            throws MismatchedAttributesException {
        Consumer<?> consumer = getConsumer(model, methodName);
        if (consumer.getType() == type) {
            return (Consumer<T>) consumer;
        } else {
            throw new MismatchedAttributesException(String.format(
                    "Consumer type %s does not match method value type %s.",
                    consumer.getType(), type));
        }
    }

    /**
     * Get a specific producer from the model object.
     * @param model The object in which to find the producible.
     * @param methodName The name of the producible method.
     * @return The producer.
     */
    public Producer<?> getProducer(Object model, String methodName) {
        return getProducersFromModel(model).stream().filter(
                p -> p.getMethod().getName().equalsIgnoreCase(methodName))
                .findFirst().get();
    }

    /**
     * Get a specific producer from the model object and throw an exception if it is not of the specified type.
     * @param model The object in which to find the producible.
     * @param methodName The name of the producible method.
     * @return The producer.
     */
    @SuppressWarnings("unchecked")
    public <T> Producer<T> getProducer(Object model, String methodName, Class<T> type)
            throws MismatchedAttributesException {
        Producer<?> producer = getProducer(model, methodName);
        if (producer.getType() == type) {
            return (Producer<T>) producer;
        } else {
            throw new MismatchedAttributesException(String.format(
                    "Producer type %s does not match method return type %s.",
                    producer.getType(), type));
        }
    }
}
