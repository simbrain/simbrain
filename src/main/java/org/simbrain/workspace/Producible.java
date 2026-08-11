package org.simbrain.workspace;

import org.simbrain.workspace.couplings.CouplingManagerKt;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Producible annotation marks a method as a potential producer for a coupling.
 *
 * @author Tim Shea
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Producible {

    /**
     * Use this if you would like the producer to be described by name you provide rather than a method name.
     * For more complex descriptions you can use {@link #customDescriptionMethod()}.
     */
    String description() default "";

    /**
     * The name of a method that returns a custom description for the producer.
     */
    String customDescriptionMethod() default "";

    /**
     * (For attributes of type double[] only).
     * <br>
     * The name of a method returning a {@code List<AttributeComponent>} describing each component of the
     * array produced by this producer: a stable key and a display name apiece. Example: a neuron collection
     * returns its neurons' ids paired with their labels, which a bar chart uses to name its bars and a time
     * series uses to follow a neuron's series when the collection's membership changes.
     *
     * @return the name of the array components method.
     */
    String arrayComponentsMethod() default "";

    /**
     * Whether this method should be visible in the coupling panels and menus by default. User
     * visibility settings will override this value.
     */
    boolean defaultVisibility() default true;

    /**
     * @see Attribute#priority
     */
    int priority() default CouplingManagerKt.LOW_PRIORITY;

    /**
     * The name of a method that can be used to set priority in a way that depends on the state of the base object.
     * See {@link org.simbrain.network.core.Neuron} for examples
     */
    String customPriorityMethod() default "";

}
