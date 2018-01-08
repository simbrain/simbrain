package org.simbrain.workspace;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public abstract class Attribute {
    protected Object baseObject;
    protected Method method;
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
        this.baseObject = baseObject;
        this.method = method;
        description = method.getName();
    }

    /** Initializing constructor */
    public Attribute(Object baseObject, Method method, String description) {
        this.baseObject = baseObject;
        this.method = method;
        this.description = description;
    }

    public abstract Type getType();

    /**
     * Returns a string used to differentiate attribute types within a WorkspaceComponent.
     * Return value should be unique to the given method within the component.
     */
    public String getId() {
        if (description.isEmpty()) {
            description = baseObject.getClass().getSimpleName();
        }
        return description + ":" + method.getName() ;
    }

    @Override
    public String toString() {
        String typeName;
        if (((Class<?>) getType()).isArray()) {
            typeName = "array[" + ((Class<?>) getType()).getComponentType()
                    + "]";
        } else {
            typeName = getType().toString();
        }

        if (description.isEmpty()) {
            description = baseObject.getClass().getSimpleName();
        }

        if(usesKey()) {
            return  getId() + "<" + typeName + "," + key + ">";
        }

        return getId() + "<" + typeName + ">";
    }

    /**
     * If the method requires a key to produce or consume a value, return true.
     */
    public boolean usesKey() {
        if (this instanceof Producer) {
            return method.getParameterCount() > 0;
        } else {
            return method.getParameterCount() > 1;
        }
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
