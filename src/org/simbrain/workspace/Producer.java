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
     * @param description description for the gui
     * @param idMethod id method for base object, used in description
     * @param customMethod method reference for custom descriptions
     * @param visibility whether this attribute is visible in the gui
     */
    public Producer(Object baseObject, Method method, String description,
                    Method idMethod, Method customMethod, Method arrayDescriptionMethod,  boolean visibility) {
        super(baseObject, method, description, idMethod, customMethod, visibility);
        this.arrayDescriptionMethod = arrayDescriptionMethod;
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

}