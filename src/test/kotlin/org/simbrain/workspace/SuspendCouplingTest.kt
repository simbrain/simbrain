/**
 * Tests for suspend-native attribute invocation: suspend functions as producibles and consumables, the
 * value-type recovery that lets them match plain attributes, and the cache-time validation that rejects
 * attribute methods the reflection layer cannot invoke.
 */
package org.simbrain.workspace

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SuspendAttributeContainer : AttributeContainer {
    override val id = "Suspend container"

    var received = 0.0
    var receivedText = ""
    var consumerThreadName = ""

    @Producible
    suspend fun produceDouble(): Double {
        yield()
        return 2.5
    }

    @Producible
    suspend fun produceText(): String {
        yield()
        return "hello"
    }

    @Producible
    suspend fun produceOffThread(): Double = withContext(Dispatchers.Default) { 7.25 }

    @Consumable
    suspend fun consumeDouble(value: Double) {
        yield()
        received += value
    }

    @Consumable
    suspend fun consumeOffThread(value: Double) = withContext(Dispatchers.Default) {
        consumerThreadName = Thread.currentThread().name
        received += value
    }
}

class PlainAttributeContainer : AttributeContainer {
    override val id = "Plain container"

    var received = 0.0
    var receivedText = ""

    @Producible
    fun produceDouble() = 1.5

    @Consumable
    fun consumeDouble(value: Double) {
        received += value
    }

    @Consumable
    fun consumeText(value: String) {
        receivedText = value
    }
}

@JvmInline
value class Score(val value: Double)

class InvalidAttributeContainer : AttributeContainer {
    override val id = "Invalid container"

    @Consumable
    fun consumeScore(score: Score) {
    }

    @Consumable
    fun consumeWithDefault(value: Double = 0.0) {
    }
}

class SuspendCouplingTest {

    private val workspace = Workspace()

    private val couplingManager
        get() = workspace.couplingManager

    @Test
    fun `suspend producible delivers to plain consumable`() {
        val producing = SuspendAttributeContainer()
        val consuming = PlainAttributeContainer()
        with(couplingManager) {
            producing.getProducer("produceDouble") couple consuming.getConsumer("consumeDouble")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(2.5, consuming.received)
    }

    @Test
    fun `plain producible delivers to suspend consumable`() {
        val producing = PlainAttributeContainer()
        val consuming = SuspendAttributeContainer()
        with(couplingManager) {
            producing.getProducer("produceDouble") couple consuming.getConsumer("consumeDouble")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(1.5, consuming.received)
    }

    @Test
    fun `suspend producible delivers to suspend consumable`() {
        val container = SuspendAttributeContainer()
        with(couplingManager) {
            container.getProducer("produceDouble") couple container.getConsumer("consumeDouble")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(2.5, container.received)
    }

    @Test
    fun `attributes that dispatch to other contexts still deliver`() {
        val container = SuspendAttributeContainer()
        with(couplingManager) {
            container.getProducer("produceOffThread") couple container.getConsumer("consumeOffThread")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals(7.25, container.received)
        assertTrue(container.consumerThreadName.isNotEmpty())
    }

    @Test
    fun `suspend string producible matches plain string consumable`() {
        val producing = SuspendAttributeContainer()
        val consuming = PlainAttributeContainer()
        with(couplingManager) {
            producing.getProducer("produceText") couple consuming.getConsumer("consumeText")
        }
        runBlocking { couplingManager.updateCouplings() }
        assertEquals("hello", consuming.receivedText)
    }

    @Test
    fun `suspend double producer reports the primitive type plain getters declare`() {
        val suspending = SuspendAttributeContainer()
        val plain = PlainAttributeContainer()
        with(couplingManager) {
            val suspendType = suspending.getProducer("produceDouble").type
            val plainType = plain.getProducer("produceDouble").type
            assertEquals(plainType, suspendType)
            assertEquals(Double::class.javaPrimitiveType, suspendType)
        }
    }

    @Test
    fun `repeated updates accumulate through a suspend consumable`() {
        val producing = PlainAttributeContainer()
        val consuming = SuspendAttributeContainer()
        with(couplingManager) {
            producing.getProducer("produceDouble") couple consuming.getConsumer("consumeDouble")
        }
        runBlocking {
            repeat(10) { couplingManager.updateCouplings() }
        }
        assertEquals(15.0, consuming.received)
    }

    @Test
    fun `value class attribute methods are rejected with a clear error`() {
        val container = InvalidAttributeContainer()
        val mangled = container.javaClass.methods.first { it.name.startsWith("consumeScore") }
        val exception = assertThrows<IllegalArgumentException> {
            with(couplingManager) { container.getConsumer(mangled) }
        }
        assertTrue("alue class" in exception.message!!) { "Unexpected message: ${exception.message}" }
    }

    @Test
    fun `default parameter values are rejected with a clear error`() {
        val container = InvalidAttributeContainer()
        val exception = assertThrows<IllegalArgumentException> {
            with(couplingManager) { container.getConsumer("consumeWithDefault") }
        }
        assertTrue("efault parameter" in exception.message!!) { "Unexpected message: ${exception.message}" }
    }
}
