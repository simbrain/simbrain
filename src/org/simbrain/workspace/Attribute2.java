package org.simbrain.workspace;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

public abstract class Attribute2 {

    public WorkspaceComponent parentComponent;
    protected Object baseObject;
    protected Method method;
    private String description = "";

    /**
     * For case where the getter or setter has an additional keyed argument,
     * e.g. getValue(key) or setValue(value, key).
     */
    protected Object key;

    public abstract Type getType();

    // This must uniquely identify the attribute within the component.
    //  up to the person annotating to obey this contract.
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

    // TODO: Not tested yet
    /**
     * If the method requires a key to produce or consume a value, return true.
     */
    public boolean usesKey() {
        if (this instanceof Producer2) {
            return method.getParameterCount() > 0;
        } else {
            return method.getParameterCount() > 1;
        }
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the baseObject
     */
    public Object getBaseObject() {
        return baseObject;
    }

    /**
     * @return the method
     */
    public Method getMethod() {
        return method;
    }
}
