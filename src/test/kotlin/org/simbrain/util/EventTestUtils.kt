/** Test support for checking that a view's event subscriptions ended with it. */
package org.simbrain.util

/** Handlers subscribed to every event of this events object. */
fun FlowEvents.subscriberTotal(): Int {
    var total = 0
    var type: Class<*>? = javaClass
    while (type != null && type != Any::class.java) {
        type.declaredFields.forEach { field ->
            field.isAccessible = true
            total += when (val event = field.get(this)) {
                is FlowEvents.FlowEvent<*> -> event.subscriberCount
                is FlowEvents.AwaitableEvent<*> -> event.subscriberCount
                is FlowEvents.NoArgAwaitableEvent -> event.subscriberCount
                else -> 0
            }
        }
        type = type.superclass
    }
    return total
}
