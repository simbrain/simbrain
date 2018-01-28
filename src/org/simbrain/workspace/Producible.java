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
     * The description to use for this producible instead of the method name.
     */
    String description() default "";

    /**
     * The name of a method that returns a custom id for the base object of this producible.
     */
    String idMethod() default "";

    /**
     * Whether this method should be visible in the coupling panels and menus by default. User
     * visibility settings will override this value.
     */
    boolean defaultVisibility() default true;

}
