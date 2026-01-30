package org.simbrain.world.imageworld

import com.thoughtworks.xstream.XStream
import org.simbrain.util.getSimbrainXStream
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.world.imageworld.serialization.BufferedImageConverter
import org.simbrain.world.imageworld.serialization.CouplingArrayConverter
import java.io.InputStream
import java.io.OutputStream

/**
 * The interface between image world and the desktop level.
 * Manages couplings and persistence.
 */
class ImageWorldComponent : WorkspaceComponent {
    /**
     * The image world this component displays.
     */
    val world: ImageWorld

    constructor() : super("") {
        this.world = ImageWorld()
    }

    /**
     * Create named component.
     */
    constructor(name: String) : super(name) {
        this.world = ImageWorld()
    }

    override val attributeContainers: List<AttributeContainer>
        get() = buildList {
            add(world.imageAlbum)
            add(world.imagePipelineCollection)
            addAll(world.imagePipelineCollection.pipelines)
        }

    override val xml: String?
        get() = xStream.toXML(world)

    override fun save(output: OutputStream, format: String?) {
        xStream.toXML(world, output)
    }

    /**
     * Deserialize an ImageAlbumComponent.
     *
     * @param name name of component
     * @param world the deserialized world
     */
    constructor(name: String, world: ImageWorld) : super(name) {
        this.world = world
    }

    companion object {
        /**
         * Open a saved ImageWorldComponent from an XML input stream.
         *
         * @param input  The input stream to read.
         * @param name   The name of the new world component.
         * @param format The format of the input stream. Should be xml.
         * @return A deserialized ImageWorldComponent.
         */
        fun open(input: InputStream?, name: String, format: String?): ImageWorldComponent {
            val world = xStream.fromXML(input) as ImageWorld
            return ImageWorldComponent(name, world)
        }

        val xStream: XStream
            /**
             * Create an xstream from this class.
             */
            get() {
                val stream = getSimbrainXStream()
                stream.registerConverter(BufferedImageConverter())
                stream.registerConverter(CouplingArrayConverter())
                return stream
            }
    }
}