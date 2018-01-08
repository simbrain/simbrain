package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class Consumer<V> extends Attribute {

    public Consumer(Object baseObject, Method method) {
        super(baseObject, method);
    }

    void setValue(V value) {
        try {
            if (key == null) {
                method.invoke(baseObject, value);
            } else {
                method.invoke(baseObject, value, key);
            }
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
