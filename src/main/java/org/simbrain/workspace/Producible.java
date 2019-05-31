package org.simbrain.workspace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collections;
import java.util.List;

/**
 * Producible annotation marks a method as a potential producer for a coupling.
 *
 * @author Tim Shea
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Producible {

    /**
     * Use this if you would like the producer to be described by a simple name.
     * For more complex descriptions you can use {@link #customDescriptionMethod()}.
     */
    String description() default "";

    /**
     * The name of a method that returns a custom id for the base object of the producer.
     */
    String idMethod() default "";

    /**
     * The name of a method that returns a custom description for the producer.
     */
    String customDescriptionMethod() default "";

    /**
     * (For attributes of type double[] only).
     * <br>
     * The name of a method that returns an array of Strings which describe
     * each component of the double array produced by this producer.  Example:
     * a neuron array could return an array of neuron labels ["Neuron 1", "Neuron 2",..]
     * that could then be used as bar-chart labels.
     *
     * @return the name of the array description method.
     */
    String arrayDescriptionMethod() default "";

    /**
     * Whether this method should be visible in the coupling panels and menus by default. User
     * visibility settings will override this value.
     */
    boolean defaultVisibility() default true;

}
