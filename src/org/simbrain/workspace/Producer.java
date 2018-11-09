package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * The part of a {@link Coupling} that send values to a {@link Consumable}.
 *
 * @param <V> The type of value to be produced.  Mostly double or double[].
 */
public class Producer<V> extends Attribute {

    /**
     * Contruct a producer.
     *
     * @param baseObject object producing values
     * @param method the "setter" that produces values
     * @param description description for the gui
     * @param idMethod id method for base object, used in description
     * @param customMethod method reference for custom descriptions
     * @param visibility whether this attribute is visible in the gui
     */
    public Producer(Object baseObject, Method method, String description, Method idMethod, Method customMethod,  boolean visibility) {
        super(baseObject, method, description, idMethod, customMethod, visibility);
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