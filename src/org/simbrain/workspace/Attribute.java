package org.simbrain.workspace;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Superclass of {@link Consumer} and {@link Producer}, which together comprise
 * {@link Coupling} objects.  Attributes are basically objects with a getter or
 * setter methods that can be invoked to produce a value. Utility methods for
 * determining how attributes are displayed in tne GUI are also provided.
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
public abstract class Attribute {

    /**
     * The object that contains the getter or setter to be called.
     */
    protected Object baseObject;

    /**
     * The getter method (for produces) or setter method (for consumers).
     */
    protected Method method;

    /**
     * Optional method used to get an id for the attribute, which can be used in
     * serialization and in GUI presentation.
     */
    protected Method idMethod;

    /**
     * Optional method that supplies a custom description method. Used when each
     * attribute object of a type should be described differently than the
     * default described in {@link #description}.
     */
    protected Method customDescriptionMethod;

    /**
     * The String description of the attribute. By default has this form:
     * ID:methodName(Type).  E.g. Neuron1:getActivation(Double).
     * <p>
     * This can be customized in a few ways: directly using {@link
     * #setDescription(String)}, or using  the annotation element {@link
     * Consumable#description()} or {@link Producible#description()}, or
     * indirectly using {@link #customDescriptionMethod}. In the latter case any
     * direct setting of the descriptio is overwritten.
     */
    protected String description = "";

    /**
     * Initializing constructor.
     *
     * @param baseObject        The object that contains the getter or setter to
     *                          be called.
     * @param method            The getter method (for produces) or setter
     *                          method (for consumers).
     * @param description       optional simple custom description (just a
     *                          string)
     * @param idMethod          method which returns an id for the object.
     * @param customDescription method which returns a custom description
     *                          method
     */
    public Attribute(Object baseObject, Method method, String description, Method idMethod, Method customDescription) {
        this.baseObject = baseObject;
        this.method = method;
        this.description = description;
        this.customDescriptionMethod = customDescription;
        this.idMethod = idMethod;
    }

    /**
     * Returns the type of the attribute. For a producer the return type of a
     * getter; for a consumer the argument type of a setter.
     *
     * @return the type for this consumer or producer
     */
    public abstract Type getType();

    /**
     * Returns a string id, e.g. "Neuron15" or "Sensor5".
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

    /**
     * Used in {@link org.simbrain.workspace.gui.couplingmanager.AttributePanel}'s
     * custom cell renderer.
     */
    @Override
    public String toString() {
        return getDescription();
    }

    /**
     * Return the nicely formatted type name of this attribute.
     */
    public String getTypeName() {
        if (((Class<?>) getType()).isArray()) {
            return ((Class<?>) getType()).getComponentType().getSimpleName() + " array";
        } else {
            return ((Class<?>) getType()).getSimpleName();
        }
    }

    /**
     * Return the description associated with this attribute. For use in the
     * GUI. Returns
     *
     * <ol>
     * <li>The results of the {@link #customDescriptionMethod} if set</li>
     * <li>The {@link #description} if it's not empty.
     * <li>A default format ID:methodName(Type).  E.g. Neuron25:getActivation(Double).
     * </ol>
     *
     * @return the description
     */
    public String getDescription() {
        String customDesc = getCustomDescription();
        if (customDesc != null) {
            return customDesc;
        }
        if (!description.isEmpty()) {
            return description;
        }

        // The default description format
        return getId() + ":" + method.getName() + " (" + getTypeName() + ")";
    }

    private String getCustomDescription() {
        if (customDescriptionMethod == null) {
            return null;
        } else {
            try {
                return (String) customDescriptionMethod.invoke(baseObject);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                // Should never happen
                throw new AssertionError(ex);
            }
        }
    }

    /**
     * Used to customize a simple description.
     *
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the object on which the method will be invoked.
     */
    public Object getBaseObject() {
        return baseObject;
    }

    /**
     * @return the method to invoke to get or set this attribute.
     */
    public Method getMethod() {
        return method;
    }
}
