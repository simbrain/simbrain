package org.simbrain.util.propertyeditor

/**
 * Indicates that an object can be copied.
 *
 * Used by the [ObjectTypeEditor], which creates and edits a prototype object and then makes copies of
 * that object. For example, if editing a bunch of neurons that have different update rules, setting one of them to a
 * certain udpate rule  will require that they all now have that rule. A single prototype rule is copied and written
 * to all the neurons.
 *
 * @author Jeff Yoshimi
 */
interface CopyableObject : EditableObject {
    /**
     * Return a deep copy of this object.
     */
    fun copy(): CopyableObject

    /**
     * Returns a list of type options to be used in combo boxes in a property editor when changing the type of this
     * object.
     */
    fun getTypeList(): List<Class<out CopyableObject>>? = null
}