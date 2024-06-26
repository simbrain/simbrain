@file:JvmName("EventKt")

package org.simbrain.util

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.ZonedDateTime
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicLong
import java.util.function.BiConsumer
import java.util.function.Consumer
import kotlin.system.measureNanoTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Event objects are fired with `fireX()` functions and handled with `on()` functions.
 *
 * No-arg, one-arg, and two-arg event objects are provided. The change events are only handled if `before` and `after`
 * actually changed.
 *
 * Events are launched in a coroutine context and return a [Deferred] object. If `await()` is called on this object,
 * execution will wait until all event handlers for which `wait = true` have finished executing. For example
 * ```
 *     event.on(wait = true) {delay(1000); print("event1 ")}
 *     event.on() {delay(2000); print("event2 ")}
 *     event.fire().await()
 *     print("done ")
 * ```
 * This prints `event1 done event2`. If `await()` is not called the result is `done event1 event2`
 *
 * `fireAndBlock()` is an adapter which allows events to be fired outside of suspend functions.
 *
 * When handling a large number of events (for example, updating a thousand synapses, each of which triggers
 * a screen refresh), throttling and debouncing can be enabled. For this set `interval` to a value greater than 0.
 * See [here](https://css-tricks.com/debouncing-throttling-explained-examples/). If (as in the synapse -> screen refresh case), not
 * all events must be handled, regular events can be used. If all events must be handled, BatchEvents can be used.
 * All batched events are handled in arbitrary order between throttle and debounce intervals.
 *
 * Events can be logged by seeing [useEventDebug] to true.
 *
 * For a sense of how events work see [EventTesting]
 *
 */
open class Events(val timeout: Duration = 5.seconds): CoroutineScope {

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

        private var debounceCounter: AtomicLong = AtomicLong(0L)
        private val mutex = Mutex()

        private var shouldClearQueue: Boolean = false

        /**
         * Helper function for registering suspending event handlers.
         *
         * @return A function that can be called to unregister the event handler.
         */
        protected fun onSuspendHelper(dispatcher: CoroutineDispatcher?, wait: Boolean, run: suspend (new: Any?, old: Any?) -> Unit): () -> Boolean? {
            val eventObjectHandler = EventObjectHandler(dispatcher, wait, run)
            eventMapping.getOrPut(this@EventObject) { ConcurrentLinkedQueue() }.add(eventObjectHandler)
            return {
                eventMapping[this@EventObject]?.remove(eventObjectHandler)
            }
        }

        /**
         * Helper function for registering non-suspending event handlers.
         *
         * @return A function that can be called to unregister the event handler.
         */
        protected fun onHelper(dispatcher: CoroutineDispatcher?, wait: Boolean, run: (new: Any?, old: Any?) -> Unit): () -> Boolean? {
            val eventObjectHandler = EventObjectHandler(dispatcher, wait, run)
            eventMapping.getOrPut(this@EventObject) { ConcurrentLinkedQueue() }.add(eventObjectHandler)
            return {
                eventMapping[this@EventObject]?.remove(eventObjectHandler)
            }

        }

        /**
         * The main event handling code is here. All other fire functions should route through this one.
         */
        private suspend fun runAllHandlers(run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit) = eventMapping[this@EventObject]
            ?.map { (dispatcher, wait, handler, stackTrace) ->
                try {
                    suspend fun runAll() = if (dispatcher != null) {
                        launch(dispatcher) { run(handler) }.let { if (wait) withTimeout(timeout) { it.join() } else it }
                    } else {
                        launch { run(handler) }.let { if (wait) withTimeout(timeout) { it.join() } else it }
                    }
                    if (!useEventDebug) {
                        runAll()
                    } else {
                        var result: Any? = null
                        val nanoTime = measureNanoTime {
                            result = runAll()
                        }
                        debugChannel.send(DebugInfo(
                            System.currentTimeMillis(),
                            nanoTime.nanoseconds,
                            stackTrace?.dropWhile { it.className.contains("java.lang.Thread") }?.dropWhile { it.className.contains("util.Event") }?.firstOrNull().toString(),
                            dispatcher,
                            wait
                        ))
                        result
                    }
                } catch (e: TimeoutCancellationException) {
                    throw IllegalStateException("Event time out on dispatcher $dispatcher. Event handler created by ${stackTrace.contentDeepToString()}")
                }
            }?.filterIsInstance<Job>()

        protected fun fireAllHelper(run: suspend (suspend (new: Any?, old: Any?) -> Unit) -> Unit): Deferred<Boolean> {
            if (eventMapping[this@EventObject].isNullOrEmpty()) return CompletableDeferred(true)
            val now = System.currentTimeMillis()
            if (interval == 0) {
                return async {
                    runAllHandlers(run)?.joinAll()
                    true
                }
            }
            return when (timingMode) {
                TimingMode.Throttle -> async {
                    mutex.withLock {
                        if (now >= intervalEndTime) {
                            intervalEndTime = now + interval
                            runAllHandlers(run)
                            true
                        } else {
                            false
                        }
                    }
                }
                TimingMode.Debounce -> async {
                    val seq = debounceCounter.incrementAndGet()
                    delay(interval.toLong())
                    mutex.withLock {
                        val seqNow = debounceCounter.get()
                        if (seq == seqNow) {
                            runAllHandlers(run)
                            true
                        } else {
                            false
                        }
                    }
                }
            }
        }

        protected fun batchFireAllHelper(new: Any?, old: Any?): Deferred<Boolean> = async {
            val now = System.currentTimeMillis()
            mutex.withLock {
                if (shouldClearQueue) {
                    batchNew.clear()
                    batchOld.clear()
                    shouldClearQueue = false
                }
            }
            new?.let { batchNew.add(it) }
            old?.let { batchOld.add(it) }
            if (interval == 0) {
                mutex.withLock {
                    runAllHandlers { handler -> handler(batchNew, batchOld) }?.joinAll()
                    shouldClearQueue = true
                    true
                }
            } else {
                when (timingMode) {
                    TimingMode.Throttle -> {
                        mutex.withLock {
                            if (now >= intervalEndTime) {
                                intervalEndTime = now + interval
                                runAllHandlers { handler -> handler(batchNew, batchOld) }?.joinAll()
                                shouldClearQueue = true
                                true
                            } else {
                                false
                            }
                        }
                    }
                    TimingMode.Debounce -> {
                        val seq = debounceCounter.incrementAndGet()
                        delay(interval.toLong())
                        mutex.withLock {
                            val seqNow = debounceCounter.get()
                            if (seq == seqNow) {
                                runAllHandlers { handler -> handler(batchNew, batchOld) }?.joinAll()
                                shouldClearQueue = true
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
            }

        }

        suspend inline fun <T> printTiming(block: suspend () -> T): T {
            if (useEventDebug) {
                var result: T? = null
                val timing = measureNanoTime {
                    result = block()
                }
                println("Event ${this@Events::class.simpleName} from ${Thread.getAllStackTraces()[Thread.currentThread()]?.dropWhile { it.className.contains("java.lang.Thread") }?.dropWhile { it.className.contains("util.Event") }?.firstOrNull().toString()} took ${timing.nanoseconds} to complete.")
                return result!!
            } else {
                return block()
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
         * Like java fireAndBlock() but suspends rather than blocking, so that the GUI remains responsive.
         */
        fun fire() = fireAllHelper { handler -> handler(null, null) }

        /**
         * Java fire and block. Fire event and wait for it to terminate before continuing.
         */
        fun fireAndBlock() = runBlocking {
            printTiming { fire().await() }
        }

    }

    /**
     * Events that take one argument, e.g. neuronAdded.fire(newNeuron), neuronAdded.on{ newNeuron -> ...}.
     */
    inner class OneArgEvent<T>(override val interval: Int = 0, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: suspend (new: T) -> Unit) = onSuspendHelper(dispatcher, wait) {
                new, _ -> handler(new as T)
        }

        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: Consumer<T>) = onHelper(dispatcher, wait) {
                new, _ -> handler.accept(new as T)
        }

        fun fire(new: T) = fireAllHelper { handler -> handler(new, null) }

        fun fireAndBlock(new: T) = runBlocking {
            printTiming { fire(new).await() }
        }

    }

    inner class BatchOneArgEvent<T>(override val interval: Int, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        /**
         * Note: batch events are handled in arbitrary order
         */
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: suspend (new: ConcurrentLinkedQueue<T>) -> Unit) = onSuspendHelper(dispatcher, wait) {
                new, _ -> handler(new as ConcurrentLinkedQueue<T>)
        }

        /**
         * Note: batch events are handled in arbitrary order
         */
        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: Consumer<ConcurrentLinkedQueue<T>>) = onHelper(dispatcher, wait) {
                new, _ -> handler.accept(new as ConcurrentLinkedQueue<T>)
        }

        fun fire(new: T) = batchFireAllHelper(new, null)

        fun fireAndBlock(new: T) = runBlocking {
            printTiming { fire(new).await() }
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

        fun fire(new: T, old: T) = fireAllHelper { handler -> if (new != old) handler(new, old) }

        fun fireAndBlock(new: T, old: T) = runBlocking {
            printTiming { fire(new, old).await() }
        }

    }

    inner class BatchChangedEvent<T>(override val interval: Int = 0, override var timingMode: TimingMode =  TimingMode.Debounce) : EventObject() {

        /**
         * Note: batch events are handled in arbitrary order
         */
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: suspend (new: ConcurrentLinkedQueue<T>, old: ConcurrentLinkedQueue<T>) -> Unit) = onSuspendHelper(dispatcher, wait) {
                new, old -> handler(new as ConcurrentLinkedQueue<T>, old as ConcurrentLinkedQueue<T>)
        }

        /**
         * Note: batch events are handled in arbitrary order
         */
        @JvmOverloads
        @Suppress("UNCHECKED_CAST")
        fun on(dispatcher: CoroutineDispatcher? = null, wait: Boolean = false, handler: BiConsumer<ConcurrentLinkedQueue<T>, ConcurrentLinkedQueue<T>>) = onHelper(dispatcher, wait) {
                new, old -> handler.accept(new as ConcurrentLinkedQueue<T>, old as ConcurrentLinkedQueue<T>)
        }

        fun fire(new: T, old: T) = batchFireAllHelper(new, old)

        fun fireAndBlock(new: T, old: T) = runBlocking {
            printTiming { fire(new, old).await() }
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

data class DebugInfo(val timeStamp: Long, val duration: Duration, val initiator: String?, val dispatcher: CoroutineDispatcher?, val wait: Boolean) {

    val timeStampString: String get() {
        val time = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timeStamp), TimeZone.getDefault().toZoneId())
        val formatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
        return time.format(formatter)
    }

    override fun toString(): String {
        return "DebugInfo(timeStamp=${timeStampString}, duration=$duration, initiator='$initiator', dispatcher=$dispatcher, wait=$wait)"
    }
}

/**
 * Use when [useEventDebug] is true to collect debug information about events.
 */
val debugChannel by lazy { Channel<DebugInfo>(Channel.UNLIMITED) }

/**
 * If set to true stack traces are printed out on event timeouts.
 */
val useEventDebug = false.also {
    if (it) {
        println("Event Debug Mode is on. It could have performance impacts, especially in evolution.")
        GlobalScope.launch {
            debugChannel.consumeEach { message ->
                println(message)
            }
        }
    }
}

// class BadThing {
//     var thing = false
//         set(value) {
//             if (field == value) println("bad thing happened")
//             field = value
//         }
// }
//
// suspend fun main() {
//     val seqSupplier = AtomicLong(0)
//     val badThing = BadThing()
//
//     val mutex = Mutex()
//
//     suspend fun doStuff(): Boolean {
//         val seq = seqSupplier.incrementAndGet()
//         delay(10L)
//         val seqNow = seqSupplier.get()
//         return mutex.withLock {
//             if (seq == seqNow) {
//                 val currentBadThing = badThing.thing
//                 badThing.thing = !currentBadThing
//                 true
//             } else {
//                 false
//             }
//         }
//     }
//
//     var intervalEndTime = System.currentTimeMillis()
//
//     suspend fun doThrottleStuff(): Boolean {
//         val now = System.currentTimeMillis()
//         return mutex.withLock {
//             if (now >= intervalEndTime) {
//                 intervalEndTime = now + 10L
//                 val currentBadThing = badThing.thing
//                 badThing.thing = !currentBadThing
//                 true
//             } else {
//                 false
//             }
//         }
//     }
//
//     val count = 1000000
//
//     val timing = measureNanoTime {
//         (0 until count).map {
//             GlobalScope.async {
//                 doThrottleStuff()
//             }
//         }.awaitAll().count { it }.let { println(it) }
//     }
//
//     println(timing.nanoseconds / count)
// }