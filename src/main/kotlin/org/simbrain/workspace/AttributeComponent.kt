/**
 * Identity and naming for the individual components of an array-valued attribute, and the rules consumers use
 * to display them. Producers supply these through [Producible.arrayComponentsMethod].
 */
package org.simbrain.workspace

/**
 * One component of an array-valued attribute, e.g. one neuron of a neuron collection's activation array.
 *
 * [key] identifies the component across updates, so a consumer holding per-component state, such as a time
 * series accumulating history, can follow it when components are added, removed, or reordered. It only has to
 * be stable and unique within one producer; a neuron's id serves, and a plain index is a reasonable choice for
 * a fixed-size array whose components have no identity of their own.
 *
 * [name] is what a user sees, e.g. a neuron's label. Unlike [key] it may be empty or repeated, which is why
 * the two are separate.
 */
data class AttributeComponent(val key: String, val name: String)

/**
 * Give components whose names repeat a positional suffix, so a consumer can tell two same-named components
 * apart, e.g. two neurons both labelled "Alpha" become "Alpha[0]" and "Alpha[1]". Names that occur once and
 * all keys are left alone.
 */
fun List<AttributeComponent>.disambiguateNames(): List<AttributeComponent> {
    val occurrences = groupingBy { it.name }.eachCount()
    val timesSeen = HashMap<String, Int>()
    return map { component ->
        val index = timesSeen.merge(component.name, 1, Int::plus)!! - 1
        if ((occurrences[component.name] ?: 0) > 1) {
            component.copy(name = "${component.name}[$index]")
        } else {
            component
        }
    }
}
