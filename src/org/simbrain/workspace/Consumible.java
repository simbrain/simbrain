package org.simbrain.workspace;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

/**
 * Consumible annotation marks a method as a potential consumer for a coupling.
 * These will generally be setters
 *
 * @author Tim Shea
 * @author Jeff Yoshimi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Consumible {

    //TODO: Rename?
    /**
     * The name of a method that returns a customized description for this
     * producible. 
     */
    String customDescriptionMethod() default "";

    //TODO
    boolean visible() default true;
    
    // TODO: Better name?  Give example.  
    String indexListMethod() default "";

}
