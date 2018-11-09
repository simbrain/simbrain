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
     * @param description description for the gui
     * @param idMethod id method for base object, used in description
     * @param customDescMethod method reference for custom descriptions
     * @param visibility whether this attribute is visible in the gui
     */
    public Consumer(Object baseObject, Method method, String description, Method idMethod, Method customDescMethod, boolean visibility) {
        super(baseObject, method, description, idMethod, customDescMethod, visibility);
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
