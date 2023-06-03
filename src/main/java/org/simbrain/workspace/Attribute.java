package org.simbrain.workspace;

import org.jetbrains.annotations.NotNull;
import org.simbrain.util.Utils;
import org.simbrain.workspace.couplings.Coupling;

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
    protected AttributeContainer baseObject;

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
     * direct setting of the description is overwritten.
     */
    protected String description = "";

    /**
     * When creating coupling automatically, the highest priority items in an attribute container
     * are coupled. For example, {@code neuronGroup couple projectionPlot}, selects the
     * highest priority producer in neuronGroup and couples that to the hightest priority
     * consumer in projectionPlot.
     *
     * Use priority = 1 for highest priority.
     */
    protected int priority = 100;

    /**
     * Initializing constructor.
     *
     * @param baseObject        The object that contains the getter or setter to
     *                          be called.
     * @param method            The getter method (for produces) or setter
     *                          method (for consumers).
     */
    public Attribute(AttributeContainer baseObject, Method method) {
        this.baseObject = baseObject;
        this.method = method;
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
        if (baseObject.getId() == null) {
            return baseObject.getClass().getSimpleName();
        } else {
            return baseObject.getId();
        }
    }

    /**
     * Used in {@link org.simbrain.workspace.gui.couplingmanager.AttributePanel}'s
     * custom cell renderer.
     */
    @Override
    public String toString() {
        return getSimpleDescription();
    }

    /**
     * Returns a human-readable description of an attribute. Example: Neuron1:activation
     *
     * @return the simplified description
     */
    public String getSimpleDescription() {

        // Description is based on a custom method
        String customDesc = getCustomDescription();
        if (customDesc != null) {
            return customDesc;
        }

        // Description is based on id + a provided description
        if (!description.isEmpty()) {
            return getId() + ":" + description;
        }

        // Description is based on id and a simplified version of the method name
        // simpleMethodName gets rid of get and set, add spaces, and capitalize first letter
        String simpleMethodName = method.getName();
        if (method.getName().startsWith("get")) {
            simpleMethodName = simpleMethodName.replaceFirst("get", "");
        } else if (method.getName().startsWith("set")) {
            simpleMethodName = simpleMethodName.replaceFirst("set", "");
        }
        simpleMethodName = Utils.splitCamelCase(simpleMethodName);
        simpleMethodName = Utils.upperCaseFirstLetter(simpleMethodName.toLowerCase());
        return getId() + ":" + simpleMethodName;
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

    public AttributeContainer getBaseObject() {
        return baseObject;
    }

    public Method getMethod() {
        return method;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Attribute attribute = (Attribute) o;

        if (!getBaseObject().equals(attribute.getBaseObject())) return false;
        return getMethod().equals(attribute.getMethod());
    }

    @Override
    public int hashCode() {
        int result = getBaseObject().hashCode();
        result = 31 * result + getMethod().hashCode();
        return result;
    }

    /**
     * Base builder for {@link Consumer} and {@link Producer} instances.
     * Create custom instances of the specific implementation of {@link Attribute} by using
     * the setter methods on this class.
     * After zero or more of these, use the build() method to create a specific {@link Attribute} instance.
     * If no special set-up is needed, just use, for example, in the case of Consumer,
     * {@code Consumer.builder(container, method).build()} or the short-cut equivalent
     * {@code Consumer.create(container, method)}.
     *
     * @param <B> The type of the builder to return when building
     * @param <T> The type of the final product to return when finish building.
     */
    public abstract static class AttributeBuilder<
            B extends AttributeBuilder,
            T extends Attribute
            > {

        /**
         * Uniform access to the product being build. Only used in this abstract class
         * where the product cannot be instantiate yet.
         *
         * @return the product being build
         */
        protected abstract T product();

        /**
         * Set a simple custom description string.
         *
         * @param description a simple custom description
         * @return the Builder instance (for use in chained initialization)
         */
        public B description(String description) {
            product().description = description;
            return (B) this;
        }

        /**
         * Set method used to get an id for the attribute.
         *
         * @param idMethod the method used to get an id for the attribute
         * @return the Builder instance (for use in chained initialization)
         */
        public B idMethod(Method idMethod) {
            product().idMethod = idMethod;
            return (B) this;
        }

        /**
         * Set method that supplies a custom description method.
         *
         * @param customDescription the custom description method
         * @return the Builder instance (for use in chained initialization)
         */
        public B customDescription(Method customDescription) {
            product().customDescriptionMethod = customDescription;
            return (B) this;
        }

        /**
         * Builds a instance of specific Attribute of given states.
         * @return the final product
         */
        public abstract T build();

        @NotNull
        public B priority(int priority) {
            product().priority = priority;
            return (B) this;
        }
    }
}
