package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * {@inheritDoc}.
 *
 * @param <V> The type of value to be consumed.  Mostly double or double[].
 */
public class Producer<V> extends Attribute {

    /**
     * {@inheritDoc}.
     */
    public Producer(Object baseObject, Method method, String description, Method idMethod, Method customMethod) {
        super(baseObject, method, description, idMethod, customMethod);
    }

    /**
     * Return the value of the producer.
     *
     * @return current value
     */
    V getValue() {
        try {
            return (V) method.invoke(baseObject);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            // Should never happen
            throw new RuntimeException(ex);
        }
    }

    @Override
    public Type getType() {
        return method.getReturnType();
    }
}