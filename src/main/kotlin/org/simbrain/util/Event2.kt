package org.simbrain.util

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer

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
    private val eventMapping = HashMap<EventObject, LinkedList<Pair<CoroutineDispatcher?, suspend (new: Any?, old: Any?) -> Unit>>>()

    enum class TimingMode {
        Throttle, Debounce
    }

    abstract inner class EventObject {

        abstract val interval: Int

        private var intervalEndTime = System.currentTimeMillis()

        abstract var timingMode: TimingMode

        private val batchNew = mutableListOf<Any?>()
        private val batchOld = mutableListOf<Any?>()

        private var job: Job? = null

        protected fun onSuspendHelper(dispatcher: CoroutineDispatcher?, run: suspend (new: Any?, old: Any?) -> Unit) {
            eventMapping.getOrPut(this@EventObject) { LinkedList() }.add(dispatcher to run)
        }

        protected fun onHelper(dispatcher: CoroutineDispatcher?, run: (new: Any?, old: Any?) -> Unit) {
            eventMapping.getOrPut(this@EventObject) { LinkedList() }.add(dispatcher to { new, old -> async { run(new, old) } })
        }

        private suspend inline fun runAllHandlers(crossinline run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit) = eventMapping[this@EventObject]
            ?.groupBy { (dispatcher) -> dispatcher }
            ?.flatMap { (dispatcher, group) ->
                if (dispatcher != null) {
                    withContext(dispatcher) {
                        group.map { (_, handler) -> async { run(handler) } }
                    }
                } else {
                    group.map { (_, handler) -> async { run(handler) } }
                }
            }
            ?.awaitAll()

        protected suspend fun fireAndSuspendHelper(run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit) = fireAndForgetHelper(run).join()

        protected suspend fun batchFireAndSuspendHelper(new: Any?, old: Any?) = batchedFireAndForgetHelper(new, old).join()

        protected fun fireAndForgetHelper(run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit): Job {
            val now = System.currentTimeMillis()
            if (interval == 0) {
                return launch { runAllHandlers(run) }
            }
            return when (timingMode) {
                TimingMode.Throttle -> launch {
                    if (now >= intervalEndTime) {
                        intervalEndTime = now + interval
                        runAllHandlers(run)
                    }
                }
                TimingMode.Debounce -> launch {
                    job?.cancel()
                    job = launch {
                        delay(interval.toLong())
                        runAllHandlers(run)
                    }
                }
            }
        }

        protected fun batchedFireAndForgetHelper(new: Any?, old: Any?): Job {
            if (interval == 0) {
                return launch {
                    runAllHandlers { handler -> handler(batchNew, batchOld) }
                    batchNew.clear()
                    batchOld.clear()
                }
            }
            val now = System.currentTimeMillis()
            batchNew.add(new)
            batchOld.add(old)
            return when (timingMode) {
                TimingMode.Throttle -> launch {
                    if (now >= intervalEndTime) {
                        intervalEndTime = now + interval
                        runAllHandlers { handler -> handler(batchNew, batchOld) }
                        batchNew.clear()
                        batchOld.clear()
                    }
                }
                TimingMode.Debounce -> launch {
                    job?.cancel()
                    job = launch {
                        delay(interval.toLong())
                        runAllHandlers { handler -> handler(batchNew, batchOld) }
                        batchNew.clear()
                        batchOld.clear()
                    }
                    job?.join()
                }
            }
        }
    }

    /**
     * No argument events, e.g. neuronChanged.fire() and neuronChanged.on { .. do stuff...}.
     */
    inner class NoArgEvent(override val interval: Int = 0, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        /**
         * Kotlin "on"
         */
        fun on(dispatcher: CoroutineDispatcher? = null, handler: suspend () -> Unit) = onSuspendHelper(dispatcher) {
                _, _ -> handler()
        }

        /**
         * Java "on"
         */
        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher? = null, handler: java.lang.Runnable) = onHelper(dispatcher) {
            _, _ -> handler.run()
        }

        /**
         * Kotlin "fire". By itself it's like "fireAndForget".
         */
        fun fireAndForget() = fireAndForgetHelper { handler -> handler(null, null) }

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

    }

    /**
     * Add events, e.g. neuronAdded.fire(newNeuron), neuronAdded.on{ newNeuron -> ...}.
     * Functinos are the same as in the no-arg case.
     */
    inner class AddedEvent<T>(override val interval: Int = 0, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, handler: suspend (new: T) -> Unit) = onSuspendHelper(dispatcher) {
                new, _ -> handler(new as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, handler: Consumer<T>) = onHelper(dispatcher) {
                new, _ -> handler.accept(new as T)
        }

        fun fireAndForget(new: T) = fireAndForgetHelper { handler -> handler(new, null) }

        suspend fun fireAndSuspend(new: T) = fireAndSuspendHelper { handler -> handler(new, null) }

        fun fireAndBlock(new: T) {
            runBlocking {
                fireAndSuspend(new)
            }
        }

    }

    inner class BatchAddedEvent<T>(override val interval: Int = 0, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, handler: suspend (new: List<T>) -> Unit) = onSuspendHelper(dispatcher) {
                new, _ -> handler(new as List<T>)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, handler: Consumer<List<T>>) = onHelper(dispatcher) {
                new, _ -> handler.accept(new as List<T>)
        }

        fun fireAndForget(new: T) = batchedFireAndForgetHelper(new, null)

        suspend fun fireAndSuspend(new: T) = batchFireAndSuspendHelper(new, null)

        fun fireAndBlock(new: T) {
            runBlocking {
                fireAndSuspend(new)
            }
        }
    }

    /**
     * Removed events, e.g. neuronRemoved.fire(oldNeuron), neuronRemoved.on{ oldNeuron -> ...}. If no handling needed
     * just use no-arg.
     * Functions are the same as in the no-arg case.
     */
    inner class RemovedEvent<T>(override val interval: Int = 0, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, handler: (old: T) -> Unit) = onSuspendHelper(dispatcher) {
                _, old -> handler(old as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, handler: Consumer<T>) = onHelper(dispatcher) {
                _, old -> handler.accept(old as T)
        }

        fun fireAndForget(old: T) = fireAndForgetHelper { handler -> handler(null, old) }

        suspend fun fireAndSuspend(old: T) = fireAndSuspendHelper { handler -> handler(null, old) }

        fun fireAndBlock(old: T) {
            runBlocking {
                fireAndSuspend(old)
            }
        }

    }

    /**
     * Changed events, e.g. updateRuleChanged.fire(oldRule, newRule), updateRuleChanged.on{ or, nr -> ...}.
     * Functions are the same as in the no-arg case.
     */
    inner class ChangedEvent<T>(override val interval: Int = 0, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        @Suppress("UNCHECKED_CAST")

        fun on(dispatcher: CoroutineDispatcher? = null, handler: (new: T, old: T) -> Unit) = onSuspendHelper(dispatcher) {
                new, old -> handler(new as T, old as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, handler: BiConsumer<T, T>) = onHelper(dispatcher) {
                new, old -> handler.accept(new as T, old as T)
        }

        suspend fun fireAndForget(new: T, old: T) = fireAndForgetHelper { handler -> if (new != old) handler(new, old) }

        suspend fun fireAndSuspend(new: T, old: T) = fireAndSuspendHelper { handler -> if (new != old) handler(new, old) }

        fun fireAndBlock(new: T, old: T) {
            runBlocking {
                fireAndSuspend(new, old)
            }
        }

    }

    inner class BatchChangedEvent<T>(override val interval: Int = 0, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, handler: suspend (new: List<T>, old: List<T>) -> Unit) = onSuspendHelper(dispatcher) {
                new, old -> handler(new as List<T>, old as List<T>)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, handler: BiConsumer<List<T>, List<T>>) = onHelper(dispatcher) {
                new, old -> handler.accept(new as List<T>, old as List<T>)
        }

        fun fireAndForget(new: T, old: T) = batchedFireAndForgetHelper(new, old)

        suspend fun fireAndSuspend(new: T, old: T) = batchFireAndSuspendHelper(new, old)

        fun fireAndBlock(new: T, old: T) {
            runBlocking {
                fireAndSuspend(new, old)
            }
        }
    }

}
