package org.simbrain.network.core

import org.simbrain.util.createConstructorCallingConverter

/**
 * Use this annotation to mark the constructor that should be used by [createConstructorCallingConverter].
 * If annotating a constructor that is not used except for serialization, that constructor should be private
 * @param names parameter names in the order they appear in the constructor (In Java reflection, names are not
 *              preserved, so this is necessary)
 */
@Target(AnnotationTarget.CONSTRUCTOR)
annotation class XStreamConstructor(val names: Array<String> = [])
