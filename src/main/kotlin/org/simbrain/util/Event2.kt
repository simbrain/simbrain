package org.simbrain.util

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import javax.swing.SwingUtilities

/**
 * Event objects corresponding to no-arg, adding, removing, and changing objects. Each object has a set of functions
 * on it that allow for firing them and waiting (via blocking in java or suspending in kotlin), and firing and
 * "forgetting". They are also associated with event handling "on" functions, which can be associated with a
 * dispatcher within which that block is executed.
 *
 * Provides for a convenient api. Just implement the events you need and all the event firing and handling functions
 * are provided.
 *
 * For examples see [TrainerEvents2]
 */
open class Events2 {

    /**
     * Associates events to their listeners
     */
    private val eventMapping = HashMap<EventObject, LinkedList<Pair<CoroutineDispatcher, (new: Any?, old: Any?) -> Unit>>>()

    abstract inner class EventObject {

        protected fun onHelper(dispatcher: CoroutineDispatcher, run: (new: Any?, old: Any?) -> Unit) {
            eventMapping.getOrPut(this@EventObject) { LinkedList() }.add(dispatcher to run)
        }

        protected suspend fun fireHelper(run: ((new: Any?, old: Any?) -> Unit) -> Unit) = coroutineScope {
            async {
                eventMapping[this@EventObject]
                    ?.groupBy { (dispatcher) -> dispatcher }
                    ?.flatMap { (dispatcher, group) ->
                        withContext(dispatcher) {
                            group.map { (_, handler) -> async { run(handler) } }
                        }
                    }
                    ?.awaitAll()
            }
        }

        fun fireAndForgetHelper(run: ((new: Any?, old: Any?) -> Unit) -> Unit) {
            SwingUtilities.invokeLater {
                eventMapping[this@EventObject]?.forEach { (_, handler) -> run(handler) }
            }
        }
    }

    /**
     * No argument events, e.g. neuronChanged.fire() and neuronChanged.on { .. do stuff...}.
     */
    inner class NoArgEvent: EventObject() {

        /**
         * Kotlin "on"
         */
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: () -> Unit) = onHelper(dispatcher) {
                _, _ -> handler()
        }

        /**
         * Java "on"
         */
        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: java.lang.Runnable) = onHelper(dispatcher) {
            _, _ -> handler.run()
        }

        /**
         * Kotlin "fire". By itself it's like "fireAndForget".
         */
        suspend fun fire() = fireHelper { handler -> handler(null, null) }

        /**
         * Like java fireAndBlock() but suspends rather than blocking, so that the GUI remains responsive.
         */
        suspend fun fireAndSuspend() = fire().await()

        /**
         * Java fire and block. Fire event and wait for it to terminate before continuing.
         */
        fun fireAndBlock() {
            runBlocking {
                fire().await()
            }
        }

        /**
         * Java fire and forget.
         */
        fun fireAndForget() = fireAndForgetHelper { handler -> handler(null, null) }

    }

    /**
     * Add events, e.g. neuronAdded.fire(newNeuron), neuronAdded.on{ newNeuron -> ...}.
     * Functinos are the same as in the no-arg case.
     */
    inner class AddedEvent<T>: EventObject() {
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: (new: T) -> Unit) = onHelper(dispatcher) {
            new, _ -> handler(new as T)
        }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: Consumer<T>) = onHelper(dispatcher) {
                new, _ -> handler.accept(new as T)
        }

        suspend fun fire(new: T) = fireHelper { handler -> handler(new, null) }

        suspend fun fireAndSuspend(new: T) = fire(new).await()

        fun fireAndBlock(new: T) {
            runBlocking {
                fire(new).await()
            }
        }

        fun fireAndForget(new: T) = fireAndForgetHelper { handler -> handler(new, null) }

    }

    /**
     * Removed events, e.g. neuronRemoved.fire(oldNeuron), neuronRemoved.on{ oldNeuron -> ...}. If no handling needed
     * just use no-arg.
     * Functions are the same as in the no-arg case.
     */
    inner class RemovedEvent<T>: EventObject() {
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: (old: T) -> Unit) = onHelper(dispatcher) {
                _, old -> handler(old as T)
        }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: Consumer<T>) = onHelper(dispatcher) {
                _, old -> handler.accept(old as T)
        }

        suspend fun fire(old: T) = fireHelper { handler -> handler(null, old) }

        suspend fun fireAndSuspend(old: T) = fire(old).await()

        fun fireAndBlock(old: T) {
            runBlocking {
                fire(old).await()
            }
        }

        fun fireAndForget(old: T) = fireAndForgetHelper { handler -> handler(null, old) }

    }

    /**
     * Changed events, e.g. updateRuleChanged.fire(oldRule, newRule), updateRuleChanged.on{ or, nr -> ...}.
     * Functions are the same as in the no-arg case.
     */
    inner class ChangedEvent<T>: EventObject() {
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: (new: T, old: T) -> Unit) = onHelper(dispatcher) {
                new, old -> handler(new as T, old as T)
        }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: BiConsumer<T, T>) = onHelper(dispatcher) {
                new, old -> handler.accept(new as T, old as T)
        }

        suspend fun fire(new: T, old: T) = fireHelper { handler -> handler(new, old) }

        suspend fun fireAndSuspend(new: T, old: T) = fire(new, old).await()

        fun fireAndBlock(new: T, old: T) {
            runBlocking {
                fire(new, old).await()
            }
        }

        fun fireAndForget(new: T, old: T) = fireAndForgetHelper { handler -> handler(new, old) }

    }

}
