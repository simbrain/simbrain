package org.simbrain.util.piccolo

import com.Ostermiller.util.CSVParser
import com.thoughtworks.xstream.annotations.XStreamAlias
import com.thoughtworks.xstream.converters.MarshallingContext
import com.thoughtworks.xstream.converters.UnmarshallingContext
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import com.thoughtworks.xstream.converters.reflection.ReflectionProvider
import com.thoughtworks.xstream.io.HierarchicalStreamReader
import com.thoughtworks.xstream.io.HierarchicalStreamWriter
import com.thoughtworks.xstream.mapper.Mapper
import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Network
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

/**
 * Tiled `<data>` tag object representation.
 *
 * Cannot handle xml encoded data for now.
 */
@XStreamAlias("data")
class TiledData(val gid: MutableList<MutableList<Int>>) {
    constructor(width: Int, height: Int) : this(MutableList(height) { MutableList(width) { 0 } })
    operator fun get(x: Int, y: Int) = gid[y][x]
    operator fun set(x: Int, y: Int, tileId: Int) {
        gid[y][x] = tileId
    }
}

/**
 * Custom serializer that stores [Network.networkModels], which is a map, as a flat list of [NetworkModel]s.
 */
class TiledDataConverter(mapper: Mapper, reflectionProvider: ReflectionProvider) :
    ReflectionConverter(mapper, reflectionProvider, TiledData::class.java) {

    override fun marshal(source: Any?, writer: HierarchicalStreamWriter, context: MarshallingContext) {
        val data = source as TiledData
        writer.addAttribute("encoding", "csv")
        val csv = data.gid.joinToString("\n") { it.joinToString(",") }
        writer.setValue(csv)
    }

    /**
     * Decode the raw [data] into a list of tile ids.
     *
     * @return the list of tile id this data represents.
     */
    private fun decodeData(value: String, encoding: String, compression: String?): List<List<Int>> {

        fun decodeCSV() =
            CSVParser(value.byteInputStream()).allValues
                .map { row ->
                    row.filter { it.isNotEmpty() }
                        .map { it.toInt() }
                }

        fun decodeBase64() = value.split("[ \n]".toRegex())
            .map { row -> row.let { Base64.getDecoder().decode(it)!! } }


        fun ByteArray.decompressGzip() = GZIPInputStream(ByteArrayInputStream(this))
            .use { it.readAllBytes()!! }

        fun ByteArray.decompressZlib() = InflaterInputStream(ByteArrayInputStream(this)).readAllBytes()!!

        fun ByteBuffer.asIntSequence() = sequence {
            while(hasRemaining()) {
                yield(int)
            }
        }

        return when (encoding) {
            "csv" -> decodeCSV()
            "base64" -> when(compression) {
                "gzip" -> decodeBase64().map { it.decompressGzip() }
                "zlib" -> decodeBase64().map { it.decompressZlib() }
                else -> decodeBase64()
            }.map { ByteBuffer.wrap(it).apply { order(ByteOrder.LITTLE_ENDIAN) }.asIntSequence().toList() }
            else -> throw IllegalStateException("Unknown encoding $encoding")
        }

    }

    override fun unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext): Any {
        val encoding = reader.getAttribute("encoding")
        val compression = reader.getAttribute("compression")
        val gid = decodeData(reader.value, encoding, compression)
            .map { it.toMutableList() }
            .toMutableList()
        return TiledData(gid)
    }
}