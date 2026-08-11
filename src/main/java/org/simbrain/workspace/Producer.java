package org.simbrain.workspace;

import org.simbrain.workspace.couplings.Coupling;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The part of a {@link Coupling} that send values to a {@link Consumable}.
 *
 */
public class Producer extends Attribute {

    /**
     * See {@link Producible#arrayComponentsMethod()}.
     * So far the only use cases are for producers. If consumer uses cases
     * are found this can be moved to the attribute level.
     */
    private Method arrayComponentsMethod;

    /**
     * Contruct a producer.
     *
     * @param baseObject object producing values
     * @param method the "setter" that produces values
     */
    private Producer(AttributeContainer baseObject, Method method) {
        super(baseObject, method);
    }

    /**
     * Create an Producer with no custom options.
     *
     * @param baseObject The object that contains the getter to be called
     * @param method The getter method
     * @return a Producer with only required fields.
     */
    public static Producer create(AttributeContainer baseObject, Method method) {
        return builder(baseObject, method).build();
    }

    /**
     * Return the value of the producer.
     *
     * @return current value
     */
    public Object getValue() {
        try {
            return method.invoke(baseObject);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            // Should never happen
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Type getType() {
        return method.getReturnType();
    }

    /**
     * The components of what this producer sends, in order, or empty when it declares no
     * {@link Producible#arrayComponentsMethod()}. Names here are raw, so they may repeat; see
     * {@link #getDisplayComponents()} for the form a consumer should show.
     */
    @SuppressWarnings("unchecked")
    public List<AttributeComponent> getComponents() {
        if (arrayComponentsMethod == null) {
            return new ArrayList<>();
        }
        try {
            List<AttributeComponent> components = (List<AttributeComponent>) arrayComponentsMethod.invoke(baseObject);
            // A plain ArrayList, not List.of/stream().toList(): these end up stored on plot models, and XStream
            // refuses to restore the serialization proxy that Java's immutable lists write themselves as.
            return components == null ? new ArrayList<>() : new ArrayList<>(components);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * The components of what this producer sends, ready to display: as {@link #getComponents()} but with
     * repeated names given a positional suffix so a consumer can tell them apart. Consumers naming a whole
     * attribute rather than its components, such as a single time series fed by a scalar coupling, should use
     * {@link #getSimpleDescription()} instead.
     */
    public List<AttributeComponent> getDisplayComponents() {
        return AttributeComponentKt.disambiguateNames(getComponents());
    }

    /**
     * The display names of {@link #getDisplayComponents()}, for consumers that only label what they show and
     * keep no per-component state.
     */
    public List<String> getDisplayNames() {
        return getDisplayComponents().stream()
                .map(AttributeComponent::getName)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get the builder to create and customize a Producer.
     *
     * @param baseObject The object that contains the getter to be called
     * @param method The getter method
     * @return the builder
     */
    public static ProducerBuilder builder(AttributeContainer baseObject, Method method) {
        return new ProducerBuilder(baseObject, method);
    }

    public static class ProducerBuilder extends AttributeBuilder<
            ProducerBuilder,
            Producer
            > {

        /**
         * The product from this builder.
         */
        private Producer product;

        /**
         * Construct a builder.
         *
         * @param baseObject The object that contains the getter to be called
         * @param method The getter method
         */
        ProducerBuilder(AttributeContainer baseObject, Method method) {
            product = new Producer(baseObject, method);
        }

        /**
         * Set an array description method.
         * {@see Producible#arrayComponentsMethod()}.
         *
         * @param arrayComponentsMethod the array description method to set
         * @return the Builder instance (for use in chained initialization)
         */
        public ProducerBuilder arrayComponentsMethod(Method arrayComponentsMethod) {
            product.arrayComponentsMethod = arrayComponentsMethod;
            return this;
        }

        @Override
        protected Producer product() {
            return product;
        }

        @Override
        public Producer build() {
            return product;
        }
    }
}