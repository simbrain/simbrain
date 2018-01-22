package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;

public class Producer<V> extends Attribute {

    public Producer(Object baseObject, Method method, String description) {
        super(baseObject, method, description);
    }

    public Producer(Object baseObject, Method method, String description, Method idMethod) {
        super(baseObject, method, description, idMethod);
    }

    V getValue() {
        try {
            if (key == null) {
                return (V) method.invoke(baseObject);
            } else {
                return (V) method.invoke(baseObject, new Object[] { key });
            }
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