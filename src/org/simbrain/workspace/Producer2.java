package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

public class Producer2<V> extends Attribute2 {

    public Producer2(WorkspaceComponent wc, Object baseObject, Method method) {
        this.parentComponent = wc;
        this.baseObject = baseObject;
        this.method = method;
    }

    V getValue() {
        try {
            if (key == null) {
                return (V) method.invoke(baseObject);
            } else {
                // TODO: Not yet tested.
                return (V) method.invoke(baseObject, new Object[] { key });
            }
        } catch (IllegalAccessException | IllegalArgumentException
                | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public Type getType() {
        return method.getReturnType();
    }
}