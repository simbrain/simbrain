package org.simbrain.workspace;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Stream;

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
        List<Producer<?>> producers = new ArrayList<>();
        for (Method method : model.getClass().getMethods()) {
            if (isProducible(method)) {
                producers.add(createProducer(model, method));
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
            if (isConsumable(method)) {
                consumers.add(createConsumer(model, method));
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
        Stream<Method> stream = Arrays.stream(model.getClass().getMethods());
        Optional<Method> method = stream.filter(m -> m.getName().equals(methodName)).findFirst();
        if (method.isPresent()) {
            return createConsumer(model, method.get());
        } else {
            throw new IllegalArgumentException(String.format(
                    "No consumable method with name %s was found in class %s.",
                    methodName, model.getClass().getSimpleName()));
        }
    }

    /**
     * Get a specific consumer from the model object and throw an exception if it is not of the specified type.
     * @param model The object in which to find the consumable.
     * @param methodName The name of the consumable method.
     * @return The consumer.
     */
    @SuppressWarnings("unchecked")
    public <T> Consumer<T> getConsumer(Object model, String methodName, Class<T> type) throws NoSuchMethodException {
        Consumer<?> consumer = getConsumer(model, methodName);
        if (consumer.getType() == type) {
            return (Consumer<T>) consumer;
        } else {
            throw new NoSuchMethodException(String.format(
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
        try {
            Method method = model.getClass().getMethod(methodName);
            return createProducer(model, method);
        } catch (NoSuchMethodException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Get a specific producer from the model object and throw an exception if it is not of the specified type.
     * @param model The object in which to find the producible.
     * @param methodName The name of the producible method.
     * @return The producer.
     */
    @SuppressWarnings("unchecked")
    public <T> Producer<T> getProducer(Object model, String methodName, Class<T> type) throws NoSuchMethodException {
        Producer<?> producer = getProducer(model, methodName);
        if (producer.getType().equals(type)) {
            return (Producer<T>) producer;
        } else {
            throw new NoSuchMethodException(String.format(
                    "Producer with type %s does not match method return type %s.",
                    producer.getType(), type));
        }
    }

    /** Return whether the specified method is producible. */
    private boolean isProducible(Method method) {
        return method.getAnnotation(Producible.class) != null;
    }

    /** Return whether the specified method is consumable. */
    private boolean isConsumable(Method method) {
        return method.getAnnotation(Consumable.class) != null;
    }

    /** Create a producer from the specified method on the model object. */
    private Producer<?> createProducer(Object model, Method method) {
        Producible annotation = method.getAnnotation(Producible.class);
        if (annotation == null) {
            throw new IllegalArgumentException(String.format("Method %s is not producible.", method.getName()));
        }
        Method idMethod = getIdMethod(model, annotation.idMethod());
        String description = annotation.description().isEmpty() ? method.getName() : annotation.description();
        return new Producer(model, method, description, idMethod);
    }

    /** Create a consumer from the specified method on the model object. */
    private Consumer<?> createConsumer(Object model, Method method) {
        Consumable annotation = method.getAnnotation(Consumable.class);
        if (annotation == null) {
            throw new IllegalArgumentException(String.format("Method %s is not consumable.", method.getName()));
        }
        Method idMethod = getIdMethod(model, annotation.idMethod());
        String description = annotation.description().isEmpty() ? method.getName() : annotation.description();
        return new Consumer(model, method, description, idMethod);
    }

    private Method getIdMethod(Object model, String idMethodName) {
        try {
            return model.getClass().getMethod(idMethodName);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }
}
