package org.simbrain.plot.pixelplot

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.io.xml.DomDriver
import org.simbrain.workspace.AttributeContainer
import org.simbrain.workspace.WorkspaceComponent
import org.simbrain.world.imageworld.serialization.BufferedImageConverter
import org.simbrain.world.imageworld.serialization.CouplingArrayConverter
import java.io.InputStream
import java.io.OutputStream

/**
 * The interface between pixel display world and the desktop level.
 * Manages couplings and persistence.
 */
class PixelPlotComponent : WorkspaceComponent {
    /**
     * The image world this component displays.
     */
    @JvmField
    val pixelPlot: PixelPlot = PixelPlot()

    /**
     * Create an Image World Component from a Image World.
     */
    constructor(title: String?) : super(title!!)

    /**
     * Deserialize an ImageAlbumComponent.
     */
    constructor(name: String?, matrix: PixelPlot?) : super(name!!)

    override suspend fun update() {
        pixelPlot.clearData()
    }

    override val attributeContainers: List<AttributeContainer>
        get() {
            val containers: MutableList<AttributeContainer> = ArrayList()
            containers.add(pixelPlot)
            return containers
        }

    override fun save(output: OutputStream, format: String?) {
        xStream.toXML(pixelPlot, output)
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
        fun open(input: InputStream?, name: String?, format: String?): PixelPlotComponent {
            val matrix = xStream.fromXML(input) as PixelPlot
            return PixelPlotComponent(name, matrix)
        }

        val xStream: XStream
            /**
             * Create an xstream from this class.
             */
            get() {
                val stream = XStream(DomDriver())
                stream.registerConverter(BufferedImageConverter())
                stream.registerConverter(CouplingArrayConverter())
                return stream
            }
    }
}
