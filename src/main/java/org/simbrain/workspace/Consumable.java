package org.simbrain.workspace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Consumable annotation marks a method as a potential consumer for a coupling.
 * These will generally be setters
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Consumable {

    /**
     * Use this if you would like the producer to be described by name you provide rather than a method name.
     * For more complex descriptions you can use {@link #customDescriptionMethod()}.
     */
    String description() default "";

    /**
     * The name of a method that returns a custom description for the attribute.
     */
    String customDescriptionMethod() default "";

    /**
     * Whether this method should be visible in the coupling panels and menus by default. User
     * visibility settings will override this value.
     */
    boolean defaultVisibility() default true;

    int priority() default 100;
}
