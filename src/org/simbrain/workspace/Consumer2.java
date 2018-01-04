package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;

public class Consumer2<V> extends Attribute2 {

    public Consumer2(WorkspaceComponent wc, Object baseObject, Method method) {
        this.parentComponent = wc;
        this.baseObject = baseObject;
        this.method = method;
    }

    void setValue(V value) {
        try {
            if (key == null) {
                method.invoke(baseObject, new Object[] { value });
            } else {
                method.invoke(baseObject,
                        new Object[] { value, key });
            }
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Type getType() {
        return method.getGenericParameterTypes()[0];
    }

}
