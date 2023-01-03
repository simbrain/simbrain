package org.simbrain.util

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import java.util.*
import java.util.function.BiConsumer
import java.util.function.Consumer
import javax.swing.JButton
import javax.swing.SwingUtilities

open class Events2 {

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

    inner class NoArgEvent: EventObject() {
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: () -> Unit) = onHelper(dispatcher) {
                _, _ -> handler()
        }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: java.lang.Runnable) = onHelper(dispatcher) {
            _, _ -> handler.run()
        }

        suspend fun fire() = fireHelper { handler -> handler(null, null) }

        fun fireAndBlock() {
            runBlocking {
                fire().await()
            }
        }

        fun fireAndForget() = fireAndForgetHelper { handler -> handler(null, null) }

    }

    inner class AddedEvent<T>: EventObject() {
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: (new: T) -> Unit) = onHelper(dispatcher) {
            new, _ -> handler(new as T)
        }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: Consumer<T>) = onHelper(dispatcher) {
                new, _ -> handler.accept(new as T)
        }

        suspend fun fire(new: T) = fireHelper { handler -> handler(new, null) }

        fun fireAndBlock(new: T) {
            runBlocking {
                fire(new).await()
            }
        }

        fun fireAndForget(new: T) = fireAndForgetHelper { handler -> handler(new, null) }

    }

    inner class RemovedEvent<T>: EventObject() {
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: (old: T) -> Unit) = onHelper(dispatcher) {
                _, old -> handler(old as T)
        }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: Consumer<T>) = onHelper(dispatcher) {
                _, old -> handler.accept(old as T)
        }

        suspend fun fire(old: T) = fireHelper { handler -> handler(null, old) }

        fun fireAndBlock(old: T) {
            runBlocking {
                fire(old).await()
            }
        }

        fun fireAndForget(old: T) = fireAndForgetHelper { handler -> handler(null, old) }

    }

    inner class ChangedEvent<T>: EventObject() {
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: (new: T, old: T) -> Unit) = onHelper(dispatcher) {
                new, old -> handler(new as T, old as T)
        }

        @JvmOverloads
        fun on(dispatcher: CoroutineDispatcher = Dispatchers.Swing, handler: BiConsumer<T, T>) = onHelper(dispatcher) {
                new, old -> handler.accept(new as T, old as T)
        }

        suspend fun fire(new: T, old: T) = fireHelper { handler -> handler(new, old) }

        fun fireAndBlock(new: T, old: T) {
            runBlocking {
                fire(new, old).await()
            }
        }

        fun fireAndForget(new: T, old: T) = fireAndForgetHelper { handler -> handler(new, old) }

    }

}

class TestEvents: Events2() {
    val thingChanged = ChangedEvent<String>()
}

class ClassUsingTestEvents {
    val events = TestEvents()

    val button = JButton("")

    fun thing() {
        events.thingChanged.on { old, new ->
            button.text = Date().toString()
        }
        events.thingChanged.fireAndBlock("1", "2")
    }
}