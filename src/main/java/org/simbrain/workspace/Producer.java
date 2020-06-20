package org.simbrain.workspace;

import org.simbrain.workspace.couplings.Coupling;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * The part of a {@link Coupling} that send values to a {@link Consumable}.
 *
 */
public class Producer extends Attribute {

    /**
     * See {@link Producible#arrayDescriptionMethod()}.
     * So far the only use cases are for producers. If consumer uses cases
     * are found this can be moved to the attribute level.
     */
    private Method arrayDescriptionMethod;

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
     * See {@link Producible#arrayDescriptionMethod()}.
     * @return an array of string descriptions, one for each component of the
     *  value this producer returns.
     */
    public String[] getLabelArray() {
        if (arrayDescriptionMethod == null) {
            return null;
        } else {
            try {
                return (String[]) arrayDescriptionMethod.invoke(baseObject);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new AssertionError(ex);
            }
        }
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
         * {@see Producible#arrayDescriptionMethod()}.
         *
         * @param arrayDescriptionMethod the array description method to set
         * @return the Builder instance (for use in chained initialization)
         */
        public ProducerBuilder arrayDescriptionMethod(Method arrayDescriptionMethod) {
            product.arrayDescriptionMethod = arrayDescriptionMethod;
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