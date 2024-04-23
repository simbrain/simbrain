package org.simbrain.workspace.serialization

import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import kotlin.reflect.full.companionObject
import kotlin.reflect.full.companionObjectInstance
import kotlin.reflect.full.functions

class WorkspaceComponentDeserializer {


    /**
     * A map used to retrieve workspace components given their uris.
     */
    private val componentKeys: MutableMap<String, WorkspaceComponent> = HashMap()

    /**
     * Returns the workspace component associated with the given uri.
     *
     * @param uri The uri for the component to retrieve.
     * @return The component for the uri.
     */
    fun getComponent(uri: String): WorkspaceComponent? {
        return componentKeys[uri]
    }

    /**
     * Deserializes a workspace component using the information from the
     * provided component and input stream.
     *
     * @param archivedComponent The component entry from the archive contents.
     * @param input             The input stream to read data from.
     * @return The deserialized WorkspaceComponent.
     */
    @Throws(ReflectiveOperationException::class)
    fun deserializeWorkspaceComponent(
        archivedComponent: ArchivedWorkspaceComponent,
        input: InputStream?
    ): WorkspaceComponent {
        val componentClass = Class.forName(archivedComponent.className)
        val wc = deserializeWorkspaceComponent(componentClass, archivedComponent.name, input, null)
        componentKeys[archivedComponent.getUri()] = wc
        wc.setChangedSinceLastSave(false)
        return wc
    }

    companion object {
        /**
         * Deserialized a component for the given class, input and input format.
         *
         * @param componentClass the class of the component
         * @param name           the name of the component
         * @param input          the input stream
         * @param format         the format of the data
         * @return a new component
         */
        @Throws(ReflectiveOperationException::class)
        fun deserializeWorkspaceComponent(
            componentClass: Class<*>,
            name: String?,
            input: InputStream?,
            format: String?
        ): WorkspaceComponent {

            val openFunction = componentClass.kotlin.companionObject?.functions?.find { it.name == "open" }

            if (openFunction != null) {
                val obj = openFunction.call(componentClass.kotlin.companionObjectInstance, input, name, format)
                if (obj is WorkspaceComponent) {
                    obj.setChangedSinceLastSave(false)
                    return obj
                } else {
                    throw ReflectiveOperationException("Incompatible open method return type in class $componentClass")
                }
            } else {
                val method = componentClass.getMethod(
                    "open",
                    InputStream::class.java,
                    String::class.java,
                    String::class.java
                )
                val obj = method.invoke(null, input, name, format)
                if (obj is WorkspaceComponent) {
                    obj.setChangedSinceLastSave(false)
                    return obj
                } else {
                    throw ReflectiveOperationException("Incompatible open method return type in class $componentClass")
                }
            }
        }
    }


}