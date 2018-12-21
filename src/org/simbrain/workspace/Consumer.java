package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * The part of a {@link Coupling} that receives values from a {@link Producer}.
 *
 * @param <V> The type of value to be consumed.  Mostly double or double[].
 */
public class Consumer<V> extends Attribute {

    /**
     * Contruct a consumer.
     *
     * @param baseObject object consuming values
     * @param method the "getter" that consumes values
     */
    private Consumer(AttributeContainer baseObject, Method method) {
        super(baseObject, method);
    }

    /**
     * Create an consumer with no custom options.
     *
     * @param baseObject The object that contains the setter to be called
     * @param method The setter method
     * @return a consumer with only required fields.
     */
    public static Consumer create(AttributeContainer baseObject, Method method) {
        return builder(baseObject, method).build();
    }

    /**
     * Update the consumer by setting its value.
     *
     * @param value the value to set
     */
    void setValue(V value) {
        try {
            method.invoke(baseObject, value);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            // Should never happen
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Type getType() {
        return method.getGenericParameterTypes()[0];
    }

    /**
     * Get the builder to create and customize a Consumer.
     *
     * @param baseObject The object that contains the setter to be called
     * @param method The setter method
     * @return the builder
     */
    public static ConsumerBuilder builder(AttributeContainer baseObject, Method method) {
        return new ConsumerBuilder(baseObject, method);
    }

    public static class ConsumerBuilder extends AttributeBuilder<
            ConsumerBuilder,
            Consumer
            > {

        /**
         * The product from this builder.
         */
        private Consumer product;

        /**
         * Construct a builder.
         *
         * @param baseObject The object that contains the setter to be called
         * @param method The setter method
         */
        ConsumerBuilder(AttributeContainer baseObject, Method method) {
            product = new Consumer(baseObject, method);
        }

        @Override
        protected Consumer product() {
            return product;
        }

        @Override
        public Consumer build() {
            return product;
        }
    }
}
