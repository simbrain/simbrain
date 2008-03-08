package org.simbrain.workspace;

import java.lang.reflect.Type;

/**
 * Defines the base API for consuming and producing attributes.
 * 
 * @author Matt Watson
 */
public interface Attribute {

    /**
     * Return the name of this consuming attribute.
     *
     * @return the name of this consuming attribute.
     */
    String getAttributeDescription();

    /**
     * returns the type of the generic parameter.
     * 
     * @return the type of the generic parameter.
     */
    Type getType();
    
    AttributeHolder getParent();
    
    /**
     * Sets the id for this Attribute.  This is used for
     * serialization.  Most Attributes can extend AbstractAttribute
     * and inherit the proper behavior.  Any class that implements
     * this interface must store the id and return it from getId.
     * 
     * @param id The id for this attribute.
     */
//    void setId(int id);
    
    /**
     * Returns the id for this Attribute.  This is used for
     * serialization.  Most Attributes can extend AbstractAttribute
     * and inherit the proper behavior.
     * 
     * @return The id for this attribute.
     */
//    int getId();
}
