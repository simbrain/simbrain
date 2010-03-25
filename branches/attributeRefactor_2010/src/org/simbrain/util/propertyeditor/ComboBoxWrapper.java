package org.simbrain.util.propertyeditor;

/**
 * Wrapper for any object to be represented by a combo box in a
 * ReflectivePropertyEditor.
 *
 * @author Jeff Yoshimi
 */
public interface ComboBoxWrapper {

    /**
     * Return the array of objects that the combo box will select among.
     * toString must be overridden to provide the text for the combo box.
     *
     * @return the array of objects to be put in the combo box.
     */
    public Object[] getObjects();

    /**
     * Return the current object to be initially presented in the combo box.
     *
     * @return the initially selected object for the combo box
     */
    public Object getCurrentObject();

}
