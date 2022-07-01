@file:JvmName("XStreamUtils")

package org.simbrain.util

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.xml.DomDriver
import com.thoughtworks.xstream.mapper.Mapper
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.javaType

/**
 * Returns an XStream instance with default Simbrain settings, including backwards compatibility with earlier xml,
 * and turning off security warning, and formatting xml as utf-8.
 *
 * @return the properly initialized XStream object
 */
fun getSimbrainXStream(): XStream {
    return XStream(DomDriver("UTF-8")).apply {
        ignoreUnknownElements()
        allowTypesByWildcard(
            arrayOf(
                "org.simbrain.**", "java.awt.**", "org.jfree.**", "javax.swing.event.**", "java.beans.**",
                "smile.math.**", "java.util.concurrent.**"
            )
        )
    }
}

/**
 * XStream support for classes that require a primary constructor call.
 *
 * Limitation: Field names must match primary constructor param names.
 * Untested on Java classes.
 */
fun <T : Any> createConstructorCallingConverter(
    clazz: Class<T>,
    mapper: Mapper,
    reflectionProvider: ReflectionProvider
): ReflectionConverter {
    return object : ReflectionConverter(mapper, reflectionProvider, clazz) {

        @OptIn(ExperimentalStdlibApi::class)
        override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {

            // cls can be a subclass of clazz
            @Suppress("UNCHECKED_CAST") // checked by canConvert
            val cls: KClass<out T> = (try {
                Class.forName(reader.nodeName).kotlin
            } catch (e: ClassNotFoundException) {
                Class.forName(reader.getAttribute("class")).kotlin
            }) as KClass<out T>

            // Java Fields
            val fieldMap = sequence {
                var currentClass = cls.java
                currentClass.declaredFields.forEach { yield(it) }
                while (currentClass != clazz) {
                    @Suppress("UNCHECKED_CAST") // checked by canConvert
                    currentClass = currentClass.superclass as Class<out T>
                    currentClass.declaredFields.forEach { yield(it) }
                }
            }.associateBy { it.name }

            val fieldValueMap = HashMap<String, Any?>()

            // Kotlin Properties, mainly used for delegations
            val propertyMap =
                cls.declaredMemberProperties.filterIsInstance<KMutableProperty<*>>().associateBy { it.name }
            val propertyValueMap = HashMap<String, Any?>()

            fun read() {
                val nodeName = reader.nodeName
                fieldMap[nodeName]?.let {
                    val fieldValue = context.convertAnother(reader.value, it.type)
                    fieldValueMap[nodeName] = fieldValue
                    return@read
                }
                propertyMap[nodeName]?.let {
                    propertyValueMap[nodeName] =
                        context.convertAnother(reader.value, it.returnType.javaType as Class<*>)
                }
            }

            fun stepIn() {
                if (!reader.nodeName.startsWith("\$\$delegate")) {
                    read()
                }
                while (reader.hasMoreChildren()) {
                    reader.moveDown()
                    stepIn()
                    reader.moveUp()
                }
            }
            stepIn()

            val unmarshallingObject = cls.primaryConstructor?.let { constructor ->
                val paramNames = constructor.parameters
                val paramValues = paramNames.associateWith { param -> param.name?.let { fieldValueMap[it] } }
                    .filterValues { it != null }
                constructor.callBy(paramValues)
            } ?: throw NoSuchMethodException("Could not find primary constructor for ${cls.simpleName}")

            fieldValueMap.forEach { (name, value) ->
                fieldMap[name]?.let {
                    val oldAccessible = it.canAccess(unmarshallingObject)
                    it.isAccessible = true
                    it.set(unmarshallingObject, value)
                    it.isAccessible = oldAccessible
                }
            }

            propertyValueMap.forEach { (name, value) ->
                propertyMap[name]?.setter?.call(unmarshallingObject, value)
            }

            return unmarshallingObject
        }

        override fun canConvert(type: Class<*>?): Boolean {
            return super.canConvert(type) || type?.kotlin?.isSubclassOf(clazz.kotlin) == true
        }
    }
}