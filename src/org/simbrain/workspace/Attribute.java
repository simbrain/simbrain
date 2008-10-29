package org.simbrain.workspace;

import java.lang.reflect.Type;

/**
 * Defines the base API for consuming and producing attributes.
 * 
 * @author Matt Watson
 */
public interface Attribute {

    /**
     * Returns the name of this consuming attribute.
     * This is used for serialization and in interface
     * elements which display attributes.
     *
     * NOTE 1: This description must be unique relative to
     * other Attributes in an AttributeHolder.
     * 
     * NOTE 2: The description of the attribute must be exactly
     * the same after deserialization as it was before serialization,
     * even if the description of the attribute was not serialized.
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
    
    /**
     * Returns a reference the AttributeHolder which holds this Attribute.

     * @return parent AttributeHolder
     */
    AttributeHolder getParent();

}
