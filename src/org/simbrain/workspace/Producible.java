package org.simbrain.workspace;

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

    // Override if this is an array producer and if you want to make it possible
    // to have separate ids for each member of the list, e.g. when coupling to a barchart
    //List<String> listDescriptorsMethod() default "";

    /**
     * Whether this method should be visible in the coupling panels and menus by default. User
     * visibility settings will override this value.
     */
    boolean defaultVisibility() default true;

}
