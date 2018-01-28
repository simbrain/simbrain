package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class Consumer<V> extends Attribute {

    public Consumer(Object baseObject, Method method, String description) {
        super(baseObject, method, description);
    }

    public Consumer(Object baseObject, Method method, String description, Method idMethod) {
        super(baseObject, method, description, idMethod);
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
