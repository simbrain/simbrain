package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * {@inheritDoc}
 *
 * @param <V> The type of value to be consumed.  Mostly double or double[].
 */
public class Consumer<V> extends Attribute {

    /**
     * {@inheritDoc}.
     */
    public Consumer(Object baseObject, Method method, String description, Method idMethod, Method customDescMethod) {
        super(baseObject, method, description, idMethod, customDescMethod);
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
}
