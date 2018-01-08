package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class Producer<V> extends Attribute {

    public Producer(Object baseObject, Method method) {
        this.baseObject = baseObject;
        this.method = method;
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