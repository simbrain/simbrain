package org.simbrain.util

import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.function.BiConsumer
import java.util.function.Consumer

val useEventDebug = false

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

    override val coroutineContext = Dispatchers.Default + job

    /**
     * Associates events to their listeners
     */
    private val eventMapping = HashMap<EventObject, ConcurrentLinkedQueue<EventObjectHandler>>()

    enum class TimingMode {
        Throttle, Debounce
    }

    abstract inner class EventObject {

        abstract val interval: Int

        private var intervalEndTime = System.currentTimeMillis()

        abstract var timingMode: TimingMode

        private val batchNew = ConcurrentLinkedQueue<Any?>()
        private val batchOld = ConcurrentLinkedQueue<Any?>()

        private var job: Job? = null

        protected fun onSuspendHelper(dispatcher: CoroutineDispatcher?, wait: Boolean, run: suspend (new: Any?, old: Any?) -> Unit) {
            val eventObjectHandler = EventObjectHandler(dispatcher, wait, run)
            eventMapping.getOrPut(this@EventObject) { ConcurrentLinkedQueue() }.add(eventObjectHandler)
        }

        protected fun onHelper(dispatcher: CoroutineDispatcher?, wait: Boolean, run: (new: Any?, old: Any?) -> Unit) {
            val eventObjectHandler = EventObjectHandler(dispatcher, wait, run)
            eventMapping.getOrPut(this@EventObject) { ConcurrentLinkedQueue() }.add(eventObjectHandler)
        }

        private suspend inline fun runAllHandlers(crossinline run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit) = eventMapping[this@EventObject]
            ?.map { (dispatcher, wait, handler, stackTrace) ->
                try {
                    if (dispatcher != null) {
                        launch(dispatcher) { run(handler) }.let { if (wait) withTimeout(5000) { it.join() } else it }
                    } else {
                        launch { run(handler) }.let { if (wait) withTimeout(5000) { it.join() } else it }
                    }
                } catch (e: TimeoutCancellationException) {
                    throw IllegalStateException("Event time out on dispatcher $dispatcher. Event handler created by ${stackTrace.contentDeepToString()}")
                }
            }?.filterIsInstance<Job>()

        protected suspend fun fireAndSuspendHelper(run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit) {
            val now = System.currentTimeMillis()
            if (interval == 0) {
                runAllHandlers(run)
                return
            }
            when (timingMode) {
                TimingMode.Throttle -> {
                    if (now >= intervalEndTime) {
                        intervalEndTime = now + interval
                        runAllHandlers(run)
                    }
                }
                TimingMode.Debounce -> {
                    job?.cancel()
                    job = launch {
                        delay(interval.toLong())
                        runAllHandlers(run)
                    }
                }
            }
        }

        protected fun batchFireAndSuspendHelper(new: Any?, old: Any?): Job {
            val now = System.currentTimeMillis()
            new?.let { batchNew.add(it) }
            old?.let { batchOld.add(it) }
            if (interval == 0) {
                return launch {
                    runAllHandlers { handler -> handler(batchNew, batchOld) }?.joinAll()
                    batchNew.clear()
                    batchOld.clear()
                }
            }
            return when (timingMode) {
                TimingMode.Throttle -> launch {
                    if (now >= intervalEndTime) {
                        intervalEndTime = now + interval
                        runAllHandlers { handler -> handler(batchNew, batchOld) }?.joinAll()
                        batchNew.clear()
                        batchOld.clear()
                    }
                }
                TimingMode.Debounce -> launch {
                    job?.cancel()
                    job = launch {
                        delay(interval.toLong())
                        runAllHandlers { handler -> handler(batchNew, batchOld) }?.joinAll()
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
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: suspend () -> Unit) = onSuspendHelper(dispatcher, wait) {
                _, _ -> handler()
        }

        /**
         * Java "on"
         */
        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: java.lang.Runnable) = onHelper(dispatcher, wait) {
            _, _ -> handler.run()
        }

        /**
         * Kotlin "fire". By itself it's like "fireAndForget".
         */
        @Deprecated(message = "Blocking is now determinate by `on`", replaceWith = ReplaceWith("fireAndBlock()"))
        fun fireAndForget() = fireAndBlock()

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
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: suspend (new: T) -> Unit) = onSuspendHelper(dispatcher, wait) {
                new, _ -> handler(new as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: Consumer<T>) = onHelper(dispatcher, wait) {
                new, _ -> handler.accept(new as T)
        }

        @Deprecated(message = "Blocking is now determinate by `on`", replaceWith = ReplaceWith("fireAndBlock(new)"))
        fun fireAndForget(new: T) = fireAndBlock(new)

        suspend fun fireAndSuspend(new: T) = fireAndSuspendHelper { handler -> handler(new, null) }

        fun fireAndBlock(new: T) {
            runBlocking {
                fireAndSuspend(new)
            }
        }

    }

    inner class BatchAddedEvent<T>(override val interval: Int, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: suspend (new: Collection<T>) -> Unit) = onSuspendHelper(dispatcher, wait) {
                new, _ -> handler(new as Collection<T>)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: Consumer<Collection<T>>) = onHelper(dispatcher, wait) {
                new, _ -> handler.accept(new as Collection<T>)
        }

        @Deprecated(message = "Blocking is now determinate by `on`", replaceWith = ReplaceWith("fireAndBlock(new)"))
        fun fireAndForget(new: T) = fireAndBlock(new)

        fun fireAndSuspend(new: T) = batchFireAndSuspendHelper(new, null)

        fun fireAndBlock(new: T) {
            runBlocking {
                fireAndSuspend(new).join()
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
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: (old: T) -> Unit) = onSuspendHelper(dispatcher, wait) {
                _, old -> handler(old as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: Consumer<T>) = onHelper(dispatcher, wait) {
                _, old -> handler.accept(old as T)
        }

        @Deprecated(message = "Blocking is now determinate by `on`", replaceWith = ReplaceWith("fireAndBlock(old)"))
        fun fireAndForget(old: T) = fireAndBlock(old)

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

        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: (new: T, old: T) -> Unit) = onSuspendHelper(dispatcher, wait) {
                new, old -> handler(new as T, old as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: BiConsumer<T, T>) = onHelper(dispatcher, wait) {
                new, old -> handler.accept(new as T, old as T)
        }

        @Deprecated(message = "Blocking is now determinate by `on`", replaceWith = ReplaceWith("fireAndBlock(new, old)"))
        fun fireAndForget(new: T, old: T) = fireAndBlock(new, old)

        suspend fun fireAndSuspend(new: T, old: T) = fireAndSuspendHelper { handler -> if (new != old) handler(new, old) }

        fun fireAndBlock(new: T, old: T) {
            runBlocking {
                fireAndSuspend(new, old)
            }
        }

    }

    inner class BatchChangedEvent<T>(override val interval: Int = 0, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: suspend (new: List<T>, old: List<T>) -> Unit) = onSuspendHelper(dispatcher, wait) {
                new, old -> handler(new as List<T>, old as List<T>)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: BiConsumer<List<T>, List<T>>) = onHelper(dispatcher, wait) {
                new, old -> handler.accept(new as List<T>, old as List<T>)
        }

        @Deprecated(message = "Blocking is now determinate by `on`", replaceWith = ReplaceWith("fireAndBlock(new, old)"))
        fun fireAndForget(new: T, old: T) = fireAndBlock(new, old)

        suspend fun fireAndSuspend(new: T, old: T) = batchFireAndSuspendHelper(new, old)

        fun fireAndBlock(new: T, old: T) {
            runBlocking {
                fireAndSuspend(new, old)
            }
        }
    }

}

data class EventObjectHandler(
    val dispatcher: CoroutineDispatcher?,
    val wait: Boolean,
    val handler: suspend (new: Any?, old: Any?) -> Unit,
    val stackTraceElements: Array<StackTraceElement>? = if (useEventDebug) Thread.getAllStackTraces()[Thread.currentThread()] else null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as EventObjectHandler

        if (dispatcher != other.dispatcher) return false
        if (wait != other.wait) return false
        if (handler != other.handler) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dispatcher?.hashCode() ?: 0
        result = 31 * result + wait.hashCode()
        result = 31 * result + handler.hashCode()
        return result
    }
}
