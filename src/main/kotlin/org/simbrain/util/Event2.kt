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
open class Events2: CoroutineScope {

    private val job = SupervisorJob()

    override val coroutineContext = Dispatchers.Swing + job

    /**
     * Associates events to their listeners
     */
    private val eventMapping = HashMap<EventObject, LinkedList<Pair<CoroutineDispatcher, suspend (new: Any?, old: Any?) -> Unit>>>()

    abstract inner class EventObject {

        abstract val debounce: Int

        private var debounceEndTime = System.currentTimeMillis()

        protected fun onSuspendHelper(dispatcher: CoroutineDispatcher, run: suspend (new: Any?, old: Any?) -> Unit) {
            eventMapping.getOrPut(this@EventObject) { LinkedList() }.add(dispatcher to run)
        }

        protected fun onHelper(dispatcher: CoroutineDispatcher, run: (new: Any?, old: Any?) -> Unit) {
            eventMapping.getOrPut(this@EventObject) { LinkedList() }.add(dispatcher to { new, old -> async { run(new, old) } })
        }

        private suspend inline fun runAllHandlers(crossinline run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit) = eventMapping[this@EventObject]
            ?.groupBy { (dispatcher) -> dispatcher }
            ?.flatMap { (dispatcher, group) ->
                withContext(dispatcher) {
                    group.map { (_, handler) -> async { run(handler) } }
                }
            }
            ?.awaitAll()

        protected suspend fun fireAndSuspendHelper(run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit) {
            val now = System.currentTimeMillis()
            async {
                if (now >= debounceEndTime) {
                    debounceEndTime = now + debounce
                    runAllHandlers(run)
                }
            }.await()
        }

        protected suspend fun fireAndForgetHelper(run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit) {
            val now = System.currentTimeMillis()
            launch {
                if (now >= debounceEndTime) {
                    debounceEndTime = now + debounce
                    runAllHandlers(run)
                }
            }
        }

        fun fireAndForgetJavaHelper(run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit) {
            val now = System.currentTimeMillis()
            if (now < debounceEndTime) return
            debounceEndTime = now + debounce
            SwingUtilities.invokeLater {
                eventMapping[this@EventObject]?.forEach { (_, handler) -> runBlocking { run(handler) } }
            }
        }
    }

    /**
     * No argument events, e.g. neuronChanged.fire() and neuronChanged.on { .. do stuff...}.
     */
    inner class NoArgEvent(override val debounce: Int = 0) : EventObject() {

        /**
         * Kotlin "on"
         */
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: suspend () -> Unit) = onSuspendHelper(dispatcher) {
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
        suspend fun fireAndForget() = fireAndForgetHelper { handler -> handler(null, null) }

        /**
         * Like java fireAndBlock() but suspends rather than blocking, so that the GUI remains responsive.
         */
        suspend fun fireAndSuspend() = fireAndSuspendHelper { handler -> handler(null, null) }

        /**
         * Java fire and block. Fire event and wait for it to terminate before continuing.
         */
        fun fireAndBlock() {
            runBlocking {
                fireAndSuspend()
            }
        }

        /**
         * Java fire and forget.
         */
        fun fireAndForgetJava() = fireAndForgetJavaHelper { handler -> handler(null, null) }

    }

    /**
     * Add events, e.g. neuronAdded.fire(newNeuron), neuronAdded.on{ newNeuron -> ...}.
     * Functinos are the same as in the no-arg case.
     */
    inner class AddedEvent<T>(override val debounce: Int = 0) : EventObject() {

        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: suspend (new: T) -> Unit) = onSuspendHelper(dispatcher) {
                new, _ -> handler(new as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: Consumer<T>) = onHelper(dispatcher) {
                new, _ -> handler.accept(new as T)
        }

        suspend fun fireAndForget(new: T) = fireAndForgetHelper { handler -> handler(new, null) }

        suspend fun fireAndSuspend(new: T) = fireAndSuspendHelper { handler -> handler(new, null) }

        fun fireAndBlock(new: T) {
            runBlocking {
                fireAndSuspend(new)
            }
        }

        fun fireAndForgetJava(new: T) = fireAndForgetJavaHelper { handler -> handler(new, null) }

    }

    /**
     * Removed events, e.g. neuronRemoved.fire(oldNeuron), neuronRemoved.on{ oldNeuron -> ...}. If no handling needed
     * just use no-arg.
     * Functions are the same as in the no-arg case.
     */
    inner class RemovedEvent<T>(override val debounce: Int = 0) : EventObject() {

        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: (old: T) -> Unit) = onSuspendHelper(dispatcher) {
                _, old -> handler(old as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: Consumer<T>) = onHelper(dispatcher) {
                _, old -> handler.accept(old as T)
        }

        suspend fun fireAndForget(old: T) = fireAndForgetHelper { handler -> handler(null, old) }

        suspend fun fireAndSuspend(old: T) = fireAndSuspendHelper { handler -> handler(null, old) }

        fun fireAndBlock(old: T) {
            runBlocking {
                fireAndSuspend(old)
            }
        }

        fun fireAndForgetJava(old: T) = fireAndForgetJavaHelper { handler -> handler(null, old) }

    }

    /**
     * Changed events, e.g. updateRuleChanged.fire(oldRule, newRule), updateRuleChanged.on{ or, nr -> ...}.
     * Functions are the same as in the no-arg case.
     */
    inner class ChangedEvent<T>(override val debounce: Int = 0) : EventObject() {

        @Suppress("UNCHECKED_CAST")

        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: (new: T, old: T) -> Unit) = onSuspendHelper(dispatcher) {
                new, old -> handler(new as T, old as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: BiConsumer<T, T>) = onHelper(dispatcher) {
                new, old -> handler.accept(new as T, old as T)
        }

        suspend fun fireAndForget(new: T, old: T) = fireAndForgetHelper { handler -> if (new != old) handler(new, old) }

        suspend fun fireAndSuspend(new: T, old: T) = fireAndSuspendHelper { handler -> if (new != old) handler(new, old) }

        fun fireAndBlock(new: T, old: T) {
            runBlocking {
                fireAndSuspend(new, old)
            }
        }

        fun fireAndForgetJava(new: T, old: T) = fireAndForgetJavaHelper { handler -> if (new != old) handler(new, old) }

    }

}
