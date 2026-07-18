package org.simbrain.world.textworld

import kotlinx.coroutines.Dispatchers
import org.simbrain.util.ProvidesDisplayTokenizer
import org.simbrain.util.Tokenizer
import org.simbrain.util.getSimbrainXStream
import org.simbrain.util.projection.KDTreeConvertor
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import java.io.InputStream
import java.io.OutputStream

/**
 * ReaderComponent is the container for the readerworld, which adds
 * producers.
 */
class TextWorldComponent : WorkspaceComponent {

    var world: TextWorld
        private set

    /**
     * Creates a new frame of type TextWorld.
     *
     * @param name name of this component
     */
    constructor(name: String) : super(name) {
        world = TextWorld()
        init()
    }

    /**
     * Construct a component from an existing world; used in deserializing.
     *
     * @param name     name of component
     * @param newWorld provided world
     */
    constructor(name: String, newWorld: TextWorld) : super(name) {
        world = newWorld
        init()
    }

    /**
     * Initialize attribute types.
     */
    private fun init() {
        world.events.atEnd.on(Dispatchers.Default) {
            if (world.stopAtEnd) {
                workspace.stop()
            }
        }
    }

    /**
     * When a document coupling into this world is created from a producer that knows its own
     * tokenization, adopt that tokenizer for display, so token boxes match the source's truth.
     */
    override fun onWorkspaceAttached() {
        workspace.couplingManager.events.couplingAdded.on(Dispatchers.Default) { coupling ->
            val provider = coupling.producer.baseObject
            if (coupling.consumer.baseObject === world &&
                coupling.consumer.method.name in textDocumentConsumers &&
                provider is ProvidesDisplayTokenizer
            ) {
                world.displayTokenizer = provider.displayTokenizer.copy() as Tokenizer<*>
            }
        }
    }

    override fun save(output: OutputStream, format: String?) {
        getTextWorldXStream().toXML(world, output)
    }

    override suspend fun update() {
        world.update()
    }

    override val attributeContainers: List<AttributeContainer>
        get() = listOf<AttributeContainer>(world)

    companion object {

        private val textDocumentConsumers = setOf("setTextIfChanged", "addTextAtEnd", "addTextAtCursor")

        fun open(input: InputStream, name: String, format: String?): TextWorldComponent {
            val newWorld = getTextWorldXStream().fromXML(input) as TextWorld
            return TextWorldComponent(name, newWorld)
        }
    }
}

fun getTextWorldXStream() = getSimbrainXStream().apply {
    registerConverter(KDTreeConvertor())
}