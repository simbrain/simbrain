package org.simbrain.workspace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

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

    /** The description to use for this consumable instead of the method name. */
    String description() default "";

    /** The name of a method that returns a custom id for the base object of this consumable. */
    String idMethod() default "";

    /**
     * Whether this method should be visible in the coupling panels and menus by default. User
     * visibility settings will override this value.
     */
    boolean defaultVisibility() default true;

}
