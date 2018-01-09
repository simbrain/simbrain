package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.function.Function;

public abstract class Attribute {
    protected Object baseObject;
    protected Method method;
    protected Method idMethod;
    protected String description = "";

    /**
     * For case where the getter or setter has an additional keyed argument,
     * e.g. getValue(key) or setValue(value, key).
     */
    protected Object key;

    /** Default constructor */
    public Attribute() {}

    /** Initializing constructor */
    public Attribute(Object baseObject, Method method) {
        this(baseObject, method, method.getName());
    }

    /** Initializing constructor */
    public Attribute(Object baseObject, Method method, String description) {
        this.baseObject = baseObject;
        this.method = method;
        this.description = description;
    }

    /** Initializing constructor */
    public Attribute(Object baseObject, Method method, Method idMethod) {
        this(baseObject, method, method.getName(), idMethod);
    }

    /** Initializing constructor */
    public Attribute(Object baseObject, Method method, String description, Method idMethod) {
        this.baseObject = baseObject;
        this.method = method;
        this.description = description;
        this.idMethod = idMethod;
    }

    public abstract Type getType();

    /**
     * Returns a string used to differentiate attributes.
     */
    public String getId() {
        if (idMethod == null) {
            return baseObject.getClass().getSimpleName();
        } else {
            try {
                return (String) idMethod.invoke(baseObject);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                // Should never happen
                throw new AssertionError(ex);
            }
        }
    }

    @Override
    public String toString() {
        String typeName;
        if (((Class<?>) getType()).isArray()) {
            typeName = ((Class<?>) getType()).getComponentType().getSimpleName() + " array";
        } else {
            typeName = ((Class<?>) getType()).getSimpleName();
        }
        return getId() + " " + method.getName() + " (" + typeName + ")";
    }

    /** @return the description */
    public String getDescription() {
        return description;
    }

    /** @param description the description to set */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the object on which the method will be invoked. */
    public Object getBaseObject() {
        return baseObject;
    }

    /** @return the method to invoke to get or set this attribute. */
    public Method getMethod() {
        return method;
    }
}
