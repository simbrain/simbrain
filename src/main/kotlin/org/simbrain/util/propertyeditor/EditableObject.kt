package org.simbrain.util.propertyeditor

/**
 * Indicates that objects that can be edited in an [AnnotatedPropertyEditor]. Returns a name that is used in the
 * GUI and has a commit method that can be overridden if special actions are needed when committing.
 *
 * @author Jeff Yoshimi
 */
interface EditableObject {

    /**
     * Name of this editable object (used in editor title bar and other locations)
     * Use [CustomTypeName] if class based access is required.
     */
    val name: String
        get() = "No-name (be sure getName() is overridden)"

    /**
     * A method to be invoked at the end of [AnnotatedPropertyEditor.commitChanges]
     */
    fun onCommit() {}
}